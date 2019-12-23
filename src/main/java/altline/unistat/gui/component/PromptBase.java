package altline.unistat.gui.component;

import altline.unistat.App;

/**
 * A standard implementation of a {@link Prompt}. FXML GUI controllers should extend this and call {@link #accept()} and
 * {@link #cancel()} in accordance to the user's requests.
 */
public abstract class PromptBase implements Prompt {

	private boolean accepted = false;

	/**
	 * {@inheritDoc}
	 * <p>
	 * It is safe to call this from a thread other than the JavaFX Application Thread since the showing of the Prompt is
	 * posted to the JavaFX event queue, unless called from that thread (in which case it executes synchronously).
	 */
	@Override
	public boolean acquireInput() throws InterruptedException {
		showAndWait();
		// We reset #accepted to false so that next time, if the prompt is somehow closed not by calling #accept or
		// #cancel, we return false
		if (accepted) {
			accepted = false;
			return true;
		} else return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * It is safe to call this from a thread other than the JavaFX Application Thread since the showing of the Prompt is
	 * posted to the JavaFX event queue, unless called from that thread (in which case it executes synchronously).
	 */
	@Override
	public void show() {
		onShowing();

		App.runFx(() -> {
			getStage().show();
		});
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * It is safe to call this from a thread other than the JavaFX Application Thread since the showing of the Prompt is
	 * posted to the JavaFX event queue, unless called from that thread (in which case it executes synchronously).
	 */
	@Override
	public void showAndWait() throws InterruptedException {
		onShowing();

		App.runFxAndWait(() -> {
			getStage().showAndWait();
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void accept() {
		accepted = true;
		getStage().close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void cancel() {
		accepted = false;
		getStage().close();
	}

	/**
	 * Called just before this prompt shows to the user, assuming that it is shown by using the Prompt's own interface
	 * methods.
	 */
	protected void onShowing() {
	}

}
