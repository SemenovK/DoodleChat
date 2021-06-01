package com.ksemenov.doodlechat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Scanner;


public class ServerApp {
    public static Scanner scanner = new Scanner(System.in);
    private static final Logger LOG = LogManager.getLogger(ServerApp.class);
    static SocketService socketService;

    public static void main(String[] args) {

        final int port = 13020;
        String commandString;

        socketService = new SocketService(port);
        LOG.info("Server started");

        //Мониторим ввода в консоль
        while(socketService.isActive()){
            commandString = scanner.nextLine();

            //Выключить сервер
            if(commandString.equalsIgnoreCase(Commands.QUIT.getStrValue())){
                socketService.setInactive();
            }
            //Послать сообщение всем пользователям в онлайне
            else if(commandString.contains(Commands.SEND.getStrValue())){
                sendMessageToAllExec(commandString);
            }
            //показать список подключенных пользователей
            else if(commandString.contains(Commands.SHOW_CLIENTS.getStrValue())){
                for (Map.Entry<ConnectedSocket,User> entry: socketService.getConnectedClients().entrySet()) {
                    System.out.println(entry.getValue().nickname);
                }
            }

        }
        scanner.close();

    }

    private static void sendMessageToAllExec(String commandString) {
        int beginPos = commandString.indexOf(Commands.SEND.getStrValue());
        beginPos+=Commands.SEND.getStrValue().length();
        socketService.broadcastMessage(commandString.substring(beginPos).trim());
    }
}
