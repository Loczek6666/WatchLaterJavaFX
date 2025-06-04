package org.example.watchlater.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.watchlater.model.TMDBMovie;
import java.util.logging.Logger;

public class MovieCard extends VBox {
    private static final Logger logger = Logger.getLogger(MovieCard.class.getName());

    public static class MovieCardEvent extends javafx.event.Event {
        public static final javafx.event.EventType<MovieCardEvent> MOVIE_SELECTED = 
            new javafx.event.EventType<>(javafx.event.Event.ANY, "MOVIE_SELECTED");
        public static final javafx.event.EventType<MovieCardEvent> ADD_TO_WATCHLIST = 
            new javafx.event.EventType<>(javafx.event.Event.ANY, "ADD_TO_WATCHLIST");
        public static final javafx.event.EventType<MovieCardEvent> REMOVE_FROM_WATCHLIST = 
            new javafx.event.EventType<>(javafx.event.Event.ANY, "REMOVE_FROM_WATCHLIST");
        public static final javafx.event.EventType<MovieCardEvent> MARK_AS_WATCHED = 
            new javafx.event.EventType<>(javafx.event.Event.ANY, "MARK_AS_WATCHED");
        public static final javafx.event.EventType<MovieCardEvent> MARK_AS_UNWATCHED = 
            new javafx.event.EventType<>(javafx.event.Event.ANY, "MARK_AS_UNWATCHED");

        private final TMDBMovie movie;

        public MovieCardEvent(javafx.event.EventType<MovieCardEvent> eventType, TMDBMovie movie) {
            super(eventType);
            this.movie = movie;
        }

        public TMDBMovie getMovie() {
            return movie;
        }
    }

    private final TMDBMovie movie;
    private final ImageView posterView;
    private final Label titleLabel;
    private final Label yearLabel;
    private final Label ratingLabel;
    private final Button watchlistBtn;
    private final Button watchedBtn;
    private boolean inWatchlist;
    private boolean watched;

    public MovieCard(TMDBMovie movie, String imageBaseUrl) {
        this.movie = movie;
        this.posterView = new ImageView();
        this.titleLabel = new Label();
        this.yearLabel = new Label();
        this.ratingLabel = new Label();
        this.watchlistBtn = new Button("Add to Watchlist");
        this.watchedBtn = new Button("Mark as Watched");
        this.inWatchlist = false;
        this.watched = false;

        setupLayout();
        setupEventHandlers();
        loadMovieData(imageBaseUrl);
    }

    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(10));
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        setEffect(new javafx.scene.effect.DropShadow(5, Color.rgb(0, 0, 0, 0.2)));
        setMaxWidth(200);
        setMinWidth(200);

        posterView.setFitWidth(180);
        posterView.setFitHeight(270);
        posterView.setPreserveRatio(true);
        StackPane posterContainer = new StackPane(posterView);
        posterContainer.setBackground(new Background(new BackgroundFill(Color.rgb(240, 240, 240), null, null)));
        posterContainer.setPrefSize(180, 270);

        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);

        yearLabel.setFont(Font.font("System", 12));
        yearLabel.setTextFill(Color.GRAY);

        ratingLabel.setFont(Font.font("System", 12));
        ratingLabel.setTextFill(Color.rgb(255, 165, 0));

        VBox buttonBox = new VBox(5);
        buttonBox.setAlignment(Pos.CENTER);
        watchlistBtn.setMaxWidth(Double.MAX_VALUE);
        watchedBtn.setMaxWidth(Double.MAX_VALUE);
        watchedBtn.setVisible(false);
        buttonBox.getChildren().addAll(watchlistBtn, watchedBtn);

        getChildren().addAll(posterContainer, titleLabel, yearLabel, ratingLabel, buttonBox);
    }

    private void setupEventHandlers() {
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                fireEvent(new MovieCardEvent(MovieCardEvent.MOVIE_SELECTED, movie));
            }
        });

        watchlistBtn.setOnAction(e -> {
            if (inWatchlist) {
                fireEvent(new MovieCardEvent(MovieCardEvent.REMOVE_FROM_WATCHLIST, movie));
            } else {
                fireEvent(new MovieCardEvent(MovieCardEvent.ADD_TO_WATCHLIST, movie));
            }
        });

        watchedBtn.setOnAction(e -> {
            if (watched) {
                fireEvent(new MovieCardEvent(MovieCardEvent.MARK_AS_UNWATCHED, movie));
            } else {
                fireEvent(new MovieCardEvent(MovieCardEvent.MARK_AS_WATCHED, movie));
            }
        });
    }

    private void loadMovieData(String imageBaseUrl) {
        titleLabel.setText(movie.getTitle());
        yearLabel.setText(String.valueOf(movie.getReleaseDate().getYear()));
        ratingLabel.setText(movie.getFormattedVoteAverage());

        if (movie.getPosterPath() != null) {
            String posterUrl = movie.getFullPosterUrl(imageBaseUrl, "500");
            Image posterImage = new Image(posterUrl, true);
            posterView.setImage(posterImage);
        }
    }

    public TMDBMovie getMovie() {
        return movie;
    }

    public void setInWatchlist(boolean inWatchlist) {
        if (this.inWatchlist != inWatchlist) {
            this.inWatchlist = inWatchlist;
            Platform.runLater(() -> {
                watchlistBtn.setText(inWatchlist ? "Remove from Watchlist" : "Add to Watchlist");
                watchlistBtn.getStyleClass().clear();
                watchlistBtn.getStyleClass().add(inWatchlist ? "secondary-button" : "primary-button");
                watchedBtn.setVisible(inWatchlist);
            });
        }
    }

    public void setWatched(boolean watched) {
        if (this.watched != watched) {
            this.watched = watched;
            Platform.runLater(() -> {
                watchedBtn.setText(watched ? "Mark as Unwatched" : "Mark as Watched");
                watchedBtn.getStyleClass().clear();
                watchedBtn.getStyleClass().add(watched ? "primary-button" : "secondary-button");
            });
        }
    }
} 