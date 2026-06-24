package ug.firstbank;

import javafx.application.Application;
import javafx.stage.Stage;
import ug.firstbank.ui.MainWindow;

public final class MainApp extends Application {

    private MainWindow mainWindow;

    @Override
    public void start(Stage primaryStage) {
        mainWindow = new MainWindow(primaryStage);

        primaryStage.setTitle("First Bank Uganda — New Account Opening");
        primaryStage.setScene(mainWindow.buildScene());
        primaryStage.setOnCloseRequest(e -> mainWindow.shutdown());
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (mainWindow != null) {
            mainWindow.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}