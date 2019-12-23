package altline.unistat.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import altline.unistat.App;
import altline.utils.Alerts;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class GuiBase {
	private static final Logger LOGGER = LogManager.getLogger();

	public static final String CARD_OVERVIEW = "Overview";
	public static final String CARD_BILLS = "Bills";
	public static final String CARD_CALENDAR = "Calendar";
	public static final String CARD_GENERAL_STATS = "General stats";
	public static final String CARD_ARTICLE_STATS = "Article stats";

	/**
	 * Meant to prevent the user from deselecting the nav buttons
	 */
	private EventHandler<MouseEvent> toggleHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent mouseEvent) {
			if (mouseEvent.getSource().equals(navButtons.getSelectedToggle())) {
				mouseEvent.consume();
			}
		}
	};

	private static Map<String, Pane> cardMap = new HashMap<String, Pane>();

	@FXML
	private BorderPane view;

	@FXML
	private ToggleGroup navButtons;

	@FXML
	private ToggleButton btnOverview, btnBills, btnCalendar, btnGeneralStats, btnArticleStats;

	@FXML
	private Label lblUserName;

	@FXML
	private void initialize() throws IOException {
		loadCards();

		initCardButton(btnOverview, CARD_OVERVIEW);
		initCardButton(btnBills, CARD_BILLS);
		initCardButton(btnCalendar, CARD_CALENDAR);
		initCardButton(btnGeneralStats, CARD_GENERAL_STATS);
		initCardButton(btnArticleStats, CARD_ARTICLE_STATS);

		lblUserName.textProperty().bind(new StringBinding() {
			private StringProperty userFullNameProperty;
			{
				bind(App.userManager.getUser().fullNameProperty());
			}
			@Override
			protected String computeValue() {
				return App.userManager.getUser() == null ? "" : App.userManager.getUser().getFullName();
			}

			@Override
			protected void onInvalidating() {
				unbind(userFullNameProperty);
				if (App.userManager.getUser() != null) {
					userFullNameProperty = App.userManager.getUser().fullNameProperty();
					bind(userFullNameProperty);
				} else userFullNameProperty = null;
			}
		});

		showCard(CARD_OVERVIEW);
	}

	@FXML
	private void updateUser() {
		Task<Void> updateTask = App.userManager.updateUser();
		updateTask.setOnFailed(e -> {
			Alerts.catching("Osvježavanje nije uspjelo", updateTask.getException(), LOGGER);
		});
		App.uiManager.showWorkerMonitor(updateTask);
		App.execute(updateTask);
	}

	@FXML
	private void logout() {
		App.userManager.disableAutoLogin();
		App.userManager.logout();
	}

	private void loadCards() throws IOException {
		FXMLLoader loader = new FXMLLoader(App.class.getResource("/gui/Overview.fxml"));
		cardMap.put(CARD_OVERVIEW, loader.load());

		loader = new FXMLLoader(App.class.getResource("/gui/Bills.fxml"));
		cardMap.put(CARD_BILLS, loader.load());
		
		loader = new FXMLLoader(App.class.getResource("/gui/Calendar.fxml"));
		cardMap.put(CARD_CALENDAR, loader.load());

		loader = new FXMLLoader(App.class.getResource("/gui/GeneralStats.fxml"));
		cardMap.put(CARD_GENERAL_STATS, loader.load());

		loader = new FXMLLoader(App.class.getResource("/gui/ArticleStats.fxml"));
		cardMap.put(CARD_ARTICLE_STATS, loader.load());
	}
	
	private void initCardButton(ToggleButton cardButton, String cardID) {
		cardButton.addEventFilter(MouseEvent.MOUSE_RELEASED, toggleHandler);
		cardButton.setOnAction(e -> {
			showCard(cardID);
		});
	}

	private void showCard(String cardID) {
		view.setCenter(cardMap.get(cardID));
	}

}
