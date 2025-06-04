package org.example.watchlater.model;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class User {
    private Long id;
    private String googleId;
    private String email;
    private String displayName;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastLogin;
    private Preferences preferences;

    @Data
    @Builder
    public static class Preferences {
        private Long userId;
        private List<String> preferredGenres;
        private List<String> preferredLanguages;
        private boolean notificationEnabled;
        private String theme;
        private ZonedDateTime updatedAt;

        public static Preferences createDefault() {
            return Preferences.builder()
                .preferredGenres(List.of())
                .preferredLanguages(List.of())
                .notificationEnabled(true)
                .theme("light")
                .updatedAt(ZonedDateTime.now())
                .build();
        }
    }

    public List<TMDBMovie> getWatchlist() {
        return List.of();
    }
} 