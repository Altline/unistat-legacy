package altline.unistat;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.security.auth.login.FailedLoginException;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import altline.utils.Alerts;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;


public class App extends Application {
	public static final String APPNAME = "UniStat";
	public static final String VERSION = "1.0.10";
	public static final String AUTHOR = "AltLine";
	public static final String TITLE = APPNAME + " - v" + VERSION;
	public static final boolean DEBUG_MODE = false;
	public static final int ALREADY_RUNNING = 101;
	/**
	 * The login thread waits on this object to get notified when a user logs out of the application so that the login
	 * prompt can be shown.
	 */
	public static final Object LOGIN_LOCK = new Object();

	public static final IOManager ioManager = IOManager.getInstance();
	public static final UIManager uiManager = UIManager.getInstance();
	public static final UserManager userManager = UserManager.getInstance();
	public static final WebManager webManager = WebManager.getInstance();

	private static final Logger LOGGER = LogManager.getLogger();

	private static App main;

	private static final ExecutorService taskExecutor = Executors.newCachedThreadPool();

	private Thread loginThread;
	private static boolean exiting = false;

	public static App getMain() {
		return main;
	}

	@Override
	public void init() throws Exception {
		Platform.setImplicitExit(false);

		// Did this so the JFXDatePicker contains the correct calendar layout since apparently you can't change it
		// without some ridiculous hacks
		Locale.setDefault(Locale.forLanguageTag("hr"));
	}

	@Override
	public void start(Stage primaryStage) {
		LOGGER.info("Application started ({})", VERSION);
		if (DEBUG_MODE) LOGGER.warn("Running in DEBUG MODE");

		main = this;

		ioManager.start();
		userManager.start();
		webManager.start();
		uiManager.start(primaryStage);

		startLoginThread();
	}

	@Override
	public void stop() {
		LOGGER.info("Application closing");
		exiting = true;

		synchronized (LOGIN_LOCK) {
			LOGIN_LOCK.notifyAll();
		}

		userManager.stop();
		webManager.stop();
		uiManager.stop();
		ioManager.stop();

		taskExecutor.shutdown();
	}

	/**
	 * Calls for the application to exit orderly.
	 */
	public void exit() {
		exiting = true;
		Platform.exit();
	}

	/**
	 * Run the specified Runnable on the JavaFX Application Thread at some unspecified time in the future, or
	 * immediately if called from that thread. If called from a thread that is not the JavaFX Application Thread, this
	 * method will post the specified Runnable to the event queue and then return immediately to the caller.
	 * @param runnable
	 * @see {@link Platform#runLater(Runnable)}
	 */
	public static void runFx(Runnable runnable) {
		if (Platform.isFxApplicationThread()) runnable.run();
		else Platform.runLater(runnable);
	}

	/**
	 * Run the specified Runnable on the JavaFX Application Thread at some unspecified time in the future and wait until
	 * it is executed. If this is called from the JavaFX Application Thread, the runnable is run immediately instead of
	 * being posted to the event queue.
	 * @param runnable
	 * @throws InterruptedException if interrupted while waiting for the runnable to complete
	 * @see {@link Platform#runLater(Runnable)}
	 */
	public static void runFxAndWait(Runnable runnable) throws InterruptedException {
		if (runnable == null)
			throw new NullPointerException("runnable");

		// run synchronously on JavaFX thread
		if (Platform.isFxApplicationThread()) {
			runnable.run();
			return;
		}

		final CountDownLatch doneLatch = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				runnable.run();
			} finally {
				doneLatch.countDown();
			}
		});

		doneLatch.await();
	}

	/**
	 * Call the specified {@link Callable} on the JavaFX Application Thread at some unspecified time in the future and
	 * wait until it is executed. If called from the JavaFX Application Thread, the Callable is called immediately
	 * instead of being posted to the event queue. This method can return a result given by the Callable and throw
	 * exceptions to the caller.
	 * @param callable
	 * @return the result of the callable
	 * @throws ExecutionException if callable threw an exception
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	public static <V> V runFxAndWait(Callable<V> callable) throws ExecutionException, InterruptedException {
		FutureTask<V> task = new FutureTask<V>(callable);

		if (Platform.isFxApplicationThread()) task.run();
		else Platform.runLater(task);

		return task.get();
	}

	/**
	 * Executes the specified {@link Runnable} on a background thread.
	 * @param runnable
	 * @return A {@link Future} representing pending completion of the task
	 */
	public static Future<?> execute(Runnable runnable) {
		return taskExecutor.submit(runnable);
	}

	private void startLoginThread() {
		loginThread = new Thread(() -> {
			try {
				synchronized (LOGIN_LOCK) {
					while (!exiting) {

						if (uiManager.isPrimaryStageShowing() && !userManager.isUserLoggedIn()) {
							uiManager.hidePrimaryStage();
						}

						userManager.forceLogin();
						if (exiting) break;

						if (!DEBUG_MODE) {
							try {
								Task<Void> userUpdateTask = userManager.updateUser();
								uiManager.showWorkerMonitor(userUpdateTask);
								execute(userUpdateTask).get();

							} catch (ExecutionException e) {
								Throwable cause = e.getCause();
								if (cause instanceof FailedLoginException) {
									LOGGER.error("Webserver login failed upon data fetching", e);
									Alerts.error(APPNAME, "Neuspjelo preuzimanje podataka",
											"Greška prilikom prijave u AAIEdu sustav. Potrebno je ponoviti prijavu.");

									userManager.disableAutoLogin();
									userManager.logout();
									continue;

								} else if (cause instanceof FailingHttpStatusCodeException || cause instanceof IOException) {
									LOGGER.error("User data update failed", e);
									Alerts.error(APPNAME, "Neuspjelo preuzimanje podataka",
											"Greška prilikom preuzimanja korisničkih podataka. Najnoviji podaci neće biti prikazani.");
								}

							} catch (CancellationException | InterruptedException e) {
								LOGGER.warn("", e);
							}
						}
						
						//execute(webManager.fetchData());

						try {
							uiManager.showPrimaryStage();
						} catch (Throwable e) {
							Alerts.catching("Neuspjelo otvaranje glavnog prozora", e, LOGGER);
							exit();
							return;
						}

						try {
							LOGIN_LOCK.wait();
						} catch (InterruptedException e) {
							LOGGER.warn("Main login wait interrupted", e);
						}

					}
				}
			} catch (Throwable t) {
				Alerts.catching("Error in login loop", t, LOGGER);
			}
		}, "LoginThread");
		loginThread.start();
	}

	public static void main(String[] args) {

		try {
			Path applockFile = ioManager.getPath(IOManager.F_APPLOCK);

			RandomAccessFile raf = new RandomAccessFile(applockFile.toFile(), "rw");
			FileChannel channel = raf.getChannel();
			FileLock lock = channel.tryLock();
			if (lock == null) {
				LOGGER.warn("Unable to obtain file lock - assuming another instance is already running. This instance will not launch");
				channel.close();
				raf.close();
				JOptionPane.showMessageDialog(null, "Aplikacija je već pokrenuta", "Pokretanje nije moguće", JOptionPane.ERROR_MESSAGE);
				System.exit(ALREADY_RUNNING);
			}

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (lock != null) lock.release();
						channel.close();
						raf.close();
						Files.delete(applockFile);
					} catch (IOException e) {
						LOGGER.error("", e);
					}
				}
			}));

		} catch (IOException e) {
			LOGGER.fatal("Launch error", e);
			System.exit(1);
		}

		try {
			launch(args);
		} catch (Throwable t) {
			LOGGER.fatal("", t);
		}
	}
}
