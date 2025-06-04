package org.example.watchlater.repository;

import org.example.watchlater.model.TMDBMovie;
import java.util.Optional;

public interface MovieRepository {
    void save(TMDBMovie movie);
    Optional<TMDBMovie> findByTmdbId(Integer tmdbId);
    boolean isCachedAndNotExpired(Integer tmdbId, int maxAgeInHours);
    Optional<TMDBMovie> findById(Integer id);
} 