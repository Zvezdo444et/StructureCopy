module zvezdo4et.project {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens zvezdo4et.project to javafx.fxml;
    exports zvezdo4et.project;
    exports zvezdo4et.project.controllers;
    opens zvezdo4et.project.controllers to javafx.fxml;
}