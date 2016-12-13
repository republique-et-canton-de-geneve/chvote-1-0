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

import ch.ge.ve.offlineadmin.util.LogLevel;
import ch.ge.ve.offlineadmin.util.LogMessage;
import ch.ge.ve.offlineadmin.util.ProgressTracker;

import de.jensd.fx.glyphs.GlyphIcon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * This controller defines a reusable component. It features a log table for displaying messages, as well as a progress
 * bar to track process progression
 */
public class ConsoleOutputControl extends AnchorPane implements ProgressTracker {
    private final ObservableList<LogMessage> logMessages = FXCollections.observableArrayList();
    @FXML
    private TableView<LogMessage> logTable;
    @FXML
    private ProgressBar progressBar;
    private int stepCount;
    private int currentStep = 0;

    @FXML
    public void initialize() throws IOException {
        TableColumn<LogMessage, GlyphIcon> logLevelColumn = new TableColumn<>();
        logLevelColumn.setCellValueFactory(new PropertyValueFactory<>("glyphIcon"));
        logLevelColumn.setMinWidth(50.0);
        logLevelColumn.setPrefWidth(50.0);
        logLevelColumn.setMaxWidth(50.0);

        TableColumn<LogMessage, String> messageColumn = new TableColumn<>();
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

        logTable.setItems(logMessages);
        ObservableList<TableColumn<LogMessage, ?>> tableColumns = logTable.getColumns();
        tableColumns.add(logLevelColumn);
        tableColumns.add(messageColumn);

        logTable.setEditable(false);
        // Prevent cell selection
        logTable.addEventFilter(MouseEvent.ANY, Event::consume);
        // Do not display a placeholder
        logTable.setPlaceholder(new Label(""));
        // Hide the header row
        logTable.widthProperty().addListener((observable, oldValue, newValue) -> {
            Pane header = (Pane) logTable.lookup("TableHeaderRow");
            if (header.isVisible()) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
            }
        });
        progressBar.setProgress(0f);
    }

    /**
     * Add a message to the log table, with level {@link LogLevel#OK}
     *
     * @param message the message to be added
     */
    public void logOnScreen(String message) {
        logOnScreen(message, LogLevel.OK);
    }

    /**
     * Add a message to the log table, with the specified level
     *
     * @param message  the message to be added
     * @param logLevel the level of the message
     */
    public void logOnScreen(String message, LogLevel logLevel) {
        logMessages.add(new LogMessage(logLevel, message));

        logTable.scrollTo(logMessages.size() - 1);
        logTable.getSelectionModel().clearSelection();
    }

    @Override
    public void progressMessage(String message) {
        logOnScreen(message);
    }

    @Override
    public void progressMessage(String message, LogLevel logLevel) {
        logOnScreen(message, logLevel);
    }

    @Override
    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    @Override
    public void incrementStepCount() {
        if (Platform.isFxApplicationThread()) {
            incrementProgress();
        } else {
            Platform.runLater(this::incrementProgress);
        }
    }

    private void incrementProgress() {
        progressBar.setProgress(++currentStep / (double) stepCount);
    }
}
