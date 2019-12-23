package altline.unistat.gui;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;

import altline.unistat.gui.component.PromptBase;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GuiLogin extends PromptBase {
	
	private boolean dragging;
	private double dragX, dragY;

	@FXML
	private StackPane header;
	
	@FXML
	private Button btnAccept, btnExit;

	@FXML
	private JFXTextField userIDField;

	@FXML
	private JFXPasswordField passwordField;

	@FXML
	private JFXCheckBox rememberCheckBox;
	
	@FXML
	private Label lblFailMessage;
	
	@FXML
	private void initialize() {
		header.setOnMousePressed(e -> {
			dragX = e.getSceneX();
			dragY = e.getSceneY();
		});
		header.setOnMouseDragged(e -> {
			Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
			if(!dragging) {
				dragging = true;
				stage.setOpacity(0.9f);
			}
			stage.setX(e.getScreenX() - dragX);
			stage.setY(e.getScreenY() - dragY);
		});
		header.setOnMouseReleased(e -> {
			Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
			stage.setOpacity(1.0f);
			dragging = false;
		});
		
		btnAccept.setOnAction(e -> accept());
		btnExit.setOnAction(e -> cancel());
	}
	
	@Override
	public Stage getStage() {
		return (Stage) header.getScene().getWindow();
	}

	@Override
	public void reset() {
		userIDField.clear();
		passwordField.clear();
		rememberCheckBox.setSelected(false);
		lblFailMessage.setText("");
	}
	
	public void setFailMessage(String failMessage) {
		lblFailMessage.setText(failMessage);
	}

	public String getUserId() {
		return userIDField.getText();
	}

	public String getPassword() {
		return passwordField.getText();
	}

	public boolean getRemember() {
		return rememberCheckBox.isSelected();
	}

}
