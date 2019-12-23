package altline.unistat.gui.component;

import altline.unistat.Bill;
import altline.unistat.util.FXUtils;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class BillSummary extends GridPane {

	public BillSummary() {
		this(null, null);
	}

	public BillSummary(ObservableList<Bill> billList) {
		this(billList, null);
	}

	public BillSummary(String title) {
		this(null, title);
	}

	public BillSummary(ObservableList<Bill> billList, String title) {
		if (billList != null) setBills(billList);
		if (title != null) setTitle(title);

		initGUI();
	}

	private void initGUI() {
		setPrefWidth(180);
		setPadding(new Insets(10));

		final ColumnConstraints col1 = new ColumnConstraints();
		col1.setHgrow(Priority.ALWAYS);
		final ColumnConstraints col2 = new ColumnConstraints();
		col2.setHalignment(HPos.RIGHT);
		getColumnConstraints().addAll(col1, col2);
		

		final Label lblTitle = new Label();
		lblTitle.getStyleClass().add("title");
		lblTitle.setMaxWidth(Double.POSITIVE_INFINITY);
		lblTitle.setAlignment(Pos.CENTER);
		lblTitle.textProperty().bind(title);

		final Label lblBills = new Label();
		lblBills.textProperty().bind(Bindings.size(bills).asString());

		final Label lblArticles = new Label();
		lblArticles.textProperty().bind(FXUtils.createListElementBinding(() -> {
			int sum = 0;
			for (Bill bill : getBills()) {
				sum += bill.getTotalArticles();
			}
			return String.valueOf(sum);
		}, bills, e -> new Observable[] { e.totalArticlesBinding() }));

		final Label lblCost = new Label();
		lblCost.textProperty().bind(FXUtils.createListElementBinding(() -> {
			float sum = 0;
			for (Bill bill : getBills()) {
				sum += bill.getTotalCost();
			}
			return String.format("%.2f", sum);
		}, bills, e -> new Observable[] { e.totalCostBinding() }));

		final Label lblSubsidy = new Label();
		lblSubsidy.textProperty().bind(FXUtils.createListElementBinding(() -> {
			float sum = 0;
			for (Bill bill : getBills()) {
				sum += bill.getTotalSubsidy();
			}
			return String.format("%.2f", sum);
		}, bills, e -> new Observable[] { e.totalSubsidyBinding() }));
		

		add(lblTitle, 0, 0, 2, 1);
		addRow(1, new Label("Računi: "), lblBills);
		addRow(2, new Label("Artikli: "), lblArticles);
		addRow(3, new Label("Vrijednost: "), lblCost);
		addRow(4, new Label("Subvencija: "), lblSubsidy);
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- bills
	/**
	 * The list of bills to summarize.
	 */
	private ListProperty<Bill> bills = new SimpleListProperty<Bill>(this, "bills", FXCollections.observableArrayList());

	public final ListProperty<Bill> billsProperty() {
		return bills;
	}

	public final ObservableList<Bill> getBills() {
		return bills.get();
	}

	public final void setBills(ObservableList<Bill> value) {
		bills.set(value);
	}


	// --- title
	public static final String DEFAULT_TITLE = "Statistika";
	/**
	 * Text to show as the title of this summary.
	 */
	private StringProperty title = new SimpleStringProperty(this, "title", DEFAULT_TITLE);

	public final StringProperty titleProperty() {
		return title;
	}

	public final String getTitle() {
		return title.get();
	}

	public final void setTitle(String value) {
		title.set(value);
	}

}
