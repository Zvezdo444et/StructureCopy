package zvezdo4et.project.controllers;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

public class SettingsController {

    @FXML
    private Button deleteFileBtn;

    @FXML
    private Button deleteExtBtn;

    @FXML
    private Button addFileBtn;

    @FXML
    private TextField fileTextField;

    @FXML
    private TextField extTextField;

    @FXML
    private Button addExtBtn;

    @FXML
    private ListView<String> extListView;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private Button saveBtn;

    private final ObservableList<String> fileData = FXCollections.observableArrayList();
    private final ObservableList<String> extData = FXCollections.observableArrayList();
    private final File configFile = new File("settings/ignore_config.xml");
    private CopyController mainController;

    public void setMainController(CopyController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void addFileHande(ActionEvent event) {
        String text = fileTextField.getText().trim();
        if (!text.isEmpty() && !fileData.contains(text)) {
            fileData.add(text);
            fileTextField.clear();
        }
    }

    @FXML
    void deleteFileHandle(ActionEvent event) {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileData.remove(selected);
        }
    }

    @FXML
    void addExtHandle(ActionEvent event) {
        String text = extTextField.getText().trim();
        if (!text.isEmpty() && !extData.contains(text)) {
            extData.add(text);
            extTextField.clear();
        }
    }

    @FXML
    void deleteExtHandle(ActionEvent event) {
        String selected = extListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            extData.remove(selected);
        }
    }

    @FXML
    void saveHandle(ActionEvent event) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().newDocument();
            Element root = doc.createElement("ignore");
            doc.appendChild(root);

            for (String item : fileData) {
                Element el = doc.createElement("item");
                el.setTextContent(item);
                root.appendChild(el);
            }

            for (String ext : extData) {
                Element el = doc.createElement("ext");
                el.setTextContent(ext);
                root.appendChild(el);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(configFile));
            if (mainController != null) {
                mainController.refreshTree();
            }
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        fileListView.setItems(fileData);
        extListView.setItems(extData);

        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) return;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(configFile);

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                fileData.add(items.item(i).getTextContent());
            }

            NodeList exts = doc.getElementsByTagName("ext");
            for (int i = 0; i < exts.getLength(); i++) {
                extData.add(exts.item(i).getTextContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
