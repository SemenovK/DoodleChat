package com.ksemenov.doodlechat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class SocketService {
    private Socket socket;
    private int port;
    private String host;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Thread listeningSocketThread;


    public void setListeningSocketThread(Thread listeningSocketThread) {
        this.listeningSocketThread = listeningSocketThread;
    }

    public SocketService(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = new Socket(this.host, this.port);
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());

    }

    public void sendMessage(String text){
        try {
            dataOutputStream.writeUTF(text.trim());
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public Thread getListeningSocketThread() {
        return listeningSocketThread;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }
}
