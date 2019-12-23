package altline.unistat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import altline.unistat.gui.GuiBase;
import altline.unistat.gui.component.ProgressMonitor;
import altline.unistat.gui.component.Prompt;
import altline.unistat.gui.component.WorkerMonitor;
import altline.utils.Alerts;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class UIManager {
	private static final UIManager INSTANCE = new UIManager();
	private static final Logger LOGGER = LogManager.getLogger();

	public static final UIManager getInstance() {
		return INSTANCE;
	}

	public static final DateTimeFormatter SERVER_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy. H:mm:ss");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy. | H:mm:ss");
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy.");
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss");
	public static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy.");
	public static final DateTimeFormatter MONTH_TEXT_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("hr"));

	public static final StringConverter<LocalTime> TIME_STRING_CONVERTER = new StringConverter<LocalTime>() {
		@Override
		public String toString(LocalTime object) {
			return object == null ? "" : object.format(UIManager.TIME_FORMATTER);
		}

		@Override
		public LocalTime fromString(String string) {
			return LocalTime.parse(string, UIManager.TIME_FORMATTER);
		}
	};

	/**
	 * Since changing the Tooltip showing duration is not a feature until Java 9. I have to reflectively modify it.<br>
	 * Taken from: https://stackoverflow.com/a/27739605
	 * 
	 * NOTE: Apparently calling this only once will have an effect on all tooltips in the application
	 * @param tooltip
	 * @param durationMillis
	 */
	public static void hackTooltipStartTiming(Tooltip tooltip, double durationMillis) {
		try {
			Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
			fieldBehavior.setAccessible(true);
			Object objBehavior = fieldBehavior.get(tooltip);

			Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
			fieldTimer.setAccessible(true);
			Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

			objTimer.getKeyFrames().setAll(new KeyFrame(new Duration(durationMillis)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// PromptIDs
	public static final String LOGIN_FORM_ID = "LOGIN_FORM";
	public static final String BILL_EDITOR_ID = "BILL_EDITOR";
	// ----

	/** Cache of the Prompts */
	private final Map<String, Prompt> promptMap = new HashMap<String, Prompt>();

	private Stage primaryStage;
	private GuiBase guiBase;

	void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("/images/UniStat-logo-16.png")));
		this.primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("/images/UniStat-logo-32.png")));
		this.primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("/images/UniStat-logo-64.png")));
	}

	void stop() {
	}

	/**
	 * Provides a Prompt of the specified promptID. When a Prompt is requested for the first time since application
	 * start, it is loaded. Subsequent requests return the cached Prompts. Valid promptIDs can be found among the static
	 * constants of this class.
	 * @param promptID The id of the Prompt
	 * @return The Prompt corresponding to the specified promptID
	 * @throws IOException if there was an issue loading the Prompt
	 */
	public Prompt getPrompt(String promptID) throws IOException {
		if (!promptMap.containsKey(promptID)) loadPrompt(promptID);
		return promptMap.get(promptID);
	}

	public boolean isPrimaryStageShowing() {
		return primaryStage.isShowing();
	}

	/**
	 * Causes the GUI to be reloaded when {@link #showPrimaryStage()} is called. Called when there is a user change.<br>
	 * I feel that this way of handling GUI leaks memory because no listeners are unregistered anywhere with this.
	 * Should be looked into...
	 */
	public void invalidateGui() {
		guiBase = null;
	}

	/**
	 * Creates and shows (in a new utility {@link Stage}) the specified {@link ProgressMonitor}. The created stage will
	 * have the specified title string as its title.
	 * @param monitor The ProgressMonitor to show
	 * @param title The title of the stage that contains the ProgressMonitor
	 * @return The stage containing the ProgressMonitor
	 */
	public Stage showProgressMonitor(ProgressMonitor monitor, String title) {
		try {
			return App.runFxAndWait(() -> {
				Stage stage = new Stage(StageStyle.UTILITY);
				stage.setTitle(title);
				stage.setScene(new Scene(monitor));
				stage.show();
				return stage;
			});
		} catch (ExecutionException e) {
			Alerts.catching("Couldn't show progress monitor", e, LOGGER);
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
		return null;
	}

	/**
	 * Creates and shows (in a new utility {@link Stage}) a {@link WorkerMonitor} that is bound to the specified
	 * {@link Worker}. The stage is set up so that it will automatically close when the worker stops.
	 * @param worker
	 * @return The stage containing the scene with the worker monitor
	 */
	public Stage showWorkerMonitor(Worker<?> worker) {
		try {
			return App.runFxAndWait(() -> {

				switch (worker.getState()) {
				case SUCCEEDED:
				case CANCELLED:
				case FAILED:
					return null;
				default:
					break;
				}

				WorkerMonitor monitor = new WorkerMonitor(worker);

				Stage stage = new Stage(StageStyle.UTILITY);
				stage.setOnCloseRequest((e) -> e.consume());
				stage.titleProperty().bind(worker.titleProperty());
				stage.setScene(new Scene(monitor));

				worker.stateProperty().addListener((obs, oldVal, newVal) -> {
					switch (newVal) {
					case SUCCEEDED:
					case CANCELLED:
					case FAILED:
						stage.hide();
						break;
					default:
						break;
					}
				});

				stage.show();
				return stage;
			});
		} catch (ExecutionException e) {
			Alerts.catching("Couldn't show worker monitor", e, LOGGER);
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
		return null;
	}

	/**
	 * Loads the appropriate Prompt based on the specified promptID.
	 * @param promptID One of the valid promptIDs to load the Prompt
	 * @throws IOException if there was an issue loading the Prompt
	 */
	private void loadPrompt(String promptID) throws IOException {
		try {
			App.runFxAndWait(new Callable<Void>() {
				@Override
				public Void call() throws Exception {

					String path;
					StageStyle stageStyle = StageStyle.UTILITY;
					Modality modality = Modality.APPLICATION_MODAL;
					String title = App.APPNAME;

					switch (promptID) {
					case LOGIN_FORM_ID:
						path = "/gui/Login.fxml";
						stageStyle = StageStyle.UNDECORATED;
						title = App.APPNAME + " - Prijava";

						break;
					case BILL_EDITOR_ID:
						path = "/gui/BillEditor.fxml";
						title = "Račun";

						break;
					default:
						throw new IllegalArgumentException("Non-exising prompt ID: " + promptID);
					}

					FXMLLoader loader = new FXMLLoader(App.class.getResource(path));
					Scene scene = new Scene(loader.load());
					scene.getStylesheets().add(App.class.getResource("/gui/application.css").toExternalForm());

					Stage stage = new Stage(stageStyle);
					stage.initModality(modality);
					stage.setTitle(title);
					stage.setScene(scene);
					stage.sizeToScene();

					// Preventing the stage from resizing smaller than min scene size. But this needs to show the stage
					// first so nope
					/*stage.show();
					
					Node root = scene.getRoot();
					Bounds rootBounds = root.getBoundsInLocal();
					double deltaW = stage.getWidth() - rootBounds.getWidth();
					double deltaH = stage.getHeight() - rootBounds.getHeight();
					
					double prefWidth;
					double prefHeight;
					
					Orientation bias = root.getContentBias();
					if (bias == Orientation.HORIZONTAL) {
					    prefWidth = root.prefWidth(-1);
					    prefHeight = root.prefHeight(prefWidth);
					} else if (bias == Orientation.VERTICAL) {
					    prefHeight = root.prefHeight(-1);
					    prefWidth = root.prefWidth(prefHeight);
					} else {
					    prefWidth = root.prefWidth(-1);
					    prefHeight = root.prefHeight(-1);
					}
					
					stage.setMinWidth(prefWidth + deltaW);
					stage.setMinHeight(prefHeight + deltaH);
					
					stage.hide();*/

					promptMap.put(promptID, loader.getController());

					return null;
				}
			});
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			else throw new RuntimeException(cause);

		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
	}

	/**
	 * Shows the primary stage of the application, loading it if it is not loaded.
	 * @throws IOException if there was an issue loading the GUI
	 */
	void showPrimaryStage() throws IOException {
		try {
			App.runFxAndWait(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					if (guiBase == null) {
						try {
							FXMLLoader loader = new FXMLLoader(App.class.getResource("/gui/Base.fxml"));
							Pane root = loader.load();
							Scene scene = new Scene(root, 1280, 720);
							scene.getStylesheets().add(App.class.getResource("/gui/application.css").toExternalForm());
							primaryStage.setScene(scene);
							primaryStage.setTitle(App.TITLE);
							primaryStage.setOnCloseRequest(e -> {
								App.getMain().exit();
							});

							guiBase = loader.<GuiBase>getController();

						} catch (IOException e) {
							LOGGER.error("", e);
						}
					}

					primaryStage.show();
					return null;
				}

			});
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			else throw new RuntimeException(cause);

		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
	}

	void hidePrimaryStage() {
		try {
			App.runFxAndWait(() -> primaryStage.hide());
		} catch (InterruptedException e) {
			LOGGER.warn("", e);
		}
	}

}
