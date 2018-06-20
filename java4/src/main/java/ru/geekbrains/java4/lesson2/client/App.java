package ru.geekbrains.java4.lesson2.client;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import ru.geekbrains.java4.lesson2.common.Command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;


/* Класс для создания интерфейса. При запуске, открывается окно, в которое пользователь, должен
 * внести логин и пароль (используется одна Scene sc), если автризация прошла успешно открывается
 * основная сцена (scMain), сервер пересылает клиенту список файлов, и список файлов отражается в
 * таблице (TableView). Файлы и папки, которые находяться на стороне клиента, также отражаются
 * в таблице TableView. Управление файлами и каталогами осуществляется с помощью контекстного меню
 */




public class App extends Application {
    private TableView<HandlerFileForTable> table;
    private TableView<HandlerFileForTable> tableLocal;
    private Client client;
    ArrayList<HandlerFileForTable> myFiles;
    ArrayList<HandlerFileForTable> myFilesLocal;
    ObservableList<HandlerFileForTable> myList;
    ObservableList<HandlerFileForTable> myListLocal;
    ArrayList<String> logins;
    ArrayList<String> nicks;
    private String pathForCatalogue;
    private String pathForLocalCatalogue;
    private Label lblNick;


    public App(){
        this.client = new Client();
    }

    public static void main(String[] args) {

        launch(args);
    }

    // метод отрисовывает ярлыки для файлов
    private  void pullCell(TableColumn colImage){
        colImage.setCellFactory(param -> {
            final Circle circle = new Circle();
            circle.setRadius(14);


            TableCell<HandlerFileForTable, Image> cell = new TableCell<HandlerFileForTable, Image>() {
                public void updateItem(Image item, boolean empty) {
                    if (item != null) {
                        circle.setStroke(Color.WHITE);
                        circle.setStrokeWidth(2);
                        ImagePattern pattern = new ImagePattern(item);
                        circle.setFill(pattern);
                    } else {
                        circle.setStroke(Color.valueOf("#616161"));
                        circle.setFill(Color.valueOf("#616161"));
                    }
                }
            };
            cell.setGraphic(circle);
            return cell;
        });
    }

    // создание таблицы для файлов на стороне сервера
    public TableView<HandlerFileForTable> createTable(ArrayList<String> list){
        myFiles = new ArrayList<>();
        for(int i=0; i<list.size(); i++){
            myFiles.add(new HandlerFileForTable(list.get(i)));
        }
        myList = FXCollections.observableArrayList(myFiles);
        TableColumn<HandlerFileForTable, String> colFile = new TableColumn("Мои файлы");
        colFile.setCellValueFactory(new PropertyValueFactory<HandlerFileForTable, String>("file"));
        colFile.setCellFactory(TextFieldTableCell.<HandlerFileForTable> forTableColumn());



        TableColumn colImage = new TableColumn("Ярлык");
        pullCell(colImage);


        colImage.setCellValueFactory(new PropertyValueFactory<HandlerFileForTable, Image>("label"));
        TableView<HandlerFileForTable> tab =  new TableView<HandlerFileForTable>();

        this.pathForCatalogue = "";  // строка для хранение пути к вложенным папкам



        tab.setRowFactory(
                new Callback<TableView<HandlerFileForTable>, TableRow<HandlerFileForTable>>() {
                    @Override
                    public TableRow<HandlerFileForTable> call(TableView<HandlerFileForTable> tableView) {
                        final TableRow<HandlerFileForTable> row = new TableRow<>();
                        // двойной шелчок мыши на папке и откроется содержимое папки
                        row.setOnMouseClicked(event->{
                            if (event.getClickCount() == 2 && (!row.isEmpty())) {
                                HandlerFileForTable rowData = row.getItem();
                                if(rowData.getFile().lastIndexOf(".")==-1) {
                                    pathForCatalogue = pathForCatalogue + rowData.getFile();
                                    updateListServer();
                                    pathForCatalogue = pathForCatalogue + "\\";
                                }
                            }
                        });

                       //создание контекстного меню
                        final ContextMenu rowMenu = new ContextMenu();
                        MenuItem renameItem = new MenuItem("Переименовать");
                        MenuItem removeItem = new MenuItem("Удалить");
                        MenuItem loadItem = new MenuItem("Скачать");
                        MenuItem createItem = new MenuItem("Создать");
                        //переименование
                        renameItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Новое имя файла или папки");
                                alert.setHeaderText("Введите новое имя файла");
                                TextField text = new TextField(row.getItem().getFile());
                                alert.getDialogPane().setContent(text);
                                Optional<ButtonType> option = alert.showAndWait();
                                if(option.get()==ButtonType.OK) {
                                        client.sendCommand(pathForCatalogue + row.getItem().getFile(), Command.RENAME, pathForCatalogue +text.getText());
                                        updateListServer();
                                    }
                                }
                        });
                        //удаление
                        removeItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                client.sendCommand(pathForCatalogue +  row.getItem().getFile(), Command.DELETE);
                                updateListServer();
                            }
                        });
                       //скачивание
                        loadItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                client.sendCommand(pathForCatalogue + row.getItem().getFile(), Command.LOADFOLDER);
                                try{Thread.sleep(500);}catch(Exception e){e.printStackTrace();}
                                HandlerFileForTable file = new HandlerFileForTable();
                                ArrayList<String> list = file.pullListFile(Paths.get(client.getFolderLocal() +"\\"+ pathForLocalCatalogue));
                                refreshTable(list, false);
                            }
                        });
                        // создание папки
                        createItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Новая папка");
                                alert.setHeaderText("Введите имя папки");
                                TextField text = new TextField("Новая папка");
                                alert.getDialogPane().setContent(text);
                                //alert.show();
                                boolean b=true;
                                while(b)  {
                                    Optional<ButtonType> option = alert.showAndWait();
                                    if(option.get()==ButtonType.OK) {
                                    int check = 0;
                                    for (int i = 0; i < client.getListClientFile().size(); i++) {
                                        if (client.getListClientFile().get(i).equals(text.getText())) break;
                                        else check++;
                                    }
                                    if (check == client.getListClientFile().size()) {
                                        client.sendCommand(pathForCatalogue + text.getText(), Command.CREATEFOLDER);
                                        b = false;
                                        updateListServer();
                                    }
                                    else {
                                        alert.setHeaderText("Данное имя уже используется. Придумайте новое");
                                    }
                                }
                                }
                            }
                        });
                        rowMenu.getItems().addAll(renameItem, removeItem, loadItem, createItem);
                        row.contextMenuProperty().bind(
                                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                        .then(rowMenu)
                                        .otherwise((ContextMenu)null));
                        return row;
                    }
                });


        tab.setItems(myList);
        tab.getColumns().addAll(colImage, colFile);
        return tab;
    }
// создание таблицы для хранения файлов на стороне клиента
    private TableView<HandlerFileForTable> createTableLocalFile(String pathLocal){
        Path path = Paths.get(pathLocal);
        myFilesLocal = new ArrayList<>();
        HandlerFileForTable file = new HandlerFileForTable();
        client.setListClientFileLocal(file.pullListFile(path));
        for(int i = 0; i<client.getListClientFileLocal().size(); i++){
            myFilesLocal.add(new HandlerFileForTable(client.getListClientFileLocal().get(i)));
        }
        myListLocal = FXCollections.observableArrayList(myFilesLocal);
        TableColumn<HandlerFileForTable, String> colFile = new TableColumn<HandlerFileForTable, String>("Мои файлы");
        colFile.setCellValueFactory(new PropertyValueFactory<HandlerFileForTable, String>("file"));
        colFile.setCellFactory(TextFieldTableCell.<HandlerFileForTable> forTableColumn());


        TableColumn colImage = new TableColumn("Ярлык");
        pullCell(colImage);


        colImage.setCellValueFactory(new PropertyValueFactory<HandlerFileForTable, Image>("label"));
        TableView<HandlerFileForTable> tab =  new TableView<HandlerFileForTable>();
        this.pathForLocalCatalogue="";

        tab.setRowFactory(
                new Callback<TableView<HandlerFileForTable>, TableRow<HandlerFileForTable>>() {
                    @Override
                    public TableRow<HandlerFileForTable> call(TableView<HandlerFileForTable> tableView) {
                        final TableRow<HandlerFileForTable> row = new TableRow<>();

                        row.setOnMouseClicked(event->{
                            if (event.getClickCount() == 2 && (!row.isEmpty())) {
                                HandlerFileForTable rowData = row.getItem();
                                if(rowData.getFile().lastIndexOf(".")==-1) {
                                    pathForLocalCatalogue = pathForLocalCatalogue + rowData.getFile();
                                    client.setListClientFileLocal(rowData.pullListFile(Paths.get(pathLocal + "\\" + pathForLocalCatalogue)));
                                    refreshTable(client.getListClientFileLocal(), false);
                                    tableLocal.refresh();
                                    pathForLocalCatalogue = pathForLocalCatalogue + "\\";
                                }
                            }
                        });

                        final ContextMenu rowMenu = new ContextMenu();
                        MenuItem renameItem = new MenuItem("Переименовать");
                        MenuItem removeItem = new MenuItem("Удалить");
                        MenuItem sendItem = new MenuItem("Отправить");
                        MenuItem createItem = new MenuItem("Создать");
                        renameItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {

                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Новое имя файла или папки");
                                alert.setHeaderText("Введите новое имя файла");
                                TextField text = new TextField(row.getItem().getFile());
                                alert.getDialogPane().setContent(text);
                                //alert.show();
                                Optional<ButtonType> option = alert.showAndWait();
                                if(option.get()==ButtonType.OK){
                                    client.renameFile(pathForLocalCatalogue + row.getItem().getFile(),
                                            pathForLocalCatalogue +text.getText());
                                    HandlerFileForTable file = new HandlerFileForTable();
                                    ArrayList<String> list = file.pullListFile(Paths.get(client.getFolderLocal() +"\\"+ pathForLocalCatalogue));
                                    refreshTable(list, false);
                                }
                            }
                        });

                        removeItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                if(Files.isDirectory(Paths.get(client.getFolderLocal() + "\\" + row.getItem().getFile()))) {
                                    client.deleteFolder(pathForLocalCatalogue + row.getItem().getFile());
                                } else {
                                    client.deleteFile(pathForLocalCatalogue + row.getItem().getFile());
                                }
                                HandlerFileForTable file = new HandlerFileForTable();
                                ArrayList<String> list = file.pullListFile(Paths.get(client.getFolderLocal() +"\\"+ pathForLocalCatalogue));
                                refreshTable(list, false);
                            }
                        });

                        sendItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {
                                if(Files.isDirectory(Paths.get(client.getFolderLocal() + "\\" + pathForLocalCatalogue + row.getItem().getFile()))){
                                    client.sendListFile(Paths.get(client.getFolderLocal() + "\\"+ pathForLocalCatalogue + row.getItem().getFile()));
                                } else {
                                    client.sendFile(Paths.get(client.getFolderLocal() + "\\"+ pathForLocalCatalogue + row.getItem().getFile()));
                                }
                                updateListServer();
                            }
                        });

                        createItem.setOnAction(new EventHandler<ActionEvent>() {

                            @Override
                            public void handle(ActionEvent event) {

                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle("Новая папка");
                                alert.setHeaderText("Введите имя папки");
                                TextField text = new TextField("Новая папка");
                                alert.getDialogPane().setContent(text);
                                //alert.show();
                                boolean b = true;
                                while (b) {
                                    Optional<ButtonType> option = alert.showAndWait();
                                    if (option.get() == ButtonType.OK) {
                                        int check = 0;
                                        for (int i = 0; i < client.getListClientFileLocal().size(); i++) {
                                            if (client.getListClientFileLocal().get(i).equals(text.getText())) break;
                                            else check++;
                                        }
                                        if (check == client.getListClientFileLocal().size()) {
                                            client.createFolder(pathForLocalCatalogue + text.getText());
                                            HandlerFileForTable file = new HandlerFileForTable();
                                            ArrayList<String> list = file.pullListFile(Paths.get(client.getFolderLocal() +"\\"+ pathForLocalCatalogue));
                                            refreshTable(list, false);
                                            b = false;
                                        } else {
                                            alert.setHeaderText("Данное имя уже используется. Придумайте новое");
                                        }
                                    }
                                }
                            }
                        });
                        rowMenu.getItems().addAll(renameItem, removeItem, sendItem, createItem);
                        row.contextMenuProperty().bind(
                                Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                        .then(rowMenu)
                                        .otherwise((ContextMenu)null));
                        return row;
                    }
                });


        tab.setItems(myListLocal);
        tab.getColumns().addAll(colImage, colFile);

        return tab;
    }
//обновление таблиц
    private void refreshTable(ArrayList<String> list, boolean server){
        ArrayList<HandlerFileForTable> listNew = new ArrayList<>();
        for(int i=0; i<list.size(); i++){
            listNew.add(new HandlerFileForTable(list.get(i)));
        }
        if(server) {
            myFiles = listNew;
            myList = FXCollections.observableArrayList(myFiles);
            table.setItems(myList);
        } else {
            myFilesLocal = listNew;
            myListLocal = FXCollections.observableArrayList(myFilesLocal);
            tableLocal.setItems(myListLocal);
        }
    }


//отрисовка форм интерфейса
    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();  //интерфейс для авторизации
        Group rootMain = new Group(); // основной интефейс
        Group rootReg = new Group(); // интерфейс для регистрации
        Scene sc = new Scene(root, 300, 200);
        Scene scMain = new Scene(rootMain, 800, 500);
        Scene scReg = new Scene(rootReg, 400, 250);



        rootMain.getStylesheets().add("MyStyle.css");
        root.getStylesheets().add("MyStyle.css");
        rootReg.getStylesheets().add("MyStyle.css");
        Rectangle rightPanel = new Rectangle(0,0,140,700);
        rightPanel.setFill(Color.GRAY);

        Circle avatar = new Circle(60);

        Image image = new Image("file:/C:/Users/Yaroslav/Documents/Projects/Drobox/java4/src/main/resources/vador.jpg");
        ImagePattern pattern = new ImagePattern(image);
        avatar.setStroke(Color.WHITE);
        avatar.setStrokeWidth(2);
        avatar.setFill(pattern);
        avatar.setCenterX(70);
        avatar.setCenterY(90);

        lblNick = new Label();
        lblNick.setTranslateX(0);
        lblNick.setTranslateY(150);
        lblNick.setMaxWidth(150);
        lblNick.setMinWidth(150);

        TextArea login = new TextArea();
        TextArea parol = new TextArea();
        Button authButton = new Button("Вход");
        Button addNewClientButton = new Button("Регистрация");

        TextArea loginReg = new TextArea();
        TextArea parolReg = new TextArea();
        TextArea nickReg = new TextArea();
        TextArea folderReg = new TextArea();
        Button regButton = new Button("Зарегистрироваться");
        Button addFolderButton = new Button("...");
        Button goStartButton = new Button("Назад");



        Label loginLbl = new Label("Логин");
        Label parolLbl = new Label("Пароль");
        Label loginlbl = new Label("Логин");
        Label parollbl = new Label("Пароль");
        Label folderLbl = new Label("Папка");
        Label nickLbl = new Label("Ник");
        Label controlLbl = new Label("");



        Label lblTableS = new Label("Облако");
        Label lblTableL = new Label("Локальная папка");

        lblTableS.setTranslateX(150);
        lblTableS.setTranslateY(20);

        lblTableL.setTranslateX(470);
        lblTableL.setTranslateY(20);


        Button addNewFileButton = new Button("Загрузить файл");
        Button createFolderButton = new Button("Загрузить папку");

        Button backFolderButton = new Button("<<");
        Button backFolderLocalButton = new Button("<<");

        addNewFileButton.setTranslateX(10);
        addNewFileButton.setTranslateY(200);

        createFolderButton.setTranslateX(10);
        createFolderButton.setTranslateY(235);

        backFolderButton.setTranslateX(220);
        backFolderButton.setTranslateY(15);

        backFolderLocalButton.setTranslateX(600);
        backFolderLocalButton.setTranslateY(15);

        addNewFileButton.setMaxSize(120,25);
        addNewFileButton.setMinSize(120,25);

        createFolderButton.setMaxSize(120,25);
        createFolderButton.setMinSize(120,25);

        backFolderButton.setMaxSize(40,20);
        backFolderButton.setMinSize(40,20);

        backFolderLocalButton.setMaxSize(40,20);
        backFolderLocalButton.setMinSize(40,20);

        login.setMaxSize(200,25);
        login.setMinSize(200,25);
        parol.setMaxSize(200,25);
        parol.setMinSize(200,25);
        authButton.setMaxSize(250,25);
        authButton.setMinSize(250,25);

        addNewClientButton.setMaxSize(250,25);
        addNewClientButton.setMinSize(250,25);
        login.setTranslateX(70);
        login.setTranslateY(40);
        parol.setTranslateX(70);
        parol.setTranslateY(75);
        authButton.setTranslateX(20);
        authButton.setTranslateY(110);
        addNewClientButton.setTranslateX(20);
        addNewClientButton.setTranslateY(145);



        loginReg.setMaxSize(300,25);
        loginReg.setMinSize(300,25);
        parolReg.setMaxSize(300,25);
        parolReg.setMinSize(300,25);
        folderReg.setMaxSize(250,25);
        folderReg.setMinSize(250,25);

        nickReg.setMaxSize(300,25);
        nickReg.setMinSize(300,25);

        regButton.setMaxSize(300,25);
        regButton.setMinSize(300,25);

        goStartButton.setMaxSize(300,25);
        goStartButton.setMinSize(300,25);

        addFolderButton.setMaxSize(40,25);
        addFolderButton.setMinSize(40,25);

        loginReg.setTranslateX(70);
        loginReg.setTranslateY(40);
        parolReg.setTranslateX(70);
        parolReg.setTranslateY(75);
        nickReg.setTranslateX(70);
        nickReg.setTranslateY(110);
        folderReg.setTranslateX(70);
        folderReg.setTranslateY(145);
        folderReg.setEditable(false);
        regButton.setTranslateX(70);
        regButton.setTranslateY(180);
        goStartButton.setTranslateX(70);
        goStartButton.setTranslateY(215);
        addFolderButton.setTranslateX(330);
        addFolderButton.setTranslateY(145);
        loginLbl.setTranslateX(15);
        loginLbl.setTranslateY(45);
        loginlbl.setTranslateX(15);
        loginlbl.setTranslateY(45);
        parollbl.setTranslateX(15);
        parollbl.setTranslateY(75);
        parolLbl.setTranslateX(15);
        parolLbl.setTranslateY(80);
        nickLbl.setTranslateX(15);
        nickLbl.setTranslateY(115);
        folderLbl.setTranslateX(15);
        folderLbl.setTranslateY(150);

        controlLbl.setTranslateX(10);
        controlLbl.setTranslateY(10);

        regButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean check = true;
                if(loginReg.getText().length()==0){
                    loginReg.setStyle("-fx-background-color: red");
                    check = false;
                }
                if(parolReg.getText().length()==0){
                    parolReg.setStyle("-fx-background-color: red");
                    check = false;
                }
                if(nickReg.getText().length()==0){
                    nickReg.setStyle("-fx-background-color: red");
                    check = false;
                }
                if(folderReg.getText().length()==0){
                    folderReg.setStyle("-fx-background-color: red");
                    check = false;
                }
                if(!check){
                   controlLbl.setText("Заполните все поля");
                   return;}
                else {
                    logins = client.getLogins();
                    nicks = client.getNicks();
                    int i = 0;
                    for(;i<logins.size();i++)
                        if(logins.get(i).equals(loginReg.getText())) break;
                    if(i<logins.size()) {
                        loginReg.setStyle("-fx-background-color: red");
                        controlLbl.setText("Данный логин уже занят");
                        return;}
                    i = 0;
                    for(;i<nicks.size();i++)
                        if(nicks.get(i).equals(nickReg.getText())) break;
                    if(i<nicks.size()) {
                        nickReg.setStyle("-fx-background-color: red");
                        controlLbl.setText("Данный ник уже занят");
                        return;}
                        String[] dates = new String[4];
                        dates[0] = loginReg.getText();
                        dates[1] = parolReg.getText();
                        dates[2] = nickReg.getText();
                        dates[3] = folderReg.getText();
                    client.registrateNewClient(dates, Command.CREATECLIENT);
                    primaryStage.setScene(sc);
                }
            }
        });

        loginReg.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                loginReg.setStyle("-fx-background-color: grey");
            }
        });

        parolReg.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                parolReg.setStyle("-fx-background-color: grey");
            }
        });

        nickReg.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                nickReg.setStyle("-fx-background-color: grey");
            }
        });

        folderReg.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                folderReg.setStyle("-fx-background-color: grey");
            }
        });

        addFolderButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Stage stage = primaryStage;
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File dir = directoryChooser.showDialog(stage);
                if(dir!=null){
                    folderReg.setText(dir.toString());
                } else folderReg.setText(null);
            }
        });


        authButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                if(login.getText().length() ==0){
                    System.out.println("Поле логин не заполнено");
                    return;
                }
                if(parol.getText().length() ==0){
                    System.out.println("Поле пароль не заполнено");
                    return;
                }
                if(!client.checkAccessClient(login.getText(), parol.getText())){
                    login.clear();
                    parol.clear();
                    return;
                }
                client.setLogin(login.getText());
                primaryStage.setScene(scMain);
                primaryStage.setTitle("Мои файлы");
                table = createTable(client.getListClientFile());
                tableLocal = createTableLocalFile(client.getFolderLocal());
                table.setTranslateX(150);
                table.setTranslateY(40);
                table.setMinWidth(300);
                table.setMinHeight(400);
                rootMain.getChildren().add(table);
                tableLocal.setTranslateX(470);
                tableLocal.setTranslateY(40);
                tableLocal.setMinWidth(300);
                tableLocal.setMinHeight(400);
                rootMain.getChildren().add(tableLocal);
                lblNick.setText(client.getNick());
            }
        });
        final Stage stage = primaryStage;
        createFolderButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File dir = directoryChooser.showDialog(stage);
                if(dir!=null) {
                    client.sendListFile(Paths.get(dir.getPath()));
                    updateListServer();
                }
            }
        });

        addNewClientButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                client.sendCommand(Command.GETLOGIN); //запрос списка логинов
                primaryStage.setScene(scReg);
                primaryStage.setTitle("Регистрация");
                try{Thread.sleep(500);}catch(Exception e){e.printStackTrace();}
                logins = client.getLogins();
                nicks = client.getNicks();
            }
        });

        addNewFileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(stage);
                if(file!=null) {
                    client.sendFile(file.toPath());
                    try{Thread.sleep(500);}catch(Exception e){e.printStackTrace();}
                    updateListServer();
                }
            }
        });

        backFolderButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(pathForCatalogue.length()>0) {
                    String s[] = pathForCatalogue.split("\\\\");
                    String str ="";
                    for(int i = 0; i<s.length-1; i++){
                        str = str + s[i] + "\\";
                    }
                    client.clearListFile();
                    client.sendCommand(str, Command.GETLISTFILES); // получение списка файлов из под католога
                    try {
                        Thread.sleep(500);
                    }catch(Exception e) {e.printStackTrace();}
                    refreshTable(client.getListClientFile(), true);
                    pathForCatalogue = str;
                }
            }});

        backFolderLocalButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (pathForLocalCatalogue.length() > 0) {
                    String s[] =  pathForLocalCatalogue.split("\\\\");
                    String str = "";
                    for (int i = 0; i < s.length - 1; i++) {
                        str = str + s[i] + "\\";
                    }
                    HandlerFileForTable file = new HandlerFileForTable();
                    ArrayList<String> list = file.pullListFile(Paths.get(client.getFolderLocal() +"\\"+ str));
                    refreshTable(list, false);
                    pathForLocalCatalogue = str;
                }
            }
        });

        goStartButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.setScene(sc);
                primaryStage.setTitle("Старт");
            }
        });


        root.getChildren().add(loginlbl);
        root.getChildren().add(parollbl);
        root.getChildren().add(login);
        root.getChildren().add(parol);
        root.getChildren().add(authButton);
        root.getChildren().add(addNewClientButton);

        rootMain.getChildren().add(rightPanel);

        rootMain.getChildren().add(addNewFileButton);
        rootMain.getChildren().add(createFolderButton);
        rootMain.getChildren().add(backFolderButton);
        rootMain.getChildren().add(backFolderLocalButton);
        rootMain.getChildren().add(lblTableL);
        rootMain.getChildren().add(lblTableS);
        rootMain.getChildren().add(avatar);
        rootMain.getChildren().add(lblNick);

        rootReg.getChildren().addAll(loginReg, parolReg, folderReg, loginLbl,
                goStartButton, parolLbl, nickLbl, nickReg, folderLbl, controlLbl, regButton,  addFolderButton);

        sc.setFill(Color.BLACK);
        scMain.setFill(Color.BLACK);
        scReg.setFill(Color.BLACK);
        primaryStage.setTitle("Старт");
        primaryStage.setScene(sc);
        primaryStage.show();

    }

    //метод для обновление таблицы с файлами на стороне сервера
    private void updateListServer(){
        client.clearListFile();
        client.sendCommand(pathForCatalogue, Command.GETLISTFILES); // получение списка файлов из под католога
        try {
            Thread.sleep(500);
        }catch(Exception e) {e.printStackTrace();}
        refreshTable(client.getListClientFile(), true);
    }
}
