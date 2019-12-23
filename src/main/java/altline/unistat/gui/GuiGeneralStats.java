package altline.unistat.gui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GuiGeneralStats {

	private LineChart<String, Number> costTrendChart;
	private StackedBarChart<String, Number> dailyCostChart;
	private StackedBarChart<String, Number> monthlyCostChart;
	private PieChart sourceChart;
	private StackedBarChart<String, Number> costPerSourceChart;

	@FXML
	private VBox content;

	@FXML
	private void initialize() {

		// Cost trend chart
		{
			final CategoryAxis xAxis = new CategoryAxis();
			final NumberAxis yAxis = new NumberAxis();
			costTrendChart = new LineChart<String, Number>(xAxis, yAxis);
			costTrendChart.setTitle("Trend iznosa računa");
			costTrendChart.setMinHeight(360);
			costTrendChart.setLegendVisible(false);
			costTrendChart.setCreateSymbols(false);
		}

		// Daily cost chart
		{
			final CategoryAxis xAxis = new CategoryAxis();
			final NumberAxis yAxis = new NumberAxis();
			dailyCostChart = new StackedBarChart<String, Number>(xAxis, yAxis);
			dailyCostChart.setTitle("Dnevni iznos računa");
			dailyCostChart.setMinHeight(360);
			dailyCostChart.setCategoryGap(1);

		}

		// Monthly cost chart
		{
			final CategoryAxis xAxis = new CategoryAxis();
			final NumberAxis yAxis = new NumberAxis();
			monthlyCostChart = new StackedBarChart<String, Number>(xAxis, yAxis);
			monthlyCostChart.setTitle("Mjesečni iznos računa");
			monthlyCostChart.setMinHeight(360);
		}

		// Source chart
		{
			sourceChart = new PieChart();
			sourceChart.setTitle("Količina po blagajni");
			sourceChart.setStartAngle(150);
			HBox.setHgrow(sourceChart, Priority.SOMETIMES);
		}

		// Cost per source chart
		{
			final CategoryAxis xAxis = new CategoryAxis();
			final NumberAxis yAxis = new NumberAxis();
			costPerSourceChart = new StackedBarChart<String, Number>(xAxis, yAxis);
			costPerSourceChart.setTitle("Vrijednost po blagajni");
			costPerSourceChart.setStyle("CHART_COLOR_1: CHART_COLOR_3 ; CHART_COLOR_2: CHART_COLOR_4 ;");
			HBox.setHgrow(costPerSourceChart, Priority.SOMETIMES);
		}

		final HBox sourceChartsBox = new HBox(sourceChart, costPerSourceChart);
		sourceChartsBox.setMinHeight(500);

		content.getChildren().addAll(costTrendChart, dailyCostChart, monthlyCostChart, sourceChartsBox);

		App.userManager.getUser().getElementObservableBills()
				.addListener((Change<? extends Bill> c) -> {
					Platform.runLater(() -> populateCharts());
				});
		
		populateCharts();
	}

	private void populateCharts() {
		ObservableList<Bill> bills = App.userManager.getUser().getBills();
		if(bills.isEmpty()) return;

		// Cost trend chart
		{
			final XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
			for (Bill bill : bills) {
				series.getData().add(new XYChart.Data<String, Number>(
						bill.getDateTime().format(UIManager.DATE_TIME_FORMATTER),
						bill.getTotalCost()));
			}
			costTrendChart.getData().clear();
			costTrendChart.getData().add(series);
		}

		// Daily cost chart
		{
			final XYChart.Series<String, Number> paidSeries = new XYChart.Series<String, Number>();
			final XYChart.Series<String, Number> subsidySeries = new XYChart.Series<String, Number>();
			paidSeries.setName("Vrijednost bez subvencije");
			subsidySeries.setName("Iznos subvencije");

			for (LocalDate i = bills.get(0).getDateTime().toLocalDate(); i.isBefore(LocalDate.now().plusDays(1)); i = i.plusDays(1)) {
				final LocalDate date = i;

				float cost = (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalCost())
						.sum();

				float subsidy = (float) bills.stream()
						.filter(bill -> date.equals(bill.getDateTime().toLocalDate()))
						.mapToDouble(bill -> bill.getTotalSubsidy())
						.sum();

				paidSeries.getData().add(new XYChart.Data<String, Number>(i.format(UIManager.DATE_FORMATTER), cost - subsidy));
				subsidySeries.getData().add(new XYChart.Data<String, Number>(i.format(UIManager.DATE_FORMATTER), subsidy));
			}

			dailyCostChart.getData().clear();
			dailyCostChart.getData().add(paidSeries);
			dailyCostChart.getData().add(subsidySeries);
		}

		// Monthly cost chart
		{
			final XYChart.Series<String, Number> paidSeries = new XYChart.Series<String, Number>();
			final XYChart.Series<String, Number> subsidySeries = new XYChart.Series<String, Number>();
			paidSeries.setName("Vrijednost bez subvencije");
			subsidySeries.setName("Iznos subvencije");

			for (LocalDate i = bills.get(0).getDateTime().toLocalDate(); i.isBefore(LocalDate.now().plusDays(1)); i = i.plusMonths(1)) {
				final YearMonth yearMonth = YearMonth.from(i);

				float cost = (float) bills.stream()
						.filter(bill -> yearMonth.equals(YearMonth.from(bill.getDateTime())))
						.mapToDouble(bill -> bill.getTotalCost())
						.sum();

				float subsidy = (float) bills.stream()
						.filter(bill -> yearMonth.equals(YearMonth.from(bill.getDateTime())))
						.mapToDouble(bill -> bill.getTotalSubsidy())
						.sum();

				paidSeries.getData().add(new XYChart.Data<String, Number>(i.format(UIManager.MONTH_FORMATTER), cost - subsidy));
				subsidySeries.getData().add(new XYChart.Data<String, Number>(i.format(UIManager.MONTH_FORMATTER), subsidy));
			}

			monthlyCostChart.getData().clear();
			monthlyCostChart.getData().add(paidSeries);
			monthlyCostChart.getData().add(subsidySeries);
		}

		// Source chart
		{
			sourceChart.getData().clear();

			HashMap<String, Integer> amountMap = new HashMap<>();
			for (Bill bill : App.userManager.getUser().getBills()) {
				amountMap.merge(bill.getSource(), bill.getTotalArticles(), Integer::sum);
			}

			amountMap.keySet().stream().sorted().forEach(source -> {
				sourceChart.getData().add(new PieChart.Data(source, amountMap.get(source)));
			});
		}

		// Cost per source chart
		{
			final XYChart.Series<String, Number> paidSeries = new XYChart.Series<String, Number>();
			final XYChart.Series<String, Number> subsidySeries = new XYChart.Series<String, Number>();
			paidSeries.setName("Vrijednost bez subvencije");
			subsidySeries.setName("Iznos subvencije");

			HashMap<String, Float> paidMap = new HashMap<>();
			HashMap<String, Float> subsidyMap = new HashMap<>();

			for (Bill bill : App.userManager.getUser().getBills()) {
				paidMap.merge(bill.getSource(), bill.getTotalCost() - bill.getTotalSubsidy(), Float::sum);
				subsidyMap.merge(bill.getSource(), bill.getTotalSubsidy(), Float::sum);
			}

			paidMap.keySet().stream().sorted().forEach(source -> {
				paidSeries.getData().add(new XYChart.Data<String, Number>(source, paidMap.get(source)));
				subsidySeries.getData().add(new XYChart.Data<String, Number>(source, subsidyMap.get(source)));
			});

			costPerSourceChart.getData().clear();
			costPerSourceChart.getData().add(paidSeries);
			costPerSourceChart.getData().add(subsidySeries);
		}
	}

}
