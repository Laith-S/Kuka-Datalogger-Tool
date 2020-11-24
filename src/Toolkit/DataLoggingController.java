
package Toolkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import no.hials.crosscom.CrossComClient;
import no.hials.crosscom.KRL.*;
import no.hials.crosscom.KRL.structs.*;

/**
 * FXML Controller class
 * Controller class for the DataLoggingTool
 * @author Laith
 */
public class DataLoggingController implements Initializable {

    @FXML
    private CheckBox includeReadTime;
    @FXML
    private CheckBox addReadTimeCorrections;
    @FXML
    private TextField sampleTimeField;
    @FXML
    private TextField variableInput;
    @FXML
    private TextField setIPText;
    @FXML
    private TextField setPortText;
    @FXML
    private ComboBox variableTypeBox;
    @FXML
    private Button start;
    @FXML
    private Button connectClient;
    @FXML
    private Circle disconnectedCircle;
    @FXML
    private Label disconnected;
    @FXML
    private TableView table;
    @FXML
    private TableColumn id;
    @FXML
    private TableColumn type;
    @FXML
    private TableColumn name;
    @FXML
    private TableColumn value;
    @FXML
    private TableColumn sampleTime;
    @FXML
    private TableColumn readTime;
    @FXML
    private ProgressIndicator prog;
    @FXML
    private Pane controlsPane;
    @FXML
    private Pane clientPane;

    private boolean connectedToClient = false;
    private String clientIP = "localhost";
    private String portAddress = "7000";

    private Toolkit pt;

    private ObservableList<KRLVariable> data;
    private Boolean started = false;
    private TimerTask task;

    private CrossComClient cl = null;
    private List<List<String>> lists = new ArrayList<>();
    private List<File> files = new ArrayList<>();

    public void setApp(Toolkit p) {
        this.pt = p;

    }

    
        
    @FXML
    private void homeButton(ActionEvent e) {
        this.handleStop();
        this.forceDisconnectClient();
//        pt.goToMain();
    }
    
    
    public enum Types {

        KRLBool, KRLInt, KRLReal, KRLE6Axis, KRLE6Pos, KRLFrame, KRLPos;

    }
    
    
    @FXML
    private void handleExit(ActionEvent e){
        
        System.exit(-1);
        
    }
    

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        //Initialize Variable types in combobox
        variableTypeBox.getItems().addAll(Arrays.asList(Types.values()));
        variableTypeBox.getSelectionModel().select(variableTypeBox.getItems().get(0));

        //Set Table columns alignments
        id.setStyle(("-fx-alignment: CENTER;"));
        type.setStyle(("-fx-alignment: CENTER;"));
        name.setStyle(("-fx-alignment: CENTER;"));
        value.setStyle(("-fx-alignment: CENTER;"));
        readTime.setStyle(("-fx-alignment: CENTER;"));
       
        //Set PropertValues
        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        readTime.setCellValueFactory(new PropertyValueFactory<>("readTimeMillis"));
        sampleTime.setCellValueFactory(new PropertyValueFactory<>("samplePeriod"));

        //Define basic system variables
        KRLE6Axis $POS_ACT = new KRLE6Axis("$POS_ACT");
        KRLE6Pos $AXIS_ACT = new KRLE6Pos("$AXIS_ACT");
        KRLInt $OV_JOG = new KRLInt("$OV_JOG");
        KRLInt $OV_PRO = new KRLInt("$OV_PRO");

        //Add variables to table
        data = FXCollections.observableArrayList(
                $POS_ACT,
                $AXIS_ACT,
                $OV_JOG,
                $OV_PRO
        );

        table.setItems(data);

        //Start a timer to refresh table values every 1s
        task = new TimerTask() {
            @Override
            public void run() {
                if (connectedToClient && !started) {
                    for (int i = 0; i < data.size(); i++) {
                        try {
                            cl.readVariable(data.get(i));
                        } catch (IOException ex) {
                            Logger.getLogger(DataLoggingController.class.getName()).log(Level.SEVERE, null, ex);
                            Platform.runLater(() -> {
                                disconnected.setText("Disconnected!");
                                connectClient.setText("Connect");
                            });
                            connectedToClient = false;
                            disconnectedCircle.setFill(Color.RED);
                            disconnectedCircle.setVisible(true);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                table.refresh();
            }
        };

        Timer t = new Timer();
        t.schedule(task, 0, 1000);
    }

    @FXML
    private void handleAddVariableButton(ActionEvent e) throws IOException {

        //Get Type from combobox
        String typeSelected = variableTypeBox.getValue().toString();

        //Default sampleTime=1s
        int sTime = 1000;
        try {
            sTime = Integer.parseInt(sampleTimeField.getText());
        } catch (Exception ex) {
            sampleTimeField.setText("Invalid value");
            start.setDisable(false);
            return;
        }

        //Check type of variable and instantiate it. Add it to that variables list
        if (typeSelected.equalsIgnoreCase(Types.KRLBool.toString())) {
            data.add(new KRLBool(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLInt.toString())) {
            data.add(new KRLInt(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLReal.toString())) {
            data.add(new KRLReal(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLE6Axis.toString())) {
            data.add(new KRLE6Axis(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLE6Pos.toString())) {
            data.add(new KRLE6Pos(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLFrame.toString())) {
            data.add(new KRLFrame(variableInput.getText()));
        } else if (typeSelected.equalsIgnoreCase(Types.KRLPos.toString())) {
            data.add(new KRLPos(variableInput.getText()));
        }
        data.get(data.size() - 1).setSamplePeriod(sTime);

    }

    private void forceDisconnectClient(){
         if(cl!=null){
        try {
                cl.close();
                connectedToClient = false;
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }}
    //    pt.goToMain();
    }
    
    @FXML
    private void handleConnectClent() {

        //Connect to Client
        if (connectedToClient && connectClient.getText().equalsIgnoreCase("Disconnect")) {
            try {
                cl.close();
                disconnected.setText("Disconnected!");
                disconnectedCircle.setFill(Color.RED);
                disconnectedCircle.setVisible(true);
                connectedToClient = false;
                connectClient.setText("Connect");

            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        } else if (connectToClient()) {
            connectClient.setText("Disconnect");
        }

    }

    private boolean connectToClient() {

        //Connect to Client
        String ip = setIPText.getText();
        int port = 7000;
        try {
            port = Integer.parseInt(setPortText.getText());
        } catch (Exception e) {
            setPortText.setText("Invalid!");
            return false;
        }
        try {
            cl = new CrossComClient(ip, port);
            disconnectedCircle.setFill(Color.LIGHTGREEN);
            disconnectedCircle.setVisible(true);
            disconnected.setText("Connected [" + cl.getInetAddress() + ":" + cl.getPort() + "]");
            connectClient.setText("Disconnect");
            connectedToClient = true;
            return true;
        } catch (IOException ex) {
            disconnected.setText("Client Unavailable!");
            disconnectedCircle.setFill(Color.RED);
            disconnectedCircle.setVisible(true);
            connectedToClient = false;
        }
        return false;

    }

    @FXML
    private void handleStart() throws IOException, InterruptedException {

        //Check data size != 0
        if (data.size() == 0) {
            return;
        }      

        //Check connection. If not connected try to connect.
        if (!connectedToClient) {
            if (!connectToClient()) {
                return;
            }
        }

        start.setDisable(true);
        started = true;

        controlsPane.setDisable(true);
        prog.setVisible(true);
        start.setDisable(true);

        //Prepare Files based on data size
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String date = df.format(new Date());

        Long[] nextSample = new Long[data.size()];

        for (int i = 0; i < data.size(); i++) {
            lists.add(new ArrayList<>());

            lists.get(i).add("-------------------------------------------------\n"
                    + date + " Variable: " + data.get(i).getType() + " " + data.get(i).getName()
                    + '\n' + "-------------------------------------------------\n" + '\n');

            nextSample[i] = 0L;
        }

        //Start Sampling
        Long startTime = System.currentTimeMillis();

        clientPane.setDisable(true);
        
        //Sampling Thread
        Executors.newSingleThreadExecutor().execute(() -> {
            while (started) {
                for (int i = 0; i < data.size(); i++) {
                    if (nextSample[i] - System.currentTimeMillis() < 0) {
                        nextSample[i] = data.get(i).getSamplePeriod() + System.currentTimeMillis();
                        try {
                            cl.readVariable(data.get(i));
                            String temp;
                            if (addReadTimeCorrections.isSelected() && !addReadTimeCorrections.isSelected()) {
                                temp = String.format("SampleTime: %d; Value: " + data.get(i).getStringValue() 
                                        + "\n", (System.currentTimeMillis() - startTime + data.get(i).getReadTimeMillis()));
                            } else if (includeReadTime.isSelected() && addReadTimeCorrections.isSelected()) {
                                temp = String.format("SampleTime: %d; Value: " + data.get(i).getStringValue() 
                                        + "; ReadTime: %d \n", 
                                        (System.currentTimeMillis() - startTime + data.get(i).getReadTimeMillis()), data.get(i).getReadTimeMillis());
                            } else {
                                temp = String.format("SampleTime: %d; Value: " + data.get(i).getStringValue() 
                                        + '\n', (System.currentTimeMillis() - startTime));
                            }
                            lists.get(i).add(temp);
                        } catch (IOException ex) {
                            clientPane.setDisable(false);
                            lists.get(i).add(String.format("SampleTime: %d; %s\n", System.currentTimeMillis() - startTime, ex.toString()));
                            started = false;
                            Platform.runLater(() -> {
                                disconnected.setText("Disconnected!");
                                connectClient.setText("Connect");

                            });
                            connectedToClient = false;
                            disconnectedCircle.setFill(Color.RED);
                            disconnectedCircle.setVisible(true);
                            handleStop();
                        } catch (Exception e) {
                            lists.get(i).add(String.format("SampleTime: %d; %s\n", System.currentTimeMillis() - startTime, e.toString()));
                        }
                    }
                }
            }
        });
    }

    @FXML
    private void handleStop() {

        //Stop sampling and save to file
        if (started == true) {
            started = false;
            DirectoryChooser fileChooser = new DirectoryChooser();
            try {
                File fileDir = fileChooser.showDialog(pt.stage);
                if (fileDir != null) {
                    String sDir = fileDir.getPath();
                    for (int i = 0; i < data.size(); i++) {
                        File f = new File(sDir, data.get(i).getId() + "_"
                                + data.get(i).getName() + ".txt");
                        try {
                            FileWriter fw = new FileWriter(f);
                            for (String s : lists.get(i)) {
                                fw.write(s);
                            }
                            fw.flush();
                        } catch (Exception ex) {
                            System.out.println("Failed to write File");
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to get Dir!");
            }
        }

        clientPane.setDisable(false);
        controlsPane.setDisable(false);
        prog.setVisible(false);
        start.setDisable(false);

    }

    @FXML
    private void handleAbout(ActionEvent e) {

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(
                "KUKA DataLoggingTool" 
        );
        alert.setContentText(  
                "\nTHIS SOFTWARE IS "
                + "PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY "
                + "EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES "
                + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE "
                + "COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,"
                + " OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS"
                + " OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,"
                + " STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED"
                + " OF THE POSSIBILITY OF SUCH DAMAGE.\n\n"+
                "Developed By: Laith Salameen <laith.salameen@gmail.com>\n"+"Copyright (c) 2016, German Jordanian University\n" +
                "All rights reserved.\n"
                +"\nDeveloped Using: JOpenShowVar \nCopyright (c) 2014, Aalesund University College\n" +
                "All rights reserved.\n\n"
        );
        alert.show();
    }

    @FXML
    private void handleRemoveIDButton(ActionEvent e) {
        //Remove variable from data
        data.remove(table.getSelectionModel().getSelectedItem());
    }
}