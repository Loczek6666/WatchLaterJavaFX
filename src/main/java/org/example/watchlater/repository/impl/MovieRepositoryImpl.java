package org.example.watchlater.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.watchlater.config.DatabaseConfig;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.repository.MovieRepository;
import org.example.watchlater.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MovieRepositoryImpl implements MovieRepository {
    private final DatabaseConfig dbConfig;
    private final User user;
    
    public MovieRepositoryImpl(User user) {
        this.dbConfig = DatabaseConfig.getInstance();
        this.user = user;
    }
    
    @Override
    public void save(TMDBMovie movie) {

        String sql = "INSERT INTO movie_cache (" +
                    "tmdb_id, title, overview, poster_path, backdrop_path, " +
                    "release_date, vote_average, vote_count, " +
                    "original_language, original_title, popularity, adult, video, " +
                    "cache_timestamp" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (tmdb_id) DO UPDATE SET " +
                    "title = EXCLUDED.title, " +
                    "overview = EXCLUDED.overview, " +
                    "poster_path = EXCLUDED.poster_path, " +
                    "backdrop_path = EXCLUDED.backdrop_path, " +
                    "release_date = EXCLUDED.release_date, " +
                    "vote_average = EXCLUDED.vote_average, " +
                    "vote_count = EXCLUDED.vote_count, " +
                    "original_language = EXCLUDED.original_language, " +
                    "original_title = EXCLUDED.original_title, " +
                    "popularity = EXCLUDED.popularity, " +
                    "adult = EXCLUDED.adult, " +
                    "video = EXCLUDED.video, " +
                    "cache_timestamp = CURRENT_TIMESTAMP " +
                    "RETURNING *";
                    
        try (Connection conn = dbConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, movie.getTmdbId());
                stmt.setString(2, movie.getTitle());
                stmt.setString(3, movie.getOverview());
                stmt.setString(4, movie.getPosterPath());
                stmt.setString(5, movie.getBackdropPath());
                stmt.setDate(6, movie.getReleaseDate() != null ? 
                    Date.valueOf(movie.getReleaseDate()) : null);
                stmt.setDouble(7, movie.getVoteAverage());
                stmt.setInt(8, movie.getVoteCount());
                stmt.setString(9, movie.getOriginalLanguage());
                stmt.setString(10, movie.getOriginalTitle());
                stmt.setDouble(11, movie.getPopularity());
                stmt.setBoolean(12, movie.getAdult());
                stmt.setBoolean(13, movie.getVideo());
                stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        TMDBMovie savedMovie = mapMovie(rs);
                    } else {
                        throw new RuntimeException("Failed to save movie - no rows returned");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save movie", e);
        }
    }
    
    @Override
    public Optional<TMDBMovie> findByTmdbId(Integer tmdbId) {
        String sql = "SELECT * FROM movie_cache WHERE tmdb_id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tmdbId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMovie(rs));
                }
            }
        } catch (SQLException e) {
        }
        return Optional.empty();
    }
    
    @Override
    public boolean isCachedAndNotExpired(Integer tmdbId, int maxAgeInHours) {
        String sql = "SELECT EXISTS(SELECT 1 FROM movie_cache " +
                    "WHERE tmdb_id = ? AND cache_timestamp > ?)";
                    
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tmdbId);
            stmt.setTimestamp(2, Timestamp.valueOf(
                LocalDateTime.now().minusHours(maxAgeInHours)));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            log.error("Error checking movie cache: {}", tmdbId, e);
        }
        return false;
    }
    
    @Override
    public Optional<TMDBMovie> findById(Integer id) {
        String sql = "SELECT * FROM movie_cache WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMovie(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding movie by id: {}", id, e);
        }
        return Optional.empty();
    }
    
    private TMDBMovie mapMovie(ResultSet rs) throws SQLException {
        TMDBMovie movie = TMDBMovie.builder()
            .id(rs.getLong("id"))
            .tmdbId(rs.getInt("tmdb_id"))
            .title(rs.getString("title"))
            .overview(rs.getString("overview"))
            .posterPath(rs.getString("poster_path"))
            .backdropPath(rs.getString("backdrop_path"))
            .releaseDate(rs.getDate("release_date") != null ? 
                rs.getDate("release_date").toLocalDate() : null)
            .voteAverage(rs.getDouble("vote_average"))
            .voteCount(rs.getInt("vote_count"))
            .originalLanguage(rs.getString("original_language"))
            .originalTitle(rs.getString("original_title"))
            .popularity(rs.getDouble("popularity"))
            .adult(rs.getBoolean("adult"))
            .video(rs.getBoolean("video"))
            .build();
            
        Array genresArray = rs.getArray("genres");
        if (genresArray != null) {
            Integer[] genres = (Integer[]) genresArray.getArray();
            movie.setGenres(List.of(genres));
        }
        
        return movie;
    }
} 