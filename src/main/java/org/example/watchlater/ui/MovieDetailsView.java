package org.example.watchlater.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.model.User;
import org.example.watchlater.model.WatchlistEntry;
import org.example.watchlater.service.AsyncWatchlistService;
import org.example.watchlater.service.TMDBService;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

public class MovieDetailsView extends BorderPane {

    private final User user;
    private final AsyncWatchlistService watchlistService;
    private final TMDBService tmdbService;
    private final String imageBaseUrl;
    private TMDBMovie currentMovie;
    private WatchlistEntry currentEntry;

    private final ObjectProperty<Runnable> onBackAction = new SimpleObjectProperty<>();

    private final ImageView backdropView;
    private final ImageView posterView;
    private final Label titleLabel;
    private final Label taglineLabel;
    private final Label overviewLabel;
    private final Label releaseDateLabel;
    private final Label ratingLabel;
    private final Button backButton;
    private Button watchlistBtn;
    private Button watchedBtn;
    private VBox watchlistControls;
    private final ProgressIndicator loadingIndicator;

    private static final Duration FADE_DURATION = Duration.millis(300);

    public MovieDetailsView(User user, AsyncWatchlistService watchlistService, TMDBService tmdbService) {
        this.user = user;
        this.watchlistService = watchlistService;
        this.tmdbService = tmdbService;
        this.imageBaseUrl = tmdbService.getImageBaseUrl();

        this.backButton = new Button("← Back");
        this.backdropView = new ImageView();
        this.posterView = new ImageView();
        this.titleLabel = new Label();
        this.taglineLabel = new Label();
        this.overviewLabel = new Label();
        this.releaseDateLabel = new Label();
        this.ratingLabel = new Label();
        this.loadingIndicator = new ProgressIndicator();

        setupLayout();
        setupEventHandlers();
    }

    public void setOnBackAction(Runnable action) {
        onBackAction.set(action);
    }

    public Runnable getOnBackAction() {
        return onBackAction.get();
    }

    private void setupLayout() {
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        HBox mainContainer = new HBox(40);
        mainContainer.setPadding(new Insets(40));
        mainContainer.setAlignment(Pos.TOP_LEFT);
        mainContainer.setMaxWidth(Double.MAX_VALUE);
        mainContainer.setMaxHeight(Double.MAX_VALUE);
        mainContainer.setPickOnBounds(false);

        VBox posterSection = createPosterSection();
        posterSection.setPrefWidth(300);
        posterSection.setMinWidth(300);
        posterSection.setMaxWidth(300);
        posterSection.setPickOnBounds(false);

        VBox detailsSection = createDetailsSection();
        HBox.setHgrow(detailsSection, Priority.ALWAYS);
        detailsSection.setMaxWidth(Double.MAX_VALUE);
        detailsSection.setPickOnBounds(false);

        mainContainer.getChildren().addAll(posterSection, detailsSection);

        HBox backButtonContainer = new HBox(backButton);
        backButtonContainer.setPadding(new Insets(20));
        backButtonContainer.setAlignment(Pos.BOTTOM_LEFT);
        backButton.getStyleClass().add("back-button");
        backButtonContainer.setPickOnBounds(false);

        StackPane rootStack = new StackPane();
        rootStack.getChildren().addAll(mainContainer, backButtonContainer);
        StackPane.setAlignment(backButtonContainer, Pos.BOTTOM_LEFT);
        rootStack.setPickOnBounds(false);

        StackPane loadingOverlay = new StackPane(loadingIndicator);
        loadingOverlay.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.7), null, null)));
        loadingOverlay.setVisible(false);
        loadingOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        loadingOverlay.setPickOnBounds(false);

        rootStack.getChildren().add(loadingOverlay);
        setCenter(rootStack);

        loadingIndicator.progressProperty().addListener((obs, oldVal, newVal) -> {
            boolean isLoading = newVal.doubleValue() > 0 && newVal.doubleValue() < 1;
            loadingOverlay.setVisible(isLoading);
            if (isLoading) {
                loadingOverlay.toFront();
            }
        });

        mainContainer.setOnMouseClicked(null);
        backButtonContainer.setOnMouseClicked(null);
        rootStack.setOnMouseClicked(null);
        loadingOverlay.setOnMouseClicked(null);
    }

    private VBox createPosterSection() {
        VBox posterSection = new VBox(20);
        posterSection.setAlignment(Pos.TOP_CENTER);

        StackPane posterContainer = new StackPane();
        posterContainer.setPrefSize(300, 450);
        posterContainer.setMaxSize(300, 450);
        posterContainer.setBackground(new Background(new BackgroundFill(
                Color.rgb(240, 240, 240), new CornerRadii(8), null)));
        posterContainer.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));

        posterView.setFitWidth(300);
        posterView.setFitHeight(450);
        posterView.setPreserveRatio(true);
        posterContainer.getChildren().add(posterView);

        watchlistBtn = new Button("Add to Watchlist");
        watchedBtn = new Button("Mark as Watched");
        watchlistBtn.getStyleClass().add("primary-button");
        watchedBtn.getStyleClass().add("secondary-button");
        watchlistBtn.setMaxWidth(Double.MAX_VALUE);
        watchedBtn.setMaxWidth(Double.MAX_VALUE);
        watchlistBtn.setDisable(false);
        watchedBtn.setDisable(true);

        watchlistControls = new VBox(10);
        watchlistControls.getChildren().addAll(watchlistBtn, watchedBtn);
        watchlistControls.setMaxWidth(Double.MAX_VALUE);
        watchlistControls.setVisible(true);

        posterSection.getChildren().addAll(posterContainer, watchlistControls);
        return posterSection;
    }

    private VBox createDetailsSection() {
        VBox detailsSection = new VBox(20);
        detailsSection.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(detailsSection, Priority.ALWAYS);

        VBox titleSection = new VBox(5);
        titleLabel.getStyleClass().add("movie-title");
        taglineLabel.getStyleClass().add("movie-tagline");
        titleSection.getChildren().addAll(titleLabel, taglineLabel);

        GridPane metadataGrid = new GridPane();
        metadataGrid.setHgap(40);
        metadataGrid.setVgap(15);
        metadataGrid.setPadding(new Insets(20, 0, 20, 0));
        metadataGrid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(120);
        labelColumn.setPrefWidth(150);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        metadataGrid.getColumnConstraints().addAll(labelColumn, valueColumn);

        addMetadataRow(metadataGrid, 0, "Original Title:", new Label());
        addMetadataRow(metadataGrid, 1, "Release Date:", releaseDateLabel);
        addMetadataRow(metadataGrid, 3, "Rating:", ratingLabel);

        VBox overviewSection = new VBox(10);
        overviewSection.setMaxWidth(Double.MAX_VALUE);
        overviewSection.getStyleClass().add("overview-section");

        Label overviewTitle = new Label("Overview");
        overviewTitle.getStyleClass().add("subtitle");

        overviewLabel.setWrapText(true);
        overviewLabel.getStyleClass().add("movie-description");
        overviewLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(overviewLabel, Priority.ALWAYS);

        overviewSection.getChildren().addAll(overviewTitle, overviewLabel);

        detailsSection.getChildren().addAll(titleSection, metadataGrid, overviewSection);
        return detailsSection;
    }

    private void addMetadataRow(GridPane grid, int row, String label, javafx.scene.Node value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("movie-info-label");

        GridPane.setHgrow(value, Priority.ALWAYS);

        grid.add(labelNode, 0, row);
        grid.add(value, 1, row);
    }

    private void setupEventHandlers() {
        backButton.setOnAction(e -> {
            e.consume();
            Runnable action = getOnBackAction();
            if (action != null) {
                try {
                    action.run();
                } catch (Exception ex) {
                    showError("Error going back: " + ex.getMessage());
                }
            }
        });

        setOnMouseClicked(e -> {
            if (e.getTarget() == this) {
                e.consume();
            }
        });

        watchlistBtn.setOnAction(e -> {
            e.consume();
            if (currentMovie == null) {
                showError("No movie selected");
                return;
            }

            if (currentEntry == null) {
                loadingIndicator.setProgress(-1);
                loadingIndicator.setVisible(true);
                watchlistService.addToWatchlist(user.getId(), currentMovie.getTmdbId())
                    .thenAccept(entry -> Platform.runLater(() -> {
                        currentEntry = entry;
                        updateWatchlistState(true);
                        loadingIndicator.setVisible(false);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showError("Failed to add movie to watchlist: " + ex.getMessage());
                            loadingIndicator.setVisible(false);
                        });
                        return null;
                    });
            } else {
                loadingIndicator.setProgress(-1);
                loadingIndicator.setVisible(true);
                watchlistService.removeFromWatchlist(user.getId(), currentMovie.getTmdbId())
                    .thenAccept(v -> Platform.runLater(() -> {
                        currentEntry = null;
                        updateWatchlistState(false);
                        loadingIndicator.setVisible(false);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showError("Failed to remove movie from watchlist: " + ex.getMessage());
                            loadingIndicator.setVisible(false);
                        });
                        return null;
                    });
            }
        });

        watchedBtn.setOnAction(e -> {
            if (currentEntry != null) {
                if (currentEntry.isWatched()) {
                    watchlistService.markAsUnwatched(user.getId(), currentMovie.getTmdbId())
                        .thenAccept(success -> Platform.runLater(() -> {
                            if (success) {
                                currentEntry.setWatched(false);
                                updateWatchedState(false);
                                loadingIndicator.setVisible(false);
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                showError("Failed to mark movie as unwatched: " + ex.getMessage());
                                loadingIndicator.setVisible(false);
                            });
                            return null;
                        });
                } else {
                    watchlistService.markAsWatched(user.getId(), currentMovie.getTmdbId())
                        .thenAccept(success -> Platform.runLater(() -> {
                            if (success) {
                                currentEntry.setWatched(true);
                                updateWatchedState(true);
                                loadingIndicator.setVisible(false);
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                showError("Failed to mark movie as watched: " + ex.getMessage());
                                loadingIndicator.setVisible(false);
                            });
                            return null;
                        });
                }
            }
        });
    }

    public void showMovie(TMDBMovie movie) {
        if (movie == null) {
            return;
        }

        currentMovie = movie;
        currentEntry = null;

        Platform.runLater(() -> {
            try {
                titleLabel.setText("");
                taglineLabel.setText("");
                overviewLabel.setText("");
                releaseDateLabel.setText("");
                ratingLabel.setText("");
                backdropView.setImage(null);
                posterView.setImage(null);

                watchlistBtn.setDisable(false);
                watchedBtn.setDisable(true);
                watchlistBtn.setText("Add to Watchlist");
                watchedBtn.setText("Mark as Watched");
                watchlistBtn.getStyleClass().clear();
                watchlistBtn.getStyleClass().add("primary-button");
                watchedBtn.getStyleClass().clear();
                watchedBtn.getStyleClass().add("secondary-button");
                watchlistControls.setVisible(true);

                loadingIndicator.setProgress(-1);
                loadingIndicator.setVisible(true);
                ((StackPane)getCenter()).getChildren().get(1).toFront();

                titleLabel.setText(movie.getTitle());
                taglineLabel.setText(movie.getTagline() != null ? movie.getTagline() : "");
                overviewLabel.setText(movie.getOverview() != null ? movie.getOverview() : "No overview available.");

                loadImages(movie);

                checkWatchlistStatus(movie);
            } catch (Exception e) {
                showError("Error displaying movie details: " + e.getMessage());
                loadingIndicator.setVisible(false);
            }
        });
    }

    private void loadImages(TMDBMovie movie) {
        if (movie.getPosterPath() != null) {
            String posterUrl = movie.getFullPosterUrl(imageBaseUrl, "500");
            Image posterImage = new Image(posterUrl, true);
            posterView.setImage(posterImage);
        }

        if (movie.getBackdropPath() != null) {
            String backdropUrl = movie.getFullBackdropUrl(imageBaseUrl, null);
            Image backdropImage = new Image(backdropUrl, true);
            backdropImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0) {
                    Platform.runLater(() -> {
                        try {
                            backdropView.setImage(backdropImage);
                            backdropView.setOpacity(0);
                            
                            javafx.animation.Timeline fadeIn = new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(Duration.ZERO, 
                                    new javafx.animation.KeyValue(backdropView.opacityProperty(), 0)),
                                new javafx.animation.KeyFrame(FADE_DURATION, 
                                    new javafx.animation.KeyValue(backdropView.opacityProperty(), 1))
                            );
                            fadeIn.setOnFinished(e -> loadingIndicator.setVisible(false));
                            fadeIn.play();
                        } catch (Exception e) {
                            loadingIndicator.setVisible(false);
                        }
                    });
                }
            });
        } else {
            loadingIndicator.setVisible(false);
        }
    }

    private void checkWatchlistStatus(TMDBMovie movie) {
        watchlistService.isInWatchlist(user.getId(), movie.getTmdbId())
            .thenAccept(inWatchlist -> {
                if (inWatchlist) {
                    watchlistService.isWatched(user.getId(), movie.getTmdbId())
                        .thenAccept(isWatched -> {
                            currentEntry = new WatchlistEntry(
                                0L, user.getId(), movie.getTmdbId(),
                                null, isWatched, null
                            );
                            
                            if (currentMovie.getRuntime() == null || currentMovie.getBudget() == null) {
                                tmdbService.getMovieDetails(currentMovie.getTmdbId())
                                    .thenAccept(fullMovieOpt -> {
                                        Platform.runLater(() -> {
                                            try {
                                                fullMovieOpt.ifPresent(fullMovie -> {
                                                    currentMovie = fullMovie;
                                                    updateFullMovieDetails();
                                                });
                                                updateWatchlistState(true);
                                            } catch (Exception e) {
                                                showError("Error updating UI with full movie details: " + e.getMessage());
                                            }
                                        });
                                    })
                                    .exceptionally(ex -> {
                                        Platform.runLater(() -> {
                                            showError("Failed to load full movie details: " + ex.getMessage());
                                            updateWatchlistState(true);
                                        });
                                        return null;
                                    });
                            } else {
                                Platform.runLater(() -> {
                                    try {
                                        updateFullMovieDetails();
                                        updateWatchlistState(true);
                                    } catch (Exception e) {
                                        showError("Error updating UI with cached movie details: " + e.getMessage());
                                    }
                                });
                            }
                        });
                } else {
                    if (currentMovie.getRuntime() == null || currentMovie.getBudget() == null) {
                        tmdbService.getMovieDetails(currentMovie.getTmdbId())
                            .thenAccept(fullMovieOpt -> {
                                Platform.runLater(() -> {
                                    try {
                                        fullMovieOpt.ifPresent(fullMovie -> {
                                            currentMovie = fullMovie;
                                            updateFullMovieDetails();
                                        });
                                        updateWatchlistState(false);
                                    } catch (Exception e) {
                                        showError("Error updating UI with full movie details: " + e.getMessage());
                                    }
                                });
                            })
                            .exceptionally(ex -> {
                                Platform.runLater(() -> {
                                    showError("Failed to load full movie details: " + ex.getMessage());
                                    updateWatchlistState(false);
                                });
                                return null;
                            });
                    } else {
                        Platform.runLater(() -> {
                            try {
                                updateFullMovieDetails();
                                updateWatchlistState(false);
                            } catch (Exception e) {
                                showError("Error updating UI with cached movie details: " + e.getMessage());
                            }
                        });
                    }
                }
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showError("Failed to check watchlist status: " + ex.getMessage());
                    updateWatchlistState(false);
                });
                return null;
            });
    }

    private void updateFullMovieDetails() {
        if (currentMovie == null) {
            return;
        }

        Platform.runLater(() -> {
            try {
                Label originalTitleLabel = (Label)getMetadataValue(0);
                if (originalTitleLabel != null) {
                    originalTitleLabel.setText(currentMovie.getOriginalTitle() != null ? 
                        currentMovie.getOriginalTitle() : "N/A");
                }
                
                if (releaseDateLabel != null) {
                    releaseDateLabel.setText(currentMovie.getFormattedReleaseDate() != null ? 
                        currentMovie.getFormattedReleaseDate() : "N/A");
                }
                
                Label genresLabel = (Label)getMetadataValue(2);
                if (genresLabel != null) {
                    genresLabel.setText(currentMovie.getGenres() != null && !currentMovie.getGenres().isEmpty() ? 
                        currentMovie.getGenres().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")) : "N/A");
                }
                
                if (ratingLabel != null) {
                    ratingLabel.setText(currentMovie.getFormattedVoteAverage() + 
                        (currentMovie.getVoteCount() != null && currentMovie.getVoteCount() > 0 ? 
                            " (" + currentMovie.getFormattedVoteCount() + " votes)" : ""));
                }

            } catch (Exception e) {
                showError("Error updating full movie details: " + e.getMessage());
            }
        });
    }

    private javafx.scene.Node getMetadataValue(int row) {
        StackPane rootStack = (StackPane)getCenter();
        if (rootStack == null || rootStack.getChildren().isEmpty()) {
            return new Label("N/A");
        }
        
        HBox mainContainer = (HBox)rootStack.getChildren().get(0);
        if (mainContainer == null || mainContainer.getChildren().size() <= 1) {
            return new Label("N/A");
        }
        
        VBox detailsSection = (VBox)mainContainer.getChildren().get(1);
        if (detailsSection == null || detailsSection.getChildren().size() <= 1) {
            return new Label("N/A");
        }
        
        GridPane grid = (GridPane)detailsSection.getChildren().get(1);
        if (grid == null || grid.getChildren().isEmpty()) {
            return new Label("N/A");
        }
        
        int index = row * 2 + 1;
        if (index >= grid.getChildren().size()) {
            return new Label("N/A");
        }
        
        return grid.getChildren().get(index);
    }

    private void updateWatchlistState(boolean inWatchlist) {
        Platform.runLater(() -> {
            watchlistControls.setVisible(true);
            watchlistBtn.setDisable(false);
            watchedBtn.setDisable(false);
            
            if (inWatchlist) {
                watchlistBtn.setText("Remove from Watchlist");
                watchlistBtn.getStyleClass().clear();
                watchlistBtn.getStyleClass().add("secondary-button");
                watchedBtn.setVisible(true);
                watchedBtn.setDisable(false);
            } else {
                watchlistBtn.setText("Add to Watchlist");
                watchlistBtn.getStyleClass().clear();
                watchlistBtn.getStyleClass().add("primary-button");
                watchedBtn.setVisible(false);
                watchedBtn.setDisable(true);
            }
        });
    }

    private void updateWatchedState(boolean watched) {
        Platform.runLater(() -> {
            watchedBtn.setDisable(false);
            if (watched) {
                watchedBtn.setText("Mark as Unwatched");
                watchedBtn.getStyleClass().clear();
                watchedBtn.getStyleClass().add("primary-button");
            } else {
                watchedBtn.setText("Mark as Watched");
                watchedBtn.getStyleClass().clear();
                watchedBtn.getStyleClass().add("secondary-button");
            }
        });
    }

    private void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
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
} 