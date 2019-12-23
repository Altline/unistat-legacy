package altline.unistat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.security.auth.login.FailedLoginException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import altline.unistat.Bill.Entry;
import altline.unistat.gui.GuiLogin;
import altline.unistat.gui.component.ProgressMonitor;
import altline.utils.Alerts;
import altline.utils.Crypt;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public final class UserManager {
	private static final UserManager INSTANCE = new UserManager();
	private static final Logger LOGGER = LogManager.getLogger();

	public static final UserManager getInstance() {
		return INSTANCE;
	}

	/* *************************************************************************
	 *                                                                         *
	 * Private fields                                                          *
	 *                                                                         *
	 ************************************************************************* */

	private String encpwd;

	/* *************************************************************************
	 *                                                                         *
	 * Public API                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// thought I should use this instead of reloading the gui on login (avoid memory leaks and stuff...) but it causes
	// weird deadlocks when changing users and I'm sick of trying to figure out why that is
	
	// ! private ObjectBinding<ElementObservableListDecorator<Bill>> billsBinding;
	/*public void listenToBills(InvalidationListener listener) {
		if(billsBinding == null) billsBinding = Bindings.createObjectBinding(() -> getUser() == null ? null : getUser().getElementObservableBills(), userProperty());
		
		billsBinding.addListener((obs, oldVal, newVal) -> {
			if (oldVal != null) oldVal.removeListener(listener);
			if (newVal != null) newVal.addListener(listener);
			
			listener.invalidated(obs);
		});
		
	}*/

	/**
	 * Checks if a user is logged on the <u>application</u>. A user that is logged on the application does not
	 * necessarily need to be logged on the webserver.
	 * @return true if a user is logged on the application, false otherwise
	 */
	public boolean isUserLoggedIn() {
		return getUser() != null;
	}

	/**
	 * @return true if the auto-login feature is enabled, false otherwise
	 */
	public boolean isAutoLogin() {
		return Pref.getAutoLogin();
	}

	/**
	 * Checks whether the currently logged-in user has a bill issued in the same place at the same time as the specified
	 * bill (disregarding the contents of the bills)
	 * @param bill
	 * @return true if the user has a bill with the same source and dateTime as the specified bill
	 */
	public boolean billExists(Bill bill) {
		for (Bill testBill : getUser().getBills()) {
			if (bill.isSameTimePlace(testBill)) return true;
		}
		return false;
	}

	/**
	 * Checks whether the currently logged-in user has a bill that is equal to the specified bill.
	 * @param bill
	 * @return true if the user has a bill equal to the specified bill
	 */
	public boolean equalBillExists(Bill bill) {
		return getUser().getBills().contains(bill);
	}

	/**
	 * Logs the user out of the application and the webserver. If no user is logged in, this method will have no effect.
	 */
	public void logout() {
		if (!isUserLoggedIn()) return;
		LOGGER.info("Logging out...");

		try {
			App.webManager.logout();
		} catch (FailingHttpStatusCodeException | IOException e) {
			Alerts.catching("Problem logging out of webserver", e, LOGGER);
		}

		encpwd = null;
		try {
			App.runFxAndWait(() -> setUser(null));
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}

		synchronized (App.LOGIN_LOCK) {
			App.LOGIN_LOCK.notify();
		}
	}

	/**
	 * Disables the auto-login feature and removes the saved login information.
	 */
	public void disableAutoLogin() {
		LOGGER.info("Disabling automatic login");
		Pref.getPreferences().get(Pref.AL_PWD, "");
		Pref.setSavedUserId("");
		Pref.setAutoLogin(false);
	}

	/**
	 * Returns a {@link Task} that updates the data of the currently logged-in user. This includes fetching data from
	 * the webserver.<br>
	 * The updated data is saved.
	 * <h2>Task description</h2>
	 * <p>
	 * The task acquires and starts a data fetch task from {@link WebManager#fetchData()}. The task will then wait for
	 * the data fetch to finish, then schedule an action on the JavaFX thread that will apply the fetched data to the
	 * user and save the new state to disk. This task catches and rethrows the exceptions thrown in the data fetch task,
	 * as documented below.
	 * </p>
	 * <h3>Task Exceptions</h3> Notable exceptions that the task may encounter include:
	 * <ul>
	 * <li>{@link FailedLoginException} - if a webserver login is necessary and it fails</li>
	 * <li>{@link FailingHttpStatusCodeException} - if the server returns a failing status code</li>
	 * <li>{@link IOException} - if an IO problem occurs</li>
	 * <li>{@link IllegalStateException} - if no user is logged on the application</li>
	 * </ul>
	 * @return The Task that, when started, will update the user's data
	 */
	public Task<Void> updateUser() {
		if (!isUserLoggedIn()) throw new IllegalStateException("No user is logged in");

		Task<Void> updateTask = new Task<Void>() {
			@Override
			protected Void call() throws FailedLoginException, FailingHttpStatusCodeException, IOException {

				LOGGER.info("Updating user data");

				Task<UserData> fetchTask = App.webManager.fetchData();
				fetchTask.setOnCancelled(e -> {
					this.cancel();
				});
				fetchTask.titleProperty().addListener((obs, oldVal, newVal) -> {
					this.updateTitle(newVal);
				});
				fetchTask.messageProperty().addListener((obs, oldVal, newVal) -> {
					this.updateMessage(newVal);
				});
				fetchTask.progressProperty().addListener((obs, oldVal, newVal) -> {
					this.updateProgress(fetchTask.getWorkDone(), fetchTask.getTotalWork());
				});

				App.execute(fetchTask);

				do {
					try {
						UserData fetchedData = fetchTask.get();
						if (this.isCancelled()) return null;

						App.runFxAndWait(() -> {
							applyUserData(fetchedData);
							saveUser();
						});

					} catch (ExecutionException e) {
						Throwable cause = e.getCause();
						if (cause instanceof FailedLoginException) throw (FailedLoginException) cause;
						else if (cause instanceof FailingHttpStatusCodeException) throw (FailingHttpStatusCodeException) cause;
						else if (cause instanceof IOException) throw (IOException) cause;
						else throw new RuntimeException(cause);

					} catch (CancellationException e) {
						break; // action to do on fetchTask cancel is already defined above

					} catch (InterruptedException e) {
						if (this.isCancelled()) {
							fetchTask.cancel();
							return null;
						}
					}
				} while (!fetchTask.isDone());

				return null;
			}
		};

		return updateTask;
	}

	/**
	 * Saves the currently logged-in user's data to be retrieved at a later date. The saved data can be retrieved with
	 * the User's userID.
	 */
	public void saveUser() {
		if (!isUserLoggedIn()) throw new IllegalStateException("No user is logged on the application");

		LOGGER.info("Saving user data");
		App.ioManager.writeObject(getUser(), App.ioManager.getUserFile(getUser().getUserID()));
	}

	/**
	 * Adds a new {@link Bill} to the {@link User}'s list of Bills.
	 * @param source The Source string of the Bill
	 * @param dateTime The Bill's dateTime
	 * @param entries The collection of {@link Entry Bill.Entry} objects whose clones are to be added to the new Bill
	 */
	public void createBill(String source, LocalDateTime dateTime, Collection<Bill.Entry> entries) {
		try {
			App.runFxAndWait(() -> {
				getUser().addBill(new Bill(dateTime, source, entries));
				getUser().sortBills();
			});
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
	}

	/**
	 * Removes the specified Bill from the User's list of Bills if present.
	 * @param bill The Bill to remove
	 */
	public void deleteBill(Bill bill) {
		try {
			App.runFxAndWait(() -> {
				getUser().removeBill(bill);
			});
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
	}

	/* *************************************************************************
	 *                                                                         *
	 * API                                                                     *
	 *                                                                         *
	 ************************************************************************* */

	/**
	 * Mandates a user login before returning, unless the user has given the command to exit the application through the
	 * login window. In that case the method will commence application shutdown.<br>
	 * If a user is already logged in, this method will have no effect. If no user is logged in, this method will check
	 * if auto-login is enabled and log the user in if true, or prompt the user to log in if auto-login is disabled.<br>
	 * If a login is unsuccessful, the prompt will reappear until a successful login is made and this method returns.
	 */
	void forceLogin() {
		if (isUserLoggedIn()) return;

		boolean loggedIn = false;
		String userID;
		String password;

		if (isAutoLogin()) {
			LOGGER.info("Performing auto-login");

			userID = Pref.getSavedUserId();
			password = Crypt.decrypt(Pref.getPreferences().get(Pref.AL_PWD, ""));

			loggedIn = login(userID, password);
		}

		while (!loggedIn) {
			if (isAutoLogin()) LOGGER.warn("Auto-login failed");

			try {
				GuiLogin loginGui = (GuiLogin) App.uiManager.getPrompt(UIManager.LOGIN_FORM_ID);
				loginGui.reset();
				loginGui.setFailMessage(App.webManager.getLoginFailMessage());

				boolean accepted = false;
				try {
					accepted = loginGui.acquireInput();
				} catch (InterruptedException e) {
					LOGGER.debug("", e);
				}

				if (!accepted) {
					App.getMain().exit();
					return;
				}

				userID = loginGui.getUserId();
				password = loginGui.getPassword();
				boolean remember = loginGui.getRemember();

				loggedIn = login(userID, password);

				if (loggedIn && remember) enableAutoLogin(userID, password);
				else if(isAutoLogin()) disableAutoLogin();

			} catch (IOException e) {
				Alerts.catching("Greška pri otvaranju izbornika", e, LOGGER);
				App.getMain().exit();
				return;
			}
		}

		userID = null;
		password = null;
		System.gc();
	}

	/**
	 * Logs a user on the application. The login info for the application is the same login info needed to access the
	 * data that the application fetches for the user. If the login info does not match a valid account on the
	 * data-serving website, the login will fail.<br>
	 * If a valid login of a certain user occurs for the first time, a new user profile will be created for that user
	 * and used to store the fetched data. Subsequent logins of the same user will use the previously created profile
	 * and update it with fresh data.
	 * @param userID the user ID used to log on the data-serving website
	 * @param password the password used to log on the data-serving website
	 * @return true if the login was successful, false otherwise
	 */
	boolean login(String userID, String password) {
		if (isUserLoggedIn()) throw new IllegalStateException("A user is already logged in");

		ProgressMonitor loginMonitor = new ProgressMonitor();
		loginMonitor.setMessage("Prijava u tijeku...");
		loginMonitor.setProgress(-1);
		Stage loginMonitorStage = App.uiManager.showProgressMonitor(loginMonitor, "Prijava");

		if (!App.DEBUG_MODE) {
			try {
				if (!isLoginValid(userID, password)) return false;
			} catch (FailingHttpStatusCodeException | IOException e) {
				Alerts.catching("Provjera valjanosti prijave neuspjela. Pokušajte ponovno kasnije.", e, LOGGER);
				return false;
			}
		}

		encpwd = Crypt.encrypt(password);

		User user = loadUser(userID);
		if (user == null) {
			user = new User(userID);

		} else if (!user.getUserID().equals(userID)) {
			LOGGER.warn("User data corrupted. UserIDs of the logging-in user and their data file do not match");
			user = new User(userID);
		}

		final User fu = user;
		try {
			App.runFxAndWait(() -> {
				setUser(fu);
				loginMonitorStage.close();
			});
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}

		LOGGER.info("Application login successful");
		return true;
	}

	/**
	 * Enables the auto-login feature and initializes it with the specified data.
	 * @param userID The webserver userID of a user
	 * @param password The webserver password of a user
	 */
	void enableAutoLogin(String userID, String password) {
		LOGGER.info("Enabling automatic login");
		Pref.setSavedUserId(userID);
		Pref.getPreferences().put(Pref.AL_PWD, Crypt.encrypt(password));
		Pref.setAutoLogin(true);
	}

	String getUserPassword() {
		return Crypt.decrypt(encpwd);
	}

	void start() {
	}

	void stop() {
		if (isUserLoggedIn()) {
			// saveUser();
			logout();
		}
	}

	/* *************************************************************************
	 *                                                                         *
	 * Private implementation                                                  *
	 *                                                                         *
	 ************************************************************************* */

	/**
	 * Verifies the specified login details with the webserver.
	 * @param userID The webserver user ID of the user to log on
	 * @param password The webserver password of the user to log on
	 * @return true if the login details are valid (successfully logged on the webserver), false otherwise
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws IOException if an IO problem occurs
	 */
	private boolean isLoginValid(String userID, String password) throws FailingHttpStatusCodeException, IOException {
		return App.webManager.verifyLogin(userID, password);
	}

	/**
	 * Loads the previously saved data of the {@link User} with the specified userID
	 * @param userID The userID of the User whose data is to be loaded
	 * @return The User object constructed from the loaded data
	 */
	private User loadUser(String userID) {
		LOGGER.info("Loading user data");
		return (User) App.ioManager.readObject(App.ioManager.getUserFile(userID));
	}

	private void applyUserData(UserData userData) {
		User user = getUser();
		user.setFullName(userData.getFullName());
		user.setAvailableFunds(userData.getAvailableFunds());
		user.addBills(userData.getBills());
		user.sortBills();
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- user
	/**
	 * The currently logged-in user
	 */
	private final ReadOnlyObjectWrapper<User> user = new ReadOnlyObjectWrapper<User>(this, "user") {
		@Override
		protected void invalidated() {
			App.uiManager.invalidateGui();
		};
	};

	public final ReadOnlyObjectProperty<User> userProperty() {
		return user.getReadOnlyProperty();
	}

	public final User getUser() {
		return user.get();
	}

	private final void setUser(User value) {
		user.set(value);
	}

}
