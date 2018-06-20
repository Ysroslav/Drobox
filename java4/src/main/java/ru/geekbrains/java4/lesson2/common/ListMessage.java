package ru.geekbrains.java4.lesson2.common;

import java.util.ArrayList;

public class ListMessage extends AbstractMessage {
    private String msg;
    private ArrayList<String> list;
    private LogQuery logQuery;

    public ListMessage(ArrayList<String> list, String name, LogQuery logQuery){
        this.list = list;
        this.msg =  name;
        this.logQuery = logQuery;
    }

    public String getMsg(){return msg;}
    public ArrayList<String> getList(){return list;}
    public LogQuery getLogQuery(){return logQuery;}
}
