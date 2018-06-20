package ru.geekbrains.java4.lesson2.server;

/*
Общий класс для работы с файлами и папками
 */

import ru.geekbrains.java4.lesson2.common.AbstractMessage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;



public class FileHandler {

    final public static int SIZE_PART = 1024;

    public static void deleteFile(String fileName, String folder){
        try{
            Files.delete(Paths.get(folder + fileName));
        } catch(IOException e){
            e.printStackTrace();
        }
    }


    public static void renameFile(String fileName, String newFileName, String folder){
            File file = new File(folder + fileName);
            File newFile = new File(folder + newFileName);
            file.renameTo(newFile);
    }

    public static boolean checkFile(String fileName, String folder){
        return Files.exists(Paths.get(folder + fileName));
    }

    public static void addFolder(String folder){
       try {
           if (!Files.exists(Paths.get(folder))) {
               Files.createDirectory(Paths.get(folder));
           }
       }catch(Exception e){
           e.printStackTrace();
       }
    }

    public static void deleteFolder(String fileName, String folder){
        File file = new File(folder + fileName);
        deleteFile(file);
    }


    private static void deleteFile(File element) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                deleteFile(sub);
            }
        }
        element.delete();
    }

    public static void writeFile(AbstractMessage msg, Path path){
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
            FileChannel outChannel = raf.getChannel();
            outChannel.position(msg.getPartsNumber()*FileHandler.SIZE_PART);
            ByteBuffer buf = ByteBuffer.allocate(msg.getDates().length);
            buf.put(msg.getDates());
            buf.flip();
            outChannel.write(buf);
            buf.clear();
            outChannel.close();
            raf.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

}
