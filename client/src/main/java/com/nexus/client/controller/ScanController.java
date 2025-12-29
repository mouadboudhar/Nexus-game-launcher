package com.nexus.client.controller;

import com.nexus.client.service.ScanTask;
import com.nexus.client.service.ScannerService;
import com.nexus.shared.model.Game;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Scan Games view.
 * Handles scanning Steam, Epic, and System games.
 */
public class ScanController implements Initializable {

    @FXML private CheckBox steamCheckbox;
    @FXML private CheckBox epicCheckbox;
    @FXML private Label steamPathLabel;
    @FXML private Label epicPathLabel;
    @FXML private Button scanButton;
    @FXML private ProgressIndicator scanProgress;
    @FXML private Label scanStatusLabel;

    private final ScannerService scannerService = new ScannerService();
    private ScanTask currentScanTask;
    private boolean isScanning = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        steamCheckbox.setSelected(true);
        epicCheckbox.setSelected(true);

        // Detect and display actual paths
        detectPaths();

        if (scanProgress != null) {
            scanProgress.setVisible(false);
        }
    }

    /**
     * Detects Steam and Epic paths from the system.
     */
    private void detectPaths() {
        // Run detection in background
        new Thread(() -> {
            String steamPath = scannerService.getDetectedSteamPath();
            String epicPath = scannerService.getDetectedEpicPath();

            Platform.runLater(() -> {
                if (steamPath != null && !steamPath.isEmpty()) {
                    steamPathLabel.setText(steamPath);
                    steamPathLabel.getStyleClass().remove("path-not-found");
                } else {
                    steamPathLabel.setText("Steam not detected");
                    steamPathLabel.getStyleClass().add("path-not-found");
                }

                if (epicPath != null && new java.io.File(epicPath).exists()) {
                    epicPathLabel.setText(epicPath);
                    epicPathLabel.getStyleClass().remove("path-not-found");
                } else {
                    epicPathLabel.setText("Epic Games not detected");
                    epicPathLabel.getStyleClass().add("path-not-found");
                }
            });
        }).start();
    }

    @FXML
    private void onStartScan() {
        if (isScanning) {
            cancelScan();
            return;
        }

        if (!steamCheckbox.isSelected() && !epicCheckbox.isSelected()) {
            scanStatusLabel.setText("Please select at least one store to scan.");
            return;
        }

        startScan();
    }

    private void startScan() {
        isScanning = true;
        scanButton.setText("Cancel Scan");
        scanButton.getStyleClass().add("danger-button");

        if (scanProgress != null) {
            scanProgress.setVisible(true);
        }

        // Create scan task with selected sources
        currentScanTask = new ScanTask(
                steamCheckbox.isSelected(),
                epicCheckbox.isSelected(),
                true // Always scan system games
        );

        // Bind progress to UI
        if (scanProgress != null) {
            scanProgress.progressProperty().bind(currentScanTask.progressProperty());
        }

        // Update status label
        currentScanTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> scanStatusLabel.setText(newMsg));
        });

        // Handle success
        currentScanTask.setOnSucceeded(e -> {
            List<Game> games = currentScanTask.getValue();
            Platform.runLater(() -> {
                completeScan(games.size());
            });
        });

        // Handle failure
        currentScanTask.setOnFailed(e -> {
            Throwable exception = currentScanTask.getException();
            Platform.runLater(() -> {
                scanStatusLabel.setText("Scan failed: " + (exception != null ? exception.getMessage() : "Unknown error"));
                resetScanButton();
            });
        });

        // Handle cancellation
        currentScanTask.setOnCancelled(e -> {
            Platform.runLater(() -> {
                scanStatusLabel.setText("Scan cancelled.");
                resetScanButton();
            });
        });

        // Start scan in background thread
        Thread scanThread = new Thread(currentScanTask);
        scanThread.setDaemon(true);
        scanThread.start();

        StringBuilder status = new StringBuilder("Scanning: ");
        if (steamCheckbox.isSelected()) status.append("Steam ");
        if (epicCheckbox.isSelected()) status.append("Epic Games ");
        status.append("System...");
        scanStatusLabel.setText(status.toString());
    }

    private void completeScan(int gamesFound) {
        resetScanButton();
        scanStatusLabel.setText("Scan complete! Found " + gamesFound + " games.");
        System.out.println("[ScanController] Scan complete! Found " + gamesFound + " games");
    }

    private void cancelScan() {
        if (currentScanTask != null && currentScanTask.isRunning()) {
            currentScanTask.cancel();
        }
        resetScanButton();
        scanStatusLabel.setText("Scan cancelled.");
    }

    private void resetScanButton() {
        isScanning = false;
        scanButton.setText("Start Scan");
        scanButton.getStyleClass().remove("danger-button");

        if (scanProgress != null) {
            scanProgress.progressProperty().unbind();
            scanProgress.setProgress(0);
            scanProgress.setVisible(false);
        }
    }

    @FXML
    private void onBrowseSteam() {
        System.out.println("[ScanController] Browse for Steam path...");
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Steam Installation Folder");

        // Set initial directory if current path exists
        String currentPath = steamPathLabel.getText();
        if (currentPath != null && new java.io.File(currentPath).exists()) {
            chooser.setInitialDirectory(new java.io.File(currentPath));
        }

        java.io.File selectedDir = chooser.showDialog(scanButton.getScene().getWindow());
        if (selectedDir != null) {
            steamPathLabel.setText(selectedDir.getAbsolutePath());
            steamPathLabel.getStyleClass().remove("path-not-found");
        }
    }

    @FXML
    private void onBrowseEpic() {
        System.out.println("[ScanController] Browse for Epic path...");
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Epic Games Manifests Folder");

        // Set initial directory if current path exists
        String currentPath = epicPathLabel.getText();
        if (currentPath != null && new java.io.File(currentPath).exists()) {
            chooser.setInitialDirectory(new java.io.File(currentPath));
        }

        java.io.File selectedDir = chooser.showDialog(scanButton.getScene().getWindow());
        if (selectedDir != null) {
            epicPathLabel.setText(selectedDir.getAbsolutePath());
            epicPathLabel.getStyleClass().remove("path-not-found");
        }
    }
}

