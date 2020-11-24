
package Toolkit;

import java.io.IOException;
import java.io.InputStream;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;


/**
 *
 * @author Laith
 */
public class Toolkit extends Application {

    public Stage stage; //global stage to switch between controllers

    public static String fxmlDoc3="DataLogging.fxml";   
        
    private Initializable replaceSceneContent(String fxml,int width,int height) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        InputStream in = Toolkit.class.getResourceAsStream(fxml);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(Toolkit.class.getResource(fxml));
        AnchorPane page;
        try {
            page = (AnchorPane) loader.load(in);
        } finally {
            in.close();
        } 
        Scene scene = new Scene(page, width, height);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setResizable(false);
        return (Initializable) loader.getController();
    }

     
        public void gotoFXML3(){
        try {
            DataLoggingController fxml3 = (DataLoggingController) replaceSceneContent(fxmlDoc3,900,650);
            fxml3.setApp(this);
        } catch (Exception ex) {
        }
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
         try {
            stage = primaryStage;
            stage.setTitle("GJU-Kuka Datalogger");
            gotoFXML3();
            primaryStage.show();
        } catch (Exception ex) {
             System.out.println(ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        launch(args);
        System.exit(-1);
    }
}