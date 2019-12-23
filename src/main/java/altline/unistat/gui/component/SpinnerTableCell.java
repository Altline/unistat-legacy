package altline.unistat.gui.component;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A TableCell that is edited through a Spinner.
 *
 * @param <S> The type of the TableView generic type (i.e. S == TableView&lt;S&gt;). This should also match with the first generic type in TableColumn.
 * @param <T> The type of the item contained within the Cell.
 */
public class SpinnerTableCell<S, T> extends TableCell<S, T> {

	/**
	 * Provides a {@link Spinner} that allows editing of the cell content when the cell is double-clicked, or when
	 * {@link TableView#edit(int, javafx.scene.control.TableColumn)} is called. This method will only work on
	 * {@link TableColumn} instances which are of type Integer. The provided Spinner is the default Spinner of the
	 * SpinnerTableCell.
	 * 
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables editing of the content.
	 */
	public static <S> Callback<TableColumn<S, Integer>, TableCell<S, Integer>> forTableColumn() {
		return list -> new SpinnerTableCell<S, Integer>();
	}

	/**
	 * Provides a {@link Spinner} that allows editing of the cell content when the cell is double-clicked, or when
	 * {@link TableView#edit(int, javafx.scene.control.TableColumn) } is called. This method will work on any
	 * {@link TableColumn} instance, regardless of its generic type. However, to enable this, a {@link StringConverter}
	 * must be provided that will convert the given String (from what the user "spinned" or typed in) into an instance
	 * of type T. This item will then be passed along to the {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter} that can convert the given String (from what the user "spinned" or
	 *            typed in) into an instance of type T.
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables editing of the content.
	 */
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
			final SpinnerValueFactory<T> valueFactory) {
		return list -> new SpinnerTableCell<S, T>(valueFactory);
	}


	private Spinner<T> spinner;
	private SpinnerValueFactory<T> valueFactory;

	/**
	 * Creates a SpinnerTableCell with a {@link SpinnerValueFactory} that has an integer range going from
	 * {@link Integer#MIN_VALUE} to {@link Integer#MAX_VALUE} with a step of 1.
	 */
	public SpinnerTableCell() {
		this(Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
	}

	/**
	 * Creates a SpinnerTableCell with a {@link SpinnerValueFactory} initialized with the specified arguments and a step
	 * of 1.
	 * @param min The minimum allowed integer value for the Spinner
	 * @param max The maximum allowed integer value for the Spinner
	 */
	public SpinnerTableCell(int min, int max) {
		this(min, max, 1);
	}

	/**
	 * Creates a SpinnerTableCell with a {@link SpinnerValueFactory} initialized with the specified arguments and a step
	 * of 1.
	 * @param min The minimum allowed double value for the Spinner
	 * @param max The maximum allowed double value for the Spinner
	 */
	public SpinnerTableCell(double min, double max) {
		this(min, max, 1);
	}

	/**
	 * Creates a SpinnerTableCell with a {@link SpinnerValueFactory} initialized with the specified arguments.
	 * @param min The minimum allowed integer value for the Spinner
	 * @param max The maximum allowed integer value for the Spinner
	 * @param amountToStepBy The amount to increment or decrement by, per step.
	 */
	@SuppressWarnings("unchecked")
	public SpinnerTableCell(int min, int max, int amountToStepBy) {
		this((SpinnerValueFactory<T>) new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, min, amountToStepBy));
	}

	/**
	 * Creates a SpinnerTableCell with a {@link SpinnerValueFactory} initialized with the specified arguments.
	 * @param min The minimum allowed double value for the Spinner
	 * @param max The maximum allowed double value for the Spinner
	 * @param amountToStepBy The amount to increment or decrement by, per step.
	 */
	@SuppressWarnings("unchecked")
	public SpinnerTableCell(double min, double max, double amountToStepBy) {
		this((SpinnerValueFactory<T>) new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, amountToStepBy));
	}

	/**
	 * Creates a SpinnerTableCell with the specified {@link SpinnerValueFactory}
	 * @param valueFactory The {@link SpinnerValueFactory} that the {@link Spinner} will utilize
	 */
	public SpinnerTableCell(SpinnerValueFactory<T> valueFactory) {
		this.getStyleClass().add("text-field-table-cell"); // hmm
		this.valueFactory = valueFactory;
	}


	@Override
	public void startEdit() {
		if (!isEditable()
				|| !getTableView().isEditable()
				|| !getTableColumn().isEditable()) {
			return;
		}
		super.startEdit();

		if (isEditing()) {
			if (spinner == null) spinner = createSpinner();
			else valueFactory.setValue(getItem());

			setText(null);
			setGraphic(spinner);
			spinner.requestFocus();
			if (isSpinnerEditable()) spinner.getEditor().selectAll();
		}
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		setText(getString());
		setGraphic(null);
	}

	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);

		if (isEmpty()) {
			setText(null);
			setGraphic(null);
		} else {
			if (isEditing()) {
				if (valueFactory != null) {
					valueFactory.setValue(getItem());
				}
				setText(null);
				setGraphic(spinner);
			} else {
				setText(getString());
				setGraphic(null);
			}
		}
	}

	// Taken from:
	// https://stackoverflow.com/questions/23632884/how-to-commit-when-clicking-outside-an-editable-tableview-cell-in-javafx
	// Without this, cell edits will not commit if the user clicks on a different non-empty row of the TableView
	@Override
	public void commitEdit(T item) {
		// This block is necessary to support commit on losing focus, because
		// the baked-in mechanism sets our editing state to false before we can
		// intercept the loss of focus. The default commitEdit(...) method
		// simply bails if we are not editing...
		if (!isEditing() && !Objects.equals(item, getItem())) {
			TableView<S> table = getTableView();
			if (table != null) {
				TableColumn<S, T> column = getTableColumn();
				CellEditEvent<S, T> event = new CellEditEvent<>(
						table, new TablePosition<S, T>(table, getIndex(), column),
						TableColumn.editCommitEvent(), item);
				Event.fireEvent(column, event);
			}
		}

		super.commitEdit(item);
	}

	private StringConverter<T> getConverter() {
		return valueFactory.getConverter();
	}

	private String getString() {
		return getConverter() == null ? //
				getItem() == null ? "" : getItem().toString() : //
				getConverter().toString(getItem());
	}

	private Spinner<T> createSpinner() {
		valueFactory.setValue(getItem());
		final Spinner<T> spinner = new Spinner<T>(valueFactory);
		spinner.editableProperty().bind(spinnerEditable);
		spinner.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
		spinner.focusedProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == false) {
				if (getConverter() == null) throw new IllegalStateException("String converter is null");

				if (spinner.isEditable()) {
					final T editedVal = getConverter().fromString(spinner.getEditor().getText());
					if (editedVal != null && !editedVal.equals(valueFactory.getValue()))
						valueFactory.setValue(editedVal);
				}

				commitEdit(valueFactory.getValue());
			}
		});

		spinner.getEditor().setOnAction(event -> {
			if (getConverter() == null) throw new IllegalStateException("String converter is null");

			valueFactory.setValue(getConverter().fromString(spinner.getEditor().getText()));
			commitEdit(valueFactory.getValue());
			event.consume();
		});

		return spinner;
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- spinnerEditable
	private BooleanProperty spinnerEditable = new SimpleBooleanProperty(this, "spinnerEditable");

	/**
	 * Determines whether the user can directly edit text inside the table cell's Spinner.
	 */
	public final BooleanProperty spinnerEditableProperty() {
		return spinnerEditable;
	}
	public final void setSpinnerEditable(boolean value) {
		spinnerEditableProperty().set(value);
	}

	public final boolean isSpinnerEditable() {
		return spinnerEditableProperty().get();
	}

}
