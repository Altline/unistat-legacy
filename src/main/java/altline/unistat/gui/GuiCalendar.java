package altline.unistat.gui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.sun.javafx.scene.control.skin.TableHeaderRow;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import altline.unistat.gui.component.BillView;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class GuiCalendar {

	private final HashMap<LocalDate, Float> costMap = new HashMap<>();

	@FXML
	private TableView<Week> calTable;

	@FXML
	private TableColumn<Week, Day> monCol, tueCol, wedCol, thuCol, friCol, satCol, sunCol;

	@FXML
	private Label lblMonth;

	@FXML
	private VBox detailsArea;

	@FXML
	private TitledPane detailsTitle;

	@FXML
	private ListView<Bill> billList;

	@FXML
	private Label lblNoDay;

	private BillView billView;

	@FXML
	private void initialize() {
		setSelectedMonth(YearMonth.now());
		
		App.userManager.getUser().getElementObservableBills()
				.addListener((Change<? extends Bill> c) -> {
					updateCostMap();
					populateCalTable();
				});

		selectedDay.bind(Bindings.createObjectBinding(() -> {
			if (calTable.getSelectionModel().isEmpty()) return null;

			@SuppressWarnings("unchecked")
			TablePosition<Week, Day> selectedPosition = calTable.getSelectionModel().getSelectedCells().get(0);
			return selectedPosition.getTableColumn().getCellData(selectedPosition.getRow());

		}, calTable.getSelectionModel().getSelectedCells()));

		selectedDay.addListener((obs, oldVal, newVal) -> {
			billList.getSelectionModel().select(0);
		});

		lblMonth.textProperty().bind(Bindings.createStringBinding(
				() -> getSelectedMonth().format(UIManager.MONTH_FORMATTER),
				selectedMonth));

		initCalTable();
		initDetailsArea();
		updateCostMap();
	}

	private void initCalTable() {
		calTable.getSelectionModel().setCellSelectionEnabled(true);

		// disables table column reordering
		calTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
			final TableHeaderRow header = (TableHeaderRow) calTable.lookup("TableHeaderRow");
			header.reorderingProperty().addListener((o, oldVal, newVal) -> header.setReordering(false));
		});

		final NumberBinding cellHeight = Bindings.min(Bindings.max(monCol.widthProperty(), 100), calTable.heightProperty().divide(6).subtract(5));
		calTable.fixedCellSizeProperty().bind(cellHeight);

		final Callback<TableColumn<Week, Day>, TableCell<Week, Day>> cellFactory = new Callback<TableColumn<Week, Day>, TableCell<Week, Day>>() {
			@Override
			public TableCell<Week, Day> call(TableColumn<Week, Day> param) {
				return new TableCell<GuiCalendar.Week, GuiCalendar.Day>() {

					private VBox content;
					private Label lblDay;
					private Label lblBills;
					private Label lblCost;

					{
						lblDay = new Label();
						lblDay.setStyle("-fx-font-size: 18; -fx-font-weight: bold");
						lblDay.setMaxWidth(Double.MAX_VALUE);
						lblDay.setPadding(new Insets(0, 0, 0, 8));

						lblBills = new Label();
						lblBills.setStyle("-fx-font-size: 46; -fx-font-weight: bold; -fx-text-fill: #bb93c9");
						lblBills.setPadding(new Insets(-20, 0, -15, 0));
						lblBills.setMaxHeight(Double.MAX_VALUE);
						VBox.setVgrow(lblBills, Priority.ALWAYS);

						lblCost = new Label();

						content = new VBox(lblDay, lblBills, lblCost);
						content.setAlignment(Pos.CENTER);
					}

					@Override
					protected void updateItem(Day item, boolean empty) {
						super.updateItem(item, empty);

						if (empty || item == null) {
							setText(null);
							setGraphic(null);
						} else {
							lblDay.setText(String.valueOf(item.date.getDayOfMonth()));
							lblBills.textProperty().bind(Bindings.createStringBinding(
									() -> item.bills.size() > 0 ? String.valueOf(item.bills.size()) : "",
									item.bills));
							lblCost.textProperty().bind(item.costBinding.asString("%.2f"));

							this.setEffect(null);
							if (item.date.equals(LocalDate.now())) {
								// highlight today

								this.setStyle("-fx-bill-based-background: -fx-today-color");

							} else if (!item.date.getMonth().equals(getSelectedMonth().getMonth())) {
								// gray-out days that are not from the selected month

								this.setStyle("-fx-bill-based-background: white");
								ColorAdjust ca = new ColorAdjust();
								ca.setBrightness(-0.17f);
								this.setEffect(ca);

							} else {
								// paint regular cells based on the cost of the day

								final float cost = item.getCost();
								float peakCost = 0, lowestCost = Float.MAX_VALUE;
								for (Float value : costMap.values()) {
									if (value > peakCost) peakCost = value;
									if (value < lowestCost) lowestCost = value;
								}

								int highlight = peakCost - lowestCost == 0 ? 0 : (int) ((cost - lowestCost) / (peakCost - lowestCost) * 80) + 20;
								if (cost == 0) this.setStyle("-fx-bill-based-background: white");
								else this.setStyle("-fx-bill-based-background: derive(#ffc791, " + (100 - highlight) + "%)");
							}

							setGraphic(content);
						}
					}
				};
			}
		};

		final Callback<CellDataFeatures<Week, Day>, ObservableValue<Day>> cellValueFactory = new Callback<CellDataFeatures<Week, Day>, ObservableValue<Day>>() {
			@Override
			public ObservableValue<Day> call(CellDataFeatures<Week, Day> param) {
				return new ObservableValueBase<Day>() {
					@Override
					public Day getValue() {
						return param.getValue().days[getColumnIndex(param.getTableColumn())];
					}
				};
			}

			private int getColumnIndex(TableColumn<Week, Day> col) {
				if (col.equals(monCol)) return 0;
				if (col.equals(tueCol)) return 1;
				if (col.equals(wedCol)) return 2;
				if (col.equals(thuCol)) return 3;
				if (col.equals(friCol)) return 4;
				if (col.equals(satCol)) return 5;
				if (col.equals(sunCol)) return 6;
				throw new RuntimeException("Unexpected column " + col);
			}
		};

		monCol.setCellFactory(cellFactory);
		tueCol.setCellFactory(cellFactory);
		wedCol.setCellFactory(cellFactory);
		thuCol.setCellFactory(cellFactory);
		friCol.setCellFactory(cellFactory);
		satCol.setCellFactory(cellFactory);
		sunCol.setCellFactory(cellFactory);

		monCol.setCellValueFactory(cellValueFactory);
		tueCol.setCellValueFactory(cellValueFactory);
		wedCol.setCellValueFactory(cellValueFactory);
		thuCol.setCellValueFactory(cellValueFactory);
		friCol.setCellValueFactory(cellValueFactory);
		satCol.setCellValueFactory(cellValueFactory);
		sunCol.setCellValueFactory(cellValueFactory);
	}

	private void initDetailsArea() {
		lblNoDay.visibleProperty().bind(selectedDay.isNull());
		detailsArea.visibleProperty().bind(selectedDay.isNotNull());

		detailsTitle.textProperty().bind(Bindings.createStringBinding(() -> {
			return "Računi  " + (getSelectedDay() == null ? "" : getSelectedDay().date.format(UIManager.DATE_FORMATTER));
		}, selectedDay));

		billList.setCellFactory(cell -> {
			return new ListCell<Bill>() {
				@Override
				protected void updateItem(Bill item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						setText(item.getDateTime().format(UIManager.TIME_FORMATTER) + String.format(" - %.2f", item.getTotalCost()));
					}
				}
			};
		});

		billList.itemsProperty().bind(Bindings.createObjectBinding(
				() -> getSelectedDay() == null ? null : getSelectedDay().bills,
				selectedDay));

		billView = new BillView();
		VBox.setVgrow(billView, Priority.ALWAYS);
		billView.billProperty().bind(selectedBillProperty());
		billView.visibleProperty().bind(selectedBillProperty().isNotNull());
		detailsArea.getChildren().add(billView);
	}


	private void populateCalTable() {
		YearMonth month = getSelectedMonth();
		
		calTable.getItems().clear();

		int startWeek = month.atDay(1).get(WeekFields.ISO.weekOfYear());
		int endWeek = month.atEndOfMonth().get(WeekFields.ISO.weekOfYear());

		for (int i = startWeek; i <= endWeek; i++) {
			Day[] days = new Day[7];

			LocalDate monday = LocalDate.of(month.getYear(), 1, 1)
					.with(WeekFields.ISO.weekOfYear(), i)
					.with(WeekFields.ISO.dayOfWeek(), 1);

			for (int j = 0; j < 7; j++) {
				LocalDate dayDate = monday.plusDays(j);

				ArrayList<Bill> bills = new ArrayList<>();
				for (Bill bill : App.userManager.getUser().getBills()) {
					if (dayDate.equals(bill.getDateTime().toLocalDate())) bills.add(bill);
				}
				days[j] = new Day(dayDate, bills);
			}

			calTable.getItems().add(new Week(days));
		}
	}

	private void updateCostMap() {
		costMap.clear();
		for (Bill bill : App.userManager.getUser().getBills()) {
			costMap.merge(bill.getDateTime().toLocalDate(), bill.getTotalCost(), Float::sum);
		}
	}

	@FXML
	private void nextMonth() {
		setSelectedMonth(getSelectedMonth().plusMonths(1));
	}

	@FXML
	private void prevMonth() {
		setSelectedMonth(getSelectedMonth().minusMonths(1));
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- selectedMonth
	private final ObjectProperty<YearMonth> selectedMonth = new ObjectPropertyBase<YearMonth>() {
		@Override
		protected void invalidated() {
			populateCalTable();
		}

		@Override
		public Object getBean() {
			return GuiCalendar.this;
		}

		@Override
		public String getName() {
			return "selectedMonth";
		}
	};

	private final YearMonth getSelectedMonth() {
		return selectedMonth.get();
	}

	private final void setSelectedMonth(YearMonth value) {
		selectedMonth.set(value);
	}


	// --- selectedDay
	private final ObjectProperty<Day> selectedDay = new SimpleObjectProperty<GuiCalendar.Day>(this, "selectedDay");

	private final Day getSelectedDay() {
		return selectedDay.get();
	}

	private ReadOnlyObjectProperty<Bill> selectedBillProperty() {
		return billList.getSelectionModel().selectedItemProperty();
	}


	/* *************************************************************************
	 *                                                                         *
	 * Inner classes                                                           *
	 *                                                                         *
	 ************************************************************************* */

	private class Week {

		private Day[] days = new Day[7];

		private Week(Day[] days) {
			this.days = days;
		}
	}

	private class Day {

		private final LocalDate date;
		private final ObservableList<Bill> bills;
		private final FloatBinding costBinding;

		private Day(LocalDate date, Collection<Bill> bills) {
			this.date = date;
			this.bills = FXCollections.observableArrayList(bills);
			this.costBinding = new FloatBinding() {
				@Override
				protected float computeValue() {
					float cost = 0;
					for (Bill bill : bills) {
						cost += bill.getTotalCost();
					}
					return cost;
				}
			};
		}

		private float getCost() {
			return costBinding.get();
		}
	}

}
