package com.rajarata.bank.ui.fx;

import com.rajarata.bank.Bank;
import com.rajarata.bank.models.user.*;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.utils.ValidationUtil;
import com.rajarata.bank.utils.EncryptionUtil;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;

/**
 * Administrator dashboard — user management, system settings, reports, exchange rates.
 */
public class AdminDashboard {

    private final ScreenManager screenManager;
    private final Administrator admin;
    private final Bank bank;
    private final BorderPane root;
    private final StackPane contentArea;
    private VBox sidebarBtnContainer;

    public AdminDashboard(ScreenManager screenManager, Administrator admin) {
        this.screenManager = screenManager;
        this.admin = admin;
        this.bank = screenManager.getBank();
        this.contentArea = new StackPane();
        this.root = buildUI();
        showOverview();
    }

    private BorderPane buildUI() {
        BorderPane layout = new BorderPane();

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        VBox header = new VBox(4);
        header.getStyleClass().add("sidebar-header");
        Label bankName = new Label("🏦 Rajarata Bank");
        bankName.getStyleClass().add("sidebar-bank-name");
        Label sub = new Label("Admin Portal");
        sub.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(bankName, sub);

        VBox userInfo = new VBox(2);
        userInfo.getStyleClass().add("sidebar-user-info");
        Label userName = new Label(admin.getFullName());
        userName.getStyleClass().add("sidebar-username");
        Label userRole = new Label("Admin • Level " + admin.getAccessLevel());
        userRole.getStyleClass().add("sidebar-role");
        userInfo.getChildren().addAll(userName, userRole);

        sidebarBtnContainer = new VBox();
        String[][] menuItems = {
            {"📊", "Overview"},
            {"👥", "All Users"},
            {"➕", "Create Staff"},
            {"🔒", "Lock / Unlock User"},
            {"🔑", "Reset Password"},
            {"❄", "Freeze / Unfreeze Account"},
            {"💱", "Exchange Rates"},
            {"⚠", "Fraud Cases"},
            {"📈", "Reports"},
            {"🔔", "Notifications"},
            {"👤", "Profile"},
        };

        for (int i = 0; i < menuItems.length; i++) {
            Button btn = new Button(menuItems[i][0] + "  " + menuItems[i][1]);
            btn.getStyleClass().add("sidebar-btn");
            final int idx = i;
            btn.setOnAction(e -> handleMenuClick(idx));
            sidebarBtnContainer.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button logoutBtn = new Button("🚪  Logout");
        logoutBtn.getStyleClass().add("sidebar-logout-btn");
        logoutBtn.setOnAction(e -> screenManager.logout());

        sidebar.getChildren().addAll(header, userInfo, sidebarBtnContainer, spacer, logoutBtn);

        ScrollPane scrollable = new ScrollPane(contentArea);
        scrollable.setFitToWidth(true);
        scrollable.setFitToHeight(true);
        scrollable.setStyle("-fx-background-color: transparent; -fx-background: #F5F7FA;");
        contentArea.setPadding(new Insets(30));
        contentArea.setAlignment(Pos.TOP_LEFT);

        layout.setLeft(sidebar);
        layout.setCenter(scrollable);
        return layout;
    }

    private void handleMenuClick(int index) {
        for (int i = 0; i < sidebarBtnContainer.getChildren().size(); i++) {
            Button btn = (Button) sidebarBtnContainer.getChildren().get(i);
            btn.getStyleClass().removeAll("sidebar-btn-active");
            if (i == index) btn.getStyleClass().add("sidebar-btn-active");
        }
        switch (index) {
            case 0: showOverview(); break;
            case 1: showAllUsers(); break;
            case 2: showCreateStaff(); break;
            case 3: showLockUnlock(); break;
            case 4: showResetPassword(); break;
            case 5: showFreezeUnfreeze(); break;
            case 6: showExchangeRates(); break;
            case 7: showFraudCases(); break;
            case 8: showReports(); break;
            case 9: showNotifications(); break;
            case 10: showProfile(); break;
        }
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    // ======================== OVERVIEW ========================
    private void showOverview() {
        VBox page = new VBox(24);
        Label title = new Label("Admin Dashboard");
        title.getStyleClass().add("page-title");

        Map<String, User> users = bank.getAuthService().getAllUsersById();
        int customers = 0, staffCount = 0, admins = 0;
        for (User u : users.values()) {
            switch (u.getRole()) {
                case "Customer": customers++; break;
                case "Staff": staffCount++; break;
                case "Administrator": admins++; break;
            }
        }

        HBox stats = new HBox(20);
        stats.getChildren().addAll(
            createStatCard("Total Users", String.valueOf(users.size()), true),
            createStatCard("Customers", String.valueOf(customers), false),
            createStatCard("Staff", String.valueOf(staffCount), false),
            createStatCard("Admins", String.valueOf(admins), false)
        );

        page.getChildren().addAll(title, stats);
        setContent(page);
    }

    // ======================== ALL USERS ========================
    private void showAllUsers() {
        VBox page = new VBox(20);
        Label title = new Label("All Users");
        title.getStyleClass().add("page-title");

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getUserId()));
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getFullName()));
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getEmail()));
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRole()));
        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().isAccountLocked() ? "LOCKED" : "Active"));

        table.getColumns().addAll(idCol, nameCol, emailCol, roleCol, statusCol);
        table.setItems(FXCollections.observableArrayList(
            bank.getAuthService().getAllUsersById().values()
        ));

        page.getChildren().addAll(title, table);
        setContent(page);
    }

    // ======================== CREATE STAFF ========================
    private void showCreateStaff() {
        VBox page = new VBox(20);
        Label title = new Label("Create Staff Account");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(14);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        TextField nameField = createField("Full Name");
        TextField emailField = createField("Email");
        TextField phoneField = createField("Phone");
        TextField addressField = createField("Address");
        TextField dobField = createField("Date of Birth (yyyy-MM-dd)");
        TextField govIdField = createField("Government ID");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Password");
        pwdField.setPrefHeight(38);
        TextField deptField = createField("Department");
        TextField posField = createField("Position");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button createBtn = new Button("Create Staff Account");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setPrefHeight(40);
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            try {
                Staff newStaff = bank.getAuthService().createStaffAccount(
                        nameField.getText(), emailField.getText(), phoneField.getText(),
                        addressField.getText(), dobField.getText(), govIdField.getText(),
                        pwdField.getText(), deptField.getText(), posField.getText());
                resultLabel.setText("✓ Staff account created! ID: " + newStaff.getUserId());
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(
            new Label("Full Name"), nameField,
            new Label("Email"), emailField,
            new Label("Phone"), phoneField,
            new Label("Address"), addressField,
            new Label("DOB"), dobField,
            new Label("Govt ID"), govIdField,
            new Label("Password"), pwdField,
            new Label("Department"), deptField,
            new Label("Position"), posField,
            resultLabel, createBtn
        );

        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ======================== LOCK / UNLOCK ========================
    private void showLockUnlock() {
        VBox page = new VBox(20);
        Label title = new Label("Lock / Unlock User Account");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(14);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        TextField userIdField = createField("User ID");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        HBox buttons = new HBox(10);
        Button lockBtn = new Button("🔒 Lock Account");
        lockBtn.getStyleClass().add("btn-danger");
        lockBtn.setOnAction(e -> {
            try {
                boolean ok = bank.getAuthService().lockUser(userIdField.getText());
                resultLabel.setText(ok ? "✓ Account locked." : "⚠ User not found.");
                resultLabel.getStyleClass().setAll(ok ? "alert-success" : "alert-error");
                resultLabel.setVisible(true);
            } catch (UnauthorizedAccessException ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        Button unlockBtn = new Button("🔓 Unlock Account");
        unlockBtn.getStyleClass().add("btn-success");
        unlockBtn.setOnAction(e -> {
            try {
                boolean ok = bank.getAuthService().unlockUser(userIdField.getText());
                resultLabel.setText(ok ? "✓ Account unlocked." : "⚠ User not found.");
                resultLabel.getStyleClass().setAll(ok ? "alert-success" : "alert-error");
                resultLabel.setVisible(true);
            } catch (UnauthorizedAccessException ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        buttons.getChildren().addAll(lockBtn, unlockBtn);
        form.getChildren().addAll(new Label("User ID"), userIdField, buttons, resultLabel);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ======================== RESET PASSWORD ========================
    private void showResetPassword() {
        VBox page = new VBox(20);
        Label title = new Label("Reset User Password");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(14);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        TextField userIdField = createField("User ID");
        PasswordField newPwdField = new PasswordField();
        newPwdField.setPromptText("New Password");
        newPwdField.setPrefHeight(38);

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button resetBtn = new Button("🔑 Reset Password");
        resetBtn.getStyleClass().add("btn-primary");
        resetBtn.setPrefHeight(40);
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            try {
                boolean ok = bank.getAuthService().resetUserPassword(userIdField.getText(), newPwdField.getText());
                resultLabel.setText(ok ? "✓ Password reset and account unlocked." : "⚠ User not found.");
                resultLabel.getStyleClass().setAll(ok ? "alert-success" : "alert-error");
                resultLabel.setVisible(true);
            } catch (UnauthorizedAccessException ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(new Label("User ID"), userIdField, new Label("New Password"), newPwdField, resultLabel, resetBtn);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ======================== FREEZE / UNFREEZE ACCOUNT ========================
    private void showFreezeUnfreeze() {
        VBox page = new VBox(20);
        Label title = new Label("Freeze / Unfreeze Bank Account");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(14);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        TextField accNumField = createField("Bank Account Number");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        HBox buttons = new HBox(10);
        Button freezeBtn = new Button("❄ Suspend Account");
        freezeBtn.getStyleClass().add("btn-danger");
        freezeBtn.setOnAction(e -> {
            try {
                bank.getAccountService().suspendAccount(accNumField.getText());
                resultLabel.setText("✓ Account suspended/frozen successfully.");
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        Button unfreezeBtn = new Button("🔥 Reactivate Account");
        unfreezeBtn.getStyleClass().add("btn-success");
        unfreezeBtn.setOnAction(e -> {
            try {
                bank.getAccountService().reactivateAccount(accNumField.getText());
                resultLabel.setText("✓ Account reactivated successfully.");
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        buttons.getChildren().addAll(freezeBtn, unfreezeBtn);
        form.getChildren().addAll(new Label("Account Number"), accNumField, buttons, resultLabel);
        
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ======================== EXCHANGE RATES ========================
    private void showExchangeRates() {
        VBox page = new VBox(20);
        Label title = new Label("Manage Exchange Rates");
        title.getStyleClass().add("page-title");

        // Display current rates
        VBox ratesCard = new VBox(10);
        ratesCard.getStyleClass().add("card");
        Label ratesTitle = new Label("Current Rates (vs USD)");
        ratesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        ratesCard.getChildren().add(ratesTitle);

        String[] currencies = {"USD", "EUR", "GBP", "LKR"};
        for (String curr : currencies) {
            double rate = bank.getCurrencyService().getExchangeRate("USD", curr);
            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            Label currLabel = new Label(curr);
            currLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 50;");
            Label rateVal = new Label(String.format("%.4f", rate));
            rateVal.setStyle("-fx-text-fill: #4A90D9;");
            row.getChildren().addAll(currLabel, rateVal);
            ratesCard.getChildren().add(row);
        }

        // Update rate form
        VBox updateForm = new VBox(14);
        updateForm.getStyleClass().add("card");
        updateForm.setMaxWidth(500);
        Label updateTitle = new Label("Update Exchange Rate");
        updateTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<String> currBox = new ComboBox<>();
        currBox.getItems().addAll("EUR", "GBP", "LKR");
        currBox.setValue("EUR");
        currBox.setMaxWidth(Double.MAX_VALUE);

        TextField rateField = createField("New Rate (vs USD)");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button updateBtn = new Button("Update Rate");
        updateBtn.getStyleClass().add("btn-primary");
        updateBtn.setOnAction(e -> {
            try {
                double newRate = Double.parseDouble(rateField.getText());
                bank.getCurrencyService().updateExchangeRate(currBox.getValue(), newRate);
                resultLabel.setText("✓ " + currBox.getValue() + " rate updated to " + newRate);
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
                showExchangeRates(); // Refresh
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        updateForm.getChildren().addAll(updateTitle, new Label("Currency"), currBox, rateField, resultLabel, updateBtn);
        page.getChildren().addAll(title, ratesCard, updateForm);
        setContent(page);
    }

    // ======================== FRAUD CASES ========================
    private void showFraudCases() {
        VBox page = new VBox(20);
        Label title = new Label("Fraud Cases");
        title.getStyleClass().add("page-title");
        page.getChildren().add(title);

        try {
            var cases = bank.getFraudDetectionService().getAllCases();
            if (cases == null || cases.isEmpty()) {
                page.getChildren().add(new Label("No fraud cases."));
            } else {
                for (String[] c : cases.values()) {
                    if (c == null || c.length < 6) continue;
                    VBox card = new VBox(8);
                    card.getStyleClass().add("card");
                    Label caseId = new Label("Case: " + c[0]);
                    caseId.setStyle("-fx-font-weight: bold;");
                    Label info = new Label("Customer: " + c[1] + " | Account: " + c[2]);
                    Label desc = new Label(c.length > 3 ? c[3] : "");
                    desc.setWrapText(true);
                    Label status = new Label("Status: " + c[5]);
                    status.setStyle("-fx-font-weight: bold;");

                    HBox actionRow = new HBox(10);
                    ComboBox<String> statusBox = new ComboBox<>();
                    statusBox.getItems().addAll("Under Investigation", "Resolved", "Dismissed");
                    statusBox.setValue(c[5]);
                    TextField notesField = new TextField();
                    notesField.setPromptText("Notes");
                    HBox.setHgrow(notesField, Priority.ALWAYS);

                    Label updateResult = new Label();
                    updateResult.setVisible(false);

                    // Capture c[0] for use in lambda
                    final String caseIdStr = c[0];
                    Button updateBtn = new Button("Update");
                    updateBtn.getStyleClass().add("btn-secondary");
                    updateBtn.setOnAction(e -> {
                        boolean ok = bank.getFraudDetectionService().updateCase(caseIdStr, statusBox.getValue(), notesField.getText());
                        updateResult.setText(ok ? "✓ Updated" : "⚠ Failed");
                        updateResult.setStyle(ok ? "-fx-text-fill: #27AE60;" : "-fx-text-fill: #E74C3C;");
                        updateResult.setVisible(true);
                    });

                    actionRow.getChildren().addAll(statusBox, notesField, updateBtn);
                    card.getChildren().addAll(caseId, info, desc, status, actionRow, updateResult);
                    page.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("⚠ Error loading fraud cases: " + e.getMessage());
            errorLabel.getStyleClass().add("alert-error");
            errorLabel.setWrapText(true);
            page.getChildren().add(errorLabel);
        }
        setContent(page);
    }

    // ======================== REPORTS ========================
    private void showReports() {
        VBox page = new VBox(20);
        Label title = new Label("Reports");
        title.getStyleClass().add("page-title");

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefRowCount(20);
        reportArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        HBox buttons = new HBox(10);
        Button statsBtn = new Button("📊 System Statistics");
        statsBtn.getStyleClass().add("btn-secondary");
        statsBtn.setOnAction(e -> reportArea.setText(bank.getReportingService().generateSystemStatistics()));

        Button loanBtn = new Button("🏠 Loan Performance");
        loanBtn.getStyleClass().add("btn-secondary");
        loanBtn.setOnAction(e -> reportArea.setText(bank.getReportingService().generateLoanPerformanceReport()));

        Button auditBtn = new Button("📋 Audit Report");
        auditBtn.getStyleClass().add("btn-secondary");
        auditBtn.setOnAction(e -> reportArea.setText(bank.getReportingService().generateAuditReport()));

        buttons.getChildren().addAll(statsBtn, loanBtn, auditBtn);
        page.getChildren().addAll(title, buttons, reportArea);
        setContent(page);
    }

    // ======================== NOTIFICATIONS ========================
    private void showNotifications() {
        VBox page = new VBox(20);
        Label title = new Label("Notifications");
        title.getStyleClass().add("page-title");
        page.getChildren().add(title);

        try {
            var notifications = admin.getAllNotifications();
            if (notifications == null || notifications.isEmpty()) {
                page.getChildren().add(new Label("No notifications."));
            } else {
                for (var n : notifications) {
                    HBox item = new HBox(12);
                    item.getStyleClass().add("card");
                    Label icon = new Label(n.isRead() ? "📧" : "🔔");
                    VBox info = new VBox(2);
                    Label msg = new Label(n.getMessage());
                    msg.setWrapText(true);
                    Label time = new Label(n.getTimestamp());
                    time.setStyle("-fx-text-fill: #B0B8C1; -fx-font-size: 11px;");
                    info.getChildren().addAll(msg, time);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    item.getChildren().addAll(icon, info);
                    page.getChildren().add(item);
                }
            }
        } catch (Exception e) {
            page.getChildren().add(new Label("Error loading notifications."));
        }
        setContent(page);
    }

    // ======================== PROFILE ========================
    private void showProfile() {
        VBox page = new VBox(20);
        Label title = new Label("Admin Profile");
        title.getStyleClass().add("page-title");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);
        String[][] fields = {
            {"Admin ID", admin.getUserId()}, {"Name", admin.getFullName()},
            {"Email", admin.getEmail()}, {"Access Level", String.valueOf(admin.getAccessLevel())},
            {"Can Manage Admins", String.valueOf(admin.isCanManageAdmins())},
        };
        for (String[] f : fields) {
            HBox row = new HBox(12);
            Label lbl = new Label(f[0]);
            lbl.setStyle("-fx-font-weight: bold; -fx-min-width: 140; -fx-text-fill: #5A6A7A;");
            row.getChildren().addAll(lbl, new Label(f[1]));
            card.getChildren().add(row);
        }

        page.getChildren().addAll(title, card);
        setContent(page);
    }

    // ======================== HELPERS ========================
    private TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        return field;
    }

    private VBox createStatCard(String label, String value, boolean isBlue) {
        VBox card = new VBox(6);
        card.getStyleClass().add(isBlue ? "stat-card-blue" : "stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        Label val = new Label(value);
        val.getStyleClass().add(isBlue ? "stat-value-white" : "stat-value");
        Label lbl = new Label(label);
        lbl.getStyleClass().add(isBlue ? "stat-label-white" : "stat-label");
        card.getChildren().addAll(val, lbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    public BorderPane getRoot() { return root; }
}
