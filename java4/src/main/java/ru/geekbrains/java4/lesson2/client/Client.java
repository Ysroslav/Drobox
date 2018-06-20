package ru.geekbrains.java4.lesson2.client;


import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import ru.geekbrains.java4.lesson2.common.*;
import ru.geekbrains.java4.lesson2.server.FileHandler;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Класс для создания Клиента.
 * listClientFile - список файлов, которые хранятся на сервере, authFromServer - авторизирован клиент или нет
 * handleSocket - метод, который создает поток и слушает сервер, listClientFileLocal - список файлов, котрые храняться на
 * стороне клиента. После авторизации клиент получает список файлов на сервере, путь к локальной папке (путь храниться
 * в базе), затем формирует список файлов, которые лежат на стороне клиента, при регистрации нового клиента
 * в переменные logins и nicks записываются все существующие логины и ники, и проходит проверка совпадений
 * логинов и ников. Nick клиент получает в строке вместе с локальной папкой, которая затем разбивается на массив
 */

public class Client {

    private String folderServer;
    private ArrayList<String> listClientFile;
    private ArrayList<String> listClientFileLocal;
    private String folderLocal;
    private boolean authFromServer;
    private boolean isConnected;
    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;
    private LinkedList<String> listFileForSend;
    private ArrayList<String> logins;
    private ArrayList<String> nicks;
    private String login;
    private String nick;



    public Client(){
        listClientFile = new ArrayList<>();
        handleSocket("localhost", 8189);
    }

    private void handleSocket(String address, int port) {
        try {
            this.socket = new Socket(address, port);
            this.out = new ObjectEncoderOutputStream(socket.getOutputStream());
            this.in = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.isConnected = true;
        this.authFromServer = false;
        readTread();
    }

    // поток для общения с сервером

    public void readTread(){
        new Thread(()-> {
            try {
                while (isConnected) {
                    Object msg = in.readObject();
                    if(msg!=null){
                        if(msg instanceof AbstractMessage){
                            AbstractMessage newMsg = (AbstractMessage) msg;
                            LogQuery log = newMsg.getLogQuery();
                            // ответ на запрос авторизации
                            switch (log){
                                case AUTHLOG:
                                    resultAnswerAuth(newMsg.getMsg());
                                    break;
                                    // получение списка файлов от сервера
                                case FILESERVERLOG:
                                    listClientFile.add(newMsg.getMsg());
                                    break;
                                    //обработка файлов
                                case FILELOG:
                                    saveFileFromServer(newMsg);
                                    break;
                                    // получение локальной папки
                                case FOLDERLOCALLOG:
                                    getFolderServerAndNick(newMsg.getMsg());
                                    break;
                                    //получение папок из вложенных файлов
                                case LISTFOLDERLOG:
                                    saveFolderFromServerList(newMsg);
                                    break;
                                    //получение файлов из вложенных папок
                                case LISTFILELOG:
                                    saveFileFromServerList(newMsg);
                                    break;
                                    //получение логинов и ников для проверки совпадений при регистрации
                                case LISTLOG:
                                    if(newMsg.getMsg().equals("logins")) logins = newMsg.getList();
                                    else if(newMsg.getMsg().equals("nicks")) nicks = newMsg.getList();
                                    break;
                                    default:
                                        System.out.println("Неизвестная команда");
                                        return;
                            }
                        }
                    }
                }
            } catch(ClassNotFoundException | IOException e){
                e.printStackTrace();
            }
        }).start();
    }


    public String getFolderLocal(){return this.folderLocal;}

//получение локальной папки и ника клиента
    private void getFolderServerAndNick(String str){
        String[] s = str.split(" ");
        this.folderLocal = s[0];
        this.nick = s[1];
    }


// установка флагов присоединсля ли клиент к серверу
    private void resultAnswerAuth(String answer){
        String[] s = answer.split(" ");
        if(s[0].equals("true")){
            this.authFromServer =true;
            System.out.println(answer);
            //подучаем папку клиента на сервере
            this.folderServer = s[1];
        } else {
            System.out.println("Client NOT connected!!!");
            this.authFromServer = false;
        }
    }



    public void close(){
        try {
            out.close();
            in.close();
            socket.close();
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    // авторизация клиента
    public boolean checkAccessClient(String login, String parol){
        LogQuery logQuery = LogQuery.AUTHLOG;
        System.out.println(logQuery);
        AuthMessage message = new AuthMessage(login, parol, logQuery);
        sendMessage(message);

        try {
            Thread.sleep(500);
        }catch(Exception e){
            e.printStackTrace();
        }
        return authFromServer;
    }

    //  методы получения и передачи каталогов

    public void sendListFile(Path fileName){
        listFileForSend = new LinkedList<String>();
        File file = fileName.toFile();
        String path = file.getPath().substring(0, file.getPath().lastIndexOf("\\"));
        processFilesFromFolder(file);
        sendFile(fileName);
        for(int i=0; i<listFileForSend.size(); i++){
            sendFile(Paths.get(listFileForSend.get(i)), replacePathForSendToServer(listFileForSend.get(i), path));
        }
    }
//рекурсивный метод обхода подкатологов
    private void processFilesFromFolder(File folder){
        File[] folderEntries = folder.listFiles();
        for(File entry: folderEntries) {
            listFileForSend.add(entry.getPath());
            if (entry.isDirectory()) {
                processFilesFromFolder(entry);
            }
        }
    }

    private String replacePathForSendToServer(String oldPath, String folderForSend){
        return oldPath.replace(folderForSend + "\\", this.folderServer);
    }


    // метод отправки файла

    public void sendFile(Path fileName){
        try{
        if(Files.isDirectory(fileName)){
            FileMessage msgFile = new FileMessage(fileName, LogQuery.FOLDERLOG);
            sendMessage(msgFile);
        } else {
            byte[] fileData = Files.readAllBytes(fileName);
            int partCount = fileData.length / FileHandler.SIZE_PART;
            if (fileData.length % FileHandler.SIZE_PART != 0) {
                partCount++;
            }
            for (int i = 0; i < partCount; i++) {
                int startPosition = i * FileHandler.SIZE_PART;
                int endPosition = (i + 1) * FileHandler.SIZE_PART;
                if (endPosition > fileData.length) {
                    endPosition = fileData.length;
                }
                FileMessage msgFile = new FileMessage(fileName, Arrays.copyOfRange(fileData, startPosition, endPosition),
                        i, partCount, LogQuery.FILELOG);
                sendMessage(msgFile);
            }
           }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    //метод отправки вложенного файла

    public void sendFile(Path fileName, String folderServer){
        try{
            if(Files.isDirectory(fileName)) {
                FileMessage msgFile = new FileMessage(fileName, folderServer, LogQuery.LISTFOLDERLOG);
                sendMessage(msgFile);
            } else {
                byte[] fileData = Files.readAllBytes(fileName);
                int partCount = fileData.length / FileHandler.SIZE_PART;
                if (fileData.length % FileHandler.SIZE_PART != 0) {
                    partCount++;
                }
                for (int i = 0; i < partCount; i++) {
                    int startPosition = i * FileHandler.SIZE_PART;
                    int endPosition = (i + 1) * FileHandler.SIZE_PART;
                    if (endPosition > fileData.length) {
                        endPosition = fileData.length;
                    }
                    FileMessage msgFile = new FileMessage(fileName, folderServer,
                            Arrays.copyOfRange(fileData, startPosition, endPosition), i, partCount, LogQuery.LISTFILELOG);
                    sendMessage(msgFile);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    // методы отправки команд на сервер (для разных команд свои конструктры)
    public void sendCommand(String fileNameForChange, Command idCommand){
        CommandMessage msgCmd = new CommandMessage(fileNameForChange, idCommand, LogQuery.COMMANDLOG);
        sendMessage(msgCmd);
    }

    public void sendCommand(Command idCommand){
        CommandMessage msgCmd = new CommandMessage(idCommand, LogQuery.COMMANDLOG);
        sendMessage(msgCmd);
    }

    public void sendCommand(String fileNameForChange, Command idCommand, String msg){
        CommandMessage msgCmd = new CommandMessage(fileNameForChange, idCommand, msg, LogQuery.COMMANDLOG);
        sendMessage(msgCmd);
    }

    public synchronized void sendMessage(AbstractMessage message){
        try{
            out.writeObject(message);
            out.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
//методы работы с локальными файлами
    public void deleteFile(String fileName){
        if(FileHandler.checkFile(fileName, folderLocal + "\\")){
            FileHandler.deleteFile(fileName, folderLocal + "\\");
        }
    }

    public void deleteFolder(String fileName){
            FileHandler.deleteFolder(fileName, folderLocal + "\\");
    }

    public void renameFile(String fileName, String newFileName){
        if(FileHandler.checkFile(fileName, folderLocal + "\\")){
            FileHandler.renameFile(fileName, newFileName, folderLocal + "\\");
        }
    }

    //регистрация нового клиента
    public void registrateNewClient(String[] dates, Command idCommand){
        CommandMessage msgCmd = new CommandMessage(idCommand, dates, LogQuery.COMMANDLOG);
        sendMessage(msgCmd);
    }

    public void createFolder(String folder){
            FileHandler.addFolder(folderLocal + "\\" + folder);
    }

//  метод скачивания файла с сервера, повторяет метод сохранении файла, полученного от клиента
    public void saveFileFromServer(AbstractMessage msg){
            Path pathOutServer = Paths.get(folderLocal + "\\" + msg.getFileName());
            FileHandler.writeFile(msg, pathOutServer);
    }

    public void saveFolderFromServerList(AbstractMessage msg) {
        try{
            Path pathInServer = Paths.get(msg.getNewFileName());
            if(Files.notExists(pathInServer)) Files.createDirectory(pathInServer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void saveFileFromServerList(AbstractMessage msg){
            Path pathOutServer = Paths.get(msg.getNewFileName());
            FileHandler.writeFile(msg, pathOutServer);
    }

    public void clearListFile(){listClientFile.clear();}
    public void clearListFileLocal(){listClientFileLocal.clear();}
    public void setListClientFileLocal(ArrayList<String> list) {this.listClientFileLocal = list;}
    public ArrayList<String> getListClientFileLocal(){return listClientFileLocal;}

    public ArrayList<String> getListClientFile(){return listClientFile;}
    public ArrayList<String> getLogins(){return logins;}
    public ArrayList<String> getNicks(){return nicks;}
    public String getLogin(){return login;}
    public String getNick(){return nick;}
    public void setLogin(String login){this.login = login;}
    public void setNick(String nick){this.nick = nick;}
    public String getFolderServer(){return folderServer;}

}
