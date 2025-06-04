package org.example.watchlater.repository.impl;

import org.example.watchlater.config.DatabaseConfig;
import org.example.watchlater.model.TMDBMovie;
import org.example.watchlater.model.User;
import org.example.watchlater.model.WatchlistEntry;
import org.example.watchlater.repository.WatchlistRepository;
import org.example.watchlater.repository.MovieRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

public class WatchlistRepositoryImpl implements WatchlistRepository {
    private static final Logger logger = Logger.getLogger(WatchlistRepositoryImpl.class.getName());
    private final DatabaseConfig dbConfig;
    private final MovieRepository movieRepository;
    
    public WatchlistRepositoryImpl(DatabaseConfig dbConfig, MovieRepository movieRepository) {
        this.dbConfig = dbConfig;
        this.movieRepository = movieRepository;
    }
    
    private <T> T executeQuery(String operation, SQLFunction<T> queryFunction) {
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryFunction.getSql())) {
            
            queryFunction.setParameters(stmt);
            return queryFunction.execute(stmt);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error " + operation, e);
            throw new RuntimeException("Failed to " + operation, e);
        }
    }

    private interface SQLFunction<T> {
        String getSql();
        void setParameters(PreparedStatement stmt) throws SQLException;
        T execute(PreparedStatement stmt) throws SQLException;
    }

    @Override
    public CompletableFuture<WatchlistEntry> addToWatchlist(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                throw new RuntimeException("Movie not found in database");
            }
            
            return executeQuery("adding movie to watchlist", new SQLFunction<WatchlistEntry>() {
                @Override
                public String getSql() {
                    return "INSERT INTO watchlist (user_id, movie_id, watched, added_at) " +
                           "VALUES (?, ?, false, CURRENT_TIMESTAMP) " +
                           "RETURNING id, user_id, movie_id, watched, added_at, watched_at";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public WatchlistEntry execute(PreparedStatement stmt) throws SQLException {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToEntry(rs);
                        }
                        throw new SQLException("Failed to add movie to watchlist");
                    }
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> removeFromWatchlist(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                return false;
            }
            
            return executeQuery("removing movie from watchlist", new SQLFunction<Boolean>() {
                @Override
                public String getSql() {
                    return "DELETE FROM watchlist WHERE user_id = ? AND movie_id = ?";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public Boolean execute(PreparedStatement stmt) throws SQLException {
                    return stmt.executeUpdate() > 0;
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> markAsWatched(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                return false;
            }
            
            return executeQuery("marking movie as watched", new SQLFunction<Boolean>() {
                @Override
                public String getSql() {
                    return "UPDATE watchlist SET watched = true, watched_at = CURRENT_TIMESTAMP " +
                           "WHERE user_id = ? AND movie_id = ?";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public Boolean execute(PreparedStatement stmt) throws SQLException {
                    return stmt.executeUpdate() > 0;
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> markAsUnwatched(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                return false;
            }
            
            return executeQuery("marking movie as unwatched", new SQLFunction<Boolean>() {
                @Override
                public String getSql() {
                    return "UPDATE watchlist SET watched = false, watched_at = NULL " +
                           "WHERE user_id = ? AND movie_id = ?";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public Boolean execute(PreparedStatement stmt) throws SQLException {
                    return stmt.executeUpdate() > 0;
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<List<WatchlistEntry>> getWatchlist(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            return executeQuery("getting watchlist", new SQLFunction<List<WatchlistEntry>>() {
                @Override
                public String getSql() {
                    return "SELECT id, user_id, movie_id, watched, added_at, watched_at " +
                           "FROM watchlist WHERE user_id = ? ORDER BY added_at DESC";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                }

                @Override
                public List<WatchlistEntry> execute(PreparedStatement stmt) throws SQLException {
                    List<WatchlistEntry> entries = new ArrayList<>();
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            entries.add(mapResultSetToEntry(rs));
                        }
                    }
                    return entries;
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isInWatchlist(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                return false;
            }
            
            return executeQuery("checking if movie is in watchlist", new SQLFunction<Boolean>() {
                @Override
                public String getSql() {
                    return "SELECT EXISTS(SELECT 1 FROM watchlist WHERE user_id = ? AND movie_id = ?)";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public Boolean execute(PreparedStatement stmt) throws SQLException {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() && rs.getBoolean(1);
                    }
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isWatched(Long userId, Integer movieId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<TMDBMovie> movie = movieRepository.findByTmdbId(movieId);
            if (movie.isEmpty()) {
                return false;
            }
            
            return executeQuery("checking if movie is watched", new SQLFunction<Boolean>() {
                @Override
                public String getSql() {
                    return "SELECT watched FROM watchlist WHERE user_id = ? AND movie_id = ?";
                }

                @Override
                public void setParameters(PreparedStatement stmt) throws SQLException {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, movie.get().getId().intValue());
                }

                @Override
                public Boolean execute(PreparedStatement stmt) throws SQLException {
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next() && rs.getBoolean("watched");
                    }
                }
            });
        });
    }
    
    private WatchlistEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        return new WatchlistEntry(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getInt("movie_id"),
            rs.getTimestamp("added_at").toLocalDateTime(),
            rs.getBoolean("watched"),
            rs.getTimestamp("watched_at") != null ? rs.getTimestamp("watched_at").toLocalDateTime() : null
        );
    }
} 