package org.example.watchlater.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBMovie {
    private static final Logger log = LoggerFactory.getLogger(TMDBMovie.class);

    @JsonIgnore
    private Long id;
    
    @JsonProperty("id")
    private Integer tmdbId;
    
    private String title;
    private String overview;
    
    @JsonProperty("poster_path")
    private String posterPath;
    
    @JsonProperty("backdrop_path")
    private String backdropPath;
    
    @JsonProperty("release_date")
    private LocalDate releaseDate;
    
    @JsonProperty("vote_average")
    private Double voteAverage;
    
    @JsonProperty("vote_count")
    private Integer voteCount;
    
    @JsonProperty("genre_ids")
    private List<Integer> genres;
    
    private Integer runtime;
    private String status;
    private String tagline;
    private Long budget;
    private Long revenue;
    private String homepage;
    
    @JsonProperty("imdb_id")
    private String imdbId;
    
    @JsonProperty("original_language")
    private String originalLanguage;
    
    @JsonProperty("original_title")
    private String originalTitle;
    
    private Double popularity;
    private Boolean adult;
    private Boolean video;
    
    @JsonIgnore
    private LocalDateTime cacheTimestamp;

    public TMDBMovie() {}

    // Gettery i settery
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public List<Integer> getGenres() {
        return genres;
    }

    public void setGenres(List<Integer> genres) {
        this.genres = genres;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public Long getBudget() {
        return budget;
    }

    public void setBudget(Long budget) {
        this.budget = budget;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }

    public Boolean getAdult() {
        return adult;
    }

    public void setAdult(Boolean adult) {
        this.adult = adult;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }


    public void setCacheTimestamp(LocalDateTime cacheTimestamp) {
        this.cacheTimestamp = cacheTimestamp;
    }

    public String getFullPosterUrl(String baseUrl, String size) {
        if (posterPath == null || posterPath.isEmpty()) {
            log.warn("Poster path is null or empty for movie: {}", title);
            return null;
        }

        String normalizedBaseUrl = baseUrl.endsWith("/t/p/") ? baseUrl : "https://image.tmdb.org/t/p/";

        String normalizedPath = posterPath.startsWith("/") ? posterPath : "/" + posterPath;

        if (normalizedPath.matches("/w\\d+/.*")) {
            normalizedPath = normalizedPath.substring(normalizedPath.indexOf("/", 1));
        }

        String normalizedSize = size.replaceAll("[^0-9]", "");
        if (normalizedSize.isEmpty()) {
            normalizedSize = "500"; // domyślny rozmiar
        }

        return normalizedBaseUrl + "w" + normalizedSize + normalizedPath;
    }
    
    public String getFullBackdropUrl(String baseUrl, String size) {
        if (backdropPath == null || backdropPath.isEmpty()) {
            return null;
        }

        String normalizedBaseUrl = baseUrl.endsWith("/t/p/") ? baseUrl : "https://image.tmdb.org/t/p/";

        String normalizedPath = backdropPath.startsWith("/") ? backdropPath : "/" + backdropPath;

        if (normalizedPath.matches("/w\\d+/.*")) {
            normalizedPath = normalizedPath.substring(normalizedPath.indexOf("/", 1));
        }

        String normalizedSize = "original";

        return normalizedBaseUrl + normalizedSize + normalizedPath;
    }
    
    public String getFormattedRuntime() {
        if (runtime == null || runtime <= 0) {
            return "N/A";
        }
        int hours = runtime / 60;
        int minutes = runtime % 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", minutes);
    }
    
    public String getFormattedBudget() {
        if (budget == null || budget <= 0) {
            return "N/A";
        }
        return String.format("$%,d", budget);
    }
    
    public String getFormattedRevenue() {
        if (revenue == null || revenue <= 0) {
            return "N/A";
        }
        return String.format("$%,d", revenue);
    }
    
    public String getFormattedReleaseDate() {
        if (releaseDate == null) {
            return "N/A";
        }
        return releaseDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }
    
    public String getFormattedVoteAverage() {
        if (voteAverage == null || voteAverage <= 0) {
            return "N/A";
        }
        return String.format("%.1f", voteAverage);
    }
    
    public String getFormattedVoteCount() {
        if (voteCount == null || voteCount <= 0) {
            return "N/A";
        }
        return String.format("%,d", voteCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TMDBMovie tmdbMovie = (TMDBMovie) o;
        return Objects.equals(tmdbId, tmdbMovie.tmdbId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tmdbId);
    }

    @Override
    public String toString() {
        return "TMDBMovie{" +
                "id=" + id +
                ", tmdbId=" + tmdbId +
                ", title='" + title + '\'' +
                ", releaseDate=" + releaseDate +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TMDBMovie movie;

        private Builder() {
            movie = new TMDBMovie();
        }

        public Builder id(Long id) {
            movie.setId(id);
            return this;
        }

        public Builder tmdbId(Integer tmdbId) {
            movie.setTmdbId(tmdbId);
            return this;
        }

        public Builder title(String title) {
            movie.setTitle(title);
            return this;
        }

        public Builder overview(String overview) {
            movie.setOverview(overview);
            return this;
        }

        public Builder posterPath(String posterPath) {
            movie.setPosterPath(posterPath);
            return this;
        }

        public Builder backdropPath(String backdropPath) {
            movie.setBackdropPath(backdropPath);
            return this;
        }

        public Builder releaseDate(LocalDate releaseDate) {
            movie.setReleaseDate(releaseDate);
            return this;
        }

        public Builder voteAverage(Double voteAverage) {
            movie.setVoteAverage(voteAverage);
            return this;
        }

        public Builder voteCount(Integer voteCount) {
            movie.setVoteCount(voteCount);
            return this;
        }

        public Builder genres(List<Integer> genres) {
            movie.setGenres(genres);
            return this;
        }

        public Builder runtime(Integer runtime) {
            movie.setRuntime(runtime);
            return this;
        }

        public Builder status(String status) {
            movie.setStatus(status);
            return this;
        }

        public Builder tagline(String tagline) {
            movie.setTagline(tagline);
            return this;
        }

        public Builder budget(Long budget) {
            movie.setBudget(budget);
            return this;
        }

        public Builder revenue(Long revenue) {
            movie.setRevenue(revenue);
            return this;
        }

        public Builder homepage(String homepage) {
            movie.setHomepage(homepage);
            return this;
        }

        public Builder imdbId(String imdbId) {
            movie.setImdbId(imdbId);
            return this;
        }

        public Builder originalLanguage(String originalLanguage) {
            movie.setOriginalLanguage(originalLanguage);
            return this;
        }

        public Builder originalTitle(String originalTitle) {
            movie.setOriginalTitle(originalTitle);
            return this;
        }

        public Builder popularity(Double popularity) {
            movie.setPopularity(popularity);
            return this;
        }

        public Builder adult(Boolean adult) {
            movie.setAdult(adult);
            return this;
        }

        public Builder video(Boolean video) {
            movie.setVideo(video);
            return this;
        }

        public Builder cacheTimestamp(LocalDateTime cacheTimestamp) {
            movie.setCacheTimestamp(cacheTimestamp);
            return this;
        }

        public TMDBMovie build() {
            return movie;
        }
    }
} 