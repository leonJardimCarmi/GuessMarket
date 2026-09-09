package com.guessmarket.ui;

import com.guessmarket.engine.impl.EngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. יצירת מופע של המנוע
        EngineImpl engine = new EngineImpl();

        // 2. טעינת קובץ ה-FXML מתיקיית ה-resources
        URL fxmlLocation = getClass().getResource("/main-view.fxml");
        if (fxmlLocation == null) {
            throw new RuntimeException("Cannot find main-view.fxml in resources!");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // 3. העברת מופע ה-Engine ל-MainController
        MainController controller = loader.getController();
        controller.setEngine(engine);

        // 4. הגדרת ה-Scene וה-Stage (החלון הראשי)
        primaryStage.setTitle("Guess Market");
        primaryStage.setScene(new Scene(root, 1000, 650));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}