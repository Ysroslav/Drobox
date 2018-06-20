package ru.geekbrains.java4.lesson2.client;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/*
Класс для заполнения таблиц TableView
 */

public class HandlerFileForTable {

    final static String WORD_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/word.png";
    final static String EXCEL_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/excel.png";
    final static String RAR_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/archiv.png";
    final static String PDF_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/reader.png";
    final static String ACCESS_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/access.png";
    final static String FOLDER_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/folder.png";
    final static String OTHER_IMAGE = "file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/other.png";

    private ArrayList<String> listFail;
    private Path path;
    private Image label;
    private String file;

    public HandlerFileForTable(Path path){
        this.path = path;
        this.listFail = pullListFile(path);
    }

    public HandlerFileForTable(String file){
        this.file = file;
        this.label = new Image(checkFileExtension(file));
    }

    public HandlerFileForTable(){}

    private String checkFileExtension(String file){
        int i = file.lastIndexOf(".");
        if(i==-1) return FOLDER_IMAGE;
        String check = file.substring(i+1, file.length());
        switch (check){
            case "doc":
                return WORD_IMAGE;
            case "docx":
                return WORD_IMAGE;
            case "xls":
                return EXCEL_IMAGE;
            case "xlsx":
                return EXCEL_IMAGE;
            case "rar":
                return RAR_IMAGE;
            case "zip":
                return RAR_IMAGE;
            case "accde":
                return ACCESS_IMAGE;
            case "accdb":
                return ACCESS_IMAGE;
            case "mdb":
                return ACCESS_IMAGE;
            case "mde":
                return ACCESS_IMAGE;
            case "pdf":
                return PDF_IMAGE;
            default:
                return OTHER_IMAGE;
        }

    }

    public void createOrCheckFolder(Path path){
        if(!Files.exists(path)){
            try {
                Files.createDirectories(path);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public Image getLabel(){ return label;}

    public String getFile(){ return file;}

    public void setFile(String file){ this.file = file;}

    public ArrayList<String> getListFail(){
        return listFail;
    }

    public ArrayList<String> pullListFile(Path path){
        ArrayList<String> list = new ArrayList<>();
        if(Files.exists(path)){
            File folder = new File(path.toString());
            String[] files = folder.list();
            for (String fileName: files){
                list.add(fileName);
            }
        }
        return list;
    }
}
