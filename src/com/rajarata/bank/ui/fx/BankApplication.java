package com.rajarata.bank.ui.fx;

import com.rajarata.bank.Bank;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for Rajarata Digital Bank.
 * Initializes the bank singleton, sets up the stage, and shows the login screen.
 *
 * @author Rajarata University Student
 * @version 1.0
 */
public class BankApplication extends Application {

    private static Stage primaryStage;
    private static ScreenManager screenManager;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;

            // Initialize the Bank backend
            Bank bank = Bank.getInstance();
            bank.initialize();

            // Create screen manager
            screenManager = new ScreenManager(stage);

            // Configure stage
            stage.setTitle("Rajarata Digital Bank");
            stage.setWidth(1100);
            stage.setHeight(720);
            stage.setMinWidth(900);
            stage.setMinHeight(600);

            // Show login screen
            screenManager.showLoginScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showFatalError("Failed to initialize application: " + e.getMessage());
        }
    }

    private void showFatalError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Fatal Error");
        alert.setHeaderText("Application Startup Failed");
        alert.setContentText(message + "\n\nPlease ensure you have write permissions to the application folder or run as administrator.");
        alert.showAndWait();
        System.exit(1);
    }

    @Override
    public void stop() {
        Bank.getInstance().shutdown();
    }

    /** Gets the primary stage */
    public static Stage getPrimaryStage() { return primaryStage; }

    /** Gets the screen manager */
    public static ScreenManager getScreenManager() { return screenManager; }

    public static void main(String[] args) {
        launch(args);
    }
}

