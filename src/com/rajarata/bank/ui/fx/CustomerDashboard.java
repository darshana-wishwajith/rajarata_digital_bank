package com.rajarata.bank.ui.fx;

import com.rajarata.bank.Bank;
import com.rajarata.bank.models.user.Customer;
import com.rajarata.bank.models.account.Account;
import com.rajarata.bank.models.transaction.*;
import com.rajarata.bank.models.loan.*;
import com.rajarata.bank.exceptions.*;
import com.rajarata.bank.factory.AccountFactory;
import com.rajarata.bank.utils.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.util.List;

/**
 * Customer dashboard with sidebar navigation and content area.
 * Provides access to all banking operations: accounts, transactions,
 * loans, bills, currency exchange, etc.
 */
public class CustomerDashboard {

    private final ScreenManager screenManager;
    private final Customer customer;
    private final Bank bank;
    private final BorderPane root;
    private final StackPane contentArea;
    private VBox sidebarBtnContainer;

    public CustomerDashboard(ScreenManager screenManager, Customer customer) {
        this.screenManager = screenManager;
        this.customer = customer;
        this.bank = screenManager.getBank();
        this.contentArea = new StackPane();
        this.root = buildUI();
        showOverview(); // Default view
    }

    private BorderPane buildUI() {
        BorderPane layout = new BorderPane();

        // === SIDEBAR ===
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Bank name header
        VBox header = new VBox(4);
        header.getStyleClass().add("sidebar-header");
        Label bankName = new Label("🏦 Rajarata Bank");
        bankName.getStyleClass().add("sidebar-bank-name");
        Label sub = new Label("Digital Banking");
        sub.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(bankName, sub);

        // User info
        VBox userInfo = new VBox(2);
        userInfo.getStyleClass().add("sidebar-user-info");
        Label userName = new Label(customer.getFullName());
        userName.getStyleClass().add("sidebar-username");
        Label userRole = new Label("Customer • " + customer.getUserId());
        userRole.getStyleClass().add("sidebar-role");
        userInfo.getChildren().addAll(userName, userRole);

        // Navigation buttons
        sidebarBtnContainer = new VBox();
        String[][] menuItems = {
            {"📊", "Overview"},
            {"💳", "My Accounts"},
            {"➕", "Open Account"},
            {"💰", "Deposit"},
            {"💸", "Withdraw"},
            {"🔄", "Transfer"},
            {"📜", "Transactions"},
            {"🏠", "Apply Loan"},
            {"📋", "My Loans"},
            {"📱", "Pay Bills"},
            {"💱", "Exchange"},
            {"📄", "Statement"},
            {"🔔", "Notifications"},
            {"👤", "Profile"},
        };

        for (int i = 0; i < menuItems.length; i++) {
            Button btn = new Button(menuItems[i][0] + "  " + menuItems[i][1]);
            btn.getStyleClass().add("sidebar-btn");
            final int index = i;
            btn.setOnAction(e -> handleMenuClick(index));
            sidebarBtnContainer.getChildren().add(btn);
        }

        // Logout button at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("🚪  Logout");
        logoutBtn.getStyleClass().add("sidebar-logout-btn");
        logoutBtn.setOnAction(e -> screenManager.logout());

        sidebar.getChildren().addAll(header, userInfo, sidebarBtnContainer, spacer, logoutBtn);

        // === CONTENT AREA ===
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
        // Highlight active button
        for (int i = 0; i < sidebarBtnContainer.getChildren().size(); i++) {
            Button btn = (Button) sidebarBtnContainer.getChildren().get(i);
            btn.getStyleClass().removeAll("sidebar-btn-active");
            if (i == index) btn.getStyleClass().add("sidebar-btn-active");
        }
        switch (index) {
            case 0: showOverview(); break;
            case 1: showAccounts(); break;
            case 2: showOpenAccount(); break;
            case 3: showDeposit(); break;
            case 4: showWithdraw(); break;
            case 5: showTransfer(); break;
            case 6: showTransactions(); break;
            case 7: showApplyLoan(); break;
            case 8: showMyLoans(); break;
            case 9: showPayBills(); break;
            case 10: showExchange(); break;
            case 11: showStatement(); break;
            case 12: showNotifications(); break;
            case 13: showProfile(); break;
        }
    }

    /** Clears the content area and sets new content */
    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    // ====================================================================
    //  OVERVIEW
    // ====================================================================
    private void showOverview() {
        VBox page = new VBox(24);
        Label title = new Label("Welcome, " + customer.getFullName());
        title.getStyleClass().add("page-title");
        Label sub = new Label("Here's your banking overview");
        sub.getStyleClass().add("page-subtitle");

        // Stat cards row — per-currency balances
        HBox statsRow = new HBox(20);
        java.util.Map<String, Double> balances = customer.getBalanceByCurrency();
        StringBuilder balStr = new StringBuilder();
        for (java.util.Map.Entry<String, Double> entry : balances.entrySet()) {
            if (balStr.length() > 0) balStr.append("\n");
            balStr.append(entry.getKey()).append(" ").append(ValidationUtil.formatAmount(entry.getValue()));
        }
        if (balStr.length() == 0) balStr.append("No accounts");
        statsRow.getChildren().addAll(
            createStatCard("Balances", balStr.toString(), true),
            createStatCard("Accounts", String.valueOf(customer.getAccountCount()), false),
            createStatCard("Credit Score", String.valueOf(customer.getCreditScore()), false),
            createStatCard("Unread Alerts",
                String.valueOf(customer.getUnreadNotificationCount()), false)
        );

        // Quick actions
        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        HBox actions = new HBox(16);
        actions.getChildren().addAll(
            createActionButton("💰 Deposit", () -> { handleMenuClick(3); }),
            createActionButton("💸 Withdraw", () -> { handleMenuClick(4); }),
            createActionButton("🔄 Transfer", () -> { handleMenuClick(5); }),
            createActionButton("📱 Pay Bills", () -> { handleMenuClick(9); })
        );

        // Recent transactions
        Label recentTitle = new Label("Recent Transactions");
        recentTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        VBox recentBox = new VBox(8);
        recentBox.getStyleClass().add("card");
        List<Account> accounts = customer.getAccounts();
        if (accounts != null && !accounts.isEmpty()) {
            try {
                List<Transaction> recent = bank.getTransactionService()
                        .getTransactionHistory(accounts.get(0).getAccountNumber(), 1, 5);
                if (recent.isEmpty()) {
                    recentBox.getChildren().add(new Label("No recent transactions."));
                } else {
                    for (Transaction txn : recent) {
                        recentBox.getChildren().add(createTransactionRow(txn));
                    }
                }
            } catch (InvalidAccountException e) {
                recentBox.getChildren().add(new Label("No transactions available."));
            }
        } else {
            recentBox.getChildren().add(new Label("Open your first account to get started!"));
        }

        page.getChildren().addAll(title, sub, statsRow, actionsTitle, actions, recentTitle, recentBox);
        setContent(page);
    }

    // ====================================================================
    //  ACCOUNTS
    // ====================================================================
    private void showAccounts() {
        VBox page = new VBox(20);
        Label title = new Label("My Accounts");
        title.getStyleClass().add("page-title");

        VBox accountCards = new VBox(16);
        List<Account> accounts = customer.getAccounts();
        if (accounts == null || accounts.isEmpty()) {
            Label empty = new Label("You don't have any accounts yet. Open one to get started!");
            empty.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 14px;");
            accountCards.getChildren().add(empty);
        } else {
            for (Account acc : accounts) {
                accountCards.getChildren().add(createAccountCard(acc));
            }
        }

        page.getChildren().addAll(title, accountCards);
        setContent(page);
    }

    private HBox createAccountCard(Account acc) {
        HBox card = new HBox();
        card.getStyleClass().add("card");
        card.setSpacing(20);
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        Label accNum = new Label(acc.getAccountNumber());
        accNum.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label type = new Label(acc.getAccountType() + " Account (" + acc.getCurrency() + ")");
        type.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
        info.getChildren().addAll(accNum, type);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox balanceBox = new VBox(2);
        balanceBox.setAlignment(Pos.CENTER_RIGHT);
        String currSymbol = getCurrencySymbol(acc.getCurrency());
        Label balance = new Label(currSymbol + ValidationUtil.formatAmount(acc.getBalance()));
        balance.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4A90D9;");
        Label status = new Label(acc.getStatus());
        status.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                ("Active".equals(acc.getStatus()) ? "#27AE60" : "#E74C3C") + ";");
        balanceBox.getChildren().addAll(balance, status);

        card.getChildren().addAll(info, spacer, balanceBox);
        return card;
    }

    /** Returns a currency symbol for display */
    private String getCurrencySymbol(String currency) {
        if (currency == null) return "$";
        switch (currency) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "LKR": return "Rs.";
            default: return currency + " ";
        }
    }

    // ====================================================================
    //  OPEN ACCOUNT
    // ====================================================================
    private void showOpenAccount() {
        VBox page = new VBox(20);
        Label title = new Label("Open New Account");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label typeLabel = new Label("Account Type");
        typeLabel.getStyleClass().add("field-label");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Savings", "Checking", "Student", "Fixed Deposit");
        typeBox.setValue("Savings");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        // Currency selection for multi-currency accounts
        Label currLabel = new Label("Account Currency");
        currLabel.getStyleClass().add("field-label");
        ComboBox<String> currBox = new ComboBox<>();
        currBox.getItems().addAll("LKR - Sri Lankan Rupee", "USD - US Dollar", "EUR - Euro", "GBP - British Pound");
        currBox.setValue("LKR - Sri Lankan Rupee");
        currBox.setMaxWidth(Double.MAX_VALUE);

        Label minLabel = new Label("Minimum deposit: LKR " +
            ValidationUtil.formatAmount(AccountFactory.getMinimumDeposit("Savings")));
        minLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

        Label depLabel = new Label("Initial Deposit");
        depLabel.getStyleClass().add("field-label");
        TextField depositField = new TextField();
        depositField.setPromptText("Enter amount");
        depositField.setPrefHeight(38);

        // Lock-in for FD
        Label lockLabel = new Label("Lock-in Period (months)");
        lockLabel.getStyleClass().add("field-label");
        lockLabel.setVisible(false);
        ComboBox<Integer> lockBox = new ComboBox<>();
        lockBox.getItems().addAll(6, 12, 24, 36);
        lockBox.setValue(12);
        lockBox.setVisible(false);
        lockBox.setMaxWidth(Double.MAX_VALUE);

        typeBox.setOnAction(e -> {
            boolean isFD = "Fixed Deposit".equals(typeBox.getValue());
            lockLabel.setVisible(isFD);
            lockBox.setVisible(isFD);
            String selectedCurr = currBox.getValue().split(" - ")[0];
            minLabel.setText("Minimum deposit: " + selectedCurr + " " +
                ValidationUtil.formatAmount(AccountFactory.getMinimumDeposit(typeBox.getValue())));
        });

        currBox.setOnAction(e -> {
            String selectedCurr = currBox.getValue().split(" - ")[0];
            minLabel.setText("Minimum deposit: " + selectedCurr + " " +
                ValidationUtil.formatAmount(AccountFactory.getMinimumDeposit(typeBox.getValue())));
            depLabel.setText("Initial Deposit (" + selectedCurr + ")");
        });

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button createBtn = new Button("Open Account");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setPrefHeight(40);
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            try {
                double deposit = Double.parseDouble(depositField.getText());
                int lockIn = lockBox.isVisible() ? lockBox.getValue() : 0;
                String currency = currBox.getValue().split(" - ")[0];
                Account account = bank.getAccountService().openAccount(
                    customer.getUserId(), typeBox.getValue(), deposit, currency, lockIn);
                resultLabel.setText("✓ Account created! Number: " + account.getAccountNumber() +
                    " | Currency: " + account.getCurrency());
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
                depositField.clear();
            } catch (NumberFormatException ex) {
                resultLabel.setText("⚠ Please enter a valid number.");
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            } catch (InvalidAccountException | InvalidInputException ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(typeLabel, typeBox, currLabel, currBox, minLabel,
                depLabel, depositField, lockLabel, lockBox,
                resultLabel, createBtn);

        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ====================================================================
    //  DEPOSIT
    // ====================================================================
    private void showDeposit() {
        setContent(createTransactionForm("Deposit", true));
    }

    // ====================================================================
    //  WITHDRAW
    // ====================================================================
    private void showWithdraw() {
        setContent(createTransactionForm("Withdrawal", false));
    }

    private VBox createTransactionForm(String type, boolean isDeposit) {
        VBox page = new VBox(20);
        Label title = new Label(type);
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label accLabel = new Label("Account");
        accLabel.getStyleClass().add("field-label");
        ComboBox<String> accBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) {
            String sym = getCurrencySymbol(acc.getCurrency());
            accBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance()));
        }
        if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));
        accBox.setMaxWidth(Double.MAX_VALUE);

        Label amtLabel = new Label("Amount");
        amtLabel.getStyleClass().add("field-label");
        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.setPrefHeight(38);

        Label descLabel = new Label("Description (optional)");
        descLabel.getStyleClass().add("field-label");
        TextField descField = new TextField();
        descField.setPromptText(isDeposit ? "Cash deposit" : "Cash withdrawal");
        descField.setPrefHeight(38);

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button submitBtn = new Button(isDeposit ? "💰 Make Deposit" : "💸 Make Withdrawal");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setPrefHeight(40);
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> {
            try {
                String accNum = accBox.getValue().split(" \\[")[0];
                double amount = Double.parseDouble(amountField.getText());
                String desc = descField.getText().isEmpty() ?
                        (isDeposit ? "Cash deposit" : "Cash withdrawal") : descField.getText();
                Transaction txn;
                if (isDeposit) {
                    txn = bank.getTransactionService().deposit(accNum, amount, desc);
                } else {
                    txn = bank.getTransactionService().withdraw(accNum, amount, desc);
                }
                Account updatedAcc = bank.getAccountService().getAccount(accNum);
                String curr = updatedAcc.getCurrency();
                resultLabel.setText("✓ " + type + " of " + getCurrencySymbol(curr) + ValidationUtil.formatAmount(amount) + " successful! Txn: " + txn.getTransactionId());
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
                amountField.clear();
                // Refresh account balances in combo
                accBox.getItems().clear();
                for (Account acc : customer.getAccounts()) {
                    String sym = getCurrencySymbol(acc.getCurrency());
                    accBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym +
                            ValidationUtil.formatAmount(acc.getBalance()));
                }
                if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));
            } catch (NumberFormatException ex) {
                resultLabel.setText("⚠ Enter a valid number."); resultLabel.getStyleClass().setAll("alert-error"); resultLabel.setVisible(true);
            } catch (InsufficientFundsException ex) {
                resultLabel.setText("⚠ Insufficient funds.");
                resultLabel.getStyleClass().setAll("alert-error"); resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage()); resultLabel.getStyleClass().setAll("alert-error"); resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(accLabel, accBox, amtLabel, amountField, descLabel, descField, resultLabel, submitBtn);
        page.getChildren().addAll(title, form);
        return page;
    }

    // ====================================================================
    //  TRANSFER
    // ====================================================================
    private void showTransfer() {
        VBox page = new VBox(20);
        Label title = new Label("Transfer Funds");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label srcLabel = new Label("From Account");
        srcLabel.getStyleClass().add("field-label");
        ComboBox<String> srcBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) {
            String sym = getCurrencySymbol(acc.getCurrency());
            srcBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance()));
        }
        if (!srcBox.getItems().isEmpty()) srcBox.setValue(srcBox.getItems().get(0));
        srcBox.setMaxWidth(Double.MAX_VALUE);

        Label destLabel = new Label("To Account Number");
        destLabel.getStyleClass().add("field-label");
        TextField destField = new TextField();
        destField.setPromptText("Enter destination account number");
        destField.setPrefHeight(38);

        Label amtLabel = new Label("Amount");
        amtLabel.getStyleClass().add("field-label");
        TextField amtField = new TextField();
        amtField.setPromptText("Enter transfer amount");
        amtField.setPrefHeight(38);

        // Cross-currency info label
        Label crossCurrInfo = new Label();
        crossCurrInfo.setWrapText(true);
        crossCurrInfo.setStyle("-fx-background-color: #EBF5FB; -fx-padding: 10; -fx-background-radius: 6;");
        crossCurrInfo.setVisible(false);

        // Preview button for cross-currency
        Button previewBtn = new Button("🔍 Preview Conversion");
        previewBtn.getStyleClass().add("btn-secondary");
        previewBtn.setMaxWidth(Double.MAX_VALUE);
        previewBtn.setOnAction(e -> {
            try {
                String srcAcc = srcBox.getValue().split(" \\[")[0];
                String destAcc = destField.getText().trim();
                if (destAcc.isEmpty()) { crossCurrInfo.setText("⚠ Enter destination account first."); crossCurrInfo.setVisible(true); return; }
                Account src = bank.getAccountService().getAccount(srcAcc);
                Account dst = bank.getAccountService().getAccount(destAcc);
                if (!src.getCurrency().equals(dst.getCurrency())) {
                    double amt = Double.parseDouble(amtField.getText());
                    double rate = bank.getCurrencyService().getExchangeRate(src.getCurrency(), dst.getCurrency());
                    double converted = bank.getCurrencyService().convert(amt, src.getCurrency(), dst.getCurrency());
                    double fee = converted * 0.005;
                    double net = converted - fee;
                    crossCurrInfo.setText("💱 Cross-Currency Transfer\n" +
                        src.getCurrency() + " → " + dst.getCurrency() + " | Rate: 1 " + src.getCurrency() + " = " + String.format("%.4f", rate) + " " + dst.getCurrency() + "\n" +
                        "Converted: " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(converted) + "\n" +
                        "Fee (0.5%): " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(fee) + "\n" +
                        "Net amount: " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(net));
                    crossCurrInfo.setVisible(true);
                } else {
                    crossCurrInfo.setText("✓ Same currency (" + src.getCurrency() + ") — no conversion needed.");
                    crossCurrInfo.setVisible(true);
                }
            } catch (NumberFormatException ex) {
                crossCurrInfo.setText("⚠ Enter a valid amount first."); crossCurrInfo.setVisible(true);
            } catch (Exception ex) {
                crossCurrInfo.setText("⚠ " + ex.getMessage()); crossCurrInfo.setVisible(true);
            }
        });

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button transferBtn = new Button("🔄 Transfer Now");
        transferBtn.getStyleClass().add("btn-primary");
        transferBtn.setPrefHeight(40);
        transferBtn.setMaxWidth(Double.MAX_VALUE);
        transferBtn.setOnAction(e -> {
            try {
                String srcAcc = srcBox.getValue().split(" \\[")[0];
                double amount = Double.parseDouble(amtField.getText());
                Transaction txn = bank.getTransactionService().transfer(srcAcc, destField.getText().trim(), amount, null);
                resultLabel.setText("✓ Transfer successful! Txn: " + txn.getTransactionId());
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
                amtField.clear(); destField.clear(); crossCurrInfo.setVisible(false);
                // Refresh source account list
                srcBox.getItems().clear();
                for (Account acc : customer.getAccounts()) {
                    String sym = getCurrencySymbol(acc.getCurrency());
                    srcBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance()));
                }
                if (!srcBox.getItems().isEmpty()) srcBox.setValue(srcBox.getItems().get(0));
            } catch (NumberFormatException ex) {
                resultLabel.setText("⚠ Enter a valid amount."); resultLabel.getStyleClass().setAll("alert-error"); resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage()); resultLabel.getStyleClass().setAll("alert-error"); resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(srcLabel, srcBox, destLabel, destField, amtLabel, amtField, previewBtn, crossCurrInfo, resultLabel, transferBtn);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ====================================================================
    //  TRANSACTIONS
    // ====================================================================
    private void showTransactions() {
        VBox page = new VBox(20);
        Label title = new Label("Transaction History");
        title.getStyleClass().add("page-title");

        Label accLabel = new Label("Select Account");
        accLabel.getStyleClass().add("field-label");
        ComboBox<String> accBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) {
            accBox.getItems().add(acc.getAccountNumber());
        }
        if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));

        VBox tableBox = new VBox();
        tableBox.getStyleClass().add("card");

        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTimestamp()));
        dateCol.setPrefWidth(150);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getType().getDisplayName()));

        TableColumn<Transaction, String> amtCol = new TableColumn<>("Amount ($)");
        amtCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(ValidationUtil.formatAmount(cd.getValue().getAmount())));
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDescription()));

        table.getColumns().addAll(dateCol, typeCol, amtCol, descCol);

        Button loadBtn = new Button("Load Transactions");
        loadBtn.getStyleClass().add("btn-secondary");
        loadBtn.setOnAction(e -> {
            try {
                List<Transaction> txns = bank.getTransactionService()
                        .getTransactionHistory(accBox.getValue(), 1, 50);
                table.setItems(FXCollections.observableArrayList(txns));
            } catch (InvalidAccountException ex) {
                table.setItems(FXCollections.emptyObservableList());
            }
        });

        // Auto-load
        if (!accBox.getItems().isEmpty()) loadBtn.fire();
        accBox.setOnAction(e -> loadBtn.fire());

        tableBox.getChildren().add(table);
        page.getChildren().addAll(title, accLabel, accBox, loadBtn, tableBox);
        setContent(page);
    }

    // ====================================================================
    //  APPLY LOAN
    // ====================================================================
    private void showApplyLoan() {
        VBox page = new VBox(20);
        Label title = new Label("Apply for a Loan");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label typeLabel = new Label("Loan Type");
        typeLabel.getStyleClass().add("field-label");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("PERSONAL", "AUTO", "HOME", "EDUCATION");
        typeBox.setValue("PERSONAL");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        Label amtLabel = new Label("Loan Amount ($)");
        amtLabel.getStyleClass().add("field-label");
        TextField amtField = new TextField();
        amtField.setPromptText("Enter loan amount");
        amtField.setPrefHeight(38);

        Label termLabel = new Label("Term (months)");
        termLabel.getStyleClass().add("field-label");
        TextField termField = new TextField();
        termField.setPromptText("e.g., 12, 24, 60");
        termField.setPrefHeight(38);

        Label purposeLabel = new Label("Purpose");
        purposeLabel.getStyleClass().add("field-label");
        TextField purposeField = new TextField();
        purposeField.setPromptText("Describe the purpose");
        purposeField.setPrefHeight(38);

        Label empLabel = new Label("Employment Details");
        empLabel.getStyleClass().add("field-label");
        TextField empField = new TextField();
        empField.setPromptText("Job title / employer");
        empField.setPrefHeight(38);

        Label accLabel = new Label("Disbursement Account");
        accLabel.getStyleClass().add("field-label");
        ComboBox<String> accBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) accBox.getItems().add(acc.getAccountNumber());
        if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));
        accBox.setMaxWidth(Double.MAX_VALUE);

        // Credit score info
        double rate = Loan.calculateInterestRate(customer.getCreditScore());
        Label creditInfo = new Label("📊 Credit Score: " + customer.getCreditScore() +
                (rate > 0 ? " | Estimated Rate: " + String.format("%.1f%%", rate * 100) : " — Not eligible"));
        creditInfo.getStyleClass().add("alert-info");

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button applyBtn = new Button("Submit Application");
        applyBtn.getStyleClass().add("btn-primary");
        applyBtn.setPrefHeight(40);
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setOnAction(e -> {
            try {
                LoanType lt = LoanType.valueOf(typeBox.getValue());
                double amount = Double.parseDouble(amtField.getText());
                int term = Integer.parseInt(termField.getText());
                Loan loan = bank.getLoanService().applyForLoan(
                        customer.getUserId(), lt, amount, term,
                        purposeField.getText(), empField.getText(), accBox.getValue());
                resultLabel.setText("✓ Application submitted! Loan ID: " + loan.getLoanId() +
                        " | EMI: $" + ValidationUtil.formatAmount(loan.getMonthlyInstallment()));
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(typeLabel, typeBox, amtLabel, amtField, termLabel, termField,
                purposeLabel, purposeField, empLabel, empField, accLabel, accBox, creditInfo, resultLabel, applyBtn);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ====================================================================
    //  MY LOANS
    // ====================================================================
    private void showMyLoans() {
        VBox page = new VBox(20);
        Label title = new Label("My Loans");
        title.getStyleClass().add("page-title");
        page.getChildren().add(title);

        try {
            List<Loan> loans = bank.getLoanService().getCustomerLoans(customer.getUserId());
            if (loans == null || loans.isEmpty()) {
                Label empty = new Label("You don't have any loans.");
                empty.setStyle("-fx-text-fill: #7F8C8D;");
                page.getChildren().add(empty);
            } else {
                for (Loan loan : loans) {
                    VBox card = new VBox(8);
                    card.getStyleClass().add("card");
                    HBox row = new HBox(30);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label id = new Label(loan.getLoanId() != null ? loan.getLoanId() : "N/A");
                    id.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    Label type = new Label(loan.getLoanType() != null ? loan.getLoanType().getDisplayName() : "Unknown");
                    String loanSym = CurrencyUtil.getCurrencySymbol(null);
                    Label amount = new Label(loanSym + " " + ValidationUtil.formatAmount(loan.getLoanAmount()));
                    amount.setStyle("-fx-font-weight: bold; -fx-text-fill: #4A90D9;");

                    String statusText = loan.getStatus() != null ? loan.getStatus().getDisplayName() : "Unknown";
                    String statusColor = "#7F8C8D";
                    if (loan.getStatus() != null) {
                        if (loan.getStatus() == LoanStatus.ACTIVE) statusColor = "#27AE60";
                        else if (loan.getStatus() == LoanStatus.PENDING) statusColor = "#F39C12";
                        else statusColor = "#E74C3C";
                    }
                    Label status = new Label(statusText);
                    status.setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
                    row.getChildren().addAll(id, type, amount, status);

                    Label detail = new Label("Remaining: $" + ValidationUtil.formatAmount(loan.getRemainingBalance()) +
                        " | EMI: $" + ValidationUtil.formatAmount(loan.getMonthlyInstallment()) +
                        " | Rate: " + String.format("%.1f%%", loan.getInterestRate() * 100));
                    detail.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");

                    card.getChildren().addAll(row, detail);
                    page.getChildren().add(card);
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("⚠ Error loading loans: " + e.getMessage());
            errorLabel.getStyleClass().add("alert-error");
            errorLabel.setWrapText(true);
            page.getChildren().add(errorLabel);
        }
        setContent(page);
    }

    // ====================================================================
    //  PAY BILLS
    // ====================================================================
    private void showPayBills() {
        VBox page = new VBox(20);
        Label title = new Label("Pay Bills");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label provLabel = new Label("Provider");
        provLabel.getStyleClass().add("field-label");
        ComboBox<String> provBox = new ComboBox<>();
        provBox.getItems().addAll("Electricity", "Water", "Internet", "Phone");
        provBox.setValue("Electricity");
        provBox.setMaxWidth(Double.MAX_VALUE);

        Label billLabel = new Label("Bill Account Number");
        billLabel.getStyleClass().add("field-label");
        TextField billField = new TextField();
        billField.setPromptText("e.g., ELC-12345678");
        billField.setPrefHeight(38);

        Label nickLabel = new Label("Payee Nickname");
        nickLabel.getStyleClass().add("field-label");
        TextField nickField = new TextField();
        nickField.setPromptText("e.g., Home Electricity");
        nickField.setPrefHeight(38);

        Label amtLabel = new Label("Amount ($)");
        amtLabel.getStyleClass().add("field-label");
        TextField amtField = new TextField();
        amtField.setPromptText("Enter amount");
        amtField.setPrefHeight(38);

        Label accLabel = new Label("Pay From Account");
        accLabel.getStyleClass().add("field-label");
        ComboBox<String> accBox = new ComboBox<>();
        for (Account acc : customer.getAccounts())
            accBox.getItems().add(acc.getAccountNumber() + " — $" + ValidationUtil.formatAmount(acc.getBalance()));
        if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));
        accBox.setMaxWidth(Double.MAX_VALUE);

        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setVisible(false);

        Button payBtn = new Button("💳 Pay Now");
        payBtn.getStyleClass().add("btn-primary");
        payBtn.setPrefHeight(40);
        payBtn.setMaxWidth(Double.MAX_VALUE);
        payBtn.setOnAction(e -> {
            try {
                String srcAcc = accBox.getValue().split(" — ")[0];
                double amount = Double.parseDouble(amtField.getText());
                Transaction txn = bank.getBillPaymentService().payBill(
                        srcAcc, provBox.getValue().toUpperCase(), billField.getText(), amount, nickField.getText());
                resultLabel.setText("✓ Payment successful! Txn: " + txn.getTransactionId());
                resultLabel.getStyleClass().setAll("alert-success");
                resultLabel.setVisible(true);
            } catch (Exception ex) {
                resultLabel.setText("⚠ " + ex.getMessage());
                resultLabel.getStyleClass().setAll("alert-error");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(provLabel, provBox, billLabel, billField, nickLabel, nickField,
                amtLabel, amtField, accLabel, accBox, resultLabel, payBtn);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ====================================================================
    //  CURRENCY EXCHANGE
    // ====================================================================
    private void showExchange() {
        VBox page = new VBox(20);
        Label title = new Label("Currency Exchange");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Convert funds between your accounts in different currencies");
        subtitle.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px;");

        // ─── Section 1: Actual Conversion ───
        VBox conversionCard = new VBox(16);
        conversionCard.getStyleClass().add("card");
        conversionCard.setMaxWidth(550);

        Label convTitle = new Label("💱 Convert Between Accounts");
        convTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label srcLabel = new Label("From Account");
        srcLabel.getStyleClass().add("field-label");
        ComboBox<String> srcAccBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) {
            String sym = getCurrencySymbol(acc.getCurrency());
            srcAccBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance()));
        }
        if (!srcAccBox.getItems().isEmpty()) srcAccBox.setValue(srcAccBox.getItems().get(0));
        srcAccBox.setMaxWidth(Double.MAX_VALUE);

        Label dstLabel = new Label("To Account");
        dstLabel.getStyleClass().add("field-label");
        ComboBox<String> dstAccBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) {
            String sym = getCurrencySymbol(acc.getCurrency());
            dstAccBox.getItems().add(acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance()));
        }
        if (dstAccBox.getItems().size() > 1) dstAccBox.setValue(dstAccBox.getItems().get(1));
        else if (!dstAccBox.getItems().isEmpty()) dstAccBox.setValue(dstAccBox.getItems().get(0));
        dstAccBox.setMaxWidth(Double.MAX_VALUE);

        Label amtLabel = new Label("Amount");
        amtLabel.getStyleClass().add("field-label");
        TextField amtField = new TextField();
        amtField.setPromptText("Enter amount to convert");
        amtField.setPrefHeight(38);

        // Preview area
        Label previewLabel = new Label();
        previewLabel.setWrapText(true);
        previewLabel.setStyle("-fx-background-color: #EBF5FB; -fx-padding: 12; -fx-background-radius: 6;");
        previewLabel.setVisible(false);

        Label convResultLabel = new Label();
        convResultLabel.setWrapText(true);
        convResultLabel.setVisible(false);

        Button previewBtn = new Button("🔍 Preview Conversion");
        previewBtn.getStyleClass().add("btn-secondary");
        previewBtn.setMaxWidth(Double.MAX_VALUE);
        previewBtn.setOnAction(e -> {
            try {
                String srcAcc = srcAccBox.getValue().split(" \\[")[0];
                String dstAcc = dstAccBox.getValue().split(" \\[")[0];
                if (srcAcc.equals(dstAcc)) { previewLabel.setText("⚠ Select two different accounts."); previewLabel.setVisible(true); return; }
                Account src = bank.getAccountService().getAccount(srcAcc);
                Account dst = bank.getAccountService().getAccount(dstAcc);
                if (src.getCurrency().equals(dst.getCurrency())) {
                    previewLabel.setText("⚠ Both accounts use " + src.getCurrency() + ". Use Transfer instead.");
                    previewLabel.setVisible(true); return;
                }
                double amount = Double.parseDouble(amtField.getText());
                double rate = bank.getCurrencyService().getExchangeRate(src.getCurrency(), dst.getCurrency());
                double converted = bank.getCurrencyService().convert(amount, src.getCurrency(), dst.getCurrency());
                double fee = converted * 0.005;
                double net = converted - fee;
                previewLabel.setText(
                    "Rate: 1 " + src.getCurrency() + " = " + String.format("%.4f", rate) + " " + dst.getCurrency() + "\n" +
                    "Converted: " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(converted) + "\n" +
                    "Fee (0.5%): " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(fee) + "\n" +
                    "─────────────────────────\n" +
                    "You receive: " + getCurrencySymbol(dst.getCurrency()) + ValidationUtil.formatAmount(net)
                );
                previewLabel.setVisible(true);
            } catch (NumberFormatException ex) {
                previewLabel.setText("⚠ Enter a valid amount."); previewLabel.setVisible(true);
            } catch (Exception ex) {
                previewLabel.setText("⚠ " + ex.getMessage()); previewLabel.setVisible(true);
            }
        });

        Button convertBtn = new Button("💱 Convert Now");
        convertBtn.getStyleClass().add("btn-primary");
        convertBtn.setPrefHeight(40);
        convertBtn.setMaxWidth(Double.MAX_VALUE);
        convertBtn.setOnAction(e -> {
            try {
                String srcAcc = srcAccBox.getValue().split(" \\[")[0];
                String dstAcc = dstAccBox.getValue().split(" \\[")[0];
                if (srcAcc.equals(dstAcc)) { convResultLabel.setText("⚠ Select different accounts."); convResultLabel.getStyleClass().setAll("alert-error"); convResultLabel.setVisible(true); return; }
                Account src = bank.getAccountService().getAccount(srcAcc);
                Account dst = bank.getAccountService().getAccount(dstAcc);
                if (src.getCurrency().equals(dst.getCurrency())) {
                    convResultLabel.setText("⚠ Both accounts use the same currency. Use Transfer instead.");
                    convResultLabel.getStyleClass().setAll("alert-error"); convResultLabel.setVisible(true); return;
                }
                double amount = Double.parseDouble(amtField.getText());
                Transaction txn = bank.getTransactionService().transfer(srcAcc, dstAcc, amount, "Currency conversion");
                convResultLabel.setText("✓ Conversion successful! Transaction: " + txn.getTransactionId());
                convResultLabel.getStyleClass().setAll("alert-success");
                convResultLabel.setVisible(true);
                amtField.clear(); previewLabel.setVisible(false);
                // Refresh account dropdowns
                srcAccBox.getItems().clear();
                dstAccBox.getItems().clear();
                for (Account acc : customer.getAccounts()) {
                    String sym = getCurrencySymbol(acc.getCurrency());
                    String item = acc.getAccountNumber() + " [" + acc.getCurrency() + "] — " + sym + ValidationUtil.formatAmount(acc.getBalance());
                    srcAccBox.getItems().add(item);
                    dstAccBox.getItems().add(item);
                }
                if (!srcAccBox.getItems().isEmpty()) srcAccBox.setValue(srcAccBox.getItems().get(0));
                if (dstAccBox.getItems().size() > 1) dstAccBox.setValue(dstAccBox.getItems().get(1));
            } catch (NumberFormatException ex) {
                convResultLabel.setText("⚠ Enter a valid amount."); convResultLabel.getStyleClass().setAll("alert-error"); convResultLabel.setVisible(true);
            } catch (Exception ex) {
                convResultLabel.setText("⚠ " + ex.getMessage()); convResultLabel.getStyleClass().setAll("alert-error"); convResultLabel.setVisible(true);
            }
        });

        conversionCard.getChildren().addAll(convTitle, srcLabel, srcAccBox, dstLabel, dstAccBox,
                amtLabel, amtField, previewBtn, previewLabel, convResultLabel, convertBtn);

        // ─── Section 2: Rate Calculator ───
        VBox calcCard = new VBox(16);
        calcCard.getStyleClass().add("card");
        calcCard.setMaxWidth(550);

        Label calcTitle = new Label("📊 Rate Calculator");
        calcTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label fromLabel = new Label("From Currency");
        fromLabel.getStyleClass().add("field-label");
        ComboBox<String> fromBox = new ComboBox<>();
        fromBox.getItems().addAll("LKR", "USD", "EUR", "GBP");
        fromBox.setValue("LKR");
        fromBox.setMaxWidth(Double.MAX_VALUE);

        Label toLabel = new Label("To Currency");
        toLabel.getStyleClass().add("field-label");
        ComboBox<String> toBox = new ComboBox<>();
        toBox.getItems().addAll("USD", "EUR", "GBP", "LKR");
        toBox.setValue("LKR");
        toBox.setMaxWidth(Double.MAX_VALUE);

        Label calcAmtLabel = new Label("Amount");
        calcAmtLabel.getStyleClass().add("field-label");
        TextField calcAmtField = new TextField();
        calcAmtField.setPromptText("Enter amount");
        calcAmtField.setPrefHeight(38);

        Label calcResultLabel = new Label();
        calcResultLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4A90D9;");
        calcResultLabel.setVisible(false);

        Label rateLabel = new Label();
        rateLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");
        rateLabel.setVisible(false);

        Button calcBtn = new Button("🔢 Calculate");
        calcBtn.getStyleClass().add("btn-secondary");
        calcBtn.setPrefHeight(36);
        calcBtn.setMaxWidth(Double.MAX_VALUE);
        calcBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(calcAmtField.getText());
                double converted = bank.getCurrencyService().convert(amount, fromBox.getValue(), toBox.getValue());
                double rate = bank.getCurrencyService().getExchangeRate(fromBox.getValue(), toBox.getValue());
                calcResultLabel.setText(ValidationUtil.formatAmount(amount) + " " + fromBox.getValue() +
                        " = " + ValidationUtil.formatAmount(converted) + " " + toBox.getValue());
                calcResultLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4A90D9;");
                calcResultLabel.setVisible(true);
                rateLabel.setText("Rate: 1 " + fromBox.getValue() + " = " + String.format("%.4f", rate) + " " + toBox.getValue());
                rateLabel.setVisible(true);
            } catch (Exception ex) {
                calcResultLabel.setText("Error: " + ex.getMessage());
                calcResultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #E74C3C;");
                calcResultLabel.setVisible(true);
            }
        });

        calcCard.getChildren().addAll(calcTitle, fromLabel, fromBox, toLabel, toBox,
                calcAmtLabel, calcAmtField, calcBtn, calcResultLabel, rateLabel);

        page.getChildren().addAll(title, subtitle, conversionCard, calcCard);
        setContent(page);
    }

    // ====================================================================
    //  STATEMENT
    // ====================================================================
    private void showStatement() {
        VBox page = new VBox(20);
        Label title = new Label("Generate Statement");
        title.getStyleClass().add("page-title");

        VBox form = new VBox(16);
        form.getStyleClass().add("card");
        form.setMaxWidth(500);

        Label accLabel = new Label("Account");
        accLabel.getStyleClass().add("field-label");
        ComboBox<String> accBox = new ComboBox<>();
        for (Account acc : customer.getAccounts()) accBox.getItems().add(acc.getAccountNumber());
        if (!accBox.getItems().isEmpty()) accBox.setValue(accBox.getItems().get(0));
        accBox.setMaxWidth(Double.MAX_VALUE);

        Label monthLabel = new Label("Month");
        monthLabel.getStyleClass().add("field-label");
        ComboBox<Integer> monthBox = new ComboBox<>();
        for (int i = 1; i <= 12; i++) monthBox.getItems().add(i);
        monthBox.setValue(3);
        monthBox.setMaxWidth(Double.MAX_VALUE);

        Label yearLabel = new Label("Year");
        yearLabel.getStyleClass().add("field-label");
        TextField yearField = new TextField("2026");
        yearField.setPrefHeight(38);

        TextArea statementArea = new TextArea();
        statementArea.setEditable(false);
        statementArea.setPrefRowCount(15);
        statementArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        statementArea.setVisible(false);

        Button genBtn = new Button("📄 Generate");
        genBtn.getStyleClass().add("btn-primary");
        genBtn.setPrefHeight(40);
        genBtn.setMaxWidth(Double.MAX_VALUE);
        genBtn.setOnAction(e -> {
            try {
                int month = monthBox.getValue();
                int year = Integer.parseInt(yearField.getText());
                String statement = bank.getReportingService().generateMonthlyStatement(accBox.getValue(), month, year);
                statementArea.setText(statement);
                statementArea.setVisible(true);
            } catch (Exception ex) {
                statementArea.setText("Error: " + ex.getMessage());
                statementArea.setVisible(true);
            }
        });

        form.getChildren().addAll(accLabel, accBox, monthLabel, monthBox, yearLabel, yearField, genBtn, statementArea);
        page.getChildren().addAll(title, form);
        setContent(page);
    }

    // ====================================================================
    //  NOTIFICATIONS
    // ====================================================================
    private void showNotifications() {
        VBox page = new VBox(20);
        Label title = new Label("Notifications");
        title.getStyleClass().add("page-title");

        VBox list = new VBox(8);
        var notifications = customer.getAllNotifications();
        if (notifications.isEmpty()) {
            Label empty = new Label("No notifications.");
            empty.setStyle("-fx-text-fill: #7F8C8D;");
            list.getChildren().add(empty);
        } else {
            for (var notif : notifications) {
                HBox item = new HBox(12);
                item.getStyleClass().add("card");
                item.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label(notif.isRead() ? "📧" : "🔔");
                icon.setStyle("-fx-font-size: 18px;");
                VBox info = new VBox(2);
                Label msg = new Label(notif.getMessage());
                msg.setStyle(notif.isRead() ? "-fx-text-fill: #7F8C8D;" : "-fx-font-weight: bold;");
                msg.setWrapText(true);
                Label time = new Label(notif.getTimestamp());
                time.setStyle("-fx-text-fill: #B0B8C1; -fx-font-size: 11px;");
                info.getChildren().addAll(msg, time);
                HBox.setHgrow(info, Priority.ALWAYS);
                item.getChildren().addAll(icon, info);
                list.getChildren().add(item);
            }
        }

        Button markAllBtn = new Button("Mark All as Read");
        markAllBtn.getStyleClass().add("btn-secondary");
        markAllBtn.setOnAction(e -> {
            bank.getNotificationService().markAllAsRead(customer.getUserId());
            showNotifications(); // Refresh
        });

        page.getChildren().addAll(title, markAllBtn, list);
        setContent(page);
    }

    // ====================================================================
    //  PROFILE
    // ====================================================================
    private void showProfile() {
        VBox page = new VBox(20);
        Label title = new Label("My Profile");
        title.getStyleClass().add("page-title");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);

        String[][] fields = {
            {"Customer ID", customer.getUserId()},
            {"Full Name", customer.getFullName()},
            {"Email", customer.getEmail()},
            {"Phone", customer.getPhone()},
            {"Address", customer.getAddress()},
            {"Date of Birth", customer.getDateOfBirth()},
            {"Credit Score", String.valueOf(customer.getCreditScore())},
            {"Member Since", customer.getCreationDate()},
            {"Status", customer.isAccountLocked() ? "LOCKED" : "Active"},
        };

        for (String[] f : fields) {
            HBox row = new HBox(12);
            Label label = new Label(f[0]);
            label.setStyle("-fx-font-weight: bold; -fx-text-fill: #5A6A7A; -fx-min-width: 120;");
            Label value = new Label(f[1]);
            value.setStyle("-fx-text-fill: #2C3E50;");
            row.getChildren().addAll(label, value);
            card.getChildren().add(row);
        }

        // Change password section
        VBox pwdCard = new VBox(12);
        pwdCard.getStyleClass().add("card");
        pwdCard.setMaxWidth(500);
        Label pwdTitle = new Label("Change Password");
        pwdTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        PasswordField oldPwd = new PasswordField();
        oldPwd.setPromptText("Current password");
        PasswordField newPwd = new PasswordField();
        newPwd.setPromptText("New password");
        PasswordField confirmPwd = new PasswordField();
        confirmPwd.setPromptText("Confirm new password");

        Label pwdResult = new Label();
        pwdResult.setWrapText(true);
        pwdResult.setVisible(false);

        Button changePwdBtn = new Button("Change Password");
        changePwdBtn.getStyleClass().add("btn-secondary");
        changePwdBtn.setOnAction(e -> {
            if (!newPwd.getText().equals(confirmPwd.getText())) {
                pwdResult.setText("⚠ Passwords don't match.");
                pwdResult.getStyleClass().setAll("alert-error"); pwdResult.setVisible(true); return;
            }
            try {
                boolean ok = bank.getAuthService().changePassword(oldPwd.getText(), newPwd.getText());
                if (ok) {
                    pwdResult.setText("✓ Password changed.");
                    pwdResult.getStyleClass().setAll("alert-success");
                } else {
                    pwdResult.setText("⚠ Current password incorrect.");
                    pwdResult.getStyleClass().setAll("alert-error");
                }
                pwdResult.setVisible(true);
            } catch (InvalidInputException ex) {
                pwdResult.setText("⚠ " + ex.getMessage());
                pwdResult.getStyleClass().setAll("alert-error"); pwdResult.setVisible(true);
            }
        });

        pwdCard.getChildren().addAll(pwdTitle, oldPwd, newPwd, confirmPwd, pwdResult, changePwdBtn);

        page.getChildren().addAll(title, card, pwdCard);
        setContent(page);
    }

    // ====================================================================
    //  HELPER METHODS
    // ====================================================================

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

    private Button createActionButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-action");
        btn.setOnAction(e -> action.run());
        HBox.setHgrow(btn, Priority.ALWAYS);
        return btn;
    }

    private HBox createTransactionRow(Transaction txn) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));

        Label icon = new Label(txn.getType() == TransactionType.DEPOSIT ? "⬆" :
                              txn.getType() == TransactionType.WITHDRAWAL ? "⬇" : "↔");
        icon.setStyle("-fx-font-size: 16px;");

        VBox info = new VBox(2);
        Label desc = new Label(txn.getDescription());
        desc.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label date = new Label(txn.getTimestamp());
        date.setStyle("-fx-text-fill: #B0B8C1; -fx-font-size: 11px;");
        info.getChildren().addAll(desc, date);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean isPositive = txn.getType() == TransactionType.DEPOSIT;
        String sym = CurrencyUtil.getCurrencySymbol(txn.getCurrency());
        Label amount = new Label((isPositive ? "+" : "-") + sym + ValidationUtil.formatAmount(txn.getAmount()));
        amount.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " +
                (isPositive ? "#27AE60" : "#E74C3C") + ";");

        row.getChildren().addAll(icon, info, spacer, amount);
        return row;
    }

    public BorderPane getRoot() { return root; }
}
