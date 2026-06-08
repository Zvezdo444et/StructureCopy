package zvezdo4et.project.controllers;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class CopyController {
    private Set<String> ignoreList = new HashSet<>();
    private Set<String> ignoreExt = new HashSet<>();
    private File selectedRoot;

    private final File recentFilesConfig = new File("settings/recent_folders.txt");
    private final LinkedHashSet<String> recentFolders = new LinkedHashSet<>();
    private File exportDirectory;

    public File getSelectedRoot() {
        return selectedRoot;
    }

    private void showErrorMessage(String text) {
        errorLabel.setText(text);
        errorLabel.setVisible(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> errorLabel.setVisible(false));
        delay.play();
    }

    private File getDesktopPath() {
        String userHome = System.getProperty("user.home");
        File desktop = new File(userHome, "Desktop");
        return desktop.exists() ? desktop : null;
    }

    private void loadExportPath() {
        File file = new File("settings/export_path.txt");
        if (file.exists()) {
            try {
                String path = Files.readString(file.toPath()).trim();
                if (!path.isEmpty()) {
                    File dir = new File(path);
                    if (dir.exists() && dir.isDirectory()) {
                        exportDirectory = dir;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveExportPath(File dir) {
        try {
            Files.writeString(new File("settings/export_path.txt").toPath(), dir.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void chooseExportPath() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Выберите папку для сохранения");
        File choice = dc.showDialog(selectBtn.getScene().getWindow());
        if (choice != null) {
            exportDirectory = choice;
            saveExportPath(choice);
        }
    }

    private File getSaveDirectory() {
        if (exportDirectory != null && exportDirectory.exists()) {
            return exportDirectory;
        }
        String userHome = System.getProperty("user.home");
        File desktop = new File(userHome, "Desktop");
        return desktop.exists() ? desktop : new File(".");
    }

    @FXML
    private Button convertBtn;

    @FXML
    private Label errorLabel;

    @FXML
    private ImageView settingsView;

    @FXML
    private Button structureBtn;

    @FXML
    private Button selectBtn;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TreeView<File> folderTree;

    @FXML
    private Menu templateMenu;

    @FXML
    private Menu lastOpenMenu;

    @FXML
    private void closeApp() {
        javafx.application.Platform.exit();
    }

    private void loadIgnoreList() {
        ignoreList.clear();
        ignoreExt.clear();
        File settingsDir = new File("settings");
        File configFile = new File(settingsDir, "ignore_config.xml");

        if (!settingsDir.exists()) {
            settingsDir.mkdir();
        }

        if (!configFile.exists()) {
            createDefaultIgnoreConfig(configFile);
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");
            for (int i = 0; i < nList.getLength(); i++) {
                String val = nList.item(i).getTextContent().trim();
                if (val.isEmpty()) continue;
                ignoreList.add(val);
                ignoreExt.add(val.toLowerCase());
            }

            NodeList eList = doc.getElementsByTagName("ext");
            for (int i = 0; i < eList.getLength(); i++) {
                String val = eList.item(i).getTextContent().trim().toLowerCase();
                if (!val.isEmpty()) {
                    ignoreList.add(val);
                    ignoreExt.add(val);
                }
            }

            System.out.println("[DEBUG] ignoreList: " + ignoreList);
            System.out.println("[DEBUG] ignoreExt:  " + ignoreExt);
        } catch (Exception e) {
            System.err.println("Ошибка при чтении конфига: " + e.getMessage());
        }
    }

    public void refreshTree() {
        loadIgnoreList();
        if (selectedRoot != null) {
            setupDirectory(selectedRoot);
        }
    }

    private void loadRecentFolders() {
        if (recentFilesConfig.exists()) {
            try {
                List<String> lines = Files.readAllLines(recentFilesConfig.toPath());
                recentFolders.addAll(lines);
                renderRecentMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addToRecent(File folder) {
        String path = folder.getAbsolutePath();
        List<String> list = new ArrayList<>(recentFolders);
        list.remove(path);
        list.add(0, path);
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        recentFolders.clear();
        recentFolders.addAll(list);
        saveRecentToFile();
        renderRecentMenu();
    }

    private void saveRecentToFile() {
        try {
            Files.write(recentFilesConfig.toPath(), recentFolders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderRecentMenu() {
        lastOpenMenu.getItems().clear();

        if (recentFolders.isEmpty()) {
            MenuItem emptyItem = new MenuItem("Пусто");
            emptyItem.setDisable(true);
            lastOpenMenu.getItems().add(emptyItem);
            return;
        }

        for (String path : recentFolders) {
            MenuItem item = new MenuItem(path);
            item.setOnAction(e -> {
                File folder = new File(path);
                if (folder.exists()) {
                    setupDirectory(folder);
                    addToRecent(folder);
                } else {
                    showErrorMessage("Путь не найден: " + path);
                }
            });
            lastOpenMenu.getItems().add(item);
        }

        lastOpenMenu.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("Очистить меню");
        clearItem.setStyle("-fx-text-fill: red;");
        clearItem.setOnAction(e -> {
            recentFolders.clear();
            saveRecentToFile();
            renderRecentMenu();
        });
        lastOpenMenu.getItems().add(clearItem);
    }

    private void createDefaultIgnoreConfig(File file) {
        String defaultConfig =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ignore>\n" +
                        "    <item>.vs</item>\n" +
                        "    <item>vscode</item>\n" +
                        "    <item>git</item>\n" +
                        "    <item>.git</item>\n" +
                        "    <item>.gitignore</item>\n" +
                        "    <item>.gitattributes</item>\n" +
                        "    <item>.idea</item>\n" +
                        "    <item>.mvn</item>\n" +
                        "    <item>mvnw</item>\n" +
                        "    <item>mvnw.cmd</item>\n" +
                        "    <item>target</item>\n" +
                        "    <item>bin</item>\n" +
                        "    <item>Debug</item>\n" +
                        "    <item>obj</item>\n" +
                        "    <item>README.md</item>\n" +
                        "    <item>.gradle</item>\n" +
                        "    <item>run</item>\n" +
                        "    <item>build</item>\n" +
                        "    <item>exe</item>\n" +
                        "</ignore>";
        try {
            Files.writeString(file.toPath(), defaultConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onDirectoryChooser(ActionEvent event) {
        loadIgnoreList();
        DirectoryChooser dc = new DirectoryChooser();
        File choice = dc.showDialog(selectBtn.getScene().getWindow());
        if (choice != null) {
            setupDirectory(choice);
            addToRecent(choice);
        }
    }

    private void setupDirectory(File folder) {
        selectedRoot = folder;
        loadIgnoreList();

        CheckBoxTreeItem<File> rootItem = createTreeItem(folder, false);
        rootItem.setExpanded(true);
        folderTree.setRoot(rootItem);

        folderTree.setCellFactory(tv -> new CheckBoxTreeCell<>() {
            @Override
            public void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.getName());
                CheckBoxTreeItem<File> treeItem = (CheckBoxTreeItem<File>) getTreeItem();
                if (treeItem != null && treeItem.isIndependent()) {
                    setStyle("-fx-text-fill: #666688; -fx-font-style: italic;");
                } else {
                    setStyle("");
                }
            }
        });

        if (templateMenu != null) {
            templateMenu.setDisable(false);
        }
    }

    private CheckBoxTreeItem<File> createTreeItem(File file, boolean ignored) {
        CheckBoxTreeItem<File> item = new CheckBoxTreeItem<>(file) {
            @Override
            public String toString() {
                return getValue().getName();
            }
        };

        item.setSelected(false);
        item.setIndependent(ignored);

        if (ignored) {
            item.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) item.setSelected(false);
            });
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : files) {
                    boolean childIgnored = ignored || shouldIgnore(child);
                    item.getChildren().add(createTreeItem(child, childIgnored));
                }
            }
        }
        return item;
    }

    private boolean shouldIgnore(File file) {
        String name = file.getName();
        if (ignoreList.contains(name)) return true;
        if (file.isFile()) {
            if (ignoreExt.contains(getLastExtension(name).toLowerCase())) return true;
            if (ignoreExt.contains(getFullExtension(name).toLowerCase())) return true;
        }
        return false;
    }

    private String getLastExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    private String getFullExtension(String name) {
        int firstDot = name.indexOf('.');
        if (firstDot > 0 && firstDot < name.length() - 1) {
            return name.substring(firstDot + 1);
        }
        return "";
    }

    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onConvert(ActionEvent event) {
        errorLabel.setVisible(false);

        if (folderTree.getRoot() == null) {
            showErrorMessage("Вы не выбрали папку проекта!");
            return;
        }

        List<File> selectedFiles = new ArrayList<>();
        collectSelectedFiles((CheckBoxTreeItem<File>) folderTree.getRoot(), selectedFiles);

        if (selectedFiles.isEmpty()) {
            showErrorMessage("Вы не выбрали ни одного файла (галочки)!");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить результат");
        fc.setInitialDirectory(getDesktopPath());
        fc.setInitialFileName("Code.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt"));
        File saveFile = fc.showSaveDialog(convertBtn.getScene().getWindow());

        if (saveFile == null) return;

        progressBar.setVisible(true);
        convertBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                StringBuilder allContent = new StringBuilder();
                int total = selectedFiles.size();

                for (int i = 0; i < total; i++) {
                    File f = selectedFiles.get(i);
                    if (f.isFile()) {
                        allContent.append("=== ").append(f.getName()).append(" ===\n");
                        try {
                            allContent.append(Files.readString(f.toPath())).append("\n\n");
                        } catch (Exception ex) {
                            allContent.append("[Ошибка чтения файла]\n\n");
                        }
                    }
                    updateProgress(i + 1, total);
                }
                Files.writeString(saveFile.toPath(), allContent.toString());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            convertBtn.setDisable(false);
            openFile(saveFile);
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            convertBtn.setDisable(false);
            showErrorMessage("Ошибка при конвертации!");
            task.getException().printStackTrace();
        });

        progressBar.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }

    private void collectSelectedFiles(CheckBoxTreeItem<File> item, List<File> list) {
        if (shouldIgnore(item.getValue())) return;
        if (item.isSelected() || item.isIndeterminate()) {
            if (item.getValue().isFile() && item.isSelected()) {
                list.add(item.getValue());
            }
            for (TreeItem<File> child : item.getChildren()) {
                collectSelectedFiles((CheckBoxTreeItem<File>) child, list);
            }
        }
    }

    @FXML
    void onExportStructure(ActionEvent event) {
        if (folderTree.getRoot() == null) {
            showErrorMessage("Вы не выбрали папку проекта!");
            return;
        }

        List<File> selectedFiles = new ArrayList<>();
        collectSelectedFiles((CheckBoxTreeItem<File>) folderTree.getRoot(), selectedFiles);

        if (selectedFiles.isEmpty()) {
            showErrorMessage("Вы не выбрали ни одного файла (галочки)!");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить результат");
        fc.setInitialDirectory(getDesktopPath());
        fc.setInitialFileName("project_layout.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы (*.txt)", "*.txt"));
        File saveFile = fc.showSaveDialog(convertBtn.getScene().getWindow());

        if (saveFile == null) return;

        StringBuilder structure = new StringBuilder();
        generateStructureText((CheckBoxTreeItem<File>) folderTree.getRoot(), 0, structure);

        try {
            Files.writeString(saveFile.toPath(), structure.toString());
            openFile(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateStructureText(CheckBoxTreeItem<File> item, int depth, StringBuilder sb) {
        if (!item.isSelected() && !item.isIndeterminate()) return;

        sb.append("  ".repeat(depth));
        String prefix = item.getValue().isDirectory() ? "[DIR] " : "|-- ";
        sb.append(prefix).append(item.getValue().getName()).append("\n");

        for (TreeItem<File> child : item.getChildren()) {
            generateStructureText((CheckBoxTreeItem<File>) child, depth + 1, sb);
        }
    }

    @FXML
    private void handleImageClick(javafx.scene.input.MouseEvent event) {
        openSettings();
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/zvezdo4et/project/ui/Settings.fxml"));
            Parent root = loader.load();

            SettingsController settingsController = loader.getController();
            settingsController.setMainController(this);
            settingsController.tryAutoLoadTree();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Настройка конфига");
            settingsStage.initOwner(settingsView.getScene().getWindow());
            settingsStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            var iconStream = getClass().getResourceAsStream("/zvezdo4et/project/images/icon.png");
            if (iconStream != null) {
                settingsStage.getIcons().add(new Image(iconStream));
            }

            settingsStage.setScene(new Scene(root));
            settingsStage.setResizable(false);
            settingsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Не удалось открыть настройки: " + e.getMessage());
        }
    }

    private void runTemplate(List<String> targetFolders) {
        if (selectedRoot == null) return;

        progressBar.setVisible(true);
        convertBtn.setDisable(true);

        Task<Void> templateTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (String folderName : targetFolders) {
                    File targetDir = findFolder(selectedRoot, folderName);
                    if (targetDir != null && targetDir.isDirectory()) {
                        List<File> filesInFolder = new ArrayList<>();
                        collectFilesRecursive(targetDir, filesInFolder);

                        if (!filesInFolder.isEmpty()) {
                            StringBuilder content = new StringBuilder();
                            for (File f : filesInFolder) {
                                content.append("=== ").append(f.getName()).append(" ===\n");
                                try {
                                    content.append(Files.readString(f.toPath())).append("\n\n");
                                } catch (IOException e) {
                                    content.append("[Ошибка чтения файла]\n\n");
                                }
                            }

                            File saveDir = getSaveDirectory();
                            File outputFile = new File(saveDir, folderName + "_Code.txt");
                            Files.writeString(outputFile.toPath(), content.toString());
                            openFileInEditor(outputFile);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressBar.setVisible(false);
                convertBtn.setDisable(false);
                progressBar.setProgress(0);
            }

            @Override
            protected void failed() {
                progressBar.setVisible(false);
                convertBtn.setDisable(false);
                showErrorMessage("Ошибка при выполнении шаблона");
            }
        };

        new Thread(templateTask).start();
    }

    private File findFolder(File root, String name) {
        if (root.getName().equalsIgnoreCase(name)) return root;
        File[] children = root.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File found = findFolder(child, name);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void collectFilesRecursive(File folder, List<File> result) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (ignoreList.contains(name)) continue;
            if (f.isFile()) {
                String lastExt = getLastExtension(name);
                String fullExt = getFullExtension(name);
                if (ignoreExt.contains(lastExt) || ignoreExt.contains(fullExt)) continue;
            }
            if (f.isFile()) {
                result.add(f);
            } else if (f.isDirectory()) {
                collectFilesRecursive(f, result);
            }
        }
    }

    private void openFileInEditor(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @FXML
    void handleMVVM(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Model", "Models", "View", "Views", "ViewModel", "ViewModels", "Services", "Data", "Repositories"));
    }

    @FXML
    void handleMVC(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Model", "Models", "View", "Views", "Controller", "Controllers", "Service", "Services", "Data"));
    }

    @FXML
    void handleMVP(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Models", "Model", "Views", "View", "Presenters", "Presenter"));
    }

    @FXML
    void handleCleanArch(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Domain", "Application", "Infrastructure", "Presentation", "WebUI"));
    }

    @FXML
    void handleWebAPI(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Controllers", "Controller", "DTO", "DTOs", "Entity", "Entities", "Data", "Configuration", "Middleware"));
    }

    @FXML
    void handleUnity(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Scripts", "Prefabs", "Scenes", "Materials", "Editor", "Resources"));
    }

    @FXML
    void handleTG(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Config", "Configs", "Model", "Models", "Service", "Services"));
    }

    @FXML
    void handlePlugin(ActionEvent event) {
        runTemplate(List.of("Exception", "Exceptions", "Commands", "Command", "Items", "Item", "Listeners", "Listener", "Managers", "Manager", "ui"));
    }

    @FXML
    void handleANY(ActionEvent event) {
        runTemplate(List.of(
                "Aggregates", "Animations", "Animation",
                "Application", "Assets", "Audio", "Auth",
                "client", "Command", "Commands",
                "Config", "Configs",
                "Configuration", "Constants",
                "Controller", "Controllers",
                "Data", "Docs", "Domain",
                "DTO", "DTOs", "Editor",
                "Entities", "Entity", "Enum", "Enums",
                "Event", "Events", "Exception", "Exceptions",
                "Filters", "Gizmos", "Handler", "Handlers", "Helper", "Helpers",
                "Infrastructure", "init", "Item", "Items",
                "Listener", "Listeners",
                "Manager", "Managers", "Mappings", "Materials",
                "Middleware", "Mocks",
                "Model", "Models", "Persistence",
                "Prefabs", "Presentation",
                "Presenter", "Presenters", "Profiles",
                "Queries", "Query",
                "Repositories", "Resources", "Scenes", "Scripts",
                "Service", "Services", "Shader", "Shaders", "Shared", "Sounds",
                "Strategy", "StreamingAssets",
                "Tests", "Texture", "Textures",
                "ui", "UseCase", "UseCases", "util",
                "Validator", "Validators", "ValueObjects",
                "View", "Views",
                "ViewModel", "ViewModels",
                "WebUI"
        ));
    }

    @FXML
    public void initialize() {
        loadExportPath();
        loadRecentFolders();
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/zvezdo4et/project/images/settings.png")));
        settingsView.setImage(image);
    }
}