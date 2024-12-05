package org.openjfx.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.io.IOException;
import java.util.List;

public class UserController {

    @FXML
    private Label loggedInUserLabel;

    @FXML
    private ListView<String> contactsListView;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    private User loggedInUser;

    private final UserService userService = UserService.getInstance();

    public void setLoggedInUserCredentials(User user) {
        this.loggedInUser = user;
        this.loggedInUserLabel.setText(loggedInUser.getUsername());
        List<String> contacts = userService.getContacts(loggedInUser.getUsername());
        // Add retrieved contacts to the ListView
        contactsListView.getItems().addAll(contacts);
    }

    @FXML
    public void handleLogout() {
        try {
            // Perform logout logic using userService
            userService.logout(loggedInUser.getUsername());

            // Load the login scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/openjfx/LoginScene.fxml"));
            Parent root = loader.load();

            // Get the current stage
            Stage currentStage = (Stage) loggedInUserLabel.getScene().getWindow();

            // Set the new scene
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Optional: Show an error dialog to the user
            System.out.println("Error: Unable to load the login scene.");
        }
    }

    @FXML
    public void addContact(ActionEvent event) {
        String name = nameField.getText();
        String phone = phoneField.getText();

        if (name.isEmpty() || phone.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Fields cannot be empty.");
            return;
        }

        // Add contact to the database
        userService.addContact(loggedInUser.getUsername(), name, phone);

        // Update the contacts list view
        contactsListView.getItems().add(name + " - " + phone);

        // Clear input fields
        nameField.clear();
        phoneField.clear();

    }

    @FXML
    public void removeContact(ActionEvent event) {
        // Get the selected contact from the ListView
        String selectedContact = contactsListView.getSelectionModel().getSelectedItem();

        // If no contact is selected, fall back to using the name field
        if (selectedContact == null) {
            String name = nameField.getText();

            if (name.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "No contact selected or name provided.");
                return;
            }

            // Search for the contact in the ListView
            for (String contact : contactsListView.getItems()) {
                if (contact.startsWith(name + " -")) {
                    selectedContact = contact;
                    break;
                }
            }

            if (selectedContact == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Contact not found in the list.");
                return;
            }
        }

        // Parse the selected contact to extract the name
        String[] parts = selectedContact.split(" - ");
        if (parts.length != 2) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid contact format.");
            return;
        }
        String name = parts[0];
        String phone = parts[1];

        // Remove contact from the database
        boolean isRemoved = userService.removeContactByName(loggedInUser.getUsername(), name);

        if (isRemoved) {
            // Remove the contact from the ListView
            contactsListView.getItems().remove(selectedContact);

            // Clear input fields
            nameField.clear();
            phoneField.clear();

        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to remove the contact from the database.");
        }
    }

    @FXML
    private void searchContact() {
        String searchName = nameField.getText().trim(); // Trim any whitespace

        // Check if the name field is empty
        if (searchName.isEmpty()) {
            // If empty, show all contacts
            List<String> allContacts = userService.getContacts(loggedInUser.getUsername());
            if (allContacts.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Contacts", "No contacts found for the user.");
            } else {
                contactsListView.getItems().clear();
                contactsListView.getItems().addAll(allContacts);
                showAlert(Alert.AlertType.INFORMATION, "All Contacts", "Showing all contacts.");
            }
            return;
        }

        // Perform the search using UserService
        List<String> results = userService.searchContactsByName(loggedInUser.getUsername(), searchName);

        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Results", "No contacts found with the name: " + searchName);
        } else {
            // Update the ListView with the search results
            contactsListView.getItems().clear();
            contactsListView.getItems().addAll(results);
            showAlert(Alert.AlertType.INFORMATION, "Search Results", "Found " + results.size() + " contact(s).");
        }
    }


    @FXML
    public void updateContact() {
        // Get the selected contact from the ListView
        String selectedContact = contactsListView.getSelectionModel().getSelectedItem();
        if (selectedContact == null || selectedContact.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a contact to update.");
            return;
        }

        // Extract the old name and phone number from the selected contact
        String[] parts = selectedContact.split(" - ");
        if (parts.length != 2) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid contact format.");
            return;
        }
        String oldName = parts[0];
        String oldPhoneNumber = parts[1];

        // Get the new name and phone number from the input fields
        String newName = nameField.getText();
        String newPhoneNumber = phoneField.getText();

        // If both fields are empty, show an alert
        if (newName.isEmpty() && newPhoneNumber.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Both name and phone number cannot be empty.");
            return;
        }

        // Use old values if the new values are empty
        if (newName.isEmpty()) {
            newName = oldName;
        }
        if (newPhoneNumber.isEmpty()) {
            newPhoneNumber = oldPhoneNumber;
        }

        // Update the contact in the database
        boolean isUpdated = userService.updateContactByName(loggedInUser.getUsername(), oldName, newName, newPhoneNumber);

        if (isUpdated) {
            // Update the ListView
            contactsListView.getItems().set(
                    contactsListView.getSelectionModel().getSelectedIndex(),
                    newName + " - " + newPhoneNumber
            );

            // Clear input fields
            nameField.clear();
            phoneField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Contact updated successfully.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update the contact.");
        }
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
