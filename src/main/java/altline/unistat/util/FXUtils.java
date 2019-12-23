package altline.unistat.util;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;

import com.sun.javafx.collections.ElementObservableListDecorator;

import altline.utils.Alerts;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Callback;

public class FXUtils {

	/**
	 * Creates a {@link Binding} that updates whenever the state of the specified {@link ListProperty} changes. That
	 * means the Binding will update if the underlying list of the listProperty changes, or if any of the properties
	 * specified in the extractor change in any of the list's elements.
	 * 
	 * @param <T> The type of the created Binding
	 * @param <E> The type of the list's elements
	 * 
	 * @param func The function that calculates the value of this Binding
	 * @param listProperty The ListProperty that is to be the dependency of this Binding
	 * @param extractor A {@link Callback} that provides an array of {@link Observable}s which are to be the
	 *            dependencies of this Binding
	 * @return The generated Binding
	 */
	public static final <T, E> ObjectBinding<T> createListElementBinding(Callable<T> func, ListProperty<E> listProperty, Callback<E, Observable[]> extractor) {
		return new ObjectBinding<T>() {

			private ObservableList<E> wrapper;
			private ListChangeListener<E> wrapperListener;

			{
				listProperty.addListener((obs, oldVal, newVal) -> {
					
					if (oldVal != newVal) {
						unbind(oldVal);
						bind(newVal);

						if (wrapper != null) wrapper.removeListener(wrapperListener);

						attachElementListener(newVal, extractor);
					}
					
					invalidate();
				});

				attachElementListener(listProperty.get(), extractor);
			}

			@Override
			protected T computeValue() {
				try {
					return func.call();
				} catch (Exception e) {
					Alerts.catching("Exception while evaluating binding", e, LogManager.getLogger());
					return null;
				}
			}

			@Override
			public void dispose() {
				if (wrapper != null) wrapper.removeListener(wrapperListener);
			}

			private void attachElementListener(ObservableList<E> list, Callback<E, Observable[]> extractor) {
				wrapper = new ElementObservableListDecorator<>(list, extractor);
				wrapperListener = new ListChangeListener<E>() {
					@Override
					public void onChanged(Change<? extends E> c) {
						invalidate();
					}
				};

				wrapper.addListener(wrapperListener);
			}
		};
	}

}
