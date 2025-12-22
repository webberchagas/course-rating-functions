package br.com.techchallenge.rating.service;

import br.com.techchallenge.rating.exception.ValidationException;
import br.com.techchallenge.rating.model.api.RatingRequest;
import br.com.techchallenge.rating.model.api.RatingResult;
import br.com.techchallenge.rating.model.persistence.RatingDocument;
import br.com.techchallenge.rating.model.utils.NotificationStatus;
import br.com.techchallenge.rating.repository.RatingRepository;

import java.time.Instant;
import java.util.UUID;

import static br.com.techchallenge.rating.model.utils.NotificationStatus.NOT_REQUIRED;
import static br.com.techchallenge.rating.model.utils.NotificationStatus.PENDING;

public class RatingService {

    private final RatingRepository repository = new RatingRepository();

    public RatingResult create(RatingRequest input) {
        if (input == null) {
            throw new ValidationException("Request body is required");
        }

        Integer rating = validateRating(input);
        String description = validateDescription(input);
        String id = UUID.randomUUID().toString();
        boolean critical = isCriticalRating(rating);
        NotificationStatus status = critical ? PENDING : NOT_REQUIRED;

        RatingDocument document = buildDocument(id, rating, description, input.getEmail(), critical, status);

        repository.save(document);

        return buildRatingResult(document);
    }

    public void updateNotificationStatus(String id, NotificationStatus status) {
        repository.updateNotificationStatus(id, status, Instant.now());
    }

    private Integer validateRating(RatingRequest input) {
        Integer rating = input.getRating();
        if (rating == null || rating < 0 || rating > 10) {
            throw new ValidationException("The rating field is required and must be between 0 and 10");
        }
        return rating;
    }

    private String validateDescription(RatingRequest input) {
        String description = input.getDescription();
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        return description.trim();
    }

    private boolean isCriticalRating(int rating) {
        return rating < 6;
    }

    private RatingDocument buildDocument(String id, Integer rating, String description, String email, boolean critical,
                                         NotificationStatus status) {
        Instant now = Instant.now();
        return new RatingDocument(
                id,
                rating,
                description,
                email,
                critical,
                now,
                now,
                status
        );
    }

    private RatingResult buildRatingResult(RatingDocument document) {
        return new RatingResult(
                document.getId(),
                document.getRating(),
                document.getDescription(),
                document.getEmail(),
                document.getCritical(),
                document.getCreatedAt().toString()
        );
    }
}
