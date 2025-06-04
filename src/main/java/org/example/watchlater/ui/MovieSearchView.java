package org.example.watchlater.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.service.TMDBService;
import org.example.watchlater.service.AsyncWatchlistService;
import org.example.watchlater.model.User;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.layout.Priority;

public class MovieSearchView extends VBox {
    private final TMDBService tmdbService;
    private final AsyncWatchlistService watchlistService;
    private final User user;
    private final FlowPane moviesPane;
    private final ProgressIndicator loadingIndicator;
    private final Label statusLabel;
    private final TextField searchField;
    private Consumer<TMDBMovie> onMovieSelected;
    
    public MovieSearchView(TMDBService tmdbService, User user, AsyncWatchlistService watchlistService) {
        this.tmdbService = tmdbService;
        this.user = user;
        this.watchlistService = watchlistService;
        this.moviesPane = new FlowPane();
        this.loadingIndicator = new ProgressIndicator();
        this.statusLabel = new Label("Loading popular movies...");
        this.searchField = new TextField();
        
        setupLayout();
        setupEventHandlers();
        loadPopularMovies();
    }
    
    private void setupLayout() {
        setSpacing(20);
        setPadding(new Insets(20));
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setPadding(new Insets(0, 0, 20, 0));
        
        searchField.setPromptText("Search movies...");
        searchField.setPrefWidth(400);
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        searchBar.getChildren().add(searchField);

        moviesPane.setHgap(20);
        moviesPane.setVgap(20);
        moviesPane.setPadding(new Insets(10));
        moviesPane.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(moviesPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        statusLabel.setFont(Font.font("System", 14));
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setTextAlignment(TextAlignment.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);

        loadingIndicator.setVisible(true);
        loadingIndicator.setMaxSize(50, 50);

        getChildren().addAll(searchBar, scrollPane, statusLabel, loadingIndicator);
    }
    
    private void setupEventHandlers() {
        searchField.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                search(query);
            } else {
                loadPopularMovies();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                loadPopularMovies();
            }
        });
    }
    
    private void search(String query) {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Searching...");
        
        tmdbService.searchMovies(query)
            .thenAccept(movies -> {
                Platform.runLater(() -> {
                    moviesPane.getChildren().clear();
                    if (movies.isEmpty()) {
                        statusLabel.setText("No movies found. Try a different search term.");
                    } else {
                        statusLabel.setText(String.format("Found %d movies for '%s'", movies.size(), query));
                        for (TMDBMovie movie : movies) {
                            MovieCard card = new MovieCard(movie, tmdbService.getImageBaseUrl());

                            watchlistService.isInWatchlist(user.getId(), movie.getTmdbId())
                                .thenAccept(inWatchlist -> {
                                    if (inWatchlist) {
                                        watchlistService.isWatched(user.getId(), movie.getTmdbId())
                                            .thenAccept(isWatched -> {
                                                Platform.runLater(() -> {
                                                    card.setInWatchlist(true);
                                                    card.setWatched(isWatched);
                                                });
                                            });
                                    }
                                });

                            setupMovieCardEventHandlers(card, movie);
                            moviesPane.getChildren().add(card);
                        }
                    }
                    loadingIndicator.setVisible(false);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error searching movies. Please try again.");
                    loadingIndicator.setVisible(false);
                    showError("Error searching movies: " + ex.getMessage());
                });
                return null;
            });
    }
    
    private void setupMovieCardEventHandlers(MovieCard card, TMDBMovie movie) {
        card.addEventHandler(MovieCard.MovieCardEvent.ADD_TO_WATCHLIST, e -> {
            watchlistService.addToWatchlist(user.getId(), movie.getTmdbId())
                .thenAccept(entry -> Platform.runLater(() -> {
                    card.setInWatchlist(true);
                    statusLabel.setText("Movie added to watchlist");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to add movie to watchlist");
                        showError("Failed to add movie to watchlist: " + ex.getMessage());
                    });
                    return null;
                });
        });

        card.addEventHandler(MovieCard.MovieCardEvent.REMOVE_FROM_WATCHLIST, e -> {
            watchlistService.removeFromWatchlist(user.getId(), movie.getTmdbId())
                .thenRun(() -> Platform.runLater(() -> {
                    card.setInWatchlist(false);
                    card.setWatched(false);
                    statusLabel.setText("Movie removed from watchlist");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to remove movie from watchlist");
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
                        statusLabel.setText("Movie marked as watched");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to mark movie as watched");
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
                        statusLabel.setText("Movie marked as unwatched");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed to mark movie as unwatched");
                        showError("Failed to mark movie as unwatched: " + ex.getMessage());
                    });
                    return null;
                });
        });

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onMovieSelected != null) {
                onMovieSelected.accept(movie);
            }
        });
    }
    
    private void loadPopularMovies() {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Loading popular movies...");
        
        tmdbService.getPopularMovies()
            .thenAccept(movies -> {
                Platform.runLater(() -> {
                    moviesPane.getChildren().clear();
                    if (movies.isEmpty()) {
                        statusLabel.setText("No popular movies found. Please try again later.");
                    } else {
                        statusLabel.setText("Popular Movies");
                        for (TMDBMovie movie : movies) {
                            MovieCard card = new MovieCard(movie, tmdbService.getImageBaseUrl());

                            watchlistService.isInWatchlist(user.getId(), movie.getTmdbId())
                                .thenAccept(inWatchlist -> {
                                    if (inWatchlist) {
                                        watchlistService.isWatched(user.getId(), movie.getTmdbId())
                                            .thenAccept(isWatched -> {
                                                Platform.runLater(() -> {
                                                    card.setInWatchlist(true);
                                                    card.setWatched(isWatched);
                                                });
                                            });
                                    }
                                });

                            card.addEventHandler(MovieCard.MovieCardEvent.ADD_TO_WATCHLIST, e -> {
                                watchlistService.addToWatchlist(user.getId(), movie.getTmdbId())
                                    .thenAccept(entry -> Platform.runLater(() -> {
                                        card.setInWatchlist(true);
                                        card.setWatched(false);
                                        statusLabel.setText("Movie added to watchlist");
                                    }))
                                    .exceptionally(ex -> {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Failed to add movie to watchlist");
                                            showError("Failed to add movie to watchlist: " + ex.getMessage());
                                        });
                                        return null;
                                    });
                            });
                            
                            card.addEventHandler(MovieCard.MovieCardEvent.REMOVE_FROM_WATCHLIST, e -> {
                                watchlistService.removeFromWatchlist(user.getId(), movie.getTmdbId())
                                    .thenRun(() -> Platform.runLater(() -> {
                                        card.setInWatchlist(false);
                                        card.setWatched(false);
                                        statusLabel.setText("Movie removed from watchlist");
                                    }))
                                    .exceptionally(ex -> {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Failed to remove movie from watchlist");
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
                                            statusLabel.setText("Movie marked as watched");
                                        }
                                    }))
                                    .exceptionally(ex -> {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Failed to mark movie as watched");
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
                                            statusLabel.setText("Movie marked as unwatched");
                                        }
                                    }))
                                    .exceptionally(ex -> {
                                        Platform.runLater(() -> {
                                            statusLabel.setText("Failed to mark movie as unwatched");
                                            showError("Failed to mark movie as unwatched: " + ex.getMessage());
                                        });
                                        return null;
                                    });
                            });
                            
                            card.setOnMouseClicked(e -> {
                                if (e.getClickCount() == 2 && onMovieSelected != null) {
                                    onMovieSelected.accept(movie);
                                }
                            });
                            moviesPane.getChildren().add(card);
                        }
                    }
                    loadingIndicator.setVisible(false);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading popular movies. Please try again later.");
                    loadingIndicator.setVisible(false);
                    showError("Error loading popular movies: " + ex.getMessage());
                });
                return null;
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
    
    public void setOnMovieSelected(Consumer<TMDBMovie> handler) {
        this.onMovieSelected = handler;
    }

    public void refreshWatchlistStatuses() {
        if (moviesPane.getChildren().isEmpty()) {
            return;
        }

        statusLabel.setText("Refreshing watchlist statuses...");

        for (javafx.scene.Node node : moviesPane.getChildren()) {
            if (node instanceof MovieCard) {
                MovieCard card = (MovieCard) node;
                TMDBMovie movie = card.getMovie();

                watchlistService.isInWatchlist(user.getId(), movie.getTmdbId())
                    .thenAccept(inWatchlist -> {
                        Platform.runLater(() -> {
                            try {
                                card.setInWatchlist(inWatchlist);
                                if (!inWatchlist) {
                                    card.setWatched(false);
                                }
                            } catch (Exception e) {
                                System.err.println("Error updating MovieCard inWatchlist status: " + e.getMessage());
                            }
                        });

                        if (inWatchlist) {
                            watchlistService.isWatched(user.getId(), movie.getTmdbId())
                                .thenAccept(isWatched -> {
                                    Platform.runLater(() -> {
                                        try {
                                            card.setWatched(isWatched);
                                        } catch (Exception e) {
                                            System.err.println("Error updating MovieCard isWatched status: " + e.getMessage());
                                        }
                                    });
                                })
                                .exceptionally(ex -> {
                                    System.err.println("Failed to check watched status for movie " + movie.getTmdbId() + ": " + ex.getMessage());
                                    return null;
                                });
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Failed to check watchlist status for movie " + movie.getTmdbId() + ": " + ex.getMessage());
                        return null;
                    });
            }
        }
        statusLabel.setText("");
    }
} 