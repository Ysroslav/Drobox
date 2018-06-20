package ru.geekbrains.java4.lesson2.server;

/*
* Класс для управления подключением клиента и передачи информации в базу по
* отдельному клиенту, содержит методы получение и отправления файлов
* удаление, переименования и создания папкок на стороне сервера
 */

import io.netty.channel.ChannelHandlerContext;
import ru.geekbrains.java4.lesson2.common.AbstractMessage;
import ru.geekbrains.java4.lesson2.common.ChatMessage;
import ru.geekbrains.java4.lesson2.common.FileMessage;
import ru.geekbrains.java4.lesson2.common.LogQuery;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class ClientHandler {

    final private static String QUERU_AUTH ="SELECT * FROM entries WHERE Login = ? AND Pass = ?";
    final private static String QUERU_LOGINS ="SELECT Login FROM entries";
    final private static String QUERU_NICKS ="SELECT Nick FROM entries";
    final private static String INSERT_CLIENT = "INSERT INTO entries (Login, Pass, Nick, LocalFile) VALUES (?, ?, ?, ?)";
    final private static String FOLDER_CLIENTS = "D:\\Drobox\\Server\\";

    private String folderClient;
    private ArrayList<String> listFiles;
    private File catalogue;
    private String login;
    private String nick;
    private LinkedList<String> listFileForSend;

    public ClientHandler(){
    }

    public String getLogin(){return login;}

    public boolean checkClient(DBHandler db, String login, String parol){
       return db.checkAuthClient(QUERU_AUTH, login, parol);
    }

    public String getFolderLocal(DBHandler db, String login, String parol){
        String str = db.getFolderLocal(QUERU_AUTH, login, parol);
        String s[] = str.split(" ");
        this.folderClient = s[0];
        this.nick = s[1];
        return folderClient;
    }

    // метод, который авторизирует клиента, и отправляет клиенту результат авторизации, список файлов,
    // локальную папку
    public void checkAuthForClient(DBHandler db, String login, String parol, ChannelHandlerContext ctx) throws Exception{
        if(checkClient(db, login, parol)){
            this.login = login;
            //отправляем сообщение об успешном подключении
            ChatMessage ans = new ChatMessage("true" + " " + FOLDER_CLIENTS + login + "\\", LogQuery.AUTHLOG);
            ctx.channel().writeAndFlush(ans);
            //отправляем список файлов
            getListFiles(login);
            for(int i = 0; i<listFiles.size(); i++){
                ChatMessage m = new ChatMessage(listFiles.get(i), LogQuery.FILESERVERLOG);
                ctx.channel().writeAndFlush(m);
            }
            //получение локальной папки клиента
            ChatMessage m = new ChatMessage(getFolderLocal(db, login, parol) +
                    " " + this.nick, LogQuery.FOLDERLOCALLOG);  // отправка локальной папки
            ctx.channel().writeAndFlush(m);
        } else {
            ChatMessage answer = new ChatMessage("false", LogQuery.AUTHLOG);
            ctx.channel().write(answer);
            ctx.flush();
        }

    }

    public String getFolderClient(){return folderClient;}

    //метод проверяет существует ли папка с файлами клиента на сервере, если нет создает данную папку
    // и возвращает пустой список файлов, если папка существует возвращает
    // список файлов и папок в этой папке.
    public ArrayList<String> getListFiles(String login) throws Exception{
        listFiles = new ArrayList<>();
        if(checkFolderClient(login)){
            String s[] = catalogue.list();
            for(int i = 0; i<s.length; i++)
                listFiles.add(s[i]);
        } else {
            catalogue.createNewFile();
            listFiles = null;
        }
        return listFiles;
    }

    public ArrayList<String> getListFilesFolder(String folder) throws Exception{
        listFiles = new ArrayList<>();
        if(checkFolderClient(login + "\\" + folder)){
            String s[] = catalogue.list();
            for(int i = 0; i<s.length; i++)
                listFiles.add(s[i]);
        }
        return listFiles;
    }

    // получение логинов и ников для проверки совпадений в базе
    public ArrayList<String> getListForRegistration(DBHandler db, int logOrNick) throws Exception{
        ArrayList<String> list;
        if(logOrNick==1) list = db.getListForRegistration(QUERU_LOGINS);
        else list = db.getListForRegistration(QUERU_NICKS);
        return list;
    }

    // регистрация нового клиента
    public void registrateNewClient(DBHandler db, String[] dates) throws Exception{
        db.insertNewClient(INSERT_CLIENT, dates);
    }

    //создание папки на сервере
    public void addFolderNewClient(String login){
        FileHandler.addFolder(FOLDER_CLIENTS + login);
    }


    private boolean checkFolderClient(String login){
       this.catalogue = new File(FOLDER_CLIENTS + login);
       return catalogue.exists();
    }

    //обработка полученного файла
    public void saveFileFromClient(AbstractMessage msg, String login){
            Path pathInServer = Paths.get(FOLDER_CLIENTS + login + "\\" + msg.getFileName());
            FileHandler.writeFile(msg, pathInServer);
    }
    //обработка полученного файла из вложенной папки
    public void saveFileFromClient(AbstractMessage msg){
            Path pathInServer = Paths.get(msg.getNewFileName());
            FileHandler.writeFile(msg, pathInServer);
    }

    //обработка полученной папки
    public void saveFolderFromClient(AbstractMessage msg, String login) {
        try{
        Path pathInServer = Paths.get(FOLDER_CLIENTS + login + "\\" + msg.getFileName());
        if(Files.notExists(pathInServer)) Files.createDirectory(pathInServer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    //обработка полученной папки из вложенной папки
    public void saveFolderFromClientChange(AbstractMessage msg, String login) {
        try{
            Path pathInServer = Paths.get(FOLDER_CLIENTS + login + "\\" + msg.getFileNameForChange());
            if(Files.notExists(pathInServer)) Files.createDirectory(pathInServer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //метод создает папку на сервере
    public void saveFolderFromClient(AbstractMessage msg) {
        try{
            System.out.println(msg.getFileName());
            Path pathInServer = Paths.get(msg.getNewFileName());
            if(Files.notExists(pathInServer)) Files.createDirectory(pathInServer);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //удаление файла
    public void deleteFile(String fileName, String login){
        if(Files.isDirectory(Paths.get(FOLDER_CLIENTS + login + "\\" + fileName))) {
            FileHandler.deleteFolder(fileName, FOLDER_CLIENTS + login + "\\");
        } else {
            if (FileHandler.checkFile(fileName, FOLDER_CLIENTS + login + "\\")) {
                FileHandler.deleteFile(fileName, FOLDER_CLIENTS + login + "\\");
            }
        }
    }

    //переименование файла
    public void renameFile(String fileName, String newFileName, String login){
        if(FileHandler.checkFile(fileName, FOLDER_CLIENTS + login + "\\")){
            FileHandler.renameFile(fileName, newFileName, FOLDER_CLIENTS + login + "\\");
        }
    }

    //получение списка файлов для отправки клиенту
    public LinkedList<String> getListFile(Path fileName){
        listFileForSend = new LinkedList<String>();
        File file = fileName.toFile();
        processFilesFromFolder(file);
        return listFileForSend;
    }

    //рекурсивный метод обхода вложенных папок
    private void processFilesFromFolder(File folder){
        File[] folderEntries = folder.listFiles();
        for(File entry: folderEntries) {
            listFileForSend.add(entry.getPath());
            if (entry.isDirectory()) {
                processFilesFromFolder(entry);
            }
        }
    }

    public String getFolderServer(){
        return FOLDER_CLIENTS + login + "\\";
    }

//отправка файла из вложенной папки
    public void sendFile(Path fileName, String folderServer, ChannelHandlerContext ctx){
        try{
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
                    FileMessage msgFile = new FileMessage(fileName, folderServer, Arrays.copyOfRange(fileData, startPosition, endPosition),
                            i, partCount, LogQuery.LISTFILELOG);
                    ctx.channel().writeAndFlush(msgFile);
                }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
// отправка файла
    public void sendFile(Path fileName, ChannelHandlerContext ctx){
        try{
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
                    ctx.writeAndFlush(msgFile);
                }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

}
