package ru.geekbrains.java4.lesson2.common;

public class AuthMessage extends AbstractMessage {
    private String login;
    private String parol;
    private String msg;
    private LogQuery logQuery;


    public AuthMessage(String login, String parol, LogQuery logQuery){
        this.login = login;
        this.parol = parol;
        this.msg = login + " " + parol;
        this.logQuery = logQuery;
    }

public void setLogin(String login){this.login = login;}
public void setParol(String parol){this.parol = parol;}
public String getLogin(){return login;}
public String getParol(){return parol;}
public String getMsg(){return msg;}
public LogQuery getLogQuery(){return logQuery;}


}
