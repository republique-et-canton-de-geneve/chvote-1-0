package ch.ge.ve.offlineadmin;

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

import ch.ge.ve.offlineadmin.controller.InterruptibleProcessController;
import ch.ge.ve.offlineadmin.exception.ProcessInterruptedException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Security;
import java.util.ResourceBundle;

import static ch.ge.ve.offlineadmin.util.SecurityConstants.PROPERTIES_LOG4J;

/**
 * Main class
 */
public class OfflineAdminApp extends Application {
    /**
     * @param args the arguments passed
     */
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        PropertyConfigurator.configure(getLog4jProperties());
        ResourceBundle resourceBundle = getBundle();

        initializeDefaultExceptionHandler(resourceBundle);

        primaryStage.setTitle(resourceBundle.getString("primaryStage.title"));
        primaryStage.getIcons().add(new Image(OfflineAdminApp.class.getResourceAsStream("images/icon.gif")));

        BorderPane rootLayout = initRootLayout(resourceBundle);
        Scene mainScene = new Scene(rootLayout);
        mainScene.getStylesheets().add(getStyleSheet().toExternalForm());

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    private void initializeDefaultExceptionHandler(ResourceBundle resourceBundle) {
        InterruptibleProcessController interruptibleProcessController = new InterruptibleProcessController() {
            @Override
            protected ResourceBundle getResourceBundle() {
                return resourceBundle;
            }
        };
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            final ProcessInterruptedException processInterruptedException = new ProcessInterruptedException(resourceBundle.getString("exception_alert.generic-message"), e);
            interruptibleProcessController.handleInterruption(processInterruptedException);
        });
    }

    private BorderPane initRootLayout(ResourceBundle bundle) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL resource = this.getClass().getResource("/ch/ge/ve/offlineadmin/view/RootLayout.fxml");
        loader.setLocation(resource);
        loader.setResources(bundle);

        return loader.load();
    }

    private URL getStyleSheet() {
        return this.getClass().getResource("/ch/ge/ve/offlineadmin/styles/offlineadmin.css");
    }

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("ch.ge.ve.offlineadmin.bundles.offlineadmin-messages");
    }

    private InputStream getLog4jProperties() throws IOException {
        return OfflineAdminApp.class.getClassLoader().getResourceAsStream(PROPERTIES_LOG4J);
    }

}
