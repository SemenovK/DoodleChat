package com.ksemenov.doodlechat.server;

public enum Commands {
    QUIT("/quit"),SEND("/send"),SHOW_CLIENTS("/who"), AUTH("/auth"),
    PRIVATE_MSG("/w"), END_SESSION("/end"), CHANGE_NICKNAME("/rename"), DISCONNECT("/disconnect");

    private String commandString;
    Commands(String commandString){
        this.commandString = commandString;
    }
    public String getStrValue(){
        return this.commandString;
    }

}
