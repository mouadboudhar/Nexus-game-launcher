# Nexus Launcher

A modern game library desktop application built with JavaFX, Hibernate, and SQLite.

## Project Structure

```
NexusLauncher/
├── src/main/
│   ├── java/
│   │   ├── module-info.java
│   │   └── com/nexus/
│   │       ├── client/                    # JavaFX client application
│   │       │   ├── Main.java              # Bootstrap entry point
│   │       │   ├── NexusLauncherApp.java  # Main JavaFX application
│   │       │   ├── component/             # Custom UI components
│   │       │   │   └── GameCard.java
│   │       │   ├── controller/            # View controllers
│   │       │   │   ├── MainController.java
│   │       │   │   ├── LibraryController.java
│   │       │   │   ├── GameDetailsController.java
│   │       │   │   ├── ScanController.java
│   │       │   │   ├── FavoritesController.java
│   │       │   │   ├── SettingsController.java
│   │       │   │   ├── AddGameDialogController.java
│   │       │   │   └── EditGameDialogController.java
│   │       │   ├── service/               # Business logic services
│   │       │   │   ├── CombinedMetadataService.java
│   │       │   │   ├── GameLauncher.java
│   │       │   │   ├── GameService.java
│   │       │   │   ├── MetadataService.java
│   │       │   │   ├── PlaceholderMetadataService.java
│   │       │   │   ├── ScannerService.java
│   │       │   │   └── ScanTask.java
│   │       │   └── util/
│   │       │       └── PlaceholderImageUtil.java
│   │       ├── model/                     # Entity models
│   │       │   ├── Game.java
│   │       │   └── AppSettings.java
│   │       ├── repository/                # Data access layer
│   │       │   ├── GameRepository.java
│   │       │   └── SettingsRepository.java
│   │       └── util/
│   │           └── HibernateUtil.java
│   └── resources/
│       ├── hibernate.cfg.xml              # Hibernate configuration
│       ├── assets/                        # Icons and images
│       └── com/nexus/client/
│           ├── views/                     # FXML layout files
│           └── styles/
│               └── application.css        # Dark theme stylesheet
├── pom.xml                                # Maven build configuration
├── nexus.db                               # SQLite database (auto-created)
└── README.md
```

## Tech Stack

- **Frontend**: JavaFX 21 with FXML
- **Database**: SQLite with Hibernate ORM 6.4
- **Build**: Maven
- **Java**: 21+

## Requirements

- Java 21 or later
- Maven 3.8+

## Building the Project

```bash
# From the project root directory
mvn clean install
```

## Running the Application

### Using Maven
```bash
mvn javafx:run
```

### Using IDE (IntelliJ IDEA)
1. Open the project root folder in IntelliJ IDEA
2. Wait for Maven to import all dependencies
3. Run `Main.java` or `NexusLauncherApp.java`

## Features

### Implemented Views
- **Library View**: Grid display of game cards with search and filter
- **Game Details View**: Full game information with hero banner
- **Scan View**: UI to scan Steam/Epic libraries
- **Favorites View**: Shows favorited games
- **Settings View**: Toggle switches for app preferences
- **Add Game Dialog**: Modal to manually add games
- **Edit Game Dialog**: Modal to edit game details

### Navigation
- Sidebar navigation with Library, Scan, Favorites, and Settings
- Click any game card to view details
- Back button to return to library

### Styling
- Dark theme matching modern game launchers
- Colors: Gray-900 (#111827) background, Indigo-600 (#4f46e5) accents
- Custom scrollbars, buttons, and form elements
- Hover effects and animations on game cards
- Rounded window corners with custom title bar

### Game Detection
- **Steam Library**: Automatically scans and detects all installed Steam games
- **Epic Games Library**: Automatically scans and detects all installed Epic Games
- **System Games (IGDB Validated)**: Scans Windows Uninstall Registry and validates each application against the IGDB database to automatically detect standalone games (Minecraft, League of Legends, GOG games, etc.)
- Manual game addition support

## Game Metadata

The application automatically fetches game metadata (covers, descriptions, developers) from:

### Steam API (No key required)
- Works for all Steam games using their App ID
- Also searches Steam for non-Steam games that might have a Steam page
- Provides cover images, hero images, descriptions, developers, and release dates

### IGDB API (Recommended - requires free Twitch API key)
- Used for games not found on Steam
- **Also used to validate system applications as games** - This is how the app distinguishes actual games from regular software in the Windows Registry
- To enable:
  1. Go to https://dev.twitch.tv/console/apps
  2. Create an application (free)
  3. Copy `nexus.properties.example` to `nexus.properties`
  4. Add your Client ID and Client Secret

**Note:** Without IGDB credentials, system game detection is limited to known game paths (Riot Games, Minecraft). With IGDB credentials, the app can discover any game installed on your system.

### How System Game Detection Works
1. Scans Windows Uninstall Registry for all installed applications
2. Filters out known non-game software (browsers, drivers, tools, etc.)
3. For each remaining application, queries IGDB to check if it's a game
4. If IGDB returns a match with >50% name similarity, the app is classified as a game
5. Game metadata (cover, description, developer) is fetched from IGDB

### Fallback
- Placeholder images via placehold.co API for games without covers
- Displays game title on styled placeholder background

## Database

The application uses SQLite for local storage. The database file (`nexus.db`) is automatically created in the project root on first run.

### Tables
- `games` - Stores game information
- `app_settings` - Stores application preferences

## License

MIT License

