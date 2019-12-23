package altline.unistat.gui;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTimePicker;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import altline.unistat.gui.component.PromptBase;
import altline.unistat.gui.component.SpinnerTableCell;
import altline.unistat.gui.component.TextEditingTableCell;
import altline.utils.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.converter.FloatStringConverter;

public class GuiBillEditor extends PromptBase {

	private final Bill templateBill = new Bill(null, "");
	private BooleanProperty allowExsitingDateTime = new SimpleBooleanProperty();

	@FXML
	private JFXDatePicker datePicker;

	@FXML
	private JFXTimePicker timePicker;

	@FXML
	private JFXComboBox<String> sourceField;

	@FXML
	private Button btnAddEntry, btnRemoveEntry;

	@FXML
	private Label lblTotalArticles, lblTotalCost, lblTotalSubsidy, lblEntryCount;


	@FXML
	private TableView<Bill.Entry> articlesTable;

	@FXML
	private TableColumn<Bill.Entry, String> articleNameCol;

	@FXML
	private TableColumn<Bill.Entry, Integer> articleAmountCol;

	@FXML
	private TableColumn<Bill.Entry, Float> articlePriceCol;

	@FXML
	private TableColumn<Bill.Entry, Float> articleSubsidyCol;


	@FXML
	private Button btnAccept, btnCancel;


	@FXML
	private void initialize() {
		templateBill.addEntry("", 0, 1, 0);

		lblTotalArticles.textProperty().bind(templateBill.totalArticlesBinding().asString());
		lblTotalCost.textProperty().bind(templateBill.totalCostBinding().asString());
		lblTotalSubsidy.textProperty().bind(templateBill.totalSubsidyBinding().asString());
		lblEntryCount.textProperty().bind(Bindings.size(templateBill.getEntries()).asString("Stavki: %d"));

		timePicker.set24HourView(true);
		timePicker.setConverter(UIManager.TIME_STRING_CONVERTER);

		btnAddEntry.setOnAction(e -> {
			templateBill.addEntry("", 0, 1, 0);
		});

		btnRemoveEntry.disableProperty().bind(Bindings.lessThanOrEqual(Bindings.size(templateBill.getEntries()), 1));
		btnRemoveEntry.setOnAction(e -> {
			templateBill.removeEntry(selectedEntryProperty().get());
		});

		initArticlesTable();

		BooleanBinding articlesNotValid = new BooleanBinding() {
			private List<Bill.Entry> dependentEntries = new LinkedList<Bill.Entry>();

			{
				bind(templateBill.getEntries());
			}

			@Override
			protected boolean computeValue() {
				List<Bill.Entry> entries = templateBill.getEntries();
				for (int i = 0; i < entries.size(); i++) {
					if (StringUtils.isBlank(entries.get(i).getArticleName())) return true;

					for (int j = i + 1; j < entries.size(); j++) {
						if (Objects.equals(entries.get(i), entries.get(j))) return true;
					}
				}
				return false;
			}

			// This updates the correct bindings. I didn't want to create a named class just for that so I put it here
			// and invalidate the binding manually when needed
			@Override
			protected void onInvalidating() {
				Iterator<Bill.Entry> it = dependentEntries.iterator();
				while (it.hasNext()) {
					Bill.Entry entry = it.next();
					if (!templateBill.getEntries().contains(entry)) {
						unbind(entry.articleNameProperty());
						it.remove();
					}
				}

				for (Bill.Entry entry : templateBill.getEntries()) {
					if (!dependentEntries.contains(entry)) {
						bind(entry.articleNameProperty());
						dependentEntries.add(entry);
					}
				}
			}
		};
		// calls onInvalidating
		articlesNotValid.get();
		articlesNotValid.invalidate();

		templateBill.getEntries().addListener((Change<? extends Bill.Entry> c) -> {
			// update article bindings after list change
			articlesNotValid.get();
			articlesNotValid.invalidate();
		});

		btnAccept.disableProperty().bind(
				datePicker.valueProperty().isNull()
						.or(timePicker.valueProperty().isNull())
						.or(Bindings.createBooleanBinding(() -> StringUtils.isBlank(sourceField.getValue()), sourceField.valueProperty()))
						.or(Bindings.createBooleanBinding(() -> {
							// disables if there exists a bill with the same dateTime and if this editor disallows that
							if (!allowExsitingDateTime.get()) {
								for (Bill testBill : App.userManager.getUser().getBills()) {
									if (datePicker.getValue() == null ||
											timePicker.getValue() == null ||
											testBill.getDateTime().isEqual(LocalDateTime.of(datePicker.getValue(), timePicker.getValue())))
										return true;
								}
							}
							return false;
						}, allowExsitingDateTime, datePicker.valueProperty(), timePicker.valueProperty()))
						.or(articlesNotValid));

		btnAccept.setOnAction(e -> accept());
		btnCancel.setOnAction(e -> cancel());

	}

	private void initArticlesTable() {
		articleNameCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, String>("articleName"));
		articleNameCol.setCellFactory(TextEditingTableCell.forTableColumn());
		articleNameCol.setOnEditCommit(e -> {
			e.getRowValue().setArticleName(e.getNewValue());
		});

		articleAmountCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Integer>("amount"));
		articleAmountCol.setCellFactory(SpinnerTableCell.forTableColumn(new IntegerSpinnerValueFactory(1, Integer.MAX_VALUE)));
		articleAmountCol.setOnEditCommit(e -> {
			e.getRowValue().setAmount(e.getNewValue().intValue());
		});

		articlePriceCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Float>("articlePrice"));
		articlePriceCol.setCellFactory(TextEditingTableCell.forTableColumn(new FloatStringConverter()));
		articlePriceCol.setOnEditCommit(e -> {
			e.getRowValue().setArticlePrice(e.getNewValue() == null ? 0 : Utils.roundDecimal(e.getNewValue().floatValue(), 2));
		});

		articleSubsidyCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Float>("subsidy"));
		articleSubsidyCol.setCellFactory(TextEditingTableCell.forTableColumn(new FloatStringConverter()));
		articleSubsidyCol.setOnEditCommit(e -> {
			e.getRowValue().setSubsidy(e.getNewValue() == null ? 0 : Utils.roundDecimal(e.getNewValue().floatValue(), 2));
		});

		articlesTable.setItems(templateBill.getEntries());
	}

	@Override
	public Stage getStage() {
		return (Stage) datePicker.getScene().getWindow();
	}

	@Override
	public void onShowing() {
		ObservableList<String> existingSources = FXCollections.observableArrayList();
		for (Bill bill : App.userManager.getUser().getBills()) {
			if (!existingSources.contains(bill.getSource())) existingSources.add(bill.getSource());
		}
		sourceField.setItems(existingSources);
		sourceField.setValue(templateBill.getSource());

		LocalDateTime dateTime = templateBill.getDateTime();
		datePicker.setValue(dateTime == null ? null : dateTime.toLocalDate());
		timePicker.setValue(dateTime == null ? null : dateTime.toLocalTime());
	}

	@Override
	public void accept() {
		templateBill.setSource(sourceField.getValue());
		templateBill.setDateTime(LocalDateTime.of(datePicker.getValue(), timePicker.getValue()));

		super.accept();
	}

	@Override
	public void reset() {
		templateBill.setDateTime(null);
		templateBill.setSource("");
		templateBill.clearEntries();
		templateBill.addEntry(null, 0, 1, 0);
	}

	public Bill getBillTemplate() {
		return templateBill;
	}

	public void setAllowExistingDateTime(boolean value) {
		this.allowExsitingDateTime.set(value);
	}

	private ReadOnlyObjectProperty<Bill.Entry> selectedEntryProperty() {
		return articlesTable.getSelectionModel().selectedItemProperty();
	}

}
