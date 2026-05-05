# 🌟 StructureCopy

A modern JavaFX desktop utility for developers to analyze, filter, and export project structures and source code. Easily concatenate selected files into a single document or generate a visual directory tree, with built-in ignore lists and architecture-specific templates.

---

## 🚀 Main Functions

- **Directory Analysis:** Browse and visualize any project folder structure with interactive checkboxes.
- **Content Concatenation:** Select specific files/folders and merge their contents into a single, well-formatted text file (`Result.txt` style).
- **Structure Export:** Generate a clean, hierarchical text representation of your selected directory tree.
- **Architecture Templates:** One-click extraction for common frameworks/patterns (MVVM, MVC, Clean Arch, Unity, Telegram Bots, Minecraft Plugins, etc.).
- **Ignore List Management:** Easily exclude specific files, folders, or extensions (e.g., `.git`, `target`, `node_modules`, `.exe`) via a dedicated settings panel.

---

## 💡 Key Features

- **Smart Filtering:** XML-based configuration for ignored paths and file extensions, automatically applied during tree generation and export.
- **Recent Projects Tracking:** Automatically saves and provides quick access to your last 10 opened directories.
- **Async Processing:** Non-blocking UI with background tasks and a real-time progress bar for large projects.
- **Modern Dark UI:** Sleek, customizable interface with smooth CSS transitions, hover effects, and responsive layouts.
- **Flexible Export Paths:** Choose a custom output directory or fall back to the desktop.

---

## 🛠 Tech Stack

- **Java 23** (Source/Target compatibility)
- **JavaFX 17.0.6** (UI Framework)
- **Maven** (Build & Dependency Management)
- **ControlsFX** (Enhanced UI components)
- **Ikonli & BootstrapFX** (Icons & Styling utilities)
- **XML DOM Parsing** (Configuration management)

---

## 📋 Main Actions

| Action | Description |
| :--- | :--- |
| `Выбрать папку` | Load a project directory into the interactive tree view |
| `Конвертировать` | Merge selected files' contents into a single `.txt` file |
| `Получить структуру` | Export the selected directory hierarchy as a text tree |
| `Выбрать (Menu)` | Run pre-configured templates to extract specific architectural layers |
| `⚙️ Настройки` | Open the configuration panel to manage ignored files/extensions |
| `Открыть последнее` | Quickly reopen a recently accessed project from the history |

---

## 📂 Project Structure

- 📂 `controllers/` - Core application logic (`CopyController`, `SettingsController`, `StructureCopyApp`)
- 📂 `ui/` - FXML layouts and CSS styling files
- 📂 `images/` - UI assets (app icon, settings button)
- 📄 `settings/ignore_config.xml` - Auto-generated configuration for ignored paths & extensions
- 📄 `settings/recent_folders.txt` - Stores the history of recently opened directories
- 📄 `settings/export_path.txt` - Remembers your custom template export directory

---

## ⚙️ Installation & Usage

1. **Clone the repository**
   ```bash
   git clone https://github.com/Zvezdo444et/StructureCopy
   cd StructureCopy
2. **Build the project**
   Ensure you have Maven installed, then run:
    ```bash
    mvn clean package
3. **Run the application**
    Execute the StructureCopyApp main class in your IDE, or use Maven:
   ```bash
   mvn javafx:run
4. **Happy using**

or

1. **Download the installer from Release**
   Run and follow the instructions
2. **Happy using**