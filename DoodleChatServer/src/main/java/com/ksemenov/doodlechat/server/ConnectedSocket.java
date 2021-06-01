package com.ksemenov.doodlechat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


public class ConnectedSocket{
    private static final Logger LOG = LogManager.getLogger(ConnectedSocket.class);

    final static long CONNECTION_TIMEOUT = 120000;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    private ExecutorService executorService;
    private Socket socket;
    private boolean isActive;
    private boolean isAuthenticated;
    private SocketService socketService;
    private long connectionTimeMillis;

    public long getConnectionTimeMillis() {
        return connectionTimeMillis;
    }

    ConnectedSocket(Socket socket, SocketService socketService) throws IOException {
        isActive = true;
        isAuthenticated = false;
        connectionTimeMillis = System.currentTimeMillis();

        this.socketService = socketService;
        this.socket = socket;

        dataInputStream  = new DataInputStream(this.socket.getInputStream());
        dataOutputStream = new DataOutputStream(this.socket.getOutputStream());

        //поток обработки входящих сообщений от сокета

        executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        executorService.execute(() -> {
                while (isActive){
                    try {
                        String str = dataInputStream.readUTF();
                        StringAnalyse(str);
                    }
                    catch (IOException e) {
                        LOG.warn(e.getMessage() + " Listener socket thread error");
                        ConnectedSocket.this.socketCloseProcess();
                        break;
                    }
                }
        });

        //Поток контроля таймаута аутентификации
        executorService.execute(()-> {
            while(!ConnectedSocket.this.isAuthenticated){
                synchronized(this){
                    try {
                        this.wait(10);
                    } catch(InterruptedException e) {
                        LOG.warn(e.getMessage() + " Auth thread interrupted");
                    }
                }
                if(System.currentTimeMillis() - ConnectedSocket.this.getConnectionTimeMillis() >= CONNECTION_TIMEOUT && !ConnectedSocket.this.isAuthenticated){
                    LOG.info("User not authorised. Disconnect process beginning");
                    ConnectedSocket.this.socketCloseProcess();
                    break;
                }
            }
        });
    }

    private synchronized void StringAnalyse(String dataString) {
        //Запрос авторизации
        if(dataString.startsWith(Commands.AUTH.getStrValue())){
            authoriseUserByCommand(dataString);
        }
        // Отключение клиента
        else if(dataString.startsWith(Commands.END_SESSION.getStrValue()) && isAuthenticated){
            socketCloseProcess();

        }
        //Приватное сообщение
        else if(dataString.startsWith(Commands.PRIVATE_MSG.getStrValue()) && isAuthenticated){
            privateMessageProcessing(dataString);
        }
        //Изменение никнейма
        else if(dataString.startsWith(Commands.CHANGE_NICKNAME.getStrValue()) && isAuthenticated){
            changeUserNickname(dataString);
        }
        else if(dataString.startsWith(Commands.DISCONNECT.getStrValue()) && isAuthenticated){
            socketCloseProcess();
        }
        //Иначе
        else {
            if(isAuthenticated){
                socketService.broadcastMessage(socketService.getConnectedClients().get(this).nickname+": "
                + dataString.trim());

            } else
            {
                this.putToOutputStream("Authorisation needed. Time to disconnecting: "+((CONNECTION_TIMEOUT - (System.currentTimeMillis() - connectionTimeMillis)))/1000+" sec.");
            }
        }

    }

    private void changeUserNickname(String dataString) {
        String newNickName = dataString.replace(Commands.CHANGE_NICKNAME.getStrValue(),"");
        String currentNickName = socketService.getConnectedClients().get(this).nickname;
        User user = socketService.getAuthService().changeUserNickname(currentNickName, newNickName.trim());
        if (!user.nickname.equals(currentNickName)){
            StringBuilder sb = new StringBuilder();
            socketService.broadcastMessage(sb.append("User ").append(currentNickName).append(" change nickname on ").append(newNickName.trim()).toString());
        }
    }

    private void privateMessageProcessing(String dataString) {
        String[] parts = dataString.split(" ");
        StringBuilder message = new StringBuilder();
        message.append(socketService.getConnectedClients().get(this).nickname).append(" (private):");
        for (int i = 2; i < parts.length; i++) {
            message.append(" ").append(parts[i]);
        }
        socketService.sendMessageToUser(parts[1], message.toString());
    }

    private void authoriseUserByCommand(String dataString) {
        String[] parts;
        User user;
        parts = dataString.split(" ");
        user = socketService.getAuthService().getUserByNameAndPass(parts[1],parts[2]);
        if(user!=null){
            if(socketService.findConnectedUser(user) == null){
                socketService.addConnectedClients(this, user);
                this.isAuthenticated = true;
                socketService.broadcastMessage("User \""+user.nickname+"\" has entered to chat.");
                LOG.info("User \""+user.nickname+"\" successfully authorised.");
            } else {
                this.putToOutputStream("User already connected!");
            }
        } else {
            this.putToOutputStream("Wrong username or password.");
        }
    }

    private synchronized void socketCloseProcess() {
        isActive = false;
        if(socketService.getConnectedSockets().contains(this)){
            this.putToOutputStream("/end.");
        }
        if(!socketService.getConnectedClients().isEmpty() && socketService.getConnectedSockets().contains(this)){
            User usr = socketService.getConnectedClients().remove(this);
            socketService.removeConnectedSocket(this);
            socketService.broadcastMessage("User \""+usr.nickname+"\" has left the chat.");
        }
        executorService.shutdownNow();
        LOG.info("Socket connection closed");

    }

    public synchronized void putToOutputStream(String text){
        try{
            dataOutputStream.writeUTF(text);
        } catch (Exception e){
           LOG.error(e.getMessage());
        }

    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return "ConnectedSocket{" +
                "socket=" + socket +
                ", isActive=" + isActive +
                '}';
    }

}
