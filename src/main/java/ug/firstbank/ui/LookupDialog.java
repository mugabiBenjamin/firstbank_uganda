package ug.firstbank.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ug.firstbank.model.AccountRecord;
import ug.firstbank.service.AccountService;
import ug.firstbank.util.CurrencyFormatter;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class LookupDialog {

    private final Stage dialogStage;
    private final AccountService accountService;
    private final TextArea resultsArea = new TextArea();

    public LookupDialog(Window owner, AccountService accountService) {
        this.accountService = accountService;
        this.dialogStage = new Stage();
        this.dialogStage.setTitle("Find Account — First Bank Uganda");
        this.dialogStage.initOwner(owner);
        this.dialogStage.initModality(Modality.WINDOW_MODAL);
        this.dialogStage.setScene(new javafx.scene.Scene(build(), 520, 460));
    }

    public void showAndWait() {
        dialogStage.showAndWait();
    }

    private VBox build() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(buildAccountNumberTab());
        tabPane.getTabs().add(buildNinTab());
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setPromptText("Search results will appear here.");
        VBox.setVgrow(resultsArea, Priority.ALWAYS);

        VBox container = new VBox(12, tabPane, new Label("Results"), resultsArea);
        container.setPadding(new Insets(16));
        return container;
    }

    private Tab buildAccountNumberTab() {
        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("e.g. KLA-2026-000142");

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchByAccountNumber(accountNumberField.getText()));

        HBox row = new HBox(8, accountNumberField, searchButton);
        HBox.setHgrow(accountNumberField, Priority.ALWAYS);

        VBox content = new VBox(8, new Label("Account Number"), row);
        content.setPadding(new Insets(12));

        Tab tab = new Tab("By Account Number", content);
        return tab;
    }

    private Tab buildNinTab() {
        TextField ninField = new TextField();
        ninField.setPromptText("e.g. CM63738361TYWS");

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchByNin(ninField.getText()));

        HBox row = new HBox(8, ninField, searchButton);
        HBox.setHgrow(ninField, Priority.ALWAYS);

        VBox content = new VBox(8, new Label("National ID (NIN)"), row);
        content.setPadding(new Insets(12));

        Tab tab = new Tab("By NIN", content);
        return tab;
    }

    private void searchByAccountNumber(String rawAccountNumber) {
        String accountNumber = rawAccountNumber == null ? "" : rawAccountNumber.trim();
        if (accountNumber.isEmpty()) {
            showWarning("Please enter an account number to search.");
            return;
        }

        try {
            Optional<AccountRecord> result = accountService.findByAccountNumber(accountNumber);
            if (result.isPresent()) {
                resultsArea.setText(formatRecord(result.get()));
            } else {
                resultsArea.setText("No account found with number: " + accountNumber);
            }
        } catch (SQLException e) {
            showError("Lookup failed: " + e.getMessage());
        }
    }

    private void searchByNin(String rawNin) {
        String nin = rawNin == null ? "" : rawNin.trim();
        if (nin.isEmpty()) {
            showWarning("Please enter a NIN to search.");
            return;
        }

        try {
            List<AccountRecord> results = accountService.findByNin(nin);
            if (results.isEmpty()) {
                resultsArea.setText("No accounts found for NIN: " + nin);
                return;
            }

            StringBuilder text = new StringBuilder();
            for (AccountRecord record : results) {
                text.append(formatRecord(record)).append("\n\n");
            }
            resultsArea.setText(text.toString().trim());
        } catch (SQLException e) {
            showError("Lookup failed: " + e.getMessage());
        }
    }

    private String formatRecord(AccountRecord record) {
        return "Account Number: " + record.getAccountNumber() + "\n"
                + "Name: " + record.getFirstName() + " " + record.getLastName() + "\n"
                + "Account Type: " + record.getAccountType() + "\n"
                + "Branch: " + record.getBranch() + "\n"
                + "Opening Deposit: " + CurrencyFormatter.format(record.getOpeningDeposit());
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }
}