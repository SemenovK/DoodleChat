package com.ksemenov.doodlechat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;

public class DoodleChatController implements Initializable{
    private SocketService socketService;
    boolean connected = false;

    private File historyFile;
    private ReversedLinesFileReader historyFileReader;
    private FileOutputStream historyFileOutputStream;

    @FXML
    public TextArea messageList;
    @FXML
    public Button sendBtn;
    @FXML
    public TextField messageTf;
    @FXML
    MenuItem menuConnect;
    @FXML
    MenuItem menuAuthorise;

    @FXML
    public void onSendBtnAction(ActionEvent actionEvent) {
        sendMessage();
    }



    public synchronized void putMessageOnScreen(String text){
        messageList.setText(messageList.getText()+text.trim()+"\n");
        addToHistory(text.trim()+"\n");
        messageList.setScrollTop(Double.MAX_VALUE);

    }

    private void addToHistory(String text) {
        try {
            if(historyFile == null)
                return;

            if(historyFileOutputStream == null){
                historyFileOutputStream = new FileOutputStream(historyFile, historyFile.exists());
            }

            historyFileOutputStream.write(text.getBytes(StandardCharsets.UTF_8));


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void sendMessage() {
        if(this.connected) {
            String str = messageTf.getText().trim();
            socketService.sendMessage(str);
            messageTf.clear();
            messageTf.requestFocus();
        }
    }

    @FXML
    public void onMessage_tf_KeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            sendMessage();
        }

    }

    @FXML
    public void onSendBtnPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER)
            sendBtn.fire();
    }


    @FXML
    public void closeApplication(ActionEvent actionEvent) {
        disconnectProcess();
        Platform.exit();
    }

    public void connectToServer() {
        if(!connected){
            try {
                socketService = new SocketService("localhost", 13020);
                setConnected(true);
                socketService.setListeningSocketThread(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while(true){
                            try {
                                String dataString = socketService.getDataInputStream().readUTF();
                                analyseDataString(dataString);
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }

                    }
                }));
                socketService.getListeningSocketThread().setDaemon(true);
                socketService.getListeningSocketThread().start();
                authorisationWindowShow();

            } catch (IOException e){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
                setConnected(false);
            }
        } else {

            disconnectProcess();

        }

    }

    private void disconnectProcess() {
        setConnected(false);
        socketService.sendMessage("/disconnect");
        socketService.getListeningSocketThread().interrupt();
        putMessageOnScreen("You are disconnected.");
        try {
            historyFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void analyseDataString(String dataString) {
        if(dataString.startsWith("/end")){
            disconnectProcess();
        } else {
            putMessageOnScreen(dataString);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageList.clear();
        menuAuthorise.setDisable(true);
    }

    public void authoriseAttempt(String login, String password){
        String commandString = "/auth "+login+" "+password;
        socketService.sendMessage(commandString.trim());
        historyFile = new File("history_"+login.trim()+".txt");
        if(historyFile.exists()){
            try {
                historyFileReader = new ReversedLinesFileReader(historyFile, StandardCharsets.UTF_8);
                messageList.clear();
                List<String> stringList = historyFileReader.readLines(100);

                for (int i=stringList.size()-1; i>=0; i--) {
                    messageList.appendText(stringList.get(i)+"\n");
                }

                historyFileReader.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void setConnected(boolean connected) {
        menuAuthorise.setDisable(!connected);
        if(connected){
            menuConnect.setText("Disconnect");
            putMessageOnScreen("Connected. Authorisation needed.");
        } else {
            menuConnect.setText("Connect");
        }
        this.connected = connected;
    }

    public void authorisationWindowShow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginWindow.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
        //    stage.setOpacity(1);
            stage.setTitle("Log in");
            stage.setScene(new Scene(root, 200, 100));
            stage.initModality(Modality.WINDOW_MODAL);
            LoginWindowController loginWindow = loader.getController();
            loginWindow.setParentController(this);
            stage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}

