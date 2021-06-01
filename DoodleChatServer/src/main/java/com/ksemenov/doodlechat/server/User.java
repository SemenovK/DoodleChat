package com.ksemenov.doodlechat.server;

import java.util.Objects;

public class User {
    String login;
    String password;
    String nickname;
    double id;

    public User(String login, String password, String nickname, double id) {
        this.login = login;
        this.password = password;
        this.nickname = nickname;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Double.compare(user.id, id) == 0 && login.equals(user.login) && password.equals(user.password) && nickname.equals(user.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, password, nickname, id);
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", nickname='" + nickname + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
