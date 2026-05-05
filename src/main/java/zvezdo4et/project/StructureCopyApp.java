package zvezdo4et.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class StructureCopyApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(StructureCopyApp.class.getResource("/zvezdo4et/project/ui/StructureCopy.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 550);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/zvezdo4et/project/images/icon.png"))));
        stage.setTitle("StructureCopy");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}