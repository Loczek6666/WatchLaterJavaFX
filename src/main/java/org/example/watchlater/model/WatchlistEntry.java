package org.example.watchlater.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class WatchlistEntry {
    private final LongProperty id;
    private final LongProperty userId;
    private final IntegerProperty movieId;
    private final ObjectProperty<LocalDateTime> addedAt;
    private final BooleanProperty watched;
    private final ObjectProperty<LocalDateTime> watchedAt;

    public WatchlistEntry(Long id, Long userId, Integer movieId, LocalDateTime addedAt,
                         boolean watched, LocalDateTime watchedAt) {
        this.id = new SimpleLongProperty(id);
        this.userId = new SimpleLongProperty(userId);
        this.movieId = new SimpleIntegerProperty(movieId);
        this.addedAt = new SimpleObjectProperty<>(addedAt);
        this.watched = new SimpleBooleanProperty(watched);
        this.watchedAt = new SimpleObjectProperty<>(watchedAt);
    }

    public long getId() { return id.get(); }
    public LongProperty idProperty() { return id; }
    public void setId(long id) { this.id.set(id); }

    public long getUserId() { return userId.get(); }
    public LongProperty userIdProperty() { return userId; }
    public void setUserId(long userId) { this.userId.set(userId); }

    public int getMovieId() { return movieId.get(); }
    public IntegerProperty movieIdProperty() { return movieId; }
    public void setMovieId(int movieId) { this.movieId.set(movieId); }

    public LocalDateTime getAddedAt() { return addedAt.get(); }
    public ObjectProperty<LocalDateTime> addedAtProperty() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt.set(addedAt); }

    public boolean isWatched() { return watched.get(); }
    public BooleanProperty watchedProperty() { return watched; }
    public void setWatched(boolean watched) { this.watched.set(watched); }

    public LocalDateTime getWatchedAt() { return watchedAt.get(); }
    public ObjectProperty<LocalDateTime> watchedAtProperty() { return watchedAt; }
    public void setWatchedAt(LocalDateTime watchedAt) { this.watchedAt.set(watchedAt); }
} 