package org.example.watchlater.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.repository.MovieRepository;
import org.example.watchlater.service.TMDBService;
import org.example.watchlater.util.TMDBClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class TMDBServiceImpl implements TMDBService {
    private final TMDBClient tmdbClient;
    private final MovieRepository movieRepository;
    private final String imageBaseUrl;
    
    public TMDBServiceImpl(TMDBClient tmdbClient, MovieRepository movieRepository) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.imageBaseUrl = tmdbClient.getImageBaseUrl();
    }
    
    @Override
    public CompletableFuture<List<TMDBMovie>> searchMovies(String query) {
        return CompletableFuture.supplyAsync(() -> {
            List<TMDBMovie> movies = tmdbClient.searchMovies(query);
            for (TMDBMovie movie : movies) {
                movieRepository.save(movie);
            }
            return movies;
        });
    }
    
    @Override
    public CompletableFuture<Optional<TMDBMovie>> getMovieDetails(int tmdbId) {
        return CompletableFuture.supplyAsync(() -> {
            if (movieRepository.isCachedAndNotExpired(tmdbId, 24)) {
                return movieRepository.findByTmdbId(tmdbId);
            }
            
            Optional<TMDBMovie> movieOpt = tmdbClient.getMovieDetails(tmdbId);
            movieOpt.ifPresent(movieRepository::save);
            return movieOpt;
        });
    }
    
    @Override
    public String getImageBaseUrl() {
        return imageBaseUrl;
    }
    
    @Override
    public CompletableFuture<List<TMDBMovie>> getPopularMovies() {
        return CompletableFuture.supplyAsync(() -> {
            List<TMDBMovie> movies = tmdbClient.getPopularMovies();
            for (TMDBMovie movie : movies) {
                movieRepository.save(movie);
            }
            return movies;
        });
    }
} 