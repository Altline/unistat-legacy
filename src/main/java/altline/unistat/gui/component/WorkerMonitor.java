package altline.unistat.gui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;

public class WorkerMonitor extends ProgressMonitor {
	
	public WorkerMonitor() {
		this(null);
	}
	
	public WorkerMonitor(Worker<?> worker) {
		init();
		if(worker != null) setWorker(worker);
	}
	
	private void init() {
		worker.addListener((obs, oldVal, newVal) -> {
			messageProperty().unbind();
			progressProperty().unbind();
			
			setOnCancel(newVal == null ? null : () -> newVal.cancel());
			
			if(newVal != null) {
				messageProperty().bind(newVal.messageProperty());
				progressProperty().bind(newVal.progressProperty());
			}
		});
	}
	
	/* *************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 ************************************************************************* */
	
	// --- worker
	private final ObjectProperty<Worker<?>> worker = new SimpleObjectProperty<>(this, "worker");
	
	public final ObjectProperty<Worker<?>> workerProperty(){
		return worker;
	}
	
	public final Worker<?> getWorker(){
		return worker.get();
	}
	
	public final void setWorker(Worker<?> value) {
		worker.set(value);
	}

}
