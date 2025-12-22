package br.com.techchallenge.rating.repository;

import br.com.techchallenge.rating.model.persistence.RatingDocument;
import br.com.techchallenge.rating.model.utils.NotificationStatus;
import br.com.techchallenge.rating.repository.mongo.MongoClientProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.Date;

public class RatingRepository {

    private final MongoCollection<Document> collection;

    public RatingRepository() {
        String dbName = getEnvOrThrow("MONGODB_DB");
        String collectionName = getEnvOrThrow("MONGODB_COLLECTION");

        MongoDatabase db = MongoClientProvider.getClient().getDatabase(dbName);
        this.collection = db.getCollection(collectionName);
    }

    public void save(RatingDocument doc) {
        Document mongoDoc = new Document()
                .append("_id", doc.getId())
                .append("rating", doc.getRating())
                .append("description", doc.getDescription())
                .append("email", doc.getEmail())
                .append("critical", doc.getCritical())
                .append("createdAt", Date.from(doc.getCreatedAt()))
                .append("updatedAt", Date.from(doc.getUpdatedAt()))
                .append("notificationStatus", doc.getNotificationStatus().name());

        collection.insertOne(mongoDoc);
    }

    public void updateNotificationStatus(String id, NotificationStatus status, Instant updateDate) {
        collection.updateOne(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.set("notificationStatus", status.name()),
                        Updates.set("updatedAt", Date.from(updateDate))
                )
        );
    }

    private String getEnvOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + key);
        }
        return value;
    }
}
