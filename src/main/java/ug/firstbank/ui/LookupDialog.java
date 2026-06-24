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
import ug.firstbank.util.PdfExporter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class LookupDialog {

    private final Stage dialogStage;
    private final AccountService accountService;
    private final TextArea resultsArea = new TextArea();
    private final Button exportPdfButton = new Button("Export to PDF");

    // Store last search result for PDF export
    private AccountRecord lastSingleRecord = null;
    private List<AccountRecord> lastMultipleRecords = null;

    public LookupDialog(Window owner, AccountService accountService) {
        this.accountService = accountService;
        this.dialogStage = new Stage();
        this.dialogStage.setTitle("Find Account — First Bank Uganda");
        this.dialogStage.initOwner(owner);
        this.dialogStage.initModality(Modality.WINDOW_MODAL);
        this.dialogStage.setScene(new javafx.scene.Scene(build(), 520, 500));
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

        // Export button styling and behavior
        exportPdfButton.getStyleClass().add("secondary-button");
        exportPdfButton.setDisable(true);
        exportPdfButton.setOnAction(e -> handleExportPdf());

        HBox actions = new HBox(exportPdfButton);
        actions.getStyleClass().add("summary-actions");

        VBox container = new VBox(12, tabPane, new Label("Results"), resultsArea, actions);
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
        ninField.setPromptText("e.g. CM12345678ABCD");

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
                AccountRecord record = result.get();
                lastSingleRecord = record;
                lastMultipleRecords = null;

                resultsArea.setText(formatRecord(record));
                exportPdfButton.setDisable(false);
            } else {
                resultsArea.setText("No account found with number: " + accountNumber);
                exportPdfButton.setDisable(true);
                lastSingleRecord = null;
                lastMultipleRecords = null;
            }
        } catch (SQLException e) {
            showError("Lookup failed: " + e.getMessage());
            exportPdfButton.setDisable(true);
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
            lastMultipleRecords = results;
            lastSingleRecord = null;

            if (results.isEmpty()) {
                resultsArea.setText("No accounts found for NIN: " + nin);
                exportPdfButton.setDisable(true);
                return;
            }

            StringBuilder text = new StringBuilder();
            for (AccountRecord record : results) {
                text.append(formatRecord(record)).append("\n\n");
            }
            resultsArea.setText(text.toString().trim());
            exportPdfButton.setDisable(false);   // Enable even for multiple results

        } catch (SQLException e) {
            showError("Lookup failed: " + e.getMessage());
            exportPdfButton.setDisable(true);
        }
    }

    private String formatRecord(AccountRecord record) {
        return "Account Number: " + record.getAccountNumber() + "\n"
                + "Name: " + record.getFirstName() + " " + record.getLastName() + "\n"
                + "Account Type: " + record.getAccountType() + "\n"
                + "Branch: " + record.getBranch() + "\n"
                + "Opening Deposit: " + CurrencyFormatter.format(record.getOpeningDeposit());
    }

    private void handleExportPdf() {
        if (lastSingleRecord != null) {
            // Single record export (cleanest)
            exportSingleRecord(lastSingleRecord);
        } else if (lastMultipleRecords != null && !lastMultipleRecords.isEmpty()) {
            // Multiple records - export first one for now (can be enhanced later)
            exportSingleRecord(lastMultipleRecords.get(0));
        } else {
            showWarning("No account data available to export.");
        }
    }

    private void exportSingleRecord(AccountRecord record) {
        File destination = new File(System.getProperty("user.home"),
                record.getAccountNumber() + "_lookup_summary.pdf");

        try {
            PdfExporter.export(record, destination);
            showInfo("Summary exported successfully",
                    "Saved to: " + destination.getAbsolutePath());
        } catch (IOException | com.itextpdf.text.DocumentException e) {
            showError("Export failed: " + e.getMessage());
        }
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }
}