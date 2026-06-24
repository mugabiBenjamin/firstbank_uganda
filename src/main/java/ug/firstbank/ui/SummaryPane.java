package ug.firstbank.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ug.firstbank.model.AccountRecord;
import ug.firstbank.util.CurrencyFormatter;

public final class SummaryPane {

    private static final String PLACEHOLDER_TEXT =
            "No account has been opened yet.\n\nSubmit the form to see the account summary here.";

    private final VBox view;
    private final Label headingLabel = new Label("Account Summary is Below:");
    private final TextArea summaryArea = new TextArea();
    private final Button exportPdfButton = new Button("Export to PDF");

    private Runnable onExportPdf = () -> {};

    public SummaryPane() {
        this.view = build();
        showPlaceholder();
    }

    public Region getView() {
        return view;
    }

    public void setOnExportPdf(Runnable handler) {
        this.onExportPdf = handler;
    }

    public void display(AccountRecord record) {
        summaryArea.setText(buildSummaryText(record));
        exportPdfButton.setDisable(false);
    }

    public void clear() {
        showPlaceholder();
        exportPdfButton.setDisable(true);
    }

    private void showPlaceholder() {
        summaryArea.setText(PLACEHOLDER_TEXT);
        exportPdfButton.setDisable(true);
    }

    private String buildSummaryText(AccountRecord record) {
        StringBuilder text = new StringBuilder();
        text.append("Account Number:   ").append(record.getAccountNumber()).append('\n');
        text.append("Full Name:        ").append(record.getFirstName())
                .append(' ').append(record.getLastName()).append('\n');
        text.append("Account Type:     ").append(record.getAccountType()).append('\n');
        text.append("Branch:           ").append(record.getBranch()).append('\n');
        text.append("Date of Birth:    ").append(record.getDateOfBirth()).append('\n');
        text.append("National ID:      ").append(record.getNin()).append('\n');

        if (record.getSecondNin() != null) {
            text.append("Spouse NIN:       ").append(record.getSecondNin()).append('\n');
        }

        text.append("Phone:            ").append(record.getPhone()).append('\n');
        text.append("Email:            ").append(record.getEmail()).append('\n');
        text.append("Opening Deposit:  ")
                .append(CurrencyFormatter.format(record.getOpeningDeposit())).append('\n');
        text.append('\n');
        text.append(record.toSummaryLine());
        return text.toString();
    }

    private VBox build() {
        headingLabel.getStyleClass().add("summary-heading");

        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.getStyleClass().add("summary-area");
        VBox.setVgrow(summaryArea, Priority.ALWAYS);

        exportPdfButton.getStyleClass().add("secondary-button");
        exportPdfButton.setOnAction(e -> onExportPdf.run());
        exportPdfButton.setDisable(true);

        HBox actions = new HBox(exportPdfButton);
        actions.getStyleClass().add("summary-actions");

        VBox container = new VBox(12, headingLabel, summaryArea, actions);
        container.setPadding(new Insets(0, 0, 0, 0));
        container.getStyleClass().add("summary-pane");
        return container;
    }
}