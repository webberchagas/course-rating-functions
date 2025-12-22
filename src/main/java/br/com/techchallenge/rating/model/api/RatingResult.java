package br.com.techchallenge.rating.model.api;


public class RatingResult {
    private String id;
    private Integer rating;
    private String description;
    private String email;
    private Boolean critical;
    private String createdAt;

    public RatingResult() {
    }

    public RatingResult(String id, Integer rating, String description, String email, Boolean critical, String createdAt) {
        this.id = id;
        this.rating = rating;
        this.description = description;
        this.email = email;
        this.critical = critical;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getCritical() {
        return critical;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}