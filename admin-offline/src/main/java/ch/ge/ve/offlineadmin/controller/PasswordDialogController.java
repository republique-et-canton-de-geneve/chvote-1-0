package ch.ge.ve.offlineadmin.controller;

/*-
 * #%L
 * Admin offline
 * %%
 * Copyright (C) 2015 - 2016 République et Canton de Genève
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.ge.ve.offlineadmin.exception.ProcessInterruptedException;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Dialog to enter password(s)
 */
public class PasswordDialogController {
    public static final int LABEL_COL = 0;
    public static final int INPUT_COL = 1;

    private static final double GRID_GAP = 10.0;
    private static final Insets GRID_INSETS = new Insets(20, 150, 10, 10);

    private final ResourceBundle resources;
    private ConsoleOutputControl consoleOutputController;

    public PasswordDialogController(ResourceBundle resources, ConsoleOutputControl consoleOutputController) {
        this.resources = resources;
        this.consoleOutputController = consoleOutputController;
    }

    private static boolean isPasswordValid(String newValue) {
        // Length should be between 9 and 10 (incl)
        boolean validLength = newValue.length() >= 9 && newValue.length() <= 10;
        // Password should contain at least one upper, one lower and one digit
        boolean validPattern = newValue.matches(".*[A-Z].*") && newValue.matches(".*[a-z].*") && newValue.matches(".*[0-9].*");
        return validLength && validPattern;
    }

    /**
     * Display the dialogs to enter the two passwords
     * <p/>
     * Whenever requested a password confirmation input-box is displayed
     *
     * @param password1        first password
     * @param password2        second password
     * @param withConfirmation indicates if password confirmation is needed
     * @throws ProcessInterruptedException
     */
    public void promptForPasswords(StringProperty password1, StringProperty password2, boolean withConfirmation) throws ProcessInterruptedException {
        openPasswordInputDialog(password1, 1, withConfirmation);
        log("key_generation.password1_entered", withConfirmation);
        openPasswordInputDialog(password2, 2, withConfirmation);
        log("key_generation.password2_entered", withConfirmation);
    }

    private void log(String key, boolean withConfirmation) {
        if (withConfirmation) {
            consoleOutputController.logOnScreen(resources.getString(key));
        } else {
            consoleOutputController.progressMessage(resources.getString(key));
        }
    }

    private void openPasswordInputDialog(StringProperty target, int groupNumber, boolean withConfirmation) throws ProcessInterruptedException {
        Dialog<String> dialog = new Dialog<>();
        String title = String.format(resources.getString("password_dialog.title"), groupNumber);
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add("/ch/ge/ve/offlineadmin/styles/offlineadmin.css");
        dialog.getDialogPane().getStyleClass().addAll("background", "password-dialog");

        // Define and add buttons
        ButtonType confirmPasswordButtonType = new ButtonType(resources.getString("password_dialog.confirm_button"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmPasswordButtonType, ButtonType.CANCEL);

        // Create header label
        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("header-label");

        // Create the input labels and fields
        GridPane grid = new GridPane();
        grid.setHgap(GRID_GAP);
        grid.setVgap(GRID_GAP);
        grid.setPadding(GRID_INSETS);

        TextField electionOfficer1Password = new TextField();
        String electionOfficer1Label = withConfirmation ? resources.getString("password_dialog.election_officer_1")
                : resources.getString("password_dialog.election_officer");
        electionOfficer1Password.setPromptText(electionOfficer1Label);
        electionOfficer1Password.setId("passwordField1");

        TextField electionOfficer2Password = new TextField();
        String electionOfficer2Label = resources.getString("password_dialog.election_officer_2");
        electionOfficer2Password.setPromptText(electionOfficer2Label);
        electionOfficer2Password.setId("passwordField2");

        // Create error message label
        Label errorMessage = createErrorMessage(withConfirmation);
        errorMessage.setId("errorLabel");

        // Position the labels and fields
        int row = 0;
        grid.add(headerLabel, LABEL_COL, row, 2, 1);
        row++;
        grid.add(new Label(electionOfficer1Label), LABEL_COL, row);
        grid.add(electionOfficer1Password, INPUT_COL, row);
        if (withConfirmation) {
            row++;
            grid.add(new Label(electionOfficer2Label), LABEL_COL, row);
            grid.add(electionOfficer2Password, INPUT_COL, row);
        }
        row++;
        grid.add(errorMessage, LABEL_COL, row, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Perform input validation
        Node confirmButton = dialog.getDialogPane().lookupButton(confirmPasswordButtonType);
        confirmButton.setDisable(true);

        BooleanBinding booleanBinding = bindForValidity(withConfirmation, electionOfficer1Password, electionOfficer2Password, errorMessage, confirmButton);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmPasswordButtonType) {
                return electionOfficer1Password.textProperty().getValueSafe();
            } else {
                return null;
            }
        });

        Platform.runLater(electionOfficer1Password::requestFocus);

        Optional<String> result = dialog.showAndWait();

        //if not disposed then we do have binding errors if this method is run again
        booleanBinding.dispose();
        result.ifPresent(target::setValue);

        if (!result.isPresent()) {
            throw new ProcessInterruptedException("Password input cancelled");
        }
    }

    private BooleanBinding bindForValidity(boolean withConfirmation, TextField electionOfficer1Password, TextField electionOfficer2Password, Label errorMessage, Node confirmButton) {
        BooleanBinding passwordsValid = Bindings.createBooleanBinding(
                () -> withConfirmation ? arePasswordsEqualAndValid(electionOfficer1Password.textProperty(), electionOfficer2Password.textProperty()) : isPasswordValid(electionOfficer1Password.getText()),
                electionOfficer1Password.textProperty(),
                electionOfficer2Password.textProperty());
        passwordsValid.addListener((observable, werePasswordsValid, arePasswordsValid) -> {
            confirmButton.setDisable(!arePasswordsValid);
            errorMessage.setVisible(!arePasswordsValid && withConfirmation);
        });
        return passwordsValid;
    }

    private Label createErrorMessage(boolean withConfirmation) {
        Label errorMessage = new Label();
        String passwordStrengthMessage;
        if (withConfirmation) {
            passwordStrengthMessage = resources.getString("password_dialog.strength_requirements");
        } else {
            passwordStrengthMessage = resources.getString("password_dialog.strength_requirements_no_conf");
        }
        errorMessage.setText(passwordStrengthMessage);
        errorMessage.setWrapText(true);
        return errorMessage;
    }

    private boolean arePasswordsEqualAndValid(StringProperty stringProperty1, StringProperty stringProperty2) {
        return stringProperty1.getValueSafe().equals(stringProperty2.getValueSafe()) && isPasswordValid(stringProperty1.getValueSafe());
    }
}
