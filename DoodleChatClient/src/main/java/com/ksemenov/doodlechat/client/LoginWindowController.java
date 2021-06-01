package com.ksemenov.doodlechat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


import java.net.URL;
import java.util.ResourceBundle;


public class LoginWindowController implements Initializable {

    @FXML
    TextField loginField;
    @FXML
    TextField passwordField;
    @FXML
    Button btnEnter;
    @FXML
    Button btnClose;
    private DoodleChatController doodleChatController;


    @FXML
    public void onEnterClick(ActionEvent actionEvent) {
        doodleChatController.authoriseAttempt(loginField.getText().trim(), passwordField.getText().trim());
        onCloseClick(actionEvent);
    }

    @FXML
    public void onCloseClick(ActionEvent actionEvent) {

       Stage stage = (Stage) btnClose.getScene().getWindow();
       stage.close();
    }

    public void setParentController(DoodleChatController doodleChatController) {
        this.doodleChatController = doodleChatController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
