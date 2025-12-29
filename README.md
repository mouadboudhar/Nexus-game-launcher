# Nexus Launcher

A modern game library desktop application built with JavaFX.

## Project Structure

```
NexusLauncher/
├── client/                    # JavaFX client application
│   └── src/main/
│       ├── java/com/nexus/client/
│       │   ├── NexusLauncherApp.java    # Main entry point
│       │   ├── component/               # Custom UI components
│       │   │   └── GameCard.java
│       │   ├── controller/              # View controllers
│       │   │   ├── MainController.java
│       │   │   ├── LibraryController.java
│       │   │   ├── GameDetailsController.java
│       │   │   ├── ScanController.java
│       │   │   ├── FavoritesController.java
│       │   │   ├── SettingsController.java
│       │   │   └── AddGameDialogController.java
│       │   └── service/
│       │       └── MockDataService.java
│       └── resources/com/nexus/client/
│           ├── views/                   # FXML layout files
│           │   ├── MainView.fxml
│           │   ├── LibraryView.fxml
│           │   ├── GameDetailsView.fxml
│           │   ├── ScanView.fxml
│           │   ├── FavoritesView.fxml
│           │   ├── SettingsView.fxml
│           │   └── AddGameDialog.fxml
│           └── styles/
│               └── application.css      # Dark theme stylesheet
├── server/                    # Backend server (not implemented)
├── shared/                    # Shared models
│   └── src/main/java/com/nexus/shared/model/
│       └── Game.java
└── pom.xml                    # Parent Maven POM
```

## Requirements

- Java 21 or later
- Maven 3.8+
- JavaFX 21

## Building the Project

```bash
# From the project root directory
mvn clean install
```

## Running the Application

### Using Maven
```bash
cd client
mvn javafx:run
```

### Using IDE (IntelliJ IDEA)
1. Open the project root folder in IntelliJ IDEA
2. Wait for Maven to import all dependencies
3. If modules aren't detected, go to File → Reload All from Disk, then reimport Maven
4. Run `NexusLauncherApp.java` from the client module

## Features

### Implemented Views
- **Library View**: Grid display of game cards with search and filter
- **Game Details View**: Full game information with hero banner
- **Scan View**: UI to scan Steam/Epic libraries (mock)
- **Favorites View**: Shows favorited games
- **Settings View**: Toggle switches for app preferences
- **Add Game Dialog**: Modal to manually add games

### Navigation
- Sidebar navigation with Library, Scan, Favorites, and Settings
- Click any game card to view details
- Back button to return to library

### Styling
- Dark theme matching the HTML mockup
- Colors: Gray-900 (#111827) background, Indigo-600 (#4f46e5) accents
- Custom scrollbars, buttons, and form elements
- Hover effects and animations on game cards

## Mock Data

The application comes pre-loaded with 12 sample games for testing:
- Hollow Knight (Steam)
- Fortnite (Epic)
- Minecraft (Manual - Missing status)
- Factorio (Steam)
- Valheim (Steam - Favorited)
- Celeste (Steam - Favorited)
- Hades (Steam)
- The Witcher 3 (Steam - Favorited)
- Elden Ring (Steam)
- Stardew Valley (Steam)
- Rocket League (Epic)
- Fall Guys (Epic)

## Next Steps (Backend Implementation)

The following features need backend implementation:
- [ ] Database integration for game storage
- [ ] File system scanning for Steam/Epic libraries
- [ ] IGDB/Steam API integration for game metadata
- [ ] Actual game launching functionality
- [ ] User preferences persistence
- [ ] System tray integration

## License

MIT License

