package ch.ge.ve.offlineadmin.util;

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

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ResourceBundle;

/**
 *  File utilities
 */
public class FileUtils {
    private final ResourceBundle resources;

    private File initialDirectory = getUserHome();

    public FileUtils(ResourceBundle resources) {
        this.resources = resources;
    }

    /**
     * @return the user's home directory
     */
    public File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    /**
     * Prompts for the keys directory
     * @return the directory
     */
    public File promptKeyDirectory() {
        return getDirectory(resources.getString("file_utils.directory_chooser.key_directory.title"));
    }

    /**
     * Prompts for the encrypted ballots file
     * @return the file
     */
    public File promptEncryptedBallotsFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(resources.getString("file_utils.file_chooser.encrypted_ballots.title"));
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                resources.getString("file_utils.file_chooser.encrypted_ballots.filter"), "*.ser"));
        fileChooser.setInitialDirectory(getUserHome());

        return fileChooser.showOpenDialog(null);
    }

    public File getDirectory(String title, File initialDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(initialDirectory);
        return directoryChooser.showDialog(null);
    }

    private File getDirectory(String title) {
        return getDirectory(title, initialDirectory);
    }
}
