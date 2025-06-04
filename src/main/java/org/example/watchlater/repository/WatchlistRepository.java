package org.example.watchlater.repository;

import org.example.watchlater.model.WatchlistEntry;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface WatchlistRepository {
    CompletableFuture<WatchlistEntry> addToWatchlist(Long userId, Integer movieId);
    CompletableFuture<Boolean> removeFromWatchlist(Long userId, Integer movieId);
    CompletableFuture<Boolean> markAsWatched(Long userId, Integer movieId);
    CompletableFuture<Boolean> markAsUnwatched(Long userId, Integer movieId);
    CompletableFuture<List<WatchlistEntry>> getWatchlist(Long userId);
    CompletableFuture<Boolean> isInWatchlist(Long userId, Integer movieId);
    CompletableFuture<Boolean> isWatched(Long userId, Integer movieId);
} 