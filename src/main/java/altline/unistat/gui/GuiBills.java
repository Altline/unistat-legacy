package altline.unistat.gui;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import altline.unistat.gui.component.BillView;
import altline.unistat.gui.component.FormatCellFactory;
import altline.utils.Alerts;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

public class GuiBills {

	private static final Logger LOGGER = LogManager.getLogger();

	@FXML
	private Label lblTotalBills;

	@FXML
	private Button btnNewBill, btnEditBill, btnDeleteBill;


	@FXML
	private TableView<Bill> billsTable;

	@FXML
	private TableColumn<Bill, Number> articleCountCol;

	@FXML
	private TableColumn<Bill, String> sourceCol;

	@FXML
	private TableColumn<Bill, Number> costCol;

	@FXML
	private TableColumn<Bill, Number> subsidyCol;

	@FXML
	private TableColumn<Bill, LocalDateTime> dateTimeCol;


	@FXML
	private Label lblNoBill;

	@FXML
	private StackPane detailsArea;

	private BillView billView;

	@FXML
	private void initialize() {


		billView = new BillView();
		billView.billProperty().bind(selectedBillProperty());
		detailsArea.getChildren().add(billView);

		lblNoBill.visibleProperty().bind(selectedBillProperty().isNull());
		billView.visibleProperty().bind(selectedBillProperty().isNotNull());

		lblTotalBills.textProperty().bind(Bindings.size(App.userManager.getUser().getBills()).asString("Računi: %d"));

		btnNewBill.setOnAction(e -> {
			try {
				GuiBillEditor gui = (GuiBillEditor) App.uiManager.getPrompt(UIManager.BILL_EDITOR_ID);
				gui.setAllowExistingDateTime(false);
				gui.reset();

				if (gui.acquireInput()) {
					Bill template = gui.getBillTemplate();
					App.userManager.createBill(template.getSource(), template.getDateTime(), template.getEntries());
					App.userManager.saveUser();
				}

			} catch (IOException e1) {
				Alerts.catching("Greška pri otvaranju izbornika", e1, LOGGER);
			} catch (InterruptedException e1) {
				LOGGER.debug("", e1);
			}
		});

		btnEditBill.disableProperty().bind(selectedBillProperty().isNull());
		btnEditBill.setOnAction(e -> {
			try {
				GuiBillEditor gui = (GuiBillEditor) App.uiManager.getPrompt(UIManager.BILL_EDITOR_ID);
				gui.setAllowExistingDateTime(true);

				Bill editedBill = selectedBillProperty().get();
				Bill template = gui.getBillTemplate();

				template.edit(editedBill.getDateTime(), editedBill.getSource(), editedBill.getEntries());

				if (gui.acquireInput()) {
					editedBill.edit(template.getDateTime(), template.getSource(), template.getEntries());
					App.userManager.saveUser();
				}

			} catch (IOException e1) {
				Alerts.catching("Greška pri otvaranju izbornika", e1, LOGGER);
			} catch (InterruptedException e1) {
				LOGGER.debug("", e1);
			}
		});

		btnDeleteBill.disableProperty().bind(selectedBillProperty().isNull());
		btnDeleteBill.setOnAction(e -> {
			App.userManager.deleteBill(selectedBillProperty().get());
			App.userManager.saveUser();
		});

		initBillsTable();
		populate();
	}

	private void initBillsTable() {
		// an attempt to squeeze all bill table columns on the screen when the application window is at default
		// non-maximized size
		articleCountCol.minWidthProperty().bind(
				Bindings.min(billsTable.widthProperty().multiply(0.1), articleCountCol.maxWidthProperty()));

		articleCountCol.setCellValueFactory(p -> p.getValue().totalArticlesBinding());
		sourceCol.setCellValueFactory(new PropertyValueFactory<Bill, String>("source"));
		costCol.setCellValueFactory(p -> p.getValue().totalCostBinding());
		subsidyCol.setCellValueFactory(p -> p.getValue().totalSubsidyBinding());
		dateTimeCol.setCellFactory((FormatCellFactory<Bill, LocalDateTime>) value -> {
			return value.format(UIManager.DATE_TIME_FORMATTER);
		});
		dateTimeCol.setCellValueFactory(new PropertyValueFactory<Bill, LocalDateTime>("dateTime"));

		dateTimeCol.setSortType(SortType.DESCENDING);
		billsTable.getSortOrder().setAll(Arrays.asList(dateTimeCol));
	}

	private void populate() {
		SortedList<Bill> sortedList = new SortedList<Bill>(App.userManager.getUser().getBills());
		sortedList.comparatorProperty().bind(billsTable.comparatorProperty());
		billsTable.setItems(sortedList);
	}

	private ReadOnlyObjectProperty<Bill> selectedBillProperty() {
		return billsTable.getSelectionModel().selectedItemProperty();
	}

}
