package altline.unistat.gui.component;

import javafx.stage.Stage;

/**
 * A Prompt is an object that can ask the user for input.
 * <p>
 * A typical usage of a Prompt involves calling {@link #acquireInput()} which will show the Prompt and block execution
 * until the Prompt is accepted or canceled. The returned boolean will be true if the Prompt is accepted, or false if it
 * is canceled. </br>
 * After returning with true from {@link #acquireInput()}, a Prompt will contain data the user provided to it. That data
 * can be accessed through the appropriate methods defined by individual Prompt implementations.
 * </p>
 * <p>
 * The {@link #showAndWait()}, {@link #accept()} and {@link #cancel()} methods usually do not need to be called from
 * outside the Prompt because Prompt implementations should handle the accepting and canceling based on the user's
 * request, and showing of the Prompt is done by {@link #acquireInput()}.
 * </p>
 * <p>
 * A Prompt can be reset to its initial state (prior to user's inputs) by calling {@link #reset()}. This usually clears
 * all user input from the input fields, making Prompts reusable.
 * </p>
 */
public interface Prompt {

	/**
	 * Sets up the Prompt for user input, shows it to the user and waits until the Prompt is accepted or canceled. This
	 * is similar to {@link #showAndWait()}, but it allows for more operations than just showing the Prompt.
	 * @return true if the user accepted the Prompt, or false if they canceled it
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	public boolean acquireInput() throws InterruptedException;

	/**
	 * Shows the Prompt and returns immediately.
	 */
	public void show();

	/**
	 * Shows the Prompt and waits until it is closed.
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	public void showAndWait() throws InterruptedException;

	/**
	 * Accepts the Prompt. Any waiting calls on {@link #acquireInput()} will return true after this is called.
	 */
	public void accept();

	/**
	 * Accepts the Prompt. Any waiting calls on {@link #acquireInput()} will return false after this is called.
	 */
	public void cancel();

	/**
	 * Resets the Prompt to a state prior to any user input.
	 */
	public void reset();

	/**
	 * @return the Stage of the Prompt
	 */
	public Stage getStage();

}
