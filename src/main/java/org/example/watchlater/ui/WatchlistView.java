package org.example.watchlater.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.model.User;
import org.example.watchlater.model.WatchlistEntry;
import org.example.watchlater.service.AsyncWatchlistService;
import org.example.watchlater.service.TMDBService;
import org.example.watchlater.repository.MovieRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatchlistView extends BorderPane {
    private final User user;
    private final AsyncWatchlistService watchlistService;
    private final TMDBService tmdbService;
    private final MovieRepository movieRepository;
    private final String imageBaseUrl;
    private final ExecutorService executorService;

    private final ObservableList<TMDBMovie> unwatchedMovies = FXCollections.observableArrayList();
    private final ObservableList<TMDBMovie> watchedMovies = FXCollections.observableArrayList();

    private final Button statsButton;
    private final Button backButton;
    private final ProgressIndicator loadingIndicator;
    private final FlowPane unwatchedMoviesPane;
    private final FlowPane watchedMoviesPane;
    private final Label statusLabel;
    private final ScrollPane scrollPane;
    private final SimpleObjectProperty<Runnable> onStatsSelected = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Runnable> onBackAction = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Consumer<TMDBMovie>> onMovieSelected = new SimpleObjectProperty<>();

    private static final Logger logger = Logger.getLogger(WatchlistView.class.getName());

    public WatchlistView(User user, AsyncWatchlistService watchlistService, TMDBService tmdbService, MovieRepository movieRepository) {
        this.user = user;
        this.watchlistService = watchlistService;
        this.tmdbService = tmdbService;
        this.movieRepository = movieRepository;
        this.imageBaseUrl = tmdbService.getImageBaseUrl();
        this.executorService = Executors.newFixedThreadPool(4);

        // Initialize components
        this.statsButton = new Button("Statistics");
        this.backButton = new Button("← Back to Search");
        this.loadingIndicator = new ProgressIndicator();
        VBox moviesContainer = new VBox(20);
        this.unwatchedMoviesPane = new FlowPane();
        this.watchedMoviesPane = new FlowPane();
        this.statusLabel = new Label();
        Label unwatchedLabel = new Label("Movies to Watch");
        Label watchedLabel = new Label("Watched Movies");
        
        // Style labels
        unwatchedLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        watchedLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        unwatchedLabel.setPadding(new Insets(20, 0, 10, 0));
        watchedLabel.setPadding(new Insets(20, 0, 10, 0));
        unwatchedLabel.setTextFill(Color.rgb(51, 51, 51));
        watchedLabel.setTextFill(Color.rgb(51, 51, 51));

        watchedLabel.setMaxWidth(Double.MAX_VALUE);
        watchedLabel.setAlignment(Pos.CENTER);
        watchedLabel.setBackground(new Background(new BackgroundFill(Color.rgb(240, 240, 240), null, null)));
        watchedLabel.setBorder(new Border(new BorderStroke(Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, null, new BorderWidths(1, 0, 1, 0))));

        this.scrollPane = new ScrollPane(moviesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setBackground(Background.EMPTY);

        unwatchedMoviesPane.setHgap(20);
        unwatchedMoviesPane.setVgap(20);
        unwatchedMoviesPane.setPadding(new Insets(10));
        watchedMoviesPane.setHgap(20);
        watchedMoviesPane.setVgap(20);
        watchedMoviesPane.setPadding(new Insets(10));

        moviesContainer.setPadding(new Insets(20));
        moviesContainer.getChildren().addAll(
                unwatchedLabel,
            unwatchedMoviesPane,
                watchedLabel,
            watchedMoviesPane
        );

        setupLayout();
        setupEventHandlers();
        loadWatchlist();
    }

    private void setupLayout() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(20));
        topBar.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        backButton.getStyleClass().add("secondary-button");
        statsButton.getStyleClass().add("primary-button");
        backButton.setPrefWidth(150);
        statsButton.setPrefWidth(150);

        topBar.getChildren().addAll(backButton, statsButton);

        StackPane centerContent = new StackPane(scrollPane, loadingIndicator);
        loadingIndicator.setVisible(false);

        VBox rootContent = new VBox(statusLabel, centerContent);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        setTop(topBar);
        setCenter(rootContent);
    }

    private void setupEventHandlers() {
        backButton.setOnAction(e -> {
            Runnable action = getOnBackAction();
            if (action != null) {
                action.run();
            }
        });

        statsButton.setOnAction(e -> {
            Runnable action = getOnStatsSelected();
            if (action != null) {
                action.run();
            }
        });
    }

    private void loadWatchlist() {
        statusLabel.setText("Loading watchlist...");
        loadingIndicator.setVisible(true);
        unwatchedMovies.clear();
        watchedMovies.clear();

        CompletableFuture.runAsync(() -> {
            try {
                List<WatchlistEntry> entries = watchlistService.getWatchlist(user.getId()).get();
                
                if (entries.isEmpty()) {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        statusLabel.setText("Your watchlist is empty");
                    });
                    return;
                }

                List<CompletableFuture<Optional<TMDBMovie>>> movieFutures = new ArrayList<>();
                for (WatchlistEntry entry : entries) {
                    CompletableFuture<Optional<TMDBMovie>> movieFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            Optional<TMDBMovie> movieOpt = movieRepository.findById(entry.getMovieId());
                            if (movieOpt.isPresent()) {
                                return tmdbService.getMovieDetails(movieOpt.get().getTmdbId()).get();
                            } else {
                                watchlistService.removeFromWatchlist(user.getId(), entry.getMovieId())
                                    .exceptionally(ex -> {
                                        logger.warning("Failed to remove non-existent movie from watchlist: " + entry.getMovieId());
                                        return null;
                                    });
                                return Optional.empty();
                            }
                        } catch (Exception ex) {
                            logger.warning("Error processing movie: " + entry.getMovieId());
                            return Optional.empty();
                        }
                    }, executorService);
                    movieFutures.add(movieFuture);
                }

                CompletableFuture.allOf(movieFutures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        List<TMDBMovie> loadedMovies = movieFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

                        Platform.runLater(() -> {
                            unwatchedMoviesPane.getChildren().clear();
                            watchedMoviesPane.getChildren().clear();

                            for (TMDBMovie movie : loadedMovies) {
                                MovieCard card = new MovieCard(movie, imageBaseUrl);

                                watchlistService.isWatched(user.getId(), movie.getTmdbId())
                                    .thenAccept(isWatched -> Platform.runLater(() -> {
                                        card.setInWatchlist(true);
                                        card.setWatched(isWatched);

                                        if (isWatched) {
                                            watchedMoviesPane.getChildren().add(card);
                                        } else {
                                            unwatchedMoviesPane.getChildren().add(card);
                                        }
                                    }));

                                setupMovieCardEventHandlers(card, movie);
                            }
                            loadingIndicator.setVisible(false);
                            statusLabel.setText("Watchlist loaded");
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            loadingIndicator.setVisible(false);
                            showError("Failed to load watchlist: " + ex.getMessage());
                        });
                        return null;
                    });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    showError("Failed to load watchlist: " + ex.getMessage());
                });
            }
        }, executorService);
    }

    private void setupMovieCardEventHandlers(MovieCard card, TMDBMovie movie) {
        card.addEventHandler(MovieCard.MovieCardEvent.REMOVE_FROM_WATCHLIST, e -> {
            watchlistService.removeFromWatchlist(user.getId(), movie.getTmdbId())
                .thenAccept(v -> Platform.runLater(() -> {
                    unwatchedMoviesPane.getChildren().remove(card);
                    watchedMoviesPane.getChildren().remove(card);
                    statusLabel.setText("Movie removed from watchlist");
                    loadWatchlist();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError("Failed to remove movie from watchlist: " + ex.getMessage());
                    });
                    return null;
                });
        });
        
        card.addEventHandler(MovieCard.MovieCardEvent.MARK_AS_WATCHED, e -> {
            watchlistService.markAsWatched(user.getId(), movie.getTmdbId())
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        card.setWatched(true);
                        unwatchedMoviesPane.getChildren().remove(card);
                        watchedMoviesPane.getChildren().add(card);
                        statusLabel.setText("Movie marked as watched");
                        loadWatchlist();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError("Failed to mark movie as watched: " + ex.getMessage());
                    });
                    return null;
                });
        });
        
        card.addEventHandler(MovieCard.MovieCardEvent.MARK_AS_UNWATCHED, e -> {
            watchlistService.markAsUnwatched(user.getId(), movie.getTmdbId())
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        card.setWatched(false);
                        watchedMoviesPane.getChildren().remove(card);
                        unwatchedMoviesPane.getChildren().add(card);
                        statusLabel.setText("Movie marked as unwatched");
                        loadWatchlist();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError("Failed to mark movie as unwatched: " + ex.getMessage());
                    });
                    return null;
                });
        });
        
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Consumer<TMDBMovie> handler = getOnMovieSelected();
                if (handler != null) {
                    handler.accept(movie);
                }
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public Runnable getOnStatsSelected() {
        return onStatsSelected.get();
    }

    public void setOnStatsSelected(Runnable onStatsSelected) {
        this.onStatsSelected.set(onStatsSelected);
    }


    public Runnable getOnBackAction() {
        return onBackAction.get();
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction.set(onBackAction);
    }


    public Consumer<TMDBMovie> getOnMovieSelected() {
        return onMovieSelected.get();
    }

    public void setOnMovieSelected(Consumer<TMDBMovie> handler) {
        this.onMovieSelected.set(handler);
    }

} 