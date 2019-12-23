package altline.unistat.gui;

import java.util.Arrays;
import java.util.HashMap;

import altline.unistat.App;
import altline.unistat.Bill;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

public class GuiArticleStats {

	private final ObservableList<ArticleSummation> distinctArticles = FXCollections.observableArrayList();

	@FXML
	private TableView<ArticleSummation> articlesTable;

	@FXML
	private TableColumn<ArticleSummation, String> articleNameCol;

	@FXML
	private TableColumn<ArticleSummation, Number> articleAmountCol;

	@FXML
	private StackedBarChart<String, Number> articleCostChart;

	@FXML
	private BarChart<String, Number> articleAmountChart;

	@FXML
	private PieChart sourceChart;

	@FXML
	private Label lblAmount, lblCost, lblNoSubsidyCost, lblSubsidy;

	@FXML
	private void initialize() {
		selectedArticleProperty().addListener((obs, oldVal, newVal) -> {
			populateArticleDetails(newVal);

			for (Series<String, Number> series : articleCostChart.getData()) {
				for (XYChart.Data<String, Number> data : series.getData()) {

					if (oldVal != null && oldVal.name.get().equals(data.getXValue())) {
						data.getNode().setStyle(null);
					}

					if (newVal != null && newVal.name.get().equals(data.getXValue())) {
						data.getNode().setStyle("-fx-bar-fill: -fx-selected-bar");
					}
				}
			}
			for (Series<String, Number> series : articleAmountChart.getData()) {
				for (XYChart.Data<String, Number> data : series.getData()) {

					if (oldVal != null && oldVal.name.get().equals(data.getXValue())) {
						data.getNode().setStyle(null);
					}

					if (newVal != null && newVal.name.get().equals(data.getXValue())) {
						data.getNode().setStyle("-fx-bar-fill: -fx-selected-bar");
					}
				}
			}

		});

		articleNameCol.setCellValueFactory(p -> p.getValue().name);
		articleAmountCol.setCellValueFactory(p -> p.getValue().amount);

		SortedList<ArticleSummation> sortedList = new SortedList<>(distinctArticles);
		sortedList.comparatorProperty().bind(articlesTable.comparatorProperty());
		articlesTable.setItems(sortedList);

		articleAmountCol.setSortType(SortType.DESCENDING);
		articlesTable.getSortOrder().setAll(Arrays.asList(articleAmountCol, articleNameCol));

		// Binding chart data with EasyBind. Bar charts break when data is modified through this so I update them by
		// adding brand new Series
		/*ObservableList<XYChart.Data<String, Number>> paidData = EasyBind.map(distinctArticles, 
		        article -> new XYChart.Data<>(article.name.get(), article.cost.subtract(article.subsidy).getValue()));
		
		ObservableList<XYChart.Data<String, Number>> subsidyData = EasyBind.map(distinctArticles, 
		        article -> new XYChart.Data<>(article.name.get(), article.subsidy.get()));
		
		ObservableList<XYChart.Data<String, Number>> amountData = EasyBind.map(distinctArticles, 
		        article -> new XYChart.Data<>(article.name.get(), article.amount.get()));
		
		articleCostChart.getData().add(new Series<String, Number>("Vrijednost bez subvencije", paidData));
		articleCostChart.getData().add(new Series<String, Number>("Iznos subvencije", subsidyData));
		articleAmountChart.getData().add(new Series<String, Number>("Količina artikala", amountData));*/

		articleCostChart.setStyle("CHART_COLOR_1: CHART_COLOR_3 ; CHART_COLOR_2: CHART_COLOR_4 ;"); // #2fb12f #55a9f1
		articleAmountChart.setStyle("CHART_COLOR_1: CHART_COLOR_2;");
		sourceChart.setStartAngle(150);

		App.userManager.getUser().getElementObservableBills()
				.addListener((Change<? extends Bill> c) -> {
					Platform.runLater(() -> populate());
				});

		populate();
	}

	private void populate() {
		distinctArticles.clear();
		for (Bill bill : App.userManager.getUser().getBills()) {
			for (Bill.Entry entry : bill.getEntries()) {

				boolean alreadyAdded = false;
				for (ArticleSummation distinctArticle : distinctArticles) {
					if (entry.getArticleName().equals(distinctArticle.name.get())) {
						alreadyAdded = true;
						break;
					}
				}
				if (!alreadyAdded) {
					distinctArticles.add(new ArticleSummation(entry.getArticleName()));
				}
			}
		}
		distinctArticles.sort((o1, o2) -> {
			return o1.cost.get() > o2.cost.get() ? 1 : o1.cost.get() < o2.cost.get() ? -1 : 0;
		});

		populateCharts();
		populateArticleDetails(getSelectedArticle());
	}

	private void populateCharts() {
		XYChart.Series<String, Number> paidSeries = new XYChart.Series<>();
		XYChart.Series<String, Number> subsidySeries = new XYChart.Series<>();
		XYChart.Series<String, Number> amountSeries = new XYChart.Series<>();
		paidSeries.setName("Vrijednost bez subvencije");
		subsidySeries.setName("Iznos subvencije");
		amountSeries.setName("Količina artikala");

		for (ArticleSummation article : distinctArticles) {
			String name = article.name.get();
			float cost = article.cost.get();
			float subsidy = article.subsidy.get();
			int amount = article.amount.get();
			paidSeries.getData().add(new XYChart.Data<String, Number>(name, cost - subsidy));
			subsidySeries.getData().add(new XYChart.Data<String, Number>(name, subsidy));
			amountSeries.getData().add(new XYChart.Data<String, Number>(name, amount));
		}

		articleCostChart.getData().clear();
		articleAmountChart.getData().clear();

		articleCostChart.getData().add(paidSeries);
		articleCostChart.getData().add(subsidySeries);
		articleAmountChart.getData().add(amountSeries);

		applyChartHoverHandler(articleCostChart);
		applyChartHoverHandler(articleAmountChart);
	}

	private void populateArticleDetails(ArticleSummation article) {
		if (article == null) {
			lblAmount.textProperty().unbind();
			lblCost.textProperty().unbind();
			lblNoSubsidyCost.textProperty().unbind();
			lblSubsidy.textProperty().unbind();

			lblAmount.setText("0");
			lblCost.setText("0,00");
			lblNoSubsidyCost.setText("0,00");
			lblSubsidy.setText("0,00");

			sourceChart.setTitle("Odaberi artikl");
			sourceChart.getData().clear();
			return;
		}

		lblAmount.textProperty().bind(article.amount.asString());
		lblCost.textProperty().bind(article.cost.asString("%.2f"));
		lblNoSubsidyCost.textProperty().bind(article.cost.subtract(article.subsidy).asString("%.2f"));
		lblSubsidy.textProperty().bind(article.subsidy.asString("%.2f"));

		HashMap<String, Integer> amountMap = new HashMap<>();
		for (Bill bill : App.userManager.getUser().getBills()) {
			for (Bill.Entry entry : bill.getEntries()) {

				if (article.name.get().equals(entry.getArticleName())) {
					amountMap.merge(bill.getSource(), entry.getAmount(), Integer::sum);
				}
			}
		}

		sourceChart.getData().clear();
		amountMap.keySet().stream().sorted().forEach(source -> {
			sourceChart.getData().add(new PieChart.Data(source, amountMap.get(source)));
		});
		sourceChart.setTitle(article.name.get());
	}

	private <X, Y> void applyChartHoverHandler(XYChart<X, Y> barChart) {
		for (Series<X, Y> series : barChart.getData()) {
			for (XYChart.Data<X, Y> item : series.getData()) {


				EventHandler<MouseEvent> hoverHandler = new EventHandler<MouseEvent>() {
					private String originalStyle;
					Tooltip tooltip;
					{
						tooltip = new Tooltip();
						tooltip.setText(item.getXValue().toString());						
					}
					@Override
					public void handle(MouseEvent e) {
						if (e.getEventType().equals(MouseEvent.MOUSE_ENTERED)) {
							
							if (getSelectedArticle() == null || !getSelectedArticle().name.get().equals(item.getXValue())) {
								originalStyle = item.getNode().getStyle();
								item.getNode().setStyle("-fx-bar-fill: #777;");
							}
							tooltip.show(item.getNode(), e.getScreenX() + 10, e.getScreenY() + 10);

						} else if (e.getEventType().equals(MouseEvent.MOUSE_EXITED)) {
							
							if (getSelectedArticle() == null || !getSelectedArticle().name.get().equals(item.getXValue())) {
								item.getNode().setStyle(originalStyle);
							}
							tooltip.hide();

						} else if (e.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
							articlesTable.getSelectionModel().select(getArticleByName(item.getXValue().toString()));
						}
					}
				};

				item.getNode().setOnMouseEntered(hoverHandler);
				item.getNode().setOnMouseExited(hoverHandler);
				item.getNode().setOnMousePressed(hoverHandler);
			}
		}
	}

	private ArticleSummation getArticleByName(String articleName) {
		for (ArticleSummation article : distinctArticles) {
			if (articleName.equals(article.name.get())) return article;
		}
		return null;
	}

	private ReadOnlyObjectProperty<ArticleSummation> selectedArticleProperty() {
		return articlesTable.getSelectionModel().selectedItemProperty();
	}

	private ArticleSummation getSelectedArticle() {
		return selectedArticleProperty().get();
	}

	private class ArticleSummation {
		StringProperty name = new SimpleStringProperty();
		FloatProperty cost = new SimpleFloatProperty();
		FloatProperty subsidy = new SimpleFloatProperty();
		IntegerProperty amount = new SimpleIntegerProperty();

		ArticleSummation(String articleName) {
			name.set(articleName);
			for (Bill bill : App.userManager.getUser().getBills()) {
				for (Bill.Entry entry : bill.getEntries()) {

					if (articleName.equals(entry.getArticleName())) {
						cost.set(cost.get() + entry.getArticlePrice() * entry.getAmount());
						subsidy.set(subsidy.get() + entry.getSubsidy());
						amount.set(amount.get() + entry.getAmount());
					}
				}
			}
		}
	}

}
