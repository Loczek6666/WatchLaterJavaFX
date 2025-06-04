package org.example.watchlater.config;

import org.example.watchlater.model.User;
import java.time.ZonedDateTime;

public class AppConfig {
    private static final User DEFAULT_USER = User.builder()
        .id(1L)
        .email("default@example.com")
        .displayName("Default User")
        .createdAt(ZonedDateTime.now())
        .lastLogin(ZonedDateTime.now())
        .preferences(User.Preferences.createDefault())
        .build();
    
    private AppConfig() {}
    
    public static User getDefaultUser() {
        return DEFAULT_USER;
    }
} 