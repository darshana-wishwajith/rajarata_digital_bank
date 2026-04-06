package com.rajarata.bank.ui.fx;

import com.rajarata.bank.models.user.User;
import com.rajarata.bank.exceptions.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Login screen for the JavaFX banking application.
 * Provides email/password authentication with error feedback.
 */
public class LoginScreen {

    private final ScreenManager screenManager;
    private final VBox root;
    private Label statusLabel;

    public LoginScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.root = buildUI();
    }

    private VBox buildUI() {
        VBox container = new VBox();
        container.getStyleClass().add("auth-container");
        container.setAlignment(Pos.CENTER);

        // Card
        VBox card = new VBox(16);
        card.getStyleClass().add("auth-card");
        card.setAlignment(Pos.CENTER);

        // Bank logo / title
        Label bankIcon = new Label("🏦");
        bankIcon.setStyle("-fx-font-size: 40px;");

        Label title = new Label("Rajarata Digital Bank");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Sign in to your account");
        subtitle.getStyleClass().add("auth-subtitle");

        // Email field
        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("field-label");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefHeight(40);

        // Password field
        Label pwdLabel = new Label("Password");
        pwdLabel.getStyleClass().add("field-label");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Enter your password");
        pwdField.setPrefHeight(40);

        // Status label for errors/success
        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(380);
        statusLabel.setVisible(false);

        // Login button
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(42);
        loginBtn.setOnAction(e -> handleLogin(emailField.getText(), pwdField.getText()));

        // Enter key support
        pwdField.setOnAction(e -> handleLogin(emailField.getText(), pwdField.getText()));

        // Register link
        HBox registerBox = new HBox(4);
        registerBox.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Don't have an account?");
        noAccount.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.getStyleClass().add("auth-link");
        registerLink.setOnAction(e -> screenManager.showRegistrationScreen());
        registerBox.getChildren().addAll(noAccount, registerLink);

        // Forgot password link
        Hyperlink forgotLink = new Hyperlink("Forgot your password?");
        forgotLink.getStyleClass().add("auth-link");
        forgotLink.setOnAction(e -> showForgotPasswordDialog());

        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 4, 0));

        card.getChildren().addAll(
                bankIcon, title, subtitle,
                new Separator(),
                emailLabel, emailField,
                pwdLabel, pwdField,
                statusLabel,
                loginBtn,
                sep,
                registerBox,
                forgotLink
        );

        container.getChildren().add(card);
        return container;
    }

    private void handleLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }
        try {
            User user = screenManager.getBank().getAuthService().login(email, password);
            screenManager.showDashboard(user);
        } catch (AccountLockedException e) {
            showError("Account locked: " + e.getMessage());
        } catch (InvalidInputException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText("⚠ " + message);
        statusLabel.getStyleClass().setAll("alert-error");
        statusLabel.setVisible(true);
    }

    private void showForgotPasswordDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Password Recovery");
        dialog.setHeaderText("Recover your password");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setPrefWidth(380);

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");

        Label questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setVisible(false);

        TextField answerField = new TextField();
        answerField.setPromptText("Security answer");
        answerField.setVisible(false);

        PasswordField newPwdField = new PasswordField();
        newPwdField.setPromptText("New password");
        newPwdField.setVisible(false);

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);

        content.getChildren().addAll(
                new Label("Email:"), emailField,
                questionLabel, answerField, newPwdField, resultLabel
        );

        dialog.getDialogPane().setContent(content);
        
        ButtonType actionButtonType = new ButtonType("Look Up Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(actionButtonType, ButtonType.CANCEL);
        
        Button actionBtn = (Button) dialog.getDialogPane().lookupButton(actionButtonType);
        actionBtn.getStyleClass().add("btn-primary");
        
        // Prevent dialog from closing and handle state-based logic
        actionBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (actionBtn.getText().equals("Look Up Account")) {
                String question = screenManager.getBank().getAuthService().getSecurityQuestion(emailField.getText());
                if (question != null) {
                    questionLabel.setText("Security Question: " + question);
                    questionLabel.setVisible(true);
                    answerField.setVisible(true);
                    newPwdField.setVisible(true);
                    actionBtn.setText("Reset Password");
                } else {
                    resultLabel.setText("No account found for this email.");
                    resultLabel.setStyle("-fx-text-fill: #E74C3C;");
                }
            } else {
                try {
                    boolean ok = screenManager.getBank().getAuthService()
                            .recoverPassword(emailField.getText(), answerField.getText(), newPwdField.getText());
                    if (ok) {
                        resultLabel.setText("✓ Password reset! You can now login.");
                        resultLabel.setStyle("-fx-text-fill: #27AE60;");
                        actionBtn.setDisable(true);
                    } else {
                        resultLabel.setText("✗ Incorrect security answer.");
                        resultLabel.setStyle("-fx-text-fill: #E74C3C;");
                    }
                } catch (InvalidInputException ex) {
                    resultLabel.setText("✗ " + ex.getMessage());
                    resultLabel.setStyle("-fx-text-fill: #E74C3C;");
                }
            }
            event.consume(); // Keep dialog open unless user clicks Close
        });

        dialog.showAndWait();
    }

    public VBox getRoot() { return root; }
}