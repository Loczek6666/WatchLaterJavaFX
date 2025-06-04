package org.example.watchlater.service;

import org.example.watchlater.model.WatchlistEntry;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncWatchlistService {
    CompletableFuture<WatchlistEntry> addToWatchlist(Long userId, Integer movieId);
    CompletableFuture<Void> removeFromWatchlist(Long userId, Integer movieId);
    CompletableFuture<List<WatchlistEntry>> getWatchlist(Long userId);
    CompletableFuture<Boolean> markAsWatched(Long userId, Integer movieId);
    CompletableFuture<Boolean> markAsUnwatched(Long userId, Integer movieId);
    CompletableFuture<Boolean> isInWatchlist(Long userId, Integer movieId);
    CompletableFuture<Boolean> isWatched(Long userId, Integer movieId);
    void shutdown();
} 