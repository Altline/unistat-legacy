package altline.unistat.gui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressMonitor extends VBox {
	
	private Label messageLabel;
	private ProgressBar progressBar;
	private Button cancelButton;
	
	public ProgressMonitor() {
		setPrefWidth(350);
		setMinWidth(350);
		setPadding(new Insets(15));
		setSpacing(10);
		setFillWidth(true);
		setAlignment(Pos.CENTER);
		
		messageLabel = new Label();
		messageLabel.setMaxWidth(Double.MAX_VALUE);
		messageLabel.textProperty().bind(message);
		
		progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressBar.progressProperty().bind(progress);
		
		cancelButton = new Button("Odustani");
		cancelButton.visibleProperty().bind(cancelable);
		cancelButton.setOnAction(e -> {
			if(isCancelable() && getOnCancel() != null) 
				getOnCancel().run();
		});
		
		getChildren().addAll(messageLabel, progressBar, cancelButton);
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */
	
	// --- message
	private final StringProperty message = new SimpleStringProperty(this, "message", "");
	
	public final StringProperty messageProperty() {
		return message;
	}
	
	public final String getMessage() {
		return message.get();
	}
	
	public final void setMessage(String value) {
		message.set(value);
	}
	
	
	// --- progress
	private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress");
	
	public final DoubleProperty progressProperty() {
		return progress;
	}
	
	public final double getProgress() {
		return progress.get();
	}
	
	public final void setProgress(double value) {
		progress.set(value);
	}
	
	
	// --- cancelable
	private final BooleanProperty cancelable = new SimpleBooleanProperty(this, "cancelable", true);
	
	public final BooleanProperty cancelableProperty() {
		return cancelable;
	}
	
	public final boolean isCancelable() {
		return cancelable.get();
	}
	
	public final void setCancelable(boolean value) {
		cancelable.set(value);
	}
	
	
	// --- onCancel
	private final ObjectProperty<Runnable> onCancel = new SimpleObjectProperty<Runnable>(this, "onCancel");
	
	public final ObjectProperty<Runnable> onCancelProperty(){
		return onCancel;
	}
	
	public final Runnable getOnCancel() {
		return onCancel.get();
	}
	
	public final void setOnCancel(Runnable value) {
		onCancel.set(value);
	}

}
