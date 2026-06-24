package ug.firstbank.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Control;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ug.firstbank.util.DateUtils;
import ug.firstbank.validation.ValidationResult;
import ug.firstbank.validation.ValidationResult.FieldNames;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AccountFormPane {

    public record FormSnapshot(
            String firstName,
            String lastName,
            String nin,
            String secondNin,
            String email,
            String confirmEmail,
            String phone,
            String pin,
            String confirmPin,
            LocalDate dateOfBirth,
            String accountType,
            String branch,
            String depositText) {}

    private static final List<String> ACCOUNT_TYPES =
            List.of("Savings", "Current", "Fixed Deposit", "Student", "Joint");

    private static final List<String> BRANCHES =
            List.of("Kampala", "Gulu", "Mbarara", "Jinja", "Mbale");

    private static final List<String> MONTHS = List.of(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December");

    private final VBox view;

    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField ninField = new TextField();
    private final TextField secondNinField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField confirmEmailField = new TextField();
    private final TextField phoneField = new TextField();
    private final PasswordField pinField = new PasswordField();
    private final PasswordField confirmPinField = new PasswordField();

    private final ComboBox<Integer> yearCombo = new ComboBox<>();
    private final ComboBox<String> monthCombo = new ComboBox<>();
    private final ComboBox<Integer> dayCombo = new ComboBox<>();

    private final ComboBox<String> accountTypeCombo = new ComboBox<>();
    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final TextField depositField = new TextField();

    private final Button submitButton = new Button("Submit Application");
    private final Button resetButton = new Button("Reset");

    private final Map<String, Label> errorLabels = new LinkedHashMap<>();

    private Consumer<FormSnapshot> onSubmit = snapshot -> {};
    private Runnable onReset = () -> {};

    public AccountFormPane() {
        this.view = build();
        configureStaticOptions();
        configureDynamicBehavior();
        configureActions();
    }

    public Region getView() {
        return view;
    }

    public void setOnSubmit(Consumer<FormSnapshot> handler) {
        this.onSubmit = handler;
    }

    public void setOnReset(Runnable handler) {
        this.onReset = handler;
    }

    public FormSnapshot captureSnapshot() {
        return new FormSnapshot(
                firstNameField.getText(),
                lastNameField.getText(),
                ninField.getText(),
                isJointSelected() ? secondNinField.getText() : null,
                emailField.getText(),
                confirmEmailField.getText(),
                phoneField.getText(),
                pinField.getText(),
                confirmPinField.getText(),
                buildSelectedDate(),
                accountTypeCombo.getValue(),
                branchCombo.getValue(),
                depositField.getText()
        );
    }

    public void applyValidationErrors(ValidationResult result) {
        clearErrorLabels();
        result.getAllErrors().forEach(this::showFieldError);
    }

    public void showFieldError(String fieldName, String message) {
        Label label = errorLabels.get(fieldName);
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
        }
    }

    public void clearAll() {
        firstNameField.clear();
        lastNameField.clear();
        ninField.clear();
        secondNinField.clear();
        emailField.clear();
        confirmEmailField.clear();
        phoneField.clear();
        pinField.clear();
        confirmPinField.clear();
        yearCombo.getSelectionModel().clearSelection();
        monthCombo.getSelectionModel().clearSelection();
        dayCombo.getItems().clear();
        accountTypeCombo.getSelectionModel().clearSelection();
        branchCombo.getSelectionModel().clearSelection();
        depositField.clear();
        secondNinField.setDisable(true);
        clearErrorLabels();
        unlockForm();
    }

    public void lockAfterSuccess() {
        submitButton.setDisable(true);
        setFieldsDisabled(true);
    }

    private void unlockForm() {
        submitButton.setDisable(false);
        setFieldsDisabled(false);
    }

    private void setFieldsDisabled(boolean disabled) {
        firstNameField.setDisable(disabled);
        lastNameField.setDisable(disabled);
        ninField.setDisable(disabled);
        emailField.setDisable(disabled);
        confirmEmailField.setDisable(disabled);
        phoneField.setDisable(disabled);
        pinField.setDisable(disabled);
        confirmPinField.setDisable(disabled);
        yearCombo.setDisable(disabled);
        monthCombo.setDisable(disabled);
        dayCombo.setDisable(disabled);
        accountTypeCombo.setDisable(disabled);
        branchCombo.setDisable(disabled);
        depositField.setDisable(disabled);
        if (disabled || !isJointSelected()) {
            secondNinField.setDisable(true);
        } else {
            secondNinField.setDisable(false);
        }
    }

    private boolean isJointSelected() {
        return "Joint".equals(accountTypeCombo.getValue());
    }

    private LocalDate buildSelectedDate() {
        Integer year = yearCombo.getValue();
        Integer day = dayCombo.getValue();
        String monthName = monthCombo.getValue();
        Integer month = monthName == null ? null : MONTHS.indexOf(monthName) + 1;
        if (month != null && month == 0) month = null;
        return DateUtils.toLocalDate(year, month, day);
    }

    private void clearErrorLabels() {
        errorLabels.values().forEach(label -> {
            label.setText("");
            label.setVisible(false);
        });
    }

    private void configureStaticOptions() {
        accountTypeCombo.getItems().addAll(ACCOUNT_TYPES);
        branchCombo.getItems().addAll(BRANCHES);
        monthCombo.getItems().addAll(MONTHS);

        for (int year : DateUtils.birthYearRange()) {
            yearCombo.getItems().add(year);
        }

        secondNinField.setDisable(true);
        secondNinField.setPromptText("Spouse NIN — Joint accounts only");
    }

    private void configureDynamicBehavior() {
        yearCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshDayOptions());
        monthCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshDayOptions());

        accountTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean joint = "Joint".equals(newVal);
            secondNinField.setDisable(!joint);
            if (!joint) secondNinField.clear();
        });
    }

    private void refreshDayOptions() {
        Integer year = yearCombo.getValue();
        String monthName = monthCombo.getValue();
        if (year == null || monthName == null) {
            dayCombo.getItems().clear();
            return;
        }

        int month = MONTHS.indexOf(monthName) + 1;
        int maxDay = DateUtils.daysInMonth(month, year);
        Integer previouslySelected = dayCombo.getValue();

        dayCombo.getItems().clear();
        for (int day = 1; day <= maxDay; day++) {
            dayCombo.getItems().add(day);
        }

        if (previouslySelected != null && previouslySelected <= maxDay) {
            dayCombo.setValue(previouslySelected);
        }
    }

    private void configureActions() {
        submitButton.setOnAction(e -> onSubmit.accept(captureSnapshot()));
        resetButton.setOnAction(e -> onReset.run());
    }

    private VBox build() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(4);
        grid.getStyleClass().add("form-grid");

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setPrefWidth(240);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        int row = 0;
        row = addField(grid, row, "First Name", firstNameField, FieldNames.FIRST_NAME);
        row = addField(grid, row, "Last Name", lastNameField, FieldNames.LAST_NAME);
        row = addField(grid, row, "National ID (NIN)", ninField, FieldNames.NIN);
        row = addField(grid, row, "Spouse NIN (Joint only)", secondNinField, FieldNames.SECOND_NIN);
        row = addField(grid, row, "Email", emailField, FieldNames.EMAIL);
        row = addField(grid, row, "Confirm Email", confirmEmailField, FieldNames.CONFIRM_EMAIL);
        row = addField(grid, row, "Phone Number", phoneField, FieldNames.PHONE);
        row = addField(grid, row, "PIN", pinField, FieldNames.PIN);
        row = addField(grid, row, "Confirm PIN", confirmPinField, FieldNames.CONFIRM_PIN);
        row = addDobRow(grid, row);
        row = addField(grid, row, "Account Type", accountTypeCombo, FieldNames.ACCOUNT_TYPE);
        row = addField(grid, row, "Branch", branchCombo, FieldNames.BRANCH);
        row = addField(grid, row, "Opening Deposit (UGX)", depositField, FieldNames.OPENING_DEPOSIT);

        HBox actions = new HBox(12, submitButton, resetButton);
        actions.getStyleClass().add("form-actions");
        submitButton.getStyleClass().add("primary-button");
        resetButton.getStyleClass().add("secondary-button");

        VBox container = new VBox(16, grid, actions);
        container.setPadding(new Insets(8, 0, 0, 0));
        container.getStyleClass().add("form-pane");
        return container;
    }

    private int addField(GridPane grid, int row, String labelText, Control control, String fieldName) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        grid.add(label, 0, row);
        grid.add(control, 1, row);

        Label errorLabel = buildErrorLabel();
        errorLabels.put(fieldName, errorLabel);
        grid.add(errorLabel, 1, row + 1);

        return row + 2;
    }

    private int addDobRow(GridPane grid, int row) {
        Label label = new Label("Date of Birth");
        label.getStyleClass().add("field-label");
        grid.add(label, 0, row);

        HBox dobBox = new HBox(8, yearCombo, monthCombo, dayCombo);
        yearCombo.setPromptText("Year");
        monthCombo.setPromptText("Month");
        dayCombo.setPromptText("Day");
        grid.add(dobBox, 1, row);

        Label errorLabel = buildErrorLabel();
        errorLabels.put(FieldNames.DATE_OF_BIRTH, errorLabel);
        grid.add(errorLabel, 1, row + 1);

        return row + 2;
    }

    private Label buildErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setVisible(false);
        label.setWrapText(true);
        return label;
    }
}