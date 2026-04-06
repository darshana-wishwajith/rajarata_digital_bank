package com.rajarata.bank.ui.fx;

import com.rajarata.bank.models.user.Customer;
import com.rajarata.bank.exceptions.InvalidInputException;
import com.rajarata.bank.utils.ValidationUtil;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Registration screen with real-time field validation.
 * Each field is validated as the user types/leaves the field.
 */
public class RegistrationScreen {

    private final ScreenManager screenManager;
    private final ScrollPane root;

    public RegistrationScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.root = buildUI();
    }

    private ScrollPane buildUI() {
        VBox container = new VBox();
        container.getStyleClass().add("auth-container");
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40, 0, 40, 0));

        VBox card = new VBox(12);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(520);

        Label title = new Label("Create Your Account");
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label("Join Rajarata Digital Bank today");
        subtitle.getStyleClass().add("auth-subtitle");

        // ---- Fields with real-time validation ----
        // Full Name
        Label nameLabel = new Label("Full Name");
        nameLabel.getStyleClass().add("field-label");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., John Doe");
        nameField.setPrefHeight(38);
        Label nameError = createErrorLabel();
        addValidation(nameField, nameError, "Invalid name. Use letters and spaces (min 2 chars).",
                v -> ValidationUtil.isValidName(v));

        // Email
        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("field-label");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g., john@example.com");
        emailField.setPrefHeight(38);
        Label emailError = createErrorLabel();
        addValidation(emailField, emailError, "Invalid email format.",
                v -> {
                    if (!ValidationUtil.isValidEmail(v)) return false;
                    if (screenManager.getBank().getAuthService().getUserByEmail(v) != null) {
                        emailError.setText("⚠ This email is already registered.");
                        return false;
                    }
                    return true;
                });

        // Phone
        Label phoneLabel = new Label("Phone Number");
        phoneLabel.getStyleClass().add("field-label");
        TextField phoneField = new TextField();
        phoneField.setPromptText("e.g., +94771234567");
        phoneField.setPrefHeight(38);
        Label phoneError = createErrorLabel();
        addValidation(phoneField, phoneError, "Invalid phone. Use 10-15 digits.",
                v -> ValidationUtil.isValidPhone(v));

        // Address
        Label addrLabel = new Label("Address");
        addrLabel.getStyleClass().add("field-label");
        TextField addrField = new TextField();
        addrField.setPromptText("Full address (min 10 chars)");
        addrField.setPrefHeight(38);
        Label addrError = createErrorLabel();
        addValidation(addrField, addrError, "Address must be at least 10 characters.",
                v -> ValidationUtil.isValidAddress(v));

        // DOB
        Label dobLabel = new Label("Date of Birth");
        dobLabel.getStyleClass().add("field-label");
        TextField dobField = new TextField();
        dobField.setPromptText("yyyy-MM-dd");
        dobField.setPrefHeight(38);
        Label dobError = createErrorLabel();
        addValidation(dobField, dobError, "Invalid date. Format: yyyy-MM-dd, must be 18+.",
                v -> ValidationUtil.isValidDateOfBirth(v));

        // Government ID
        Label govLabel = new Label("Government ID");
        govLabel.getStyleClass().add("field-label");
        TextField govField = new TextField();
        govField.setPromptText("5-20 alphanumeric characters");
        govField.setPrefHeight(38);
        Label govError = createErrorLabel();
        addValidation(govField, govError, "Invalid ID. Use 5-20 alphanumeric characters.",
                v -> ValidationUtil.isValidGovtId(v));

        // Password
        Label pwdLabel = new Label("Password");
        pwdLabel.getStyleClass().add("field-label");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Min 8 chars, upper+lower+digit+special");
        pwdField.setPrefHeight(38);
        Label pwdError = createErrorLabel();
        addValidation(pwdField, pwdError, ValidationUtil.getPasswordRequirements(),
                v -> ValidationUtil.isValidPassword(v));

        Label pwdHint = new Label("Use 8+ chars with uppercase, lowercase, digit, and @$!%*?&");
        pwdHint.setStyle("-fx-text-fill: #B0B8C1; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");

        // Confirm Password
        Label cpwdLabel = new Label("Confirm Password");
        cpwdLabel.getStyleClass().add("field-label");
        PasswordField cpwdField = new PasswordField();
        cpwdField.setPromptText("Re-enter your password");
        cpwdField.setPrefHeight(38);
        Label cpwdError = createErrorLabel();
        cpwdField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !cpwdField.getText().isEmpty()) {
                if (!cpwdField.getText().equals(pwdField.getText())) {
                    cpwdError.setText("⚠ Passwords do not match.");
                    cpwdError.setVisible(true);
                    cpwdField.setStyle("-fx-border-color: #E74C3C;");
                } else {
                    cpwdError.setVisible(false);
                    cpwdField.setStyle("");
                }
            }
        });

        // Security Question
        Label sqLabel = new Label("Security Question");
        sqLabel.getStyleClass().add("field-label");
        TextField sqField = new TextField();
        sqField.setPromptText("e.g., What is your pet's name?");
        sqField.setPrefHeight(38);
        Label sqError = createErrorLabel();
        addValidation(sqField, sqError, "Security question is required.",
                v -> ValidationUtil.isNotEmpty(v));

        // Security Answer
        Label saLabel = new Label("Security Answer");
        saLabel.getStyleClass().add("field-label");
        TextField saField = new TextField();
        saField.setPromptText("Your answer");
        saField.setPrefHeight(38);
        Label saError = createErrorLabel();
        addValidation(saField, saError, "Security answer is required.",
                v -> ValidationUtil.isNotEmpty(v));

        // Status label
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(440);
        statusLabel.setVisible(false);

        // Register button
        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(42);
        registerBtn.setOnAction(e -> {
            // Final validation check
            boolean valid = true;
            if (!ValidationUtil.isValidName(nameField.getText())) { triggerError(nameField, nameError, "Invalid name."); valid = false; }
            if (!ValidationUtil.isValidEmail(emailField.getText())) { triggerError(emailField, emailError, "Invalid email."); valid = false; }
            if (!ValidationUtil.isValidPhone(phoneField.getText())) { triggerError(phoneField, phoneError, "Invalid phone."); valid = false; }
            if (!ValidationUtil.isValidAddress(addrField.getText())) { triggerError(addrField, addrError, "Address too short."); valid = false; }
            if (!ValidationUtil.isValidDateOfBirth(dobField.getText())) { triggerError(dobField, dobError, "Invalid DOB."); valid = false; }
            if (!ValidationUtil.isValidGovtId(govField.getText())) { triggerError(govField, govError, "Invalid Govt ID."); valid = false; }
            if (!ValidationUtil.isValidPassword(pwdField.getText())) { triggerError(pwdField, pwdError, "Weak password."); valid = false; }
            if (!pwdField.getText().equals(cpwdField.getText())) { triggerError(cpwdField, cpwdError, "Passwords don't match."); valid = false; }
            if (!ValidationUtil.isNotEmpty(sqField.getText())) { triggerError(sqField, sqError, "Required."); valid = false; }
            if (!ValidationUtil.isNotEmpty(saField.getText())) { triggerError(saField, saError, "Required."); valid = false; }

            if (!valid) return;

            try {
                Customer customer = screenManager.getBank().getAuthService().registerCustomer(
                        nameField.getText(), emailField.getText(), phoneField.getText(),
                        addrField.getText(), dobField.getText(), govField.getText(),
                        pwdField.getText(), sqField.getText(), saField.getText());
                statusLabel.setText("✓ Registration successful! Your ID: " + customer.getUserId() + ". Redirecting to login...");
                statusLabel.getStyleClass().setAll("alert-success");
                statusLabel.setVisible(true);
                // Redirect to login after a short delay
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> screenManager.showLoginScreen());
                pause.play();
            } catch (InvalidInputException ex) {
                statusLabel.setText("⚠ " + ex.getMessage());
                statusLabel.getStyleClass().setAll("alert-error");
                statusLabel.setVisible(true);
            }
        });

        // Back to login link
        HBox loginBox = new HBox(4);
        loginBox.setAlignment(Pos.CENTER);
        Label hasAccount = new Label("Already have an account?");
        hasAccount.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");
        Hyperlink loginLink = new Hyperlink("Sign in");
        loginLink.getStyleClass().add("auth-link");
        loginLink.setOnAction(e -> screenManager.showLoginScreen());
        loginBox.getChildren().addAll(hasAccount, loginLink);

        card.getChildren().addAll(
                title, subtitle, new Separator(),
                nameLabel, nameField, nameError,
                emailLabel, emailField, emailError,
                phoneLabel, phoneField, phoneError,
                addrLabel, addrField, addrError,
                dobLabel, dobField, dobError,
                govLabel, govField, govError,
                pwdLabel, pwdField, pwdHint, pwdError,
                cpwdLabel, cpwdField, cpwdError,
                sqLabel, sqField, sqError,
                saLabel, saField, saError,
                statusLabel,
                registerBtn,
                new Separator(),
                loginBox
        );

        container.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        return scrollPane;
    }

    private Label createErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setVisible(false);
        label.setWrapText(true);
        return label;
    }

    /** Real-time validation: validates when the field loses focus */
    private void addValidation(TextInputControl field, Label errorLabel, String errorMsg,
                                java.util.function.Predicate<String> validator) {
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !field.getText().isEmpty()) {
                if (!validator.test(field.getText())) {
                    errorLabel.setText("⚠ " + errorMsg);
                    errorLabel.setVisible(true);
                    field.setStyle("-fx-border-color: #E74C3C;");
                } else {
                    errorLabel.setVisible(false);
                    field.setStyle("-fx-border-color: #27AE60;");
                }
            }
        });
    }

    private void triggerError(TextInputControl field, Label errorLabel, String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        field.setStyle("-fx-border-color: #E74C3C;");
    }

    public ScrollPane getRoot() { return root; }
}

