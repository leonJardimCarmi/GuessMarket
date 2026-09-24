package com.guessmarket.ui;

import com.guessmarket.engine.impl.EngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        EngineImpl engine = new EngineImpl();

        URL fxmlLocation = getClass().getResource("/main-view.fxml");
        if (fxmlLocation == null) {
            throw new RuntimeException("Cannot find main-view.fxml in resources!");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setEngine(engine);

        ScrollPane mainScrollPane = new ScrollPane(root);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setFitToHeight(true);

        primaryStage.setTitle("Guess Market");
        primaryStage.setScene(new Scene(mainScrollPane, 1000, 650));

        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(100);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}