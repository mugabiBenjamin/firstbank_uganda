package ug.firstbank.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class AboutDialog {

    private final Stage dialogStage;

    public AboutDialog(Window owner) {
        this.dialogStage = new Stage();
        this.dialogStage.setTitle("About First Bank Uganda");
        this.dialogStage.initOwner(owner);
        this.dialogStage.initModality(Modality.WINDOW_MODAL);
        this.dialogStage.setResizable(false);
        this.dialogStage.setScene(new Scene(build(), 360, 220));
    }

    public void showAndWait() {
        dialogStage.showAndWait();
    }

    private VBox build() {
        Label title = new Label("First Bank Uganda");
        title.getStyleClass().add("brand-title");

        Label subtitle = new Label("New Account Opening Application");
        subtitle.getStyleClass().add("brand-subtitle");

        Label version = new Label("Version 1.0.0");
        Label tagline = new Label("Your Trusted Banking Partner");

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialogStage.close());

        VBox container = new VBox(8, title, subtitle, tagline, version, closeButton);
        container.setPadding(new Insets(24));
        container.getStyleClass().add("about-dialog");
        return container;
    }
}