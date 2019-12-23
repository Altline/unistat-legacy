package altline.unistat.gui.component;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;

import altline.unistat.App;
import altline.unistat.Bill;
import altline.unistat.UIManager;
import altline.utils.Alerts;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class BillView extends VBox {
	
	@FXML
	private Label lblDateTime, lblSource, lblTotalCost, lblTotalSubsidy, lblTotalArticles;

	@FXML
	private TableView<Bill.Entry> articlesTable;

	@FXML
	private TableColumn<Bill.Entry, Integer> articleAmountCol;

	@FXML
	private TableColumn<Bill.Entry, String> articleNameCol;

	@FXML
	private TableColumn<Bill.Entry, Float> articlePriceCol;

	@FXML
	private TableColumn<Bill.Entry, Float> articleSubsidyCol;

	@FXML
	private PieChart costChart;

	public BillView() {
		this(null);
	}

	public BillView(Bill bill) {
		try {
			FXMLLoader loader = new FXMLLoader(App.class.getResource("/gui/BillView.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
			initGui();
		} catch (IOException e) {
			Alerts.catching("Could not load FXML", e, LogManager.getLogger());
		}
		
		setBill(bill);
	}

	private void initGui() {
		setPadding(new Insets(8));
		
		articleNameCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, String>("articleName"));
		articleAmountCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Integer>("amount"));
		articlePriceCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Float>("articlePrice"));
		articleSubsidyCol.setCellValueFactory(new PropertyValueFactory<Bill.Entry, Float>("subsidy"));

		bill.addListener((obs, oldVal, newVal) -> {
			if (newVal == null) {
				lblDateTime.textProperty().unbind();
				lblSource.textProperty().unbind();
				lblTotalCost.textProperty().unbind();
				lblTotalSubsidy.textProperty().unbind();
				lblTotalArticles.textProperty().unbind();
				articlesTable.setItems(null);
				costChart.getData().clear();

			} else {
				lblDateTime.textProperty().bind(Bindings.createStringBinding(() -> {
					return "Vrijeme izdaje:  " + newVal.getDateTime().format(UIManager.DATE_TIME_FORMATTER);
				}, newVal.dateTimeProperty()));
				lblSource.textProperty().bind(Bindings.format("Mjesto izdaje:  %s", newVal.sourceProperty()));
				lblTotalCost.textProperty().bind(newVal.totalCostBinding().asString("Iznos računa:  %.2f"));
				lblTotalSubsidy.textProperty().bind(newVal.totalSubsidyBinding().asString("Iznos subvencije:  %.2f"));
				lblTotalArticles.textProperty().bind(newVal.totalArticlesBinding().asString("Broj artikala:  %d"));

				SortedList<Bill.Entry> sortedList = new SortedList<Bill.Entry>(newVal.getEntries());
				sortedList.comparatorProperty().bind(articlesTable.comparatorProperty());
				articlesTable.setItems(sortedList);

				costChart.getData().clear();
				newVal.getEntries().stream()
						.sorted((o1, o2) -> {
							float cost1 = o1.getArticlePrice() * o1.getAmount();
							float cost2 = o2.getArticlePrice() * o2.getAmount();
							return cost1 < cost2 ? 1 : cost1 > cost2 ? -1 : 0;
						}).forEach(entry -> {
							costChart.getData().add(new PieChart.Data(entry.getArticleName(), entry.getArticlePrice() * entry.getAmount()));
						});
			}
		});
	}


	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- bill
	private ObjectProperty<Bill> bill = new SimpleObjectProperty<Bill>(this, "bill");

	public final ObjectProperty<Bill> billProperty() {
		return bill;
	}

	public final Bill getBill() {
		return bill.get();
	}

	public final void setBill(Bill value) {
		bill.set(value);
	}

}
