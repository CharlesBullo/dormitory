package com.example.dormitory;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddTenantController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField contactField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    @FXML
    private ComboBox<String> roomComboBox;

    @FXML
    private Label roomRentLabel;

    @FXML
    private Label roomStatusLabel;

    @FXML
    private DatePicker moveInDatePicker;

    @FXML
    private Label dueDateLabel;

    @FXML
    private Label availableRoomsLabel;

    @FXML
    private StackPane contentPane;

    private DataManager dataManager;
    private ObservableList<Room> vacantRooms;

    @FXML
    public void initialize() {
        dataManager = DataManager.getInstance();

        // Set up date picker
        moveInDatePicker.setValue(LocalDate.now());
        updateDueDate();

        // Add listener to date picker
        moveInDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            updateDueDate();
        });

        // Add listener to room selection
        roomComboBox.valueProperty().addListener((obs, oldRoom, newRoom) -> {
            if (newRoom != null) {
                updateRoomDetails(newRoom);
            }
        });

        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        vacantRooms = dataManager.getVacantRooms();
        roomComboBox.getItems().clear();

        for (Room room : vacantRooms) {
            roomComboBox.getItems().add(room.getRoomNumber() + " - ₱" + String.format("%.2f", room.getMonthlyRent()));
        }

        availableRoomsLabel.setText("Available Rooms: " + vacantRooms.size());

        if (vacantRooms.isEmpty()) {
            roomComboBox.setDisable(true);
            roomRentLabel.setText("No available rooms!");
            roomStatusLabel.setText("Please add rooms first");
        } else {
            roomComboBox.setDisable(false);
        }
    }

    private void updateRoomDetails(String roomSelection) {
        if (roomSelection != null && !roomSelection.isEmpty()) {
            String roomNumber = roomSelection.split(" - ")[0];
            Room selectedRoom = dataManager.findRoomByNumber(roomNumber);

            if (selectedRoom != null) {
                roomRentLabel.setText("₱" + String.format("%.2f", selectedRoom.getMonthlyRent()));
                roomStatusLabel.setText(selectedRoom.getStatus());

                if (selectedRoom.getStatus().equals("VACANT")) {
                    roomStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else if (selectedRoom.getStatus().equals("OCCUPIED")) {
                    roomStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    roomStatusLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                }
            }
        }
    }

    private void updateDueDate() {
        LocalDate moveInDate = moveInDatePicker.getValue();
        if (moveInDate != null) {
            LocalDate dueDate = moveInDate.plusMonths(1);
            dueDateLabel.setText(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    @FXML
    private void handleSave() {
        // Validate inputs
        if (nameField.getText().isEmpty()) {
            showAlert("Error", "Please enter tenant name.");
            return;
        }

        if (contactField.getText().isEmpty()) {
            showAlert("Error", "Please enter contact number.");
            return;
        }

        if (emailField.getText().isEmpty()) {
            showAlert("Error", "Please enter email address.");
            return;
        }

        if (roomComboBox.getValue() == null) {
            showAlert("Error", "Please select a room.");
            return;
        }

        if (moveInDatePicker.getValue() == null) {
            showAlert("Error", "Please select move-in date.");
            return;
        }

        // Extract room details
        String roomSelection = roomComboBox.getValue();
        String roomNumber = roomSelection.split(" - ")[0];
        Room selectedRoom = dataManager.findRoomByNumber(roomNumber);

        if (selectedRoom == null) {
            showAlert("Error", "Selected room not found.");
            return;
        }

        // Check if room is still vacant
        if (!selectedRoom.getStatus().equals("VACANT")) {
            showAlert("Error", "This room is no longer available. Please select another room.");
            loadAvailableRooms();
            return;
        }

        // Create tenant ID
        String tenantId = "T" + System.currentTimeMillis();

        // Create new tenant (this generates username and temp password "123456")
        Tenant newTenant = new Tenant(
                tenantId,
                nameField.getText(),
                contactField.getText(),
                emailField.getText(),
                selectedRoom.getRoomNumber(),
                selectedRoom.getRoomId(),
                selectedRoom.getMonthlyRent(),
                moveInDatePicker.getValue()
        );

        // Add address if provided
        if (addressField.getText() != null && !addressField.getText().isEmpty()) {
            newTenant.setAddress(addressField.getText());
        }

        // Update room to OCCUPIED and assign tenant
        selectedRoom.setStatus("OCCUPIED");
        selectedRoom.setTenantName(newTenant.getTenantName());
        selectedRoom.setTenantId(newTenant.getTenantId());

        // Add tenant to data manager (this will also create user account)
        dataManager.addTenant(newTenant);

        // Create user account for the tenant
        createUserAccount(newTenant);

        // Update room in data manager
        dataManager.updateRoom(selectedRoom);

        // Show success message with account credentials (with peek functionality)
        showRegistrationSuccess(newTenant);

        // Clear form
        clearForm();

        // Refresh available rooms
        loadAvailableRooms();
    }

    private void createUserAccount(Tenant tenant) {
        // Hash the temporary password "123456"
        String hashedPassword = BCrypt.hashpw(tenant.getTemporaryPassword(), BCrypt.gensalt(10));

        // Insert user account into database
        String sql = "INSERT INTO users (username, password, full_name, email, role, tenant_id, is_active) " +
                "VALUES (?, ?, ?, ?, 'TENANT', ?, TRUE)";

        try (java.sql.Connection conn = DatabaseConfig.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getUsername());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, tenant.getTenantName());
            pstmt.setString(4, tenant.getEmail());
            pstmt.setString(5, tenant.getTenantId());
            pstmt.executeUpdate();

            System.out.println("✓ User account created for tenant: " + tenant.getUsername() + " (Password: 123456)");

        } catch (java.sql.SQLException e) {
            System.err.println("Error creating user account: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showRegistrationSuccess(Tenant tenant) {
        // Create a custom dialog with password peek functionality
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Registration Successful");
        dialog.setHeaderText("Tenant Registered Successfully!");

        // Create content
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: #ecf0f1;");

        // Tenant Information Section
        Label tenantInfoLabel = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        tenantInfoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label tenantInfoTitle = new Label("         TENANT INFORMATION");
        tenantInfoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label tenantDetails = new Label(String.format(
                "Tenant ID: %s\n" +
                        "Name: %s\n" +
                        "Room: %s\n" +
                        "Monthly Rent: ₱%,.2f\n" +
                        "Move-in Date: %s\n" +
                        "First Due Date: %s",
                tenant.getTenantId(),
                tenant.getTenantName(),
                tenant.getRoomNumber(),
                tenant.getMonthlyRent(),
                tenant.getMoveInDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                tenant.getDueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
        ));
        tenantDetails.setStyle("-fx-font-family: monospace;");

        // Login Credentials Section
        Label credLabel = new Label("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        credLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label credTitle = new Label("         LOGIN CREDENTIALS");
        credTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label usernameLabel = new Label("Username: " + tenant.getUsername());
        usernameLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Password field with peek functionality
        HBox passwordBox = new HBox(10);
        PasswordField passwordField = new PasswordField();
        passwordField.setText(tenant.getTemporaryPassword());
        passwordField.setEditable(false);
        passwordField.setPrefWidth(200);

        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setText(tenant.getTemporaryPassword());
        visiblePasswordField.setEditable(false);
        visiblePasswordField.setPrefWidth(200);
        visiblePasswordField.setVisible(false);

        CheckBox showPasswordCheck = new CheckBox("Show Password");
        showPasswordCheck.setOnAction(e -> {
            if (showPasswordCheck.isSelected()) {
                visiblePasswordField.setVisible(true);
                passwordField.setVisible(false);
            } else {
                visiblePasswordField.setVisible(false);
                passwordField.setVisible(true);
            }
        });

        passwordBox.getChildren().addAll(passwordField, visiblePasswordField, showPasswordCheck);

        Label passwordLabel = new Label("Temporary Password: ");
        passwordLabel.setStyle("-fx-font-weight: bold;");

        VBox credBox = new VBox(5);
        credBox.getChildren().addAll(usernameLabel, passwordLabel, passwordBox);

        // Warning Section
        Label warningLabel = new Label("⚠ IMPORTANT:");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Label warningText = new Label(
                "• The tenant can use these credentials to login\n" +
                        "• Default temporary password is: 123456\n" +
                        "• They will be prompted to change password on first login\n" +
                        "• Save these credentials and provide them to the tenant"
        );
        warningText.setStyle("-fx-font-size: 12px; -fx-text-fill: #856404;");

        VBox warningBox = new VBox(5);
        warningBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 10; -fx-border-color: #ffeaa7; -fx-border-radius: 5;");
        warningBox.getChildren().addAll(warningLabel, warningText);

        content.getChildren().addAll(
                tenantInfoLabel, tenantInfoTitle, tenantDetails,
                credLabel, credTitle, credBox,
                warningBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        // Add copy button to copy credentials
        Button copyButton = new Button("Copy Credentials");
        copyButton.setOnAction(e -> {
            String credentials = "Username: " + tenant.getUsername() + "\nPassword: " + tenant.getTemporaryPassword();
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
            clipboardContent.putString(credentials);
            clipboard.setContent(clipboardContent);

            Alert copyAlert = new Alert(Alert.AlertType.INFORMATION);
            copyAlert.setTitle("Copied");
            copyAlert.setHeaderText(null);
            copyAlert.setContentText("Credentials copied to clipboard!");
            copyAlert.showAndWait();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(copyButton);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        content.getChildren().add(buttonBox);

        dialog.showAndWait();
    }

    private void clearForm() {
        nameField.clear();
        contactField.clear();
        emailField.clear();
        addressField.clear();
        roomComboBox.setValue(null);
        roomRentLabel.setText("--");
        roomStatusLabel.setText("--");
        moveInDatePicker.setValue(LocalDate.now());
        updateDueDate();
    }

    @FXML
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to clear the form? All entered data will be lost.");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            clearForm();
        }
    }

    @FXML
    private void handleRefreshRooms() {
        loadAvailableRooms();
        showAlert("Info", "Room list refreshed!");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}