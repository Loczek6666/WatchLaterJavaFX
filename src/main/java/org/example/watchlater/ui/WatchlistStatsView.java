package org.example.watchlater.ui;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.example.watchlater.model.User;
import org.example.watchlater.model.WatchlistEntry;
import org.example.watchlater.service.AsyncWatchlistService;

import java.time.format.DateTimeFormatter;

public class WatchlistStatsView extends VBox {
    private final User user;
    private final AsyncWatchlistService watchlistService;
    private final ProgressIndicator loadingIndicator;
    private final Label statusLabel;
    private final PieChart watchedChart;
    private final Label totalMoviesLabel;
    private final Label watchedMoviesLabel;
    private final Label unwatchedMoviesLabel;
    private final Label lastAddedLabel;
    private final Label lastWatchedLabel;
    private final Button backButton;

    public WatchlistStatsView(User user, AsyncWatchlistService watchlistService) {
        this.user = user;
        this.watchlistService = watchlistService;
        this.loadingIndicator = new ProgressIndicator();
        this.statusLabel = new Label("Loading statistics...");
        this.watchedChart = new PieChart();
        this.totalMoviesLabel = new Label();
        this.watchedMoviesLabel = new Label();
        this.unwatchedMoviesLabel = new Label();
        this.lastAddedLabel = new Label();
        this.lastWatchedLabel = new Label();
        this.backButton = new Button("← Back");

        setupLayout();
        setupEventHandlers();
        loadStats();
    }

    private void setupLayout() {
        setSpacing(20);
        setPadding(new Insets(20));
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 20, 0));
        topBar.getChildren().add(backButton);

        Label titleLabel = new Label("Watchlist Statistics");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(20));
        statsGrid.setBackground(new Background(new BackgroundFill(Color.rgb(245, 245, 245), new CornerRadii(10), null)));

        String statLabelStyle = "-fx-font-size: 14px; -fx-text-fill: #666666;";
        String statValueStyle = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;";

        Label totalLabel = new Label("Total Movies");
        totalLabel.setStyle(statLabelStyle);
        totalMoviesLabel.setStyle(statValueStyle);
        statsGrid.add(totalLabel, 0, 0);
        statsGrid.add(totalMoviesLabel, 0, 1);

        Label watchedLabel = new Label("Watched Movies");
        watchedLabel.setStyle(statLabelStyle);
        watchedMoviesLabel.setStyle(statValueStyle);
        statsGrid.add(watchedLabel, 1, 0);
        statsGrid.add(watchedMoviesLabel, 1, 1);

        Label unwatchedLabel = new Label("Unwatched Movies");
        unwatchedLabel.setStyle(statLabelStyle);
        unwatchedMoviesLabel.setStyle(statValueStyle);
        statsGrid.add(unwatchedLabel, 2, 0);
        statsGrid.add(unwatchedMoviesLabel, 2, 1);

        Label lastAddedTitle = new Label("Last Added");
        lastAddedTitle.setStyle(statLabelStyle);
        lastAddedLabel.setStyle(statValueStyle);
        statsGrid.add(lastAddedTitle, 0, 2);
        statsGrid.add(lastAddedLabel, 0, 3);

        Label lastWatchedTitle = new Label("Last Watched");
        lastWatchedTitle.setStyle(statLabelStyle);
        lastWatchedLabel.setStyle(statValueStyle);
        statsGrid.add(lastWatchedTitle, 1, 2);
        statsGrid.add(lastWatchedLabel, 1, 3);

        watchedChart.setTitle("Watch Status");
        watchedChart.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        watchedChart.setLabelsVisible(true);
        watchedChart.setLegendVisible(true);
        watchedChart.setStartAngle(90);
        watchedChart.setClockwise(true);
        watchedChart.setPrefSize(400, 400);

        VBox chartContainer = new VBox(10);
        chartContainer.setAlignment(Pos.CENTER);
        chartContainer.setPadding(new Insets(20));
        chartContainer.setBackground(new Background(new BackgroundFill(Color.rgb(245, 245, 245), new CornerRadii(10), null)));
        chartContainer.getChildren().add(watchedChart);

        StackPane loadingContainer = new StackPane(loadingIndicator, statusLabel);
        loadingIndicator.setMaxSize(50, 50);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        getChildren().addAll(
            topBar,
            titleLabel,
            statsGrid,
            chartContainer,
            loadingContainer
        );

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        ColumnConstraints col3 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col2.setHgrow(Priority.ALWAYS);
        col3.setHgrow(Priority.ALWAYS);
        statsGrid.getColumnConstraints().addAll(col1, col2, col3);
    }

    private void setupEventHandlers() {
        backButton.setOnAction(e -> {
            Runnable action = getOnBackAction();
            if (action != null) {
                try {
                    action.run();
                } catch (Exception ex) {
                    showError("Error going back: " + ex.getMessage());
                }
            }
        });
    }

    private void loadStats() {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Loading statistics...");

        watchlistService.getWatchlist(user.getId())
            .thenAccept(entries -> {
                Platform.runLater(() -> {
                    int totalMovies = entries.size();
                    int watchedMovies = (int) entries.stream()
                        .filter(WatchlistEntry::isWatched)
                        .count();
                    int unwatchedMovies = totalMovies - watchedMovies;

                    totalMoviesLabel.setText(String.valueOf(totalMovies));
                    watchedMoviesLabel.setText(String.valueOf(watchedMovies));
                    unwatchedMoviesLabel.setText(String.valueOf(unwatchedMovies));
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

                    entries.stream()
                        .map(WatchlistEntry::getAddedAt)
                        .max(java.time.LocalDateTime::compareTo)
                        .ifPresentOrElse(
                            date -> lastAddedLabel.setText(date.format(formatter)),
                            () -> lastAddedLabel.setText("Never")
                        );

                    entries.stream()
                        .filter(WatchlistEntry::isWatched)
                        .map(WatchlistEntry::getWatchedAt)
                        .filter(java.time.LocalDateTime.class::isInstance)
                        .max(java.time.LocalDateTime::compareTo)
                        .ifPresentOrElse(
                            date -> lastWatchedLabel.setText(date.format(formatter)),
                            () -> lastWatchedLabel.setText("Never")
                        );

                    watchedChart.getData().clear();
                    PieChart.Data watchedData = new PieChart.Data("Watched", watchedMovies);
                    PieChart.Data unwatchedData = new PieChart.Data("Unwatched", unwatchedMovies);

                    
                    watchedChart.getData().addAll(watchedData, unwatchedData);

                    loadingIndicator.setVisible(false);
                    statusLabel.setText("");
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading statistics. Please try again.");
                    loadingIndicator.setVisible(false);
                });
                return null;
            });
    }

    private final ObjectProperty<Runnable> onBackAction = new SimpleObjectProperty<>();

    public void setOnBackAction(Runnable action) {
        onBackAction.set(action);
    }

    public Runnable getOnBackAction() {
        return onBackAction.get();
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

    public void refresh() {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Refreshing statistics...");
        loadStats();
    }
} 