package altline.unistat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import javax.security.auth.login.FailedLoginException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import altline.utils.Alerts;
import javafx.concurrent.Task;

final class WebManager {
	private static final WebManager INSTANCE = new WebManager();
	private static final Logger LOGGER = LogManager.getLogger();

	public static final WebManager getInstance() {
		return INSTANCE;
	}

	/**
	 * This is only the beginning part of the URL for the base page. A real base URL should have additional characters
	 * appended to this string
	 */
	private static final String GENERIC_URL_BASE = "https://issp.srce.hr/Student";
	/**
	 * This is only the beginning part of the URL for the bills page. A real bills URL should have additional characters
	 * appended to this string
	 */
	private static final String GENERIC_URL_BILLS = "https://issp.srce.hr/StudentRacun";
	/**
	 * This is only the beginning part of a login URL. A real login URL will have additional characters appended to this
	 * string
	 */
	private static final String GENERIC_URL_LOGIN = "https://login.aaiedu.hr/sso/module.php/core/loginuserpass.php";
	private static final String URL_LOGOUT = "https://issp.srce.hr/KorisnickiRacun/Odjava";

	private static final DecimalFormat FLOAT_FORMAT;

	static {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator(',');
		FLOAT_FORMAT = new DecimalFormat("0.00");
		FLOAT_FORMAT.setDecimalFormatSymbols(symbols);
	}

	private WebClient webClient;
	private HtmlPage currentPage;
	private boolean loggedIn;
	private String loggedUserId;
	private String loginFailMessage = "";

	private String urlBase;
	private String urlBills;

	void start() {
		webClient = new WebClient();
	}

	void stop() {
		webClient.close();
	}

	String getLoginFailMessage() {
		return loginFailMessage;
	}

	/**
	 * Tries to log on the webserver with the specified login details, returning true if succeeded, false if not.
	 * @param userID The webserver user ID of the user to log on
	 * @param password The webserver password of the user to log on
	 * 
	 * @return true if the user was successfully logged on the webserver, false otherwise
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws IOException if an IO problem occurs
	 */
	boolean verifyLogin(String userID, String password) throws FailingHttpStatusCodeException, IOException {
		LOGGER.info("Verifying login information with the webserver...");
		try {
			login(userID, password);
			LOGGER.info("Login information valid");
			return true;

		} catch (FailedLoginException e) {
			LOGGER.info("Login verification failed. Server message: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Returns a {@link Task} that fetches user data from the webserver.
	 * 
	 * <h2>Task description</h2>
	 * <p>
	 * The task fetches the logged-in user's data from the webserver and returns it in a {@link UserData} object. If the
	 * task is canceled while the fetch is in progress, it will return null at its earliest convenience.
	 * </p>
	 * <h3>Task Exceptions</h3> Notable exceptions that the task may encounter include:
	 * <ul>
	 * <li>{@link FailedLoginException} - if the user could not be logged on the webserver</li>
	 * <li>{@link FailingHttpStatusCodeException} - if the server returns a failing status code</li>
	 * <li>{@link IOException} - if an IO problem occurs</li>
	 * <li>{@link IllegalStateException} - if no user is logged on the application</li>
	 * </ul>
	 * @return The Task that, when started, will fetch user data from the webserver
	 */
	Task<UserData> fetchData() {
		return new DataFetchTask();
	}

	/**
	 * Logs the user off the webserver if one is thought to be logged in.
	 * 
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws IOException if an IO problem occurs
	 */
	void logout() throws FailingHttpStatusCodeException, IOException {
		if (!loggedIn) return;

		LOGGER.info("Logging off the webserver...");
		connect(URL_LOGOUT);

		loggedIn = false;
		loggedUserId = "";
		urlBase = null;
		urlBills = null;
	}

	private String getCurrentUrl() {
		if (currentPage == null) return "";
		return currentPage.getUrl().toExternalForm();
	}

	/**
	 * Logs a user on the webserver.
	 * @param userID The webserver user ID of the user to log on
	 * @param password The webserver password of the user to log on
	 * 
	 * @throws FailedLoginException if the login fails (exception message contains the webserver's response message
	 *             describing what went wrong)
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws IOException if an IO problem occurs
	 */
	private void login(String userID, String password) throws FailedLoginException, FailingHttpStatusCodeException, IOException {
		logout();

		LOGGER.info("Logging on the webserver...");
		String result = connect(GENERIC_URL_BASE);
		if (!result.startsWith(GENERIC_URL_LOGIN)) throw new IOException("Unexpected webserver response");

		HtmlForm loginForm = currentPage.getFormByName("f");
		HtmlTextInput usernameInput = loginForm.getInputByName("username");
		HtmlPasswordInput passwordInput = loginForm.getInputByName("password");
		HtmlSubmitInput submitInput = loginForm.getInputByName("Submit");

		usernameInput.type(userID);
		passwordInput.type(password);

		String loginResponse = setCurrent(submitInput.click());

		if (loginResponse.startsWith(GENERIC_URL_BASE)) {
			LOGGER.info("Login successful");

			loggedIn = true;
			loggedUserId = userID;
			urlBase = loginResponse;
			loginFailMessage = "";

			String href = currentPage.querySelector(".btn-primary").getAttributes().getNamedItem("href").getTextContent();
			urlBills = GENERIC_URL_BILLS.concat(href.substring(href.indexOf('?')));

		} else if (loginResponse.startsWith(GENERIC_URL_LOGIN)) {
			DomNode errorMsgContainer = currentPage.querySelector(".aai_messages_container");
			String errorMessage = errorMsgContainer != null ? errorMsgContainer.asText() : "--";

			loginFailMessage = errorMessage;
			throw new FailedLoginException(errorMessage);

		} else throw new IOException("Unexpected webserver response");
	}

	/*private void saveCookies() {
		LOGGER.debug("Saving cookies");
		App.ioManager.writeObject(webClient.getCookieManager().getCookies(), App.ioManager.getPath(IOManager.F_COOKIE_STORE));
	}
	
	private void loadCookies() {
		LOGGER.debug("Loading cookies");
		Path cookiesFile = App.ioManager.getPath(IOManager.F_COOKIE_STORE);
		if (Files.exists(cookiesFile)) {
			@SuppressWarnings("unchecked")
			Set<Cookie> cookies = (Set<Cookie>) App.ioManager.readObject(cookiesFile);
			for (Cookie cookie : cookies) {
				webClient.getCookieManager().addCookie(cookie);
			}
		}
	}*/

	/**
	 * Connects to the specified URL ensuring that the user is logged on the webserver.
	 * @param url The string from of the URL to connect to
	 * @return The URL string of the received page
	 * 
	 * @throws FailedLoginException if a webserver login is necessary and it fails
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws MalformedURLException if no URL object can be created from the provided URL string
	 * @throws IOException if an IO problem occurs
	 * @throws IllegalStateException if no user is logged on the application
	 */
	private String connectLoggedIn(String url) throws FailedLoginException, FailingHttpStatusCodeException, MalformedURLException, IOException {
		if (url.startsWith(GENERIC_URL_LOGIN)) throw new IllegalArgumentException("Trying to connect logged-in to the login URL makes no sense");

		User user = App.userManager.getUser();
		if (user == null) throw new IllegalStateException("No user is logged on the application");

		String currentUrl = connect(url);

		if (!Objects.equals(user.getUserID(), loggedUserId) || currentUrl.startsWith(GENERIC_URL_LOGIN)) {
			LOGGER.info("Not logged on the server. Will attempt login...");

			login(App.userManager.getUser().getUserID(), App.userManager.getUserPassword());
			currentUrl = connect(url);
		}

		return currentUrl;
	}

	/**
	 * Connects to the specified URL.
	 * @param url The string from of the URL to connect to
	 * @return The URL string of the received page
	 * 
	 * @throws FailingHttpStatusCodeException if the server returns a failing status code
	 * @throws MalformedURLException if no URL object can be created from the provided URL string
	 * @throws IOException if an IO problem occurs
	 */
	private String connect(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		LOGGER.info("Connecting to {}", url);
		return setCurrent(webClient.getPage(url));
	}

	private String setCurrent(HtmlPage page) {
		this.currentPage = page;
		return getCurrentUrl();
	}


	private class DataFetchTask extends Task<UserData> {

		@Override
		protected UserData call() throws FailedLoginException, FailingHttpStatusCodeException, IOException {
			
			final User mainUser = App.userManager.getUser();
			if (mainUser == null) throw new IllegalStateException("No user is logged on the application");

			LOGGER.info("Fetching user data");
			updateTitle("Preuzimanje podataka");

			// If somehow there is a user change and we have the wrong user logged on the server, we log out so the
			// correct
			// user logs in later
			if (!mainUser.getUserID().equals(loggedUserId)) {
				LOGGER.warn("Logged-in user mismatch between server and application!");
				logout();
			}

			if (cancelCheck()) return null;

			final UserData userData = new UserData(mainUser.getUserID());

			updateMessage("Preuzimam opće podatke...");
			boolean success = fetchGeneralData(userData);

			if (cancelCheck()) return null;

			success &= fetchBills(userData);

			if (cancelCheck()) return null;

			if (!success) {
				Alerts.warn("Problem pri preuzimanju podataka. Neki podaci možda nisu ispravni!");
			}

			return userData;
		}

		private boolean fetchGeneralData(UserData userData) throws FailedLoginException, FailingHttpStatusCodeException, IOException {
			boolean success = true;
			String resultUrl = connectLoggedIn(urlBase);

			if (resultUrl.startsWith(GENERIC_URL_BASE)) {
				try {
					DomNode node_fullName = currentPage.querySelector(".col-md-4 > h3:nth-child(1) > strong:nth-child(1)");
					userData.setFullName(node_fullName.asText());

					DomNode node_availableFunds = currentPage.querySelector("div.col-md-2:nth-child(4) > p:nth-child(8)");
					userData.setAvailableFunds(FLOAT_FORMAT.parse(node_availableFunds.getLastChild().asText()).floatValue());

				} catch (ParseException e) {
					LOGGER.warn("Parse exception on data fetch", e);
					success = false;
				}

			} else {
				LOGGER.warn("Unexpected server response to data fetch\n\tReceived: {}\n\twhen expected URL_BASE", resultUrl);
				success = false;
			}

			return success;
		}

		private boolean fetchBills(UserData userData) throws FailedLoginException, FailingHttpStatusCodeException, IOException {
			boolean success = true;
			String resultUrl = connectLoggedIn(urlBills);

			if (resultUrl.startsWith(GENERIC_URL_BILLS)) {
				final DomNodeList<DomNode> billRows = currentPage.querySelectorAll(".table > tbody:nth-child(1) > tr:nth-child(n+2)");
				final int totalBills = billRows.getLength();
				int c = 0;

				final ArrayList<Bill> bills = new ArrayList<Bill>(totalBills);

				try {
					for (DomNode row : billRows) {
						c++;

						if (cancelCheck()) return false;

						DomNodeList<DomNode> cells = row.querySelectorAll("td");

						String source = cells.get(0).asText();
						LocalDateTime dateTime = LocalDateTime.parse(cells.get(1).asText(), UIManager.SERVER_DATE_TIME_FORMATTER);

						Bill bill = new Bill(dateTime, source);

						LOGGER.debug("Pulling bill details {}/{}", c, totalBills);

						HtmlAnchor detailsBtn = (HtmlAnchor) cells.get(4).querySelector("a");
						HtmlPage detailsPage = detailsBtn.click();

						int stillExec = webClient.waitForBackgroundJavaScript(3000);
						if (stillExec > 0) {
							LOGGER.warn("Background javascript still executing after timeout (count: {})" + stillExec);
							success = false;
						}

						DomNodeList<DomNode> detailRows = detailsPage.querySelectorAll("div.cap-racun-stavke-modal-body > table > tbody > tr:nth-child(n+2)");
						for (DomNode detailRow : detailRows) {

							DomNodeList<DomNode> detailCells = detailRow.querySelectorAll("td");

							String articleName = detailCells.get(1).asText();
							float articlePrice = FLOAT_FORMAT.parse(detailCells.get(2).asText()).floatValue();
							int amount = Integer.parseInt(detailCells.get(3).asText());
							float subsidy = FLOAT_FORMAT.parse(detailCells.get(5).asText()).floatValue();

							bill.addEntry(articleName, articlePrice, amount, subsidy);
						}

						boolean shouldAdd = true;
						boolean shouldStop = false;
						for(Bill existingBill : App.userManager.getUser().getBills()) {
							if(bill.equals(existingBill)) {
								shouldStop = true;
								
							} else if(bill.isSameTimePlace(existingBill) && existingBill.isEdited()) {
								shouldAdd = false;
							}
						}
						
						if(shouldStop) {
							LOGGER.debug("Reached end of fresh bills. Stopping bill fetch");
							break;
						}

						if(shouldAdd) {
							bills.add(bill);
							updateMessage("Preuzeto računa: " + c);
							updateProgress(c, totalBills);
						} else LOGGER.info("Skipping bill {}", c);
					}

				} catch (Exception e) {
					LOGGER.warn("Exception while fetching bill data", e);
					success = false;
				}

				userData.setBills(bills);

			} else {
				LOGGER.warn("Unexpected server response to data fetch\n\tReceived: {}\n\twhen expected URL_BILLS", resultUrl);
				success = false;
			}

			return success;
		}

		@Override
		protected void cancelled() {
			LOGGER.info("User data fetch canceled");
		}

		private boolean alreadyCanceled = false;
		private boolean cancelCheck() {
			if (alreadyCanceled) return true;
			if (isCancelled()) {
				alreadyCanceled = true;
				updateMessage("Preuzimanje prekinuto");
				updateProgress(Double.NaN, Double.NaN);
				return true;
			}
			return false;
		}

	}

}