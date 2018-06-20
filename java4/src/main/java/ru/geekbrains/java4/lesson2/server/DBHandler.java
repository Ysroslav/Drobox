package ru.geekbrains.java4.lesson2.server;

import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;

/*
* Класс управления базой данных.
 */

public class DBHandler {
    private static final String CON_STR = "jdbc:sqlite:C:/Users/Yaroslav/Documents/Projects/Drobox/java4/drobox.db";


    private Connection connection;

    public DBHandler() throws SQLException {
        DriverManager.registerDriver(new JDBC());
        this.connection = DriverManager.getConnection(CON_STR);
    }

    public void closeDBHandler(){
        try{
            this.connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    // проверка существует ли клиент
    public boolean checkAuthClient(String query, String login, String parol){
        try {
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, login);
            statement.setString(2, parol);
            ResultSet rs = statement.executeQuery();
            if(rs.next()) return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
    }
// получение локальной папки и ника клиента
    public String getFolderLocal(String query, String login, String parol){
        try {
            PreparedStatement statement = this.connection.prepareStatement(query);
            statement.setString(1, login);
            statement.setString(2, parol);
            ResultSet rs = statement.executeQuery();
            if(rs.next()) return rs.getString("LocalFile") + " "+ rs.getString("Nick");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    //получение логинов и ников для проверки совпадений
    public ArrayList<String> getListForRegistration(String query){
        ArrayList<String> list = new ArrayList();
        try {
            PreparedStatement statement = this.connection.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // для вставки новой записи в базу по зарегистрированному клиенту
    public void insertNewClient(String query, String[] dates){
        try {
            PreparedStatement statement = this.connection.prepareStatement(query);
            for(int i = 0; i<dates.length; i++)
                statement.setString(i+1, dates[i]);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
