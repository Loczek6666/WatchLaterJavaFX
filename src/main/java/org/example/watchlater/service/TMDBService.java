package org.example.watchlater.service;

import org.example.watchlater.model.TMDBMovie;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TMDBService {
    CompletableFuture<List<TMDBMovie>> searchMovies(String query);
    CompletableFuture<Optional<TMDBMovie>> getMovieDetails(int tmdbId);
    CompletableFuture<List<TMDBMovie>> getPopularMovies();
    String getImageBaseUrl();
} 