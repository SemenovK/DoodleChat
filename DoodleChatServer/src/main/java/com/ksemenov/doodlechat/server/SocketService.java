package com.ksemenov.doodlechat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketService{
    private static final Logger LOG = LogManager.getLogger(SocketService.class);
    boolean isActive;
    private ServerSocket listenerSocket;
    private Set<ConnectedSocket> connectedSockets;
    private Map<ConnectedSocket,User> connectedClients;
    private int port;
    private ExecutorService executorService;
    private BaseAuthService authService;

    public Map<ConnectedSocket, User> getConnectedClients() {
        return connectedClients;
    }

    public Set<ConnectedSocket> getConnectedSockets() {
        return connectedSockets;
    }

    public SocketService(int port) {
        executorService = Executors.newSingleThreadExecutor();
        connectedSockets = new HashSet<>();
        connectedClients = new HashMap<>();
        authService = new BaseAuthService();
        authService.start();
        this.port = port;
        try {
            listenerSocket = new ServerSocket(this.port);
            isActive= true;

            executorService.execute(() -> {
                    while (SocketService.this.isActive()) {
                        ConnectedSocket s;
                        try {
                            s = new ConnectedSocket(listenerSocket.accept(), SocketService.this);
                            LOG.info("New connection accepted");
                            connectedSockets.add(s);
                        } catch (IOException e) {
                            LOG.error(e.getMessage()+" - Socket listening error");
                        }
                    }
            });

        }  catch (IOException e) {
            LOG.fatal(e.getMessage() + "Server internal error");
        }
    }

    public BaseAuthService getAuthService() {
        return authService;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setInactive() {
        isActive = false;
        broadcastMessage("Server shutting down...");
        try {
            new Socket(listenerSocket.getInetAddress(),listenerSocket.getLocalPort()).close();
        } catch (Exception e){
            LOG.warn(e.getMessage()+" - exception on server socket closing");
        }
        getAuthService().stop();
        executorService.shutdownNow();
        LOG.info("Server shouted down.");

    }

    public synchronized void sendMessageToUser(String nickname, String messageText){
        User usr = authService.findUserByNickname(nickname);
        if(usr!= null){
            findConnectionByUser(usr).putToOutputStream(messageText.trim());
        }
    }

    public synchronized void broadcastMessage(String messageText){
        for (Map.Entry<ConnectedSocket, User> entry: connectedClients.entrySet()) {
            if(entry.getKey().isActive()) {
                entry.getKey().putToOutputStream(messageText);
            }
        }
    }

    protected synchronized ConnectedSocket findConnectionByUser(User user){
        ConnectedSocket socket = null;
        for (Map.Entry<ConnectedSocket, User> entry: connectedClients.entrySet()) {
            if(entry.getValue().nickname.equals(user.nickname)){
                socket =  entry.getKey();
                break;
            }
        }
        return socket;
    }

    public synchronized void removeConnectedSocket(ConnectedSocket s) {
        connectedSockets.remove(s);

    }

    public synchronized ConnectedSocket findConnectedUser(User user){
        ConnectedSocket result = null;
        for (Map.Entry<ConnectedSocket, User> entry : connectedClients.entrySet()) {
            ConnectedSocket key = entry.getKey();
            User value = entry.getValue();
            if (value.equals(user)) {
                result = key;
                break;
            }
        }
        return result;
    }

    public synchronized void addConnectedClients(ConnectedSocket socket, User user){
        connectedClients.put(socket, user);
    }

}
