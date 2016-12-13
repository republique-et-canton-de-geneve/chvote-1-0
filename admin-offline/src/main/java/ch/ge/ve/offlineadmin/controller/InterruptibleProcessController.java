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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;

/**
 * This abstract class defines the handling of {@link ProcessInterruptedException}
 * <p/>
 * When such an error occurs, a dialog is shown with the exception stacktrace
 */
public abstract class InterruptibleProcessController {
    public static final String PROCESS_INTERRUPTED_MESSAGE = "Process interrupted";

    protected abstract ResourceBundle getResourceBundle();

    /**
     * Default handling of the exception, displays information in an alert dialog.
     *
     * @param e the exception
     */
    public void handleInterruption(ProcessInterruptedException e) {
        if (Platform.isFxApplicationThread()) {
            showAlert(e);
        } else {
            Platform.runLater(() -> showAlert(e));
        }
    }

    private void showAlert(ProcessInterruptedException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        ResourceBundle resources = getResourceBundle();
        alert.setTitle(resources.getString("exception_alert.title"));
        alert.setHeaderText(resources.getString("exception_alert.header"));
        alert.setContentText(e.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label stackTraceLabel = new Label(resources.getString("exception_alert.label"));
        TextArea stackTraceTextArea = new TextArea(exceptionText);
        stackTraceTextArea.setEditable(false);
        stackTraceTextArea.setWrapText(true);
        GridPane.setVgrow(stackTraceTextArea, Priority.ALWAYS);
        GridPane.setHgrow(stackTraceTextArea, Priority.ALWAYS);

        GridPane expandableContent = new GridPane();
        expandableContent.setPrefSize(400, 400);
        expandableContent.setMaxWidth(Double.MAX_VALUE);
        expandableContent.add(stackTraceLabel, 0, 0);
        expandableContent.add(stackTraceTextArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expandableContent);
        // Dirty Linux only fix...
        // Expandable zones cause the dialog not to resize correctly
        if (System.getProperty("os.name").matches(".*[Ll]inux.*")) {
            alert.getDialogPane().setPrefSize(600, 400);
            alert.setResizable(true);
            alert.getDialogPane().setExpanded(true);
        }

        alert.showAndWait();
    }
}
