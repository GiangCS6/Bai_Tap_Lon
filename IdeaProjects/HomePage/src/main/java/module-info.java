module org.example.homepage {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;

    opens org.example.homepage to javafx.fxml;
    exports org.example.homepage;
    exports org.example;
    opens org.example to javafx.fxml;
}