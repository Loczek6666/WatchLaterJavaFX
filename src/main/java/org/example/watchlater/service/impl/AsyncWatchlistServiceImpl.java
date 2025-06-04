package org.example.watchlater.service.impl;

import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.model.WatchlistEntry;
import org.example.watchlater.repository.MovieRepository;
import org.example.watchlater.repository.WatchlistRepository;
import org.example.watchlater.service.AsyncWatchlistService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;

public class AsyncWatchlistServiceImpl implements AsyncWatchlistService {
    private final WatchlistRepository watchlistRepository;
    private final MovieRepository movieRepository;
    private final ExecutorService executorService;

    public AsyncWatchlistServiceImpl(WatchlistRepository watchlistRepository, MovieRepository movieRepository) {
        this.watchlistRepository = watchlistRepository;
        this.movieRepository = movieRepository;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public CompletableFuture<WatchlistEntry> addToWatchlist(Long userId, Integer movieId) {
        return watchlistRepository.addToWatchlist(userId, movieId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to add to watchlist", ex);
            });
    }

    @Override
    public CompletableFuture<Void> removeFromWatchlist(Long userId, Integer movieId) {
        return watchlistRepository.removeFromWatchlist(userId, movieId)
            .thenAccept(result -> {
                if (!result) {
                    Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
                    return;
                }
            })
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to remove from watchlist", ex);
            });
    }

    @Override
    public CompletableFuture<List<WatchlistEntry>> getWatchlist(Long userId) {
        return watchlistRepository.getWatchlist(userId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to get watchlist", ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> markAsWatched(Long userId, Integer movieId) {
        return watchlistRepository.markAsWatched(userId, movieId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to mark movie as watched", ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> markAsUnwatched(Long userId, Integer movieId) {
        return watchlistRepository.markAsUnwatched(userId, movieId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to mark movie as unwatched", ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> isInWatchlist(Long userId, Integer movieId) {
        return watchlistRepository.isInWatchlist(userId, movieId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to check if movie is in watchlist", ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> isWatched(Long userId, Integer movieId) {
        return watchlistRepository.isWatched(userId, movieId)
            .exceptionally(ex -> {
                throw new RuntimeException("Failed to check if movie is watched", ex);
            });
    }


    @Override
    public void shutdown() {
        executorService.shutdown();
    }
} 