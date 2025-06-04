package org.example.watchlater;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.watchlater.config.AppConfig;
import org.example.watchlater.config.DatabaseConfig;
import org.example.watchlater.model.User;
import org.example.watchlater.service.AsyncWatchlistService;
import org.example.watchlater.service.impl.AsyncWatchlistServiceImpl;
import org.example.watchlater.service.impl.TMDBServiceImpl;
import org.example.watchlater.repository.MovieRepository;
import org.example.watchlater.repository.impl.MovieRepositoryImpl;
import org.example.watchlater.repository.impl.WatchlistRepositoryImpl;
import org.example.watchlater.util.TMDBClient;

import java.util.logging.Logger;
import java.util.logging.Level;

public class WatchLaterApplication extends Application {
    private static final Logger logger = Logger.getLogger(WatchLaterApplication.class.getName());
    private AsyncWatchlistService watchlistService;
    private TMDBServiceImpl tmdbService;
    private User currentUser;
    private MovieRepository movieRepository;
    
    @Override
    public void init() throws Exception {
        super.init();
        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            dbConfig.initializeDatabase();

            currentUser = AppConfig.getDefaultUser();

            this.movieRepository = new MovieRepositoryImpl(currentUser);
            TMDBClient tmdbClient = new TMDBClient();
            this.tmdbService = new TMDBServiceImpl(tmdbClient, movieRepository);
            this.watchlistService = new AsyncWatchlistServiceImpl(
                new WatchlistRepositoryImpl(dbConfig, movieRepository),
                movieRepository
            );
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing application", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            MainController controller = new MainController(currentUser, watchlistService, tmdbService, movieRepository);
            loader.setController(controller);
            Scene scene = new Scene(loader.load(), 1200, 800);

            String cssPath = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle("Watch Later");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

            controller.start();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting application", e);
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        try {
            DatabaseConfig.getInstance().close();
            
            // Clean up resources if necessary
            if (watchlistService != null) {
                watchlistService.shutdown();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error stopping application", e);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }

    public static class Main {
        public static void main(String[] args) {
            WatchLaterApplication.main(args);
        }
    }
}