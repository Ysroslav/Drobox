package ru.geekbrains.java4.lesson2.common;

import java.io.Serializable;
import java.util.ArrayList;

/*
* Родительский класс для передачи объектов на сервер. В классе определенны переменные и методы
* которые переопределяются в классах потомках
 */

public class AbstractMessage implements Serializable {
    LogQuery logQuery;
    String msg;
    byte[] dates;
    String fileName;
    Command idCommand;
    int partCount;
    int partsNumber;
    String fileNameForChange;
    ArrayList<String>list;
    String[] datesString;
    private String newFileName;
    public String getMsg(){return msg;}
    public String getFileName(){return fileName;}
    public byte[] getDates(){return dates;}
    public Command getIdCommand(){return idCommand;}
    public String getFileNameForChange(){return fileNameForChange;}
    public ArrayList<String> getList(){return list;}
    public String[] getDatesString(){return datesString;}
    public String getNewFileName(){return newFileName;}
    public int getPartsNumber(){return partsNumber;}
    public int getPartCount(){return partCount;}
    public LogQuery getLogQuery(){return logQuery;}
}
