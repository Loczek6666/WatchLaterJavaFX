package org.example.watchlater.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.watchlater.config.TMDBConfig;
import org.example.watchlater.model.TMDBMovie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TMDBClient {
    private static final Logger logger = Logger.getLogger(TMDBClient.class.getName());
    private final TMDBConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public TMDBClient() {
        this.config = TMDBConfig.getInstance();
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public String getImageBaseUrl() {
        return config.getImageBaseUrl();
    }
    
    public List<TMDBMovie> searchMovies(String query) {
        String url = String.format("%s/search/movie?api_key=%s&query=%s",
                config.getBaseUrl(), config.getApiKey(), query);

        List<TMDBMovie> movies = fetchMovieList(url);

        return movies;
    }
    
    public Optional<TMDBMovie> getMovieDetails(int tmdbId) {
        String url = String.format("%s/movie/%d?api_key=%s",
                config.getBaseUrl(), tmdbId, config.getApiKey());
        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.log(Level.SEVERE, "Failed to fetch movie details: {0}", response.code());
                    return Optional.empty();
                }
                
                String responseBody = response.body().string();
                TMDBMovie movie = objectMapper.readValue(responseBody, TMDBMovie.class);
                return Optional.of(movie);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error fetching movie details", e);
            return Optional.empty();
        }
    }
    
    public List<TMDBMovie> getPopularMovies() {
        String url = String.format("%s/movie/popular?api_key=%s",
                config.getBaseUrl(), config.getApiKey());
        List<TMDBMovie> movies = fetchMovieList(url);
        return movies;
    }
    
    public List<TMDBMovie> fetchMovieList(String url) {
        List<TMDBMovie> movies = new ArrayList<>();
        try {
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.log(Level.SEVERE, "Failed to fetch movies: HTTP {0} from {1}",
                        new Object[]{response.code(), url});
                    return movies;
                }
                
                String responseBody = response.body().string();
                
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode results = root.get("results");
                
                if (results.isArray()) {
                    for (JsonNode movieNode : results) {
                        
                        TMDBMovie movie = objectMapper.treeToValue(movieNode, TMDBMovie.class);
                        movies.add(movie);
                    }
                } else {
                    logger.log(Level.WARNING, "No 'results' array found in API response");
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error fetching movies from URL: " + url, e);
        }
        return movies;
    }
} 