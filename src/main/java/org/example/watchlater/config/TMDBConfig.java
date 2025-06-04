package org.example.watchlater.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TMDBConfig {
    private static final Logger logger = Logger.getLogger(TMDBConfig.class.getName());
    private static TMDBConfig instance;
    private final String apiKey;
    private final String baseUrl;
    private final String imageBaseUrl;
    
    private TMDBConfig() {
        Properties envProps = new Properties();
        try {
            envProps.load(new FileInputStream(".env"));
            
            this.apiKey = envProps.getProperty("TMDB_API_KEY");
            this.baseUrl = envProps.getProperty("TMDB_BASE_URL", "https://api.themoviedb.org/3");
            String rawImageBaseUrl = envProps.getProperty("TMDB_IMAGE_BASE_URL", "https://image.tmdb.org/t/p/");
            this.imageBaseUrl = rawImageBaseUrl.endsWith("/t/p/") ? rawImageBaseUrl : "https://image.tmdb.org/t/p/";
            
            if (apiKey == null) {
                throw new RuntimeException("Missing TMDB_API_KEY in .env file");
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TMDB configuration", e);
        }
    }
    
    public static synchronized TMDBConfig getInstance() {
        if (instance == null) {
            instance = new TMDBConfig();
        }
        return instance;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getImageBaseUrl() {
        return imageBaseUrl;
    }
} 