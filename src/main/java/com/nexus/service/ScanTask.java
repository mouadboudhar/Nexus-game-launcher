package com.nexus.service;

import com.nexus.model.Game;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Task for scanning games in the background.
 * Keeps the UI responsive during scanning operations.
 */
public class ScanTask extends Task<List<Game>> {

    private final ScannerService scannerService;
    private final boolean scanSteam;
    private final boolean scanEpic;
    private final boolean scanSystem;

    /**
     * Creates a scan task with all sources enabled.
     */
    public ScanTask() {
        this(true, true, true);
    }

    /**
     * Creates a scan task with configurable sources.
     */
    public ScanTask(boolean scanSteam, boolean scanEpic, boolean scanSystem) {
        this.scannerService = new ScannerService();
        this.scanSteam = scanSteam;
        this.scanEpic = scanEpic;
        this.scanSystem = scanSystem;
    }

    @Override
    protected List<Game> call() throws Exception {
        updateMessage("Initializing scanner...");
        updateProgress(0, 100);

        if (isCancelled()) {
            return new ArrayList<>();
        }

        // Use the unified scanAll which handles deduplication
        updateMessage("Scanning for games...");
        updateProgress(10, 100);

        List<Game> games = scannerService.scanAll();

        updateMessage("Scan complete! Found " + games.size() + " games.");
        updateProgress(100, 100);

        return games;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        System.out.println("[ScanTask] Scan succeeded with " + getValue().size() + " games");
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        System.out.println("[ScanTask] Scan was cancelled");
    }

    @Override
    protected void failed() {
        super.failed();
        Throwable exception = getException();
        System.err.println("[ScanTask] Scan failed: " + (exception != null ? exception.getMessage() : "Unknown error"));
        if (exception != null) {
            exception.printStackTrace();
        }
    }
}

