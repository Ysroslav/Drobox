package ru.geekbrains.java4.lesson2.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/* Класс для передачи файлов на сервер. В конструкторе происходит проверка, что клиент хочет передать файл или папку
 */

public class FileMessage extends AbstractMessage {

    private String fileName;
    private String path;
    private byte[] dates;
    private int partsNumber;
    private int partCount;
    private int size;
    private String msg;
    private ArrayList<String> listFiles;
    private String newFileName;
    private LogQuery logQuery;

    public FileMessage(Path filePaths, LogQuery logQuery) throws IOException {
            this.path = filePaths.toString();
            this.fileName = filePaths.toString().substring(filePaths.toString().lastIndexOf("\\")+1);
            this.dates = null;
            this.size = 0;
            this.logQuery = logQuery;
    }

    public FileMessage(Path filePaths, String newFileName, LogQuery logQuery) throws IOException {
        this.path = filePaths.toString();
            this.fileName = filePaths.toString().substring(filePaths.toString().lastIndexOf("\\")+1);
            this.dates = null;
            this.size = 0;
            this.newFileName = newFileName;
            this.logQuery = logQuery;
    }

    public FileMessage(Path filePaths, byte[] data, int partsNumber, int partCount, LogQuery logQuery){
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        this.dates = data;
        this.partCount = partCount;
        this.partsNumber = partsNumber;
        this.logQuery = logQuery;
    }

    public FileMessage(Path filePaths, String newFileName, byte[] data,
                       int partsNumber, int partCount, LogQuery logQuery){
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        this.dates = data;
        this.partCount = partCount;
        this.partsNumber = partsNumber;
        this.newFileName = newFileName;
        this.logQuery = logQuery;
    }

    public String getNewFileName(){return newFileName;}

    public FileMessage(ArrayList<String> list) throws IOException {

    }

    public String getFileName(){
        return fileName;
    }
    public int getPartsNumber(){return partsNumber;}
    public int getPartCount(){return partCount;}

    public String getMsg(){
        return msg;
    }

    public byte[] getDates(){
        return dates;
    }

    public String getPath(){
        return path;
    }
    public LogQuery getLogQuery(){return logQuery;}
}
