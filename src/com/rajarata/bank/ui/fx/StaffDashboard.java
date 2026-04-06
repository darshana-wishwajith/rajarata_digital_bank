package com.rajarata.bank.ui.fx;

import com.rajarata.bank.Bank;
import com.rajarata.bank.models.user.User;
import com.rajarata.bank.models.user.Staff;
import com.rajarata.bank.models.loan.*;
import com.rajarata.bank.utils.*;
import javafx.collections.FXCollections;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Staff dashboard — loan review, customer management, fraud monitoring.
 */
public class StaffDashboard {

    private final ScreenManager screenManager;
    private Staff staff;
    private final Bank bank;
    private final BorderPane root;
    private final StackPane contentArea;
    private VBox sidebarBtnContainer;
    private int currentMenuIndex = 0;
    private Timeline refreshTimer;

    public StaffDashboard(ScreenManager screenManager, Staff staff) {
        this.screenManager = screenManager;
        this.staff = staff;
        this.bank = screenManager.getBank();
        this.contentArea = new StackPane();
        this.root = buildUI();
        showOverview();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timeline(new KeyFrame(Duration.minutes(5), e -> {
            refreshNow();
        }));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void refreshNow() {
        if (root.getScene() != null && root.getScene().getWindow() != null && root.getScene().getWindow().isShowing()) {
            bank.refreshAllData();
            // Refresh staff reference
            User updated = bank.getAuthService().getUserById(this.staff.getUserId());
            if (updated instanceof Staff) {
                this.staff = (Staff) updated;
                handleMenuClick(currentMenuIndex);
            }
        }
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

    private BorderPane buildUI() {
        BorderPane layout = new BorderPane();

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        VBox header = new VBox(4);
        header.getStyleClass().add("sidebar-header");
        Label bankName = new Label("🏦 Rajarata Bank");
        bankName.getStyleClass().add("sidebar-bank-name");
        Label sub = new Label("Staff Portal");
        sub.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(bankName, sub);

        // User info & Refresh
        VBox userInfoContainer = new VBox(10);
        userInfoContainer.getStyleClass().add("sidebar-user-info");
        
        VBox userInfo = new VBox(2);
        Label userName = new Label(staff.getFullName());
        userName.getStyleClass().add("sidebar-username");
        Label userRole = new Label("Staff • " + staff.getUserId());
        userRole.getStyleClass().add("sidebar-role");
        userInfo.getChildren().addAll(userName, userRole);
        
        Button manualRefreshBtn = new Button("🔄 Sync Now");
        manualRefreshBtn.getStyleClass().add("btn-sidebar-refresh");
        manualRefreshBtn.setMaxWidth(Double.MAX_VALUE);
        manualRefreshBtn.setOnAction(e -> refreshNow());
        
        userInfoContainer.getChildren().addAll(userInfo, manualRefreshBtn);
        userInfoContainer.setPadding(new Insets(10, 15, 10, 15));

        sidebarBtnContainer = new VBox();
        String[][] menuItems = {
            {"📊", "Overview"},
            {"📋", "Loan Applications"},
            {"👥", "All Customers"},
            {"🔍", "Search Customer"},
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

        VBox scrollContent = new VBox();
        scrollContent.getChildren().addAll(userInfoContainer, sidebarBtnContainer);
        
        ScrollPane sidebarScroll = new ScrollPane(scrollContent);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(sidebarScroll, Priority.ALWAYS);

        Button logoutBtn = new Button("🚪  Logout");
        logoutBtn.getStyleClass().add("sidebar-logout-btn");
        logoutBtn.setOnAction(e -> screenManager.logout());

        sidebar.getChildren().addAll(header, sidebarScroll, logoutBtn);

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
        currentMenuIndex = index;
        switch (index) {
            case 0: showOverview(); break;
            case 1: showLoanApplications(); break;
            case 2: showAllCustomers(); break;
            case 3: showSearchCustomer(); break;
            case 4: showFraudCases(); break;
            case 5: showReports(); break;
            case 6: showNotifications(); break;
            case 7: showProfile(); break;
        }
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    // ======================== OVERVIEW ========================
    private void showOverview() {
        VBox page = new VBox(24);
        Label title = new Label("Staff Dashboard");
        title.getStyleClass().add("page-title");

        HBox stats = new HBox(20);
        int totalCustomers = 0;
        for (User u : bank.getAuthService().getAllUsersById().values()) {
            if ("Customer".equals(u.getRole())) totalCustomers++;
        }
        List<Loan> pendingLoans = bank.getLoanService().getPendingLoans();
        int fraudCount = bank.getFraudDetectionService().getAllCases().size();

        stats.getChildren().addAll(
            createStatCard("Total Customers", String.valueOf(totalCustomers), false),
            createStatCard("Pending Loans", String.valueOf(pendingLoans.size()), true),
            createStatCard("Fraud Cases", String.valueOf(fraudCount), false)
        );

        page.getChildren().addAll(title, stats);
        setContent(page);
    }

    // ======================== LOAN APPLICATIONS ========================
    private void showLoanApplications() {
        VBox page = new VBox(20);
        Label title = new Label("Pending Loan Applications");
        title.getStyleClass().add("page-title");

        List<Loan> pending = bank.getLoanService().getPendingLoans();
        if (pending.isEmpty()) {
            page.getChildren().addAll(title, new Label("No pending applications."));
            setContent(page);
            return;
        }

        for (Loan loan : pending) {
            VBox card = new VBox(10);
            card.getStyleClass().add("card");

            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            Label id = new Label(loan.getLoanId());
            id.setStyle("-fx-font-weight: bold;");
            Label cust = new Label("Customer: " + loan.getCustomerId());
            Label type = new Label(loan.getLoanType().getDisplayName());
            String loanSym = CurrencyUtil.getCurrencySymbol(null);
            Label amt = new Label(loanSym + " " + ValidationUtil.formatAmount(loan.getLoanAmount()));
            amt.setStyle("-fx-font-weight: bold; -fx-text-fill: #4A90D9;");
            row.getChildren().addAll(id, cust, type, amt);

            Label details = new Label("Term: " + loan.getTermMonths() + " mo | Credit Score: " +
                    loan.getCreditScoreAtApplication() + " | Purpose: " + loan.getPurpose());
            details.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

            HBox actions = new HBox(10);
            TextField commentsField = new TextField();
            commentsField.setPromptText("Comments");
            HBox.setHgrow(commentsField, Priority.ALWAYS);

            Label resultLabel = new Label();
            resultLabel.setVisible(false);

            Button approveBtn = new Button("✓ Approve");
            approveBtn.getStyleClass().add("btn-success");
            approveBtn.setOnAction(e -> {
                try {
                    bank.getLoanService().approveLoan(loan.getLoanId(), staff.getUserId(), commentsField.getText());
                    resultLabel.setText("✓ Approved & disbursed");
                    resultLabel.getStyleClass().setAll("alert-success");
                    resultLabel.setVisible(true);
                    approveBtn.setDisable(true);
                } catch (Exception ex) {
                    resultLabel.setText("⚠ " + ex.getMessage());
                    resultLabel.getStyleClass().setAll("alert-error");
                    resultLabel.setVisible(true);
                }
            });

            Button rejectBtn = new Button("✗ Reject");
            rejectBtn.getStyleClass().add("btn-danger");
            rejectBtn.setOnAction(e -> {
                try {
                    bank.getLoanService().rejectLoan(loan.getLoanId(), staff.getUserId(), commentsField.getText());
                    resultLabel.setText("Loan rejected.");
                    resultLabel.getStyleClass().setAll("alert-error");
                    resultLabel.setVisible(true);
                    rejectBtn.setDisable(true);
                } catch (Exception ex) {
                    resultLabel.setText("⚠ " + ex.getMessage());
                    resultLabel.getStyleClass().setAll("alert-error");
                    resultLabel.setVisible(true);
                }
            });

            actions.getChildren().addAll(commentsField, approveBtn, rejectBtn);
            card.getChildren().addAll(row, details, actions, resultLabel);
            page.getChildren().add(card);
        }

        page.getChildren().add(0, title);
        setContent(page);
    }

    // ======================== ALL CUSTOMERS ========================
    private void showAllCustomers() {
        VBox page = new VBox(20);
        Label title = new Label("All Customers");
        title.getStyleClass().add("page-title");

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getUserId()));
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getFullName()));
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getEmail()));
        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().isAccountLocked() ? "LOCKED" : "Active"));

        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(emailCol);
        table.getColumns().add(statusCol);

        Map<String, User> users = bank.getAuthService().getAllUsersById();
        var customers = FXCollections.observableArrayList(
            users.values().stream().filter(u -> "Customer".equals(u.getRole())).toList()
        );
        table.setItems(customers);

        page.getChildren().addAll(title, table);
        setContent(page);
    }

    // ======================== SEARCH CUSTOMER ========================
    private void showSearchCustomer() {
        VBox page = new VBox(20);
        Label title = new Label("Search Customer");
        title.getStyleClass().add("page-title");

        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Customer ID or email");
        searchField.setPrefHeight(38);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = new Button("🔍 Search");
        searchBtn.getStyleClass().add("btn-primary");

        VBox resultBox = new VBox(10);

        searchBtn.setOnAction(e -> {
            resultBox.getChildren().clear();
            String query = searchField.getText();
            User user = bank.getAuthService().getUserById(query);
            if (user == null) user = bank.getAuthService().getUserByEmail(query);
            if (user != null) {
                VBox card = new VBox(8);
                card.getStyleClass().add("card");
                String[][] fields = {
                    {"ID", user.getUserId()}, {"Name", user.getFullName()},
                    {"Email", user.getEmail()}, {"Phone", user.getPhone()},
                    {"Role", user.getRole()}, {"Status", user.isAccountLocked() ? "LOCKED" : "Active"}
                };
                for (String[] f : fields) {
                    HBox row = new HBox(10);
                    Label lbl = new Label(f[0] + ":");
                    lbl.setStyle("-fx-font-weight: bold; -fx-min-width: 80;");
                    row.getChildren().addAll(lbl, new Label(f[1]));
                    card.getChildren().add(row);
                }
                resultBox.getChildren().add(card);
            } else {
                Label notFound = new Label("⚠ Customer not found.");
                notFound.getStyleClass().add("alert-error");
                resultBox.getChildren().add(notFound);
            }
        });

        searchBox.getChildren().addAll(searchField, searchBtn);
        page.getChildren().addAll(title, searchBox, resultBox);
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
                page.getChildren().add(new Label("No fraud cases recorded."));
            } else {
                for (String[] c : cases.values()) {
                    if (c == null || c.length < 6) continue;
                    VBox card = new VBox(6);
                    card.getStyleClass().add("card");
                    Label caseId = new Label("Case: " + c[0]);
                    caseId.setStyle("-fx-font-weight: bold;");
                    Label info = new Label("Customer: " + c[1] + " | Account: " + c[2] +
                            (c.length > 4 ? " | Pattern: " + c[4] : ""));
                    info.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");
                    Label desc = new Label(c.length > 3 ? c[3] : "");
                    Label status = new Label("Status: " + c[5]);
                    status.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                        ("Resolved".equalsIgnoreCase(c[5]) ? "#27AE60" : "#F39C12") + ";");
                    card.getChildren().addAll(caseId, info, desc, status);
                    page.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("⚠ Error loading fraud cases: " + e.getMessage());
            errorLabel.getStyleClass().add("alert-error");
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
        Button statsBtn = new Button("System Statistics");
        statsBtn.getStyleClass().add("btn-secondary");
        statsBtn.setOnAction(e -> reportArea.setText(bank.getReportingService().generateSystemStatistics()));

        Button loanBtn = new Button("Loan Performance");
        loanBtn.getStyleClass().add("btn-secondary");
        loanBtn.setOnAction(e -> reportArea.setText(bank.getReportingService().generateLoanPerformanceReport()));

        buttons.getChildren().addAll(statsBtn, loanBtn);
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
            var notifications = staff.getAllNotifications();
            if (notifications == null || notifications.isEmpty()) {
                page.getChildren().add(new Label("No notifications."));
            } else {
                Button markAllBtn = new Button("Mark All as Read");
                markAllBtn.getStyleClass().add("btn-secondary");
                markAllBtn.setOnAction(e -> {
                    bank.getNotificationService().markAllAsRead(staff.getUserId());
                    showNotifications();
                });
                page.getChildren().add(markAllBtn);

                for (int i = notifications.size() - 1; i >= 0; i--) {
                    var n = notifications.get(i);
                    HBox item = new HBox(12);
                    item.getStyleClass().add("card");
                    item.setAlignment(Pos.CENTER_LEFT);
                    Label icon = new Label(n.isRead() ? "📧" : "🔔");
                    VBox info = new VBox(2);
                    Label msg = new Label(n.getMessage());
                    msg.setWrapText(true);
                    msg.setStyle(n.isRead() ? "-fx-text-fill: #7F8C8D;" : "-fx-font-weight: bold;");
                    Label time = new Label(n.getTimestamp());
                    time.setStyle("-fx-text-fill: #B0B8C1; -fx-font-size: 11px;");
                    info.getChildren().addAll(msg, time);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    
                    Button readBtn = new Button("Mark as Read");
                    readBtn.getStyleClass().add("btn-outline-sm");
                    final String nid = n.getNotificationId();
                    readBtn.setOnAction(e -> {
                        bank.getNotificationService().markAsReadById(staff.getUserId(), nid);
                        showNotifications();
                    });

                    item.getChildren().addAll(icon, info);
                    if (!n.isRead()) item.getChildren().add(readBtn);
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
        Label title = new Label("My Profile");
        title.getStyleClass().add("page-title");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);
        String[][] fields = {
            {"Staff ID", staff.getUserId()}, {"Name", staff.getFullName()},
            {"Email", staff.getEmail()}, {"Phone", staff.getPhone()},
            {"Department", staff.getDepartment()}, {"Position", staff.getPosition()},
        };
        for (String[] f : fields) {
            HBox row = new HBox(12);
            Label lbl = new Label(f[0]);
            lbl.setStyle("-fx-font-weight: bold; -fx-min-width: 120; -fx-text-fill: #5A6A7A;");
            row.getChildren().addAll(lbl, new Label(f[1] != null ? f[1] : "N/A"));
            card.getChildren().add(row);
        }

        page.getChildren().addAll(title, card);
        setContent(page);
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

