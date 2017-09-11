package guiproject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.ToggleSwitch;
import server.ClientHandler;
import server.Server;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Controller implements Initializable {

    private static Server server;

    @FXML
    private ListView<Scenario> allScenariosListView;

    @FXML
    private ListView<Action> allActionsListView;

    @FXML
    private ListView<Action> chosenActionsListView;

    @FXML
    private ComboBox<Receiver> receiversComboBox;

    @FXML
    private TextField ageTextField;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField lastNameTextField;

    @FXML
    private ComboBox<String> teachersComboBox;

    @FXML
    private CheckBox closeSystemCheckBox;

    @FXML
    private TextField scenarioNameTextField;

    @FXML
    private TextField actionNameTextField;

    @FXML
    private TextArea actionDescriptionTextArea;

    @FXML
    private TextArea scenarioDescriptionTextArea;

    @FXML
    private ToggleSwitch blockPeripheralsSwitch;

    @FXML
    private CheckComboBox<Receiver> receiversToBlockMultiComboBox;

    private final ObservableList<Action> allActions
            = FXCollections.observableArrayList();

    private final ObservableList<Scenario> allScenarios
            = FXCollections.observableArrayList();

    private final ObservableList<String> allTeachers
            = FXCollections.observableArrayList();

    private final ObservableList<Action> chosenActions
            = FXCollections.observableArrayList();

    private final ObservableList<Receiver> allReceivers
            = FXCollections.observableArrayList();

    private static final String fileNameOfReceivers = "./Config/receivers.ini";
    private static final String fileNameOfTeachers = "./Config/teachers.ini";

    private static final String actionsPath = "Actions/";
    private static final String scenariosPath = "Scenarios/";

    private final Map<Action, File> availableActions = new HashMap();
    private final Map<Scenario, File> availableScenarios = new HashMap();

    public void shutdown() throws InterruptedException, IOException {
        server.running = false;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        server = new Server();
        server.start();

        try {
            //load Receivers and Teachers from .ini file and add them to receivers comboBox
            loadConfigFiles();
            //load Actions from XML files
            loadActionXMLs(actionsPath, availableActions);
            allActions.addAll(availableActions.keySet());
            allActionsListView.setItems(allActions);
            //load Scenarios from XML file
            loadScenarioXMLs(scenariosPath, availableScenarios);
            allScenarios.addAll(availableScenarios.keySet());
            allScenariosListView.setItems(allScenarios);
        } catch (IOException e) {
            e.printStackTrace();
        }

        chosenActionsListView.setItems(chosenActions);
        receiversToBlockMultiComboBox.getItems().addAll(allReceivers);
        receiversToBlockMultiComboBox.setDisable(true);

        //enable/disable possibility to block keyboard/mouse input on several receivers
        blockPeripheralsSwitch.selectedProperty().addListener(new ChangeListener< Boolean >() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue)
                    receiversToBlockMultiComboBox.setDisable(false);
                else
                    receiversToBlockMultiComboBox.setDisable(true);
            }
        });
        
        //teachers ComboBox
        teachersComboBox.setItems(allTeachers);
        teachersComboBox.getSelectionModel().selectFirst();
        //name Text Field
        validateTextField(nameTextField,"textOnly");
        //last name Text Field
        validateTextField(lastNameTextField, "textOnly");
        //age Text Field
        validateTextField(ageTextField, "numberOnly");
        //scenario Name Text Field
        validateTextField(scenarioNameTextField, "filename");
        //action Name Text Field
        validateTextField(actionNameTextField, "filename");
    }


    void validateTextField(TextField field, String validationType)
    {
        if (validationType.equals("textOnly"))
        {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\sa-zA-Z*")) {
                    field.setText(newValue.replaceAll("[^\\sa-zA-Z]", ""));
                }
            });
        }
        else if (validationType.equals("numberOnly"))
        {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    field.setText(newValue.replaceAll("[^\\d]", ""));
                } else if (newValue.length() > 3) {
                    String tmp = newValue.substring(0, 3);
                    field.setText(tmp);
                } else if (!newValue.matches("")) {
                    if (Integer.valueOf(newValue) > 199) {
                        String tmp = newValue.substring(0, 2);
                        field.setText(tmp);
                    }
                }
            });
        }
        else if (validationType.equals("filename"))
        {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("[-_. A-Za-z0-9]")) {
                    field.setText(newValue.replaceAll("[^-_. A-Za-z0-9]", ""));
                }
            });
        }
    }

    private void loadActionXMLs(String path, Map<Action, File> map) throws IOException {
        List<File> actionsFiles = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        for (File file : actionsFiles) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Action.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                Action action = (Action) jaxbUnmarshaller.unmarshal(file);
                map.putIfAbsent(action, file);

            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadScenarioXMLs(String path, Map<Scenario, File> map) throws IOException {
        List<File> scenarioFiles = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        for (File file : scenarioFiles) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Scenario.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                Scenario scenario = (Scenario) jaxbUnmarshaller.unmarshal(file);
                map.putIfAbsent(scenario, file);

            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadConfigFiles() throws IOException {
        //set Receivers
        try {
            Stream<String> lines = Files.lines(Paths.get(fileNameOfReceivers));
            lines.filter(line -> line.contains(" ")).forEach(
                    line -> allReceivers.add(new Receiver(line.split(" ")[1], line.split(" ")[0])));
            receiversComboBox.setItems(allReceivers);
            receiversComboBox.getSelectionModel().selectFirst();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //set Teachers
        try {
            Stream<String> stream = Files.lines(Paths.get(fileNameOfTeachers));
            stream.forEach(line -> allTeachers.add(line));
            teachersComboBox.setItems(allTeachers);
            teachersComboBox.getSelectionModel().selectFirst();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void moveActionToRight(ActionEvent event) {
        if (allActionsListView.getSelectionModel().getSelectedItem() != null) {
            chosenActions.add(allActionsListView.getSelectionModel().getSelectedItem());
            allActions.remove(allActionsListView.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    void moveActionToLeft(ActionEvent event) {
        if (chosenActionsListView.getSelectionModel().getSelectedItem() != null) {
            allActions.add(chosenActionsListView.getSelectionModel().getSelectedItem());
            chosenActions.remove(chosenActionsListView.getSelectionModel().getSelectedItem());
        }
    }

    @FXML
    void moveActionToUp(ActionEvent event) {
        if (chosenActionsListView.getSelectionModel().getSelectedItem() != null) {
            int index = chosenActions.indexOf(chosenActionsListView.getSelectionModel().getSelectedItem());
            if (index > 0) {
                Collections.swap(chosenActions, index, index - 1);
                chosenActionsListView.getSelectionModel().select(index - 1);
            }
        }
    }

    @FXML
    void moveActionToDown(ActionEvent event) {
        if (chosenActionsListView.getSelectionModel().getSelectedItem() != null) {
            int index = chosenActions.indexOf(chosenActionsListView.getSelectionModel().getSelectedItem());
            if (index < chosenActions.size() - 1) {
                Collections.swap(chosenActions, index, index + 1);
                chosenActionsListView.getSelectionModel().select(index + 1);
            }
        }
    }

    @FXML
    void saveScenario(ActionEvent event) throws IOException {
        Scenario scenario = new Scenario();
        scenario.setName(scenarioNameTextField.getText());
        scenario.setDescription(scenarioDescriptionTextArea.getText());
        scenario.setChoosenActions(chosenActionsListView.getItems());
        String filename = scenarioNameTextField.getText();
        try {
            File file = new File("./" + scenariosPath + filename + ".xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(Scenario.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(scenario, file);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        loadScenarioXMLs(scenariosPath, availableScenarios);
        allScenarios.add(scenario);
        scenarioNameTextField.clear();
        scenarioDescriptionTextArea.clear();
        chosenActionsListView.getItems().clear();
    }

    @FXML
    void saveAction(ActionEvent event) throws IOException {
        Action action = new Action();
        action.setName(actionNameTextField.getText());
        action.setDescription(actionDescriptionTextArea.getText());
        action.setReceiver(receiversComboBox.getSelectionModel().getSelectedItem());
        action.setNodes(server.client.getRecordedClicks());

        String filename = actionNameTextField.getText();
        try {

            File file = new File("./" + actionsPath + filename + ".xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(Action.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(action, file);

        } catch (JAXBException e) {
            e.printStackTrace();
        }

        loadActionXMLs(actionsPath, availableActions);
        allActions.add(action);
        actionNameTextField.clear();
        actionDescriptionTextArea.clear();
        receiversComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    void runStudy(ActionEvent event) {
        Study study = new Study();
        study.setName(nameTextField.getText());
        study.setLastName(lastNameTextField.getText());
        study.setAge(ageTextField.getText());
        study.setCloseSystem(closeSystemCheckBox.isSelected());
        study.setTeacher(teachersComboBox.getSelectionModel().getSelectedItem());
        study.setChosenScenario(allScenariosListView.getSelectionModel().getSelectedItem());
        study.setBlockedPeripheralsOnReceivers(receiversToBlockMultiComboBox.getCheckModel().getCheckedItems());

        //TODO
        //save study?
        //run Study

        nameTextField.clear();
        lastNameTextField.clear();
        ageTextField.clear();
        closeSystemCheckBox.selectedProperty().setValue(false);
        teachersComboBox.getSelectionModel().selectFirst();
        allScenariosListView.getSelectionModel().clearSelection();
        receiversToBlockMultiComboBox.getCheckModel().clearChecks();
    }

    @FXML
    void startRecording(ActionEvent event) throws IOException {
        PrintWriter out = new PrintWriter(server.client.getSocket().getOutputStream(), true);
        out.println("record");
    }

    @FXML
    void stopRecording(ActionEvent event) throws IOException {
        PrintWriter out = new PrintWriter(server.client.getSocket().getOutputStream(), true);
        out.println("stoprecord");
    }
}
