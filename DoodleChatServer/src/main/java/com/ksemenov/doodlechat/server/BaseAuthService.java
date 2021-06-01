package com.ksemenov.doodlechat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BaseAuthService {
    private static final Logger LOG = LogManager.getLogger(BaseAuthService.class);

    static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/GB_DoodleChat";
    static final String DB_USER = "postgres";
    static final String DB_PASS = "qwerty";

    private Connection connection;

    private List<User> userList;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException e){
            LOG.error(e.getMessage()+" - Connecting database error");
        }
    }


    public BaseAuthService() {
        userList = new ArrayList<>();

    }

    public void start(){
        try {
            connection = DriverManager.getConnection(DB_URL,DB_USER, DB_PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (connection != null) {
            LOG.info("Connection to database established");
        }
        LOG.info("Authentication Service has been started");

    }


    public void stop(){

        try {
            connection.close();
            LOG.info("Disconnected from database");
        } catch (SQLException e) {
            LOG.error(e.getMessage() + "Closing database connection error");
        }
        LOG.info("Authentication Service is stopped");
    }

    public User getUserByNameAndPass(String login, String password){
        String nickName = null;
        double userID = 0;
        try {
            PreparedStatement qFindUserByLoginPassword = connection.prepareStatement("select \"UserID\",\"NickName\" from public.\"Users\" where \"Login\" = ? and \"Password\" = ?;");
            qFindUserByLoginPassword.setString(1, login);
            qFindUserByLoginPassword.setString(2,password);
            ResultSet resultSet = qFindUserByLoginPassword.executeQuery();

            while (resultSet.next()){
                nickName = resultSet.getString("NickName");
                userID = resultSet.getDouble("UserID");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(userID != 0){
            User user =  new User(login,password,nickName, userID);
            userList.add(user);
            return user;
        }

        return null;
    }

    public User findUserByNickname(String nickname){
        User usr = null;
        for (User user : userList) {
            if (user.nickname.equals(nickname)) {
                usr = user;
                break;
            }
        }
        return usr;
    }

    public User changeUserNickname(String oldNickName, String newNickname){
        User user = findUserByNickname(oldNickName);
        try {
            PreparedStatement qUpdateNickName = connection.prepareStatement("update public.\"Users\" set \"NickName\" = ? where \"Login\" = ? and \"Password\" = ?;");
            qUpdateNickName.setString(1, newNickname);
            qUpdateNickName.setString(2, user.login);
            qUpdateNickName.setString(3, user.password);
            connection.setAutoCommit(false);

            if(qUpdateNickName.executeUpdate()!=1) {
                throw new SQLException("Unable to change NickName");
            }
            connection.commit();
            connection.setAutoCommit(true);


        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return user;
        }
        user.nickname = newNickname;
        return user;
    }
}
