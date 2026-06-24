package ug.firstbank.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
// import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
// import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ug.firstbank.model.Account;
import ug.firstbank.model.CurrentAccount;
import ug.firstbank.model.FixedDepositAccount;
import ug.firstbank.model.JointAccount;
import ug.firstbank.model.SavingsAccount;
import ug.firstbank.model.StudentAccount;
import ug.firstbank.model.AccountRecord;
import ug.firstbank.persistence.AccountRepository;
import ug.firstbank.persistence.DatabaseManager;
import ug.firstbank.service.AccountService;
import ug.firstbank.util.PdfExporter;
import ug.firstbank.validation.ValidationResult;

import java.io.File;
import java.io.IOException;
// import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;

public final class MainWindow {

    private static final Map<String, Supplier<Account>> ACCOUNT_FACTORIES = Map.of(
            "Savings",       SavingsAccount::new,
            "Current",       CurrentAccount::new,
            "Fixed Deposit", FixedDepositAccount::new,
            "Student",       StudentAccount::new,
            "Joint",         JointAccount::new
    );

    private final Stage stage;
    private final DatabaseManager databaseManager;
    private final AccountService accountService;

    private final AccountFormPane formPane;
    private final SummaryPane summaryPane;

    private AccountRecord lastSavedRecord;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.databaseManager = new DatabaseManager();
        this.accountService = new AccountService(new AccountRepository(databaseManager));

        this.formPane = new AccountFormPane();
        this.summaryPane = new SummaryPane();

        wireActions();
    }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar());
        root.setCenter(buildContent());
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 980, 760);
        scene.getStylesheets().add(buildStylesheetUrl());
        return scene;
    }

    public void shutdown() {
        databaseManager.close();
    }

    private String buildStylesheetUrl() {
        return getClass().getResource("/ug/firstbank/ui/theme.css").toExternalForm();
    }

    private MenuBar buildMenuBar() {
        Menu accountMenu = new Menu("Account");
        MenuItem lookupItem = new MenuItem("Find Account…");
        lookupItem.setOnAction(e -> openLookupDialog());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());
        accountMenu.getItems().addAll(lookupItem, exitItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About First Bank Uganda");
        aboutItem.setOnAction(e -> new AboutDialog(stage).showAndWait());
        helpMenu.getItems().add(aboutItem);

        MenuBar menuBar = new MenuBar(accountMenu, helpMenu);
        menuBar.getStyleClass().add("app-menu-bar");
        return menuBar;
    }

    private Region buildContent() {
        VBox header = buildHeader();

        HBox columns = new HBox(24, formPane.getView(), summaryPane.getView());
        columns.setPadding(new Insets(0, 24, 24, 24));
        HBox.setHgrow(formPane.getView(), Priority.NEVER);
        HBox.setHgrow(summaryPane.getView(), Priority.ALWAYS);

        VBox layout = new VBox(header, columns);
        layout.getStyleClass().add("app-content");
        return layout;
    }

    private VBox buildHeader() {
        Label title = new Label("First Bank Uganda");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("New Account Opening Application");
        subtitle.getStyleClass().add("brand-subtitle");

        VBox header = new VBox(2, title, subtitle);
        header.setPadding(new Insets(24, 24, 16, 24));
        header.getStyleClass().add("app-header");
        return header;
    }

    private void wireActions() {
        formPane.setOnSubmit(this::handleSubmit);
        formPane.setOnReset(() -> {
            formPane.clearAll();
            summaryPane.clear();
        });
        summaryPane.setOnExportPdf(this::handleExportPdf);
    }

    private void handleSubmit(AccountFormPane.FormSnapshot snapshot) {
        Supplier<Account> factory = ACCOUNT_FACTORIES.get(snapshot.accountType());
        Account account = factory == null ? null : factory.get();

        if (account == null) {
            formPane.showFieldError(ValidationResult.FieldNames.ACCOUNT_TYPE,
                    "Please select an account type.");
            return;
        }

        AccountService.SubmissionResult result = accountService.submitApplication(
                snapshot.firstName(), snapshot.lastName(),
                snapshot.nin(), snapshot.secondNin(),
                snapshot.email(), snapshot.confirmEmail(),
                snapshot.phone(),
                snapshot.pin(), snapshot.confirmPin(),
                snapshot.dateOfBirth(),
                account,
                snapshot.branch(),
                snapshot.depositText(),
                false);

        handleResult(result);
    }

    private void handleResult(AccountService.SubmissionResult result) {
        if (result instanceof AccountService.SubmissionResult.Success success) {
            lastSavedRecord = success.record();
            summaryPane.display(lastSavedRecord);
            formPane.lockAfterSuccess();

        } else if (result instanceof AccountService.SubmissionResult.ValidationFailure failure) {
            formPane.applyValidationErrors(failure.result());
            showErrorSummary(failure.result());

        } else if (result instanceof AccountService.SubmissionResult.DuplicateWarning duplicate) {
            confirmDuplicateOverride(duplicate);

        } else if (result instanceof AccountService.SubmissionResult.PersistenceError error) {
            showAlert(Alert.AlertType.ERROR, "Could not open account",
                    "A database error occurred: " + error.cause().getMessage());
        }
    }

    private void confirmDuplicateOverride(AccountService.SubmissionResult.DuplicateWarning duplicate) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Possible duplicate account");
        alert.setHeaderText("An account with this NIN, type, and branch already exists.");
        alert.setContentText("NIN: " + duplicate.nin()
                + "\nAccount type: " + duplicate.accountType()
                + "\nBranch: " + duplicate.branch()
                + "\n\nOpen another account anyway?");

        alert.showAndWait().filter(response -> response == ButtonType.OK)
                .ifPresent(response -> resubmitForcingDuplicate());
    }

    private void resubmitForcingDuplicate() {
        AccountFormPane.FormSnapshot snapshot = formPane.captureSnapshot();
        Supplier<Account> factory = ACCOUNT_FACTORIES.get(snapshot.accountType());
        if (factory == null) return;

        AccountService.SubmissionResult result = accountService.submitApplication(
                snapshot.firstName(), snapshot.lastName(),
                snapshot.nin(), snapshot.secondNin(),
                snapshot.email(), snapshot.confirmEmail(),
                snapshot.phone(),
                snapshot.pin(), snapshot.confirmPin(),
                snapshot.dateOfBirth(),
                factory.get(),
                snapshot.branch(),
                snapshot.depositText(),
                true);

        handleResult(result);
    }

    private void showErrorSummary(ValidationResult validation) {
        StringBuilder body = new StringBuilder();
        validation.getAllErrors().values().forEach(msg -> body.append("• ").append(msg).append('\n'));
        showAlert(Alert.AlertType.WARNING, "Please fix the highlighted fields", body.toString());
    }

    private void handleExportPdf() {
        if (lastSavedRecord == null) return;

        File destination = new File(System.getProperty("user.home"),
                lastSavedRecord.getAccountNumber() + "_summary.pdf");
        try {
            PdfExporter.export(lastSavedRecord, destination);
            showAlert(Alert.AlertType.INFORMATION, "Summary exported",
                    "Saved to: " + destination.getAbsolutePath());
        } catch (IOException | com.itextpdf.text.DocumentException e) {
            showAlert(Alert.AlertType.ERROR, "Export failed", e.getMessage());
        }
    }

    private void openLookupDialog() {
        new LookupDialog(stage, accountService).showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}