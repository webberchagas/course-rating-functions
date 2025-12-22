package br.com.techchallenge.rating.function;

import br.com.techchallenge.rating.exception.ValidationException;
import br.com.techchallenge.rating.model.api.RatingRequest;
import br.com.techchallenge.rating.model.api.RatingResult;
import br.com.techchallenge.rating.model.event.CriticalRatingEvent;
import br.com.techchallenge.rating.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;

import java.util.Optional;

import static br.com.techchallenge.rating.model.utils.NotificationStatus.PUBLISHED;
import static br.com.techchallenge.rating.model.utils.NotificationStatus.PUBLISH_FAILED;

public class CreateCourseRatingFunction {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final RatingService service = new RatingService();

    @FunctionName("CreateCourseRatingFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "ratings",
                    authLevel = AuthorizationLevel.ANONYMOUS
            ) HttpRequestMessage<Optional<String>> request, final ExecutionContext context,
            @ServiceBusQueueOutput(
                    name = "criticalMessage",
                    queueName = "critical-ratings",
                    connection = "SERVICE_BUS_CONNECTION"
            ) OutputBinding<String> criticalMessage) {

        context.getLogger().info("CreateCourseRatingFunction - request received");

        try {
            String body = validateBody(request);
            RatingRequest dto = parseRequestBody(body);
            RatingResult result = service.create(dto);
            sendEventIfCritical(result, criticalMessage, context);
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(result)
                    .build();
        } catch (ValidationException | IllegalArgumentException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error.")
                    .build();
        }
    }

    private RatingRequest parseRequestBody(String body) {
        try {
            return mapper.readValue(body, RatingRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    private String validateBody(HttpRequestMessage<Optional<String>> request) {
        String body = request.getBody().orElse(null);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Body required. Send JSON with rating and description");
        }
        return body;
    }

    private void sendEventIfCritical(RatingResult result, OutputBinding<String> criticalMessage,
                                     ExecutionContext context) {
        if (Boolean.TRUE.equals(result.getCritical())) {
            CriticalRatingEvent event = buildCriticalRatingEvent(result);
            try {
                String message = mapper.writeValueAsString(event);
                criticalMessage.setValue(message);
                service.updateNotificationStatus(event.getId(), PUBLISHED);
                context.getLogger().info("Critical rating event, id: " + event.getId() + " sent to Service Bus.");
            } catch (Exception e) {
                service.updateNotificationStatus(event.getId(), PUBLISH_FAILED);
                context.getLogger().severe("Failed to send id: " + event.getId() + " critical rating event: " + e.getMessage());
            }
        }
    }

    private CriticalRatingEvent buildCriticalRatingEvent(RatingResult result) {
           return new CriticalRatingEvent(
                   result.getId(),
                   result.getRating(),
                   result.getDescription(),
                   result.getEmail(),
                   result.getCritical(),
                   result.getCreatedAt()
           );
    }
}
