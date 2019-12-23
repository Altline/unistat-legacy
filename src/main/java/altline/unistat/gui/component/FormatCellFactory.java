package altline.unistat.gui.component;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Used to convert values in table cells to strings.<br>
 * Usage:
 * 
 * <pre>
 * tableColumn.setCellFactory((FormatCellFactory<TableItemType, ColumnItemType>) value -> {
 * 	return value.formatted();
 * });
 * </pre>
 * 
 * or, without lambdas
 * 
 * <pre>
 * tableColumn.setCellFactory(new FormatCellFactory<TableItemType, ColumnItemType>() {
 * 	<b>@Override</b>
 *	public String format(ColumnItemType value) {
 *		return value.formatted();
 *	}
 * });
 * </pre>
 *
 * @param <E>
 * @param <T>
 */
public interface FormatCellFactory<E, T> extends Callback<TableColumn<E, T>, TableCell<E, T>> {

	@Override
	default TableCell<E, T> call(TableColumn<E, T> param) {
		return new TableCell<E, T>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty) {
					setText(null);
				} else {
					setText(format(item));
				}
			}
		};
	}

	String format(T value);

}
