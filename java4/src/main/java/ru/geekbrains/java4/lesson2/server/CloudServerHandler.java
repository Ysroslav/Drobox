package ru.geekbrains.java4.lesson2.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.java4.lesson2.common.*;
import sun.plugin2.message.Message;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;


/*
* Обработчик событий на сервере. Когда на сервер приходит объект, вызывается метод, который возращает
* LogQuery, (перечисляемый класс, который содержит перечень объектов, которые могут поступить от клиента)
 */

public class CloudServerHandler extends ChannelInboundHandlerAdapter {



    private DBHandler db;
    private ClientHandler client;
    private ArrayList<String> listFiles;

    public CloudServerHandler(){
        try{
            db = new DBHandler();
        } catch(Exception e){
            e.printStackTrace();
        }
        client = new ClientHandler();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            if (msg instanceof AbstractMessage) {
                String[] msgFromClient = null;
                if(((AbstractMessage) msg).getMsg() !=null) msgFromClient = ((AbstractMessage) msg).getMsg().split(" ");
                LogQuery log = ((AbstractMessage) msg).getLogQuery();
                switch(log){
                    case AUTHLOG:
                        // запрос на авторизацию
                        client.checkAuthForClient(db, msgFromClient[0], msgFromClient[1], ctx);
                        break;
                    case FILELOG:
                        //обработка файла
                        client.saveFileFromClient((AbstractMessage) msg, client.getLogin());
                        break;
                        //обработка команд
                    case COMMANDLOG:
                         String partOfMessage = ((AbstractMessage) msg).getMsg();
                         handleCommand(ctx, msg,partOfMessage);
                        break;
                    case FOLDERLOG:
                        // создание папки на сервере, возможно потом сделать просто обработку команды
                        client.saveFolderFromClient((AbstractMessage) msg, client.getLogin());
                        break;
                        //обработка вложенных папок
                    case LISTFOLDERLOG:
                        client.saveFolderFromClient((AbstractMessage) msg);
                        break;
                        //обработка вложенных файлов
                    case LISTFILELOG:
                        client.saveFileFromClient((AbstractMessage) msg);
                        break;
                    default:
                        System.out.printf("Неизвестная команда");
                        return;
                }
            } else {
                System.out.printf("Server received wrong object!");
                return;
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    //метод который обрабатывает команды, которые приходят от клиента
    private void handleCommand(ChannelHandlerContext ctx, Object msg, String partOfMessage) throws Exception{
        Command cmd = ((AbstractMessage) msg).getIdCommand();
        switch (cmd){
            case DELETE:
                client.deleteFile(((AbstractMessage) msg).getFileNameForChange(), client.getLogin());
                break;
            case RENAME:
                System.out.println(partOfMessage);
                client.renameFile(((AbstractMessage) msg).getFileNameForChange(), partOfMessage, client.getLogin());
                break;
            case LOAD:
                Path path = Paths.get(client.getFolderServer() + ((AbstractMessage) msg).getFileNameForChange());
                client.sendFile(path, ctx);
                break;
            case GETLOGIN:
                ListMessage listMessage = new ListMessage(client.getListForRegistration(db, 1), "logins", LogQuery.LISTLOG);
                ctx.channel().writeAndFlush(listMessage);
                ListMessage listMessage1 = new ListMessage(client.getListForRegistration(db, 2), "nicks", LogQuery.LISTLOG);
                for(int i = 0; i<client.getListForRegistration(db, 2).size();i++)
                ctx.channel().writeAndFlush(listMessage1);
                break;
            case CREATECLIENT:
                client.registrateNewClient(db, ((AbstractMessage) msg).getDatesString());
                client.addFolderNewClient(((AbstractMessage) msg).getDatesString()[0]);
                break;
            case GETLISTFILES:
                listFiles = client.getListFilesFolder(((AbstractMessage) msg).getFileNameForChange());
                for(int i = 0; i<listFiles.size(); i++){
                    ChatMessage m = new ChatMessage(listFiles.get(i), LogQuery.FILESERVERLOG);
                    ctx.channel().writeAndFlush(m);
                }
                break;
            case CREATEFOLDER:
                client.saveFolderFromClientChange((AbstractMessage) msg, client.getLogin());
                break;
                //скачать каталог
            case LOADFOLDER:
                Path pathIn = Paths.get(client.getFolderServer() + "\\" + ((AbstractMessage) msg).getFileNameForChange());
                if(Files.isDirectory(pathIn)){
                    LinkedList<String> list = client.getListFile(pathIn);
                    String str = client.getFolderClient() + pathIn.toString().substring(pathIn.toString().lastIndexOf("\\"));
                    FileMessage folderMsg = new FileMessage(pathIn, str, LogQuery.LISTFOLDERLOG);
                    ctx.channel().writeAndFlush(folderMsg);
                    for(int i = 0; i<list.size(); i++) {
                        if(Files.isDirectory(Paths.get(list.get(i)))) {
                            FileMessage fileMsg = new FileMessage(Paths.get(list.get(i)),
                                    list.get(i).replace(pathIn.toString(), str),  LogQuery.LISTFOLDERLOG);
                            ctx.channel().writeAndFlush(fileMsg);
                        } else {
                            client.sendFile(Paths.get(list.get(i)), list.get(i).replace(pathIn.toString(), str), ctx);
                        }
                    }
                } else {
                    client.sendFile(pathIn, ctx);
                }
                break;
            default:
                System.out.println("Неизвестная команда");
                return;
        }
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //ctx.flush();
        ctx.close();
    }


}
