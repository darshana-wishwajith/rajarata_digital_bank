package com.rajarata.bank.ui.fx;

import com.rajarata.bank.Bank;
import com.rajarata.bank.models.user.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Manages screen navigation and scene switching for the JavaFX application.
 * Acts as a router between Login, Registration, and Dashboard screens.
 */
public class ScreenManager {

    private final Stage stage;
    private final Bank bank;
    private String cssPath;

    public ScreenManager(Stage stage) {
        this.stage = stage;
        this.bank = Bank.getInstance();
        // Resolve CSS path
        var cssUrl = getClass().getResource("styles.css");
        this.cssPath = cssUrl != null ? cssUrl.toExternalForm() : null;
    }

    /** Shows the login screen */
    public void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen(this);
        Scene scene = new Scene(loginScreen.getRoot(), stage.getWidth(), stage.getHeight());
        applyCss(scene);
        stage.setScene(scene);
    }

    /** Shows the registration screen */
    public void showRegistrationScreen() {
        RegistrationScreen regScreen = new RegistrationScreen(this);
        Scene scene = new Scene(regScreen.getRoot(), stage.getWidth(), stage.getHeight());
        applyCss(scene);
        stage.setScene(scene);
    }

    /** Shows the appropriate dashboard based on user role */
    public void showDashboard(User user) {
        Scene scene;
        switch (user.getRole()) {
            case "Customer":
                CustomerDashboard custDash = new CustomerDashboard(this, (Customer) user);
                scene = new Scene(custDash.getRoot(), stage.getWidth(), stage.getHeight());
                break;
            case "Staff":
                StaffDashboard staffDash = new StaffDashboard(this, (Staff) user);
                scene = new Scene(staffDash.getRoot(), stage.getWidth(), stage.getHeight());
                break;
            case "Administrator":
                AdminDashboard adminDash = new AdminDashboard(this, (Administrator) user);
                scene = new Scene(adminDash.getRoot(), stage.getWidth(), stage.getHeight());
                break;
            default:
                showLoginScreen();
                return;
        }
        applyCss(scene);
        stage.setScene(scene);
    }

    /** Logs out and returns to login screen */
    public void logout() {
        bank.getAuthService().logout();
        showLoginScreen();
    }

    /** Applies the CSS stylesheet to a scene */
    private void applyCss(Scene scene) {
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }
    }

    /** Gets the Bank singleton */
    public Bank getBank() { return bank; }

    /** Gets the primary Stage */
    public Stage getStage() { return stage; }
}
