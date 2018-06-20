package ru.geekbrains.java4.lesson2.common;

public class ChatMessage extends AbstractMessage {
    private String msg;
    private LogQuery logQuery;

    public ChatMessage(String message, LogQuery logQuery){
        this.msg = message;
        this.logQuery = logQuery;
    }

    public String getMsg(){return msg;}
    public LogQuery getLogQuery(){return logQuery;}
}
