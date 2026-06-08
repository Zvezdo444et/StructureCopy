package zvezdo4et.project.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Arrays;

public class SettingsController {

    @FXML private ListView<String> ignoreListView;
    @FXML private TextField inputTextField;
    @FXML private Button addBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;

    @FXML private TreeView<File> ignoreTreeView;
    @FXML private Button chooseRootBtn;
    @FXML private Button addSelectedFromTreeBtn;
    @FXML private Label treeStatusLabel;

    private final ObservableList<String> ignoreData = FXCollections.observableArrayList();
    private final File configFile = new File("settings/ignore_config.xml");
    private CopyController mainController;
    private File treeRoot;

    public void setMainController(CopyController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        ignoreListView.setItems(ignoreData);
        loadConfig();
    }

    static boolean isCompoundExtension(String entry) {
        if (entry == null || entry.isEmpty()) return false;
        if (entry.startsWith(".")) return false;
        if (entry.contains(" ")) return false;
        if (!entry.equals(entry.toLowerCase())) return false;
        return entry.contains(".");
    }

    @FXML
    void addHandle(ActionEvent event) {
        String raw = inputTextField.getText().trim();
        if (raw.isEmpty()) return;
        String entry = raw;
        if (raw.startsWith(".") && raw.indexOf('.', 1) == -1) {
        }
        addUnique(entry);
        inputTextField.clear();
    }

    @FXML
    void deleteHandle(ActionEvent event) {
        String selected = ignoreListView.getSelectionModel().getSelectedItem();
        if (selected != null) ignoreData.remove(selected);
    }

    @FXML
    void saveHandle(ActionEvent event) {
        if (ignoreData.isEmpty()) {
            showWarn("Список игнорирования пуст.");
            return;
        }
        try {
            File settingsDir = configFile.getParentFile();
            if (!settingsDir.exists()) settingsDir.mkdirs();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().newDocument();
            Element root = doc.createElement("ignore");
            doc.appendChild(root);

            for (String entry : ignoreData) {
                String tag = isCompoundExtension(entry) ? "ext" : "item";
                Element el = doc.createElement(tag);
                el.setTextContent(entry);
                root.appendChild(el);
            }

            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            tr.transform(new DOMSource(doc), new StreamResult(configFile));

            if (mainController != null) mainController.refreshTree();
            ((Stage) saveBtn.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Не удалось сохранить:\n" + e.getMessage());
        }
    }

    @FXML
    void chooseRootForTree(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Выберите папку проекта");
        if (mainController != null) {
            File root = mainController.getSelectedRoot();
            if (root != null && root.exists()) dc.setInitialDirectory(root);
        }
        File chosen = dc.showDialog(chooseRootBtn.getScene().getWindow());
        if (chosen != null) {
            treeRoot = chosen;
            buildIgnoreTree(chosen);
            treeStatusLabel.setText("Папка: " + chosen.getAbsolutePath());
        }
    }

    public void tryAutoLoadTree() {
        if (mainController != null) {
            File root = mainController.getSelectedRoot();
            if (root != null && root.exists()) {
                treeRoot = root;
                buildIgnoreTree(root);
                treeStatusLabel.setText("Папка: " + root.getAbsolutePath());
            }
        }
    }

    private void buildIgnoreTree(File folder) {
        CheckBoxTreeItem<File> root = buildTreeItem(folder);
        root.setExpanded(true);
        ignoreTreeView.setRoot(root);
        ignoreTreeView.setCellFactory(tv -> new CheckBoxTreeCell<>() {
            @Override
            public void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                String name = item.getName();
                if (isAlreadyIgnored(item)) {
                    setStyle("-fx-text-fill: #666688; -fx-font-style: italic;");
                    setText(name + "  ✓");
                } else {
                    setStyle("");
                    setText(name);
                }
            }
        });
    }

    private boolean isAlreadyIgnored(File f) {
        String name = f.getName();
        if (ignoreData.contains(name)) return true;
        if (f.isFile()) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                String ext = name.substring(lastDot + 1).toLowerCase();
                if (ignoreData.contains(ext) || ignoreData.contains("." + ext)) return true;
            }
            int firstDot = name.indexOf('.');
            if (firstDot > 0 && firstDot != name.lastIndexOf('.')) {
                String fullExt = name.substring(firstDot + 1).toLowerCase();
                if (ignoreData.contains(fullExt)) return true;
            }
        }
        return false;
    }

    private CheckBoxTreeItem<File> buildTreeItem(File file) {
        CheckBoxTreeItem<File> item = new CheckBoxTreeItem<>(file);
        item.setSelected(false);
        item.setIndependent(false);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File child : children) item.getChildren().add(buildTreeItem(child));
            }
        }
        return item;
    }

    @FXML
    void addSelectedFromTree(ActionEvent event) {
        if (ignoreTreeView.getRoot() == null) {
            showWarn("Сначала выберите папку проекта.");
            return;
        }
        int added = collectCheckedItems((CheckBoxTreeItem<File>) ignoreTreeView.getRoot());
        if (added == 0) {
            showWarn("Не отмечено ни одного элемента.");
        } else {
            if (treeRoot != null) buildIgnoreTree(treeRoot);
        }
    }

    private int collectCheckedItems(CheckBoxTreeItem<File> item) {
        int added = 0;
        if (item.isSelected()) {
            File f = item.getValue();
            String name = f.getName();

            if (f.isDirectory()) {
                added += addUnique(name);
            } else {
                added += addUnique(name);

                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    String ext = name.substring(lastDot + 1).toLowerCase();
                    added += addUnique(ext);
                }
                int firstDot = name.indexOf('.');
                if (firstDot > 0 && firstDot != lastDot(name)) {
                    String fullExt = name.substring(firstDot + 1).toLowerCase();
                    added += addUnique(fullExt);
                }
            }
            item.setSelected(false);
        }
        for (TreeItem<File> child : item.getChildren()) {
            added += collectCheckedItems((CheckBoxTreeItem<File>) child);
        }
        return added;
    }

    private int lastDot(String s) { return s.lastIndexOf('.'); }

    private int addUnique(String value) {
        if (value == null || value.isEmpty()) return 0;
        if (ignoreData.contains(value)) return 0;
        ignoreData.add(value);
        return 1;
    }

    private void loadConfig() {
        if (!configFile.exists()) return;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(configFile);
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                String val = items.item(i).getTextContent().trim();
                if (!val.isEmpty()) addUnique(val);
            }
            NodeList exts = doc.getElementsByTagName("ext");
            for (int i = 0; i < exts.getLength(); i++) {
                String val = exts.item(i).getTextContent().trim().toLowerCase();
                if (!val.isEmpty()) addUnique(val);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Предупреждение"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Ошибка"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}