package altline.unistat.gui.component;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * This class almost completely replicates the {@link TextFieldTableCell} class but it adds the ability of committing
 * user's editing when the cell looses focus instead of only when Enter is pressed.
 * @param <T> The type of the elements contained within the TableColumn.
 */
public class TextEditingTableCell<S, T> extends TableCell<S, T> {

	/**
	 * Provides a {@link TextField} that allows editing of the cell content when the cell is double-clicked, or when
	 * {@link TableView#edit(int, javafx.scene.control.TableColumn)} is called. This method will only work on
	 * {@link TableColumn} instances which are of type String.
	 *
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
		return forTableColumn(new DefaultStringConverter());
	}

	/**
	 * Provides a {@link TextField} that allows editing of the cell content when the cell is double-clicked, or when
	 * {@link TableView#edit(int, javafx.scene.control.TableColumn) } is called. This method will work on any
	 * {@link TableColumn} instance, regardless of its generic type. However, to enable this, a {@link StringConverter}
	 * must be provided that will convert the given String (from what the user typed in) into an instance of type T.
	 * This item will then be passed along to the {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter} that can convert the given String (from what the user typed in) into
	 *            an instance of type T.
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
			final StringConverter<T> converter) {
		return list -> new TextEditingTableCell<S, T>(converter);
	}

	private TextField textField;

	/**
	 * Creates a default TextEditingTableCell with a null converter. Without a {@link StringConverter} specified, this
	 * cell will not be able to accept input from the TextField (as it will not know how to convert this back to the
	 * domain object). It is therefore strongly encouraged to not use this constructor unless you intend to set the
	 * converter separately.
	 */
	public TextEditingTableCell() {
		this(null);
	}

	/**
	 * Creates a TextEditingTableCell that provides a {@link TextField} when put into editing mode that allows editing
	 * of the cell content. This method will work on any TableColumn instance, regardless of its generic type. However,
	 * to enable this, a {@link StringConverter} must be provided that will convert the given String (from what the user
	 * typed in) into an instance of type T. This item will then be passed along to the
	 * {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter converter} that can convert the given String (from what the user typed
	 *            in) into an instance of type T.
	 */
	public TextEditingTableCell(StringConverter<T> converter) {
		this.getStyleClass().add("text-field-table-cell");
		setConverter(converter);
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
			if (textField == null) textField = createTextField();
			else textField.setText(getString());

			setText(null);
			setGraphic(textField);
			textField.selectAll();

			// requesting focus so that key input can immediately go into the
			// TextField (see RT-28132)
			textField.requestFocus();
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
				if (textField != null) {
					textField.setText(getString());
				}
				setText(null);
				setGraphic(textField);
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

	private String getString() {
		return getConverter() == null ? //
				getItem() == null ? "" : getItem().toString() : //
				getConverter().toString(getItem());
	}

	private TextField createTextField() {
		final TextField textField = new TextField(getString());
		textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
		textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == false) {
				if (getConverter() == null) {
					throw new IllegalStateException(
							"Attempting to convert text input into Object, but provided "
									+ "StringConverter is null. Be sure to set a StringConverter "
									+ "in your cell factory.");
				}
				commitEdit(getConverter().fromString(textField.getText()));
			}
		});

		// Use onAction here rather than onKeyReleased (with check for Enter),
		// as otherwise we encounter RT-34685
		textField.setOnAction(event -> {
			if (getConverter() == null) {
				throw new IllegalStateException(
						"Attempting to convert text input into Object, but provided "
								+ "StringConverter is null. Be sure to set a StringConverter "
								+ "in your cell factory.");
			}
			commitEdit(getConverter().fromString(textField.getText()));
			event.consume();
		});
		textField.setOnKeyReleased(t -> {
			if (t.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
				t.consume();
			}
		});
		
		return textField;
	}

	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */

	// --- converter
	private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this, "converter");

	/**
	 * The {@link StringConverter} property.
	 */
	public final ObjectProperty<StringConverter<T>> converterProperty() {
		return converter;
	}

	/**
	 * Sets the {@link StringConverter} to be used in this cell.
	 */
	public final void setConverter(StringConverter<T> value) {
		converterProperty().set(value);
	}

	/**
	 * Returns the {@link StringConverter} used in this cell.
	 */
	public final StringConverter<T> getConverter() {
		return converterProperty().get();
	}

}