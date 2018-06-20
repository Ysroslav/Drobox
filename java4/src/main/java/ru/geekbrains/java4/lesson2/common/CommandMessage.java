package ru.geekbrains.java4.lesson2.common;


public class CommandMessage extends AbstractMessage {


    private Command idCommand;
    private String message;
    private String fileNameForChange;
    private String msg;
    private String[] datesString;
    private LogQuery logQuery;

   public CommandMessage(String fileNameForChange, Command idCommand, String message, LogQuery logQuery){
       this.idCommand = idCommand;
       this.fileNameForChange = fileNameForChange;
       this.datesString = null;
       this.msg = message;
       this.logQuery = logQuery;
   }

    public CommandMessage(Command idCommand, LogQuery logQuery){
        this.idCommand = idCommand;
        this.datesString = null;
        this.fileNameForChange = null;
        this.msg = null;
        this.logQuery = logQuery;
    }

    public CommandMessage(String fileNameForChange, Command idCommand, LogQuery logQuery){
        this.idCommand = idCommand;
        this.fileNameForChange = fileNameForChange;
        this.datesString = null;
        this.msg = null;
        this.logQuery = logQuery;

    }

    public CommandMessage(Command idCommand, String[] dates, LogQuery logQuery){
        this.idCommand = idCommand;
        this.datesString = dates;
        this.fileNameForChange = null;
        this.msg = null;
        this.logQuery = logQuery;

    }

    public Command getIdCommand(){return idCommand;}
    public String getFileNameForChange(){return fileNameForChange;}
    public String getMsg(){return msg;}
    public String[] getDatesString(){ return datesString;}
    public LogQuery getLogQuery(){return logQuery;}
}
