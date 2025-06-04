package org.example.watchlater;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.model.User;
import org.example.watchlater.service.AsyncWatchlistService;
import org.example.watchlater.service.impl.TMDBServiceImpl;
import org.example.watchlater.ui.MovieDetailsView;
import org.example.watchlater.ui.WatchlistStatsView;
import org.example.watchlater.ui.MovieSearchView;
import org.example.watchlater.ui.WatchlistView;
import org.example.watchlater.repository.MovieRepository;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MainController {
    private static final Logger logger = Logger.getLogger(MainController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private ListView<TMDBMovie> movieListView;
    @FXML private Label statusLabel;
    @FXML private StackPane contentArea;

    private MovieDetailsView movieDetailsView;
    private WatchlistStatsView statisticsView;
    private MovieSearchView movieSearchView;

    private final User user;
    private final AsyncWatchlistService watchlistService;
    private final TMDBServiceImpl tmdbService;
    private final MovieRepository movieRepository;

    private final ObservableList<TMDBMovie> movies = FXCollections.observableArrayList();
    private final FilteredList<TMDBMovie> filteredMovies;
    
    public MainController(User user, AsyncWatchlistService watchlistService, TMDBServiceImpl tmdbService, MovieRepository movieRepository) {
        this.user = user;
        this.watchlistService = watchlistService;
        this.tmdbService = tmdbService;
        this.movieRepository = movieRepository;
        this.filteredMovies = new FilteredList<>(movies);
    }

    @FXML
    public void initialize() {

        movieDetailsView = new MovieDetailsView(user, watchlistService, tmdbService);
        movieDetailsView.setOnBackAction(this::showMovieList);

        statisticsView = new WatchlistStatsView(user, watchlistService);
        statisticsView.setOnBackAction(this::showMovieList);

        movieSearchView = new MovieSearchView(tmdbService, user, watchlistService);
        movieSearchView.setOnMovieSelected(this::showMovieDetails);

        movieListView.setCellFactory(lv -> new MovieListCell());
        movieListView.getSelectionModel().selectedItemProperty()
            .addListener(this::handleMovieSelection);
    }

    public void start() {
        Platform.runLater(this::showMovieSearch);
    }
    
    private void handleMovieSelection(ObservableValue<? extends TMDBMovie> obs, TMDBMovie oldSelection, TMDBMovie newSelection) {
        if (newSelection == null) {
            showMovieList();
            return;
        }
        
        showMovieDetails(newSelection);
    }
    
    private void showMovieList() {
        showMovieSearch();
    }

    private void showMovieSearch() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(movieSearchView);

        movieSearchView.refreshWatchlistStatuses();

        BorderPane root = (BorderPane) contentArea.getScene().getRoot();
        root.setTop(null);
    }

    private void showMovieDetails(TMDBMovie movie) {
        BorderPane root = (BorderPane) contentArea.getScene().getRoot();
        root.setTop(null);
        
        contentArea.getChildren().clear();
        contentArea.getChildren().add(movieDetailsView);
        movieDetailsView.showMovie(movie);
    }
    
    private void showStatisticsView() {
        BorderPane root = (BorderPane) contentArea.getScene().getRoot();
        root.setTop(null);
        
        contentArea.getChildren().clear();
        contentArea.getChildren().add(statisticsView);
        statisticsView.refresh();
    }
    
    @FXML
    private void handleBackToSearch() {
        showMovieList();
    }

    @FXML
    private void handleShowStatistics() {
        showStatisticsView();
    }
    
    private void showWatchlist() {
        contentArea.getChildren().clear();

        WatchlistView newWatchlistView = new WatchlistView(user, watchlistService, tmdbService, movieRepository);
        newWatchlistView.setOnStatsSelected(this::showStatisticsView);
        newWatchlistView.setOnBackAction(this::showMovieSearch);
        newWatchlistView.setOnMovieSelected(this::showMovieDetails);

        contentArea.getChildren().add(newWatchlistView);

        BorderPane root = (BorderPane) contentArea.getScene().getRoot();
        root.setTop(null);
    }

    @FXML
    private void handleShowWatchlist() {
        showWatchlist();
    }
    
    @FXML
    private void handleExit() {
        cleanup();
        System.exit(0);
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Watch Later");
        alert.setHeaderText("Watch Later - Movie Manager");
        alert.setContentText("Version 1.0.0\nA simple application to manage your movie watchlist.");
        alert.showAndWait();
    }
    
    private void cleanup() {
        watchlistService.shutdown();
    }
    
    // Custom list cell for movie items
    private class MovieListCell extends ListCell<TMDBMovie> {
        private final HBox content;
        private final Label titleLabel;
        private final Label yearLabel;
        private final Label statusLabel;
        
        public MovieListCell() {
            content = new HBox(10);
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            titleLabel = new Label();
            titleLabel.getStyleClass().add("movie-title");
            
            yearLabel = new Label();
            yearLabel.getStyleClass().add("movie-info");
            
            statusLabel = new Label();
            statusLabel.getStyleClass().add("movie-info");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            content.getChildren().addAll(titleLabel, yearLabel, spacer, statusLabel);
            content.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        }
        
        @Override
        protected void updateItem(TMDBMovie movie, boolean empty) {
            super.updateItem(movie, empty);
            
            if (empty || movie == null) {
                setGraphic(null);
                return;
            }
            
            titleLabel.setText(movie.getTitle());
            yearLabel.setText(String.valueOf(movie.getReleaseDate().getYear()));

            watchlistService.isInWatchlist(user.getId(), movie.getTmdbId())
                .thenAccept(inWatchlist -> {
                    if (inWatchlist) {
                        watchlistService.isWatched(user.getId(), movie.getTmdbId())
                            .thenAccept(isWatched -> {
                                Platform.runLater(() -> {
                                    statusLabel.setText(isWatched ? "Watched" : "Saved");
                                    statusLabel.getStyleClass().setAll("movie-info",
                                        isWatched ? "status-watched" : "status-saved");
                                });
                            });
                    } else {
                        Platform.runLater(() -> {
                            statusLabel.setText("");
                            statusLabel.getStyleClass().setAll("movie-info");
                        });
                    }
                });
            
            setGraphic(content);
        }
    }
} 