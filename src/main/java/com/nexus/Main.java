package com.nexus;

public class Main {

    public static void main(String[] args) {
        // Enable dark mode for Windows title bar (requires Windows 10 1809+)
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");

        // For Windows 11 dark title bar support
        System.setProperty("sun.java2d.uiScale", "1.0");

        // Launch the JavaFX application
        NexusLauncherApp.main(args);
    }
}