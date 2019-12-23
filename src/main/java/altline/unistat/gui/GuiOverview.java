package altline.unistat.gui;

import java.time.LocalDate;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import altline.unistat.User;
import altline.unistat.gui.component.BillSummary;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class GuiOverview {

	private LineChart<Number, Number> dailySpendingChart;
	private LineChart<Number, Number> totalSpendingChart;

	@FXML
	private VBox content;

	@FXML
	private HBox billSummaryArea;
	
	@FXML
	private Label lblAvailableFunds;

	@FXML
	private void initialize() {
		User user = App.userManager.getUser();

		lblAvailableFunds.textProperty().bind(user.availableFundsProperty().asString("%.2f"));
		
		BillSummary todaySummary = new BillSummary(
				user.getBills()
						.filtered(bill -> bill.getDateTime().toLocalDate().equals(LocalDate.now())),
				"Danas");
		BillSummary yesterdaySummary = new BillSummary(
				user.getBills()
						.filtered(bill -> bill.getDateTime().toLocalDate().equals(LocalDate.now().minusDays(1))),
				"Jučer");
		BillSummary thisWeekSummary = new BillSummary(
				user.getBills()
						.filtered(bill -> bill.getDateTime().toLocalDate().isAfter(LocalDate.now().minusWeeks(1))),
				"Ovaj tjedan");
		BillSummary thisMonthSummary = new BillSummary(
				user.getBills()
						.filtered(bill -> bill.getDateTime().toLocalDate().isAfter(LocalDate.now().minusMonths(1))),
				"Ovaj mjesec");
		BillSummary overallSummary = new BillSummary(user.getBills(), "Ukupno");

		billSummaryArea.getChildren().addAll(
				todaySummary, new Separator(Orientation.VERTICAL),
				yesterdaySummary, new Separator(Orientation.VERTICAL),
				thisWeekSummary, new Separator(Orientation.VERTICAL),
				thisMonthSummary, new Separator(Orientation.VERTICAL),
				overallSummary, new Separator(Orientation.VERTICAL));

		// Daily spending chart
		{
			final NumberAxis xAxis = new NumberAxis();
			final NumberAxis yAxis = new NumberAxis();
			xAxis.setForceZeroInRange(false);
			xAxis.setTickLabelFormatter(new StringConverter<Number>() {
				@Override
				public String toString(Number object) {
					return LocalDate.ofEpochDay(object.longValue()).format(UIManager.DATE_FORMATTER);
				}

				@Override
				public Number fromString(String string) {
					return LocalDate.parse(string, UIManager.DATE_FORMATTER).toEpochDay();
				}
			});

			dailySpendingChart = new LineChart<Number, Number>(xAxis, yAxis);
			dailySpendingChart.setTitle("Dnevna potrošnja");
		}

		// Total spending chart
		{
			final NumberAxis xAxis = new NumberAxis();
			final NumberAxis yAxis = new NumberAxis();
			xAxis.setForceZeroInRange(false);
			xAxis.setTickLabelFormatter(new StringConverter<Number>() {
				@Override
				public String toString(Number object) {
					return LocalDate.ofEpochDay(object.longValue()).format(UIManager.DATE_FORMATTER);
				}

				@Override
				public Number fromString(String string) {
					return LocalDate.parse(string, UIManager.DATE_FORMATTER).toEpochDay();
				}
			});

			totalSpendingChart = new LineChart<Number, Number>(xAxis, yAxis);
			totalSpendingChart.setTitle("Ukupna potrošnja");
			totalSpendingChart.setCreateSymbols(false);
		}

		content.getChildren().addAll(dailySpendingChart, totalSpendingChart);

		App.userManager.getUser().getElementObservableBills()
				.addListener((Change<? extends Bill> c) -> {
					Platform.runLater(() -> populateCharts());
				});

		populateCharts();
	}

	private void populateCharts() {
		ObservableList<Bill> bills = App.userManager.getUser().getBills();
		if(bills.isEmpty()) return;

		// Daily spending chart
		{
			final XYChart.Series<Number, Number> costSeries = new XYChart.Series<Number, Number>();
			final XYChart.Series<Number, Number> subsidySeries = new XYChart.Series<Number, Number>();
			costSeries.setName("Vrijednost računa");
			subsidySeries.setName("Potrošnja subvencije");
			
			for (LocalDate i = LocalDate.now().minusMonths(1); i.isBefore(LocalDate.now().plusDays(1)); i = i.plusDays(1)) {
				final LocalDate date = i;

				float cost = (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalCost())
						.sum();

				float subsidy = (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalSubsidy())
						.sum();

				costSeries.getData().add(new XYChart.Data<Number, Number>(i.toEpochDay(), cost));
				subsidySeries.getData().add(new XYChart.Data<Number, Number>(i.toEpochDay(), subsidy));
			}
			
			dailySpendingChart.getData().clear();
			dailySpendingChart.getData().add(costSeries);
			dailySpendingChart.getData().add(subsidySeries);
		}

		// Total spending chart
		{
			final XYChart.Series<Number, Number> costSeries = new XYChart.Series<Number, Number>();
			final XYChart.Series<Number, Number> subsidySeries = new XYChart.Series<Number, Number>();
			costSeries.setName("Vrijednost računa");
			subsidySeries.setName("Potrošeno subvencije");
			
			float cost = 0, subsidy = 0;
			for (LocalDate i = bills.get(0).getDateTime().toLocalDate(); i.isBefore(LocalDate.now().plusDays(1)); i = i.plusDays(1)) {
				final LocalDate date = i;

				cost += (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalCost())
						.sum();

				subsidy += (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalSubsidy())
						.sum();

				costSeries.getData().add(new XYChart.Data<Number, Number>(i.toEpochDay(), cost));
				subsidySeries.getData().add(new XYChart.Data<Number, Number>(i.toEpochDay(), subsidy));
			}
			
			totalSpendingChart.getData().clear();
			totalSpendingChart.getData().add(costSeries);
			totalSpendingChart.getData().add(subsidySeries);
		}
	}

}
