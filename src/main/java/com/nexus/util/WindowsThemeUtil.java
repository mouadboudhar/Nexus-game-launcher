package com.nexus.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import javafx.stage.Stage;

/**
 * Utility class to enable Windows dark title bar for JavaFX applications.
 * Uses JNA to call the Windows DWM (Desktop Window Manager) API.
 */
public class WindowsThemeUtil {

    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;

    /**
     * DWM API interface
     */
    public interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);
    }

    /**
     * Enables dark mode for the window title bar.
     * Should be called after stage.show() is called.
     *
     * @param stage The JavaFX stage to apply dark mode to
     */
    public static void enableDarkTitleBar(Stage stage) {
        if (!isWindows()) {
            return;
        }

        try {
            // Find window by title
            WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());

            if (hwnd == null) {
                // Try with a slight delay and enumerate windows
                Thread.sleep(100);
                hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            }

            if (hwnd == null) {
                System.err.println("[WindowsThemeUtil] Could not find window handle for: " + stage.getTitle());
                return;
            }

            // Set dark mode attribute
            IntByReference darkMode = new IntByReference(1);

            // Try Windows 10 20H1+ attribute first
            int result = Dwmapi.INSTANCE.DwmSetWindowAttribute(
                    hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE,
                    darkMode,
                    4
            );

            // If that fails, try older attribute for Windows 10 1809-1909
            if (result != 0) {
                Dwmapi.INSTANCE.DwmSetWindowAttribute(
                        hwnd,
                        DWMWA_USE_IMMERSIVE_DARK_MODE_OLD,
                        darkMode,
                        4
                );
            }

            // Force window to repaint title bar
            User32.INSTANCE.SetWindowPos(
                    hwnd, null, 0, 0, 0, 0,
                    WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_FRAMECHANGED
            );

            System.out.println("[WindowsThemeUtil] Dark title bar enabled successfully");

        } catch (Exception e) {
            System.err.println("[WindowsThemeUtil] Failed to enable dark title bar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if the current OS is Windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}

