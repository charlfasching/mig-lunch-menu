package com.capgemini.cca.mig.menu.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.capgemini.cca.mig.menu.dynamo.DynamoDB;
import com.capgemini.cca.mig.menu.model.Dish;
import com.capgemini.cca.mig.menu.model.MutationResponse;
import com.capgemini.cca.mig.menu.model.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.time.format.DateTimeParseException;
import java.util.*;

public class UpdateDishHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final DynamoDB dishDao = DynamoDB.instance();

    public UpdateDishHandler() {
        getObjectMapper();
    }

    @SneakyThrows
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: " + request.getClass().toString());
        logger.log("BODY:" + request.getBody());
        Map<String, String> pathParameters = request.getPathParameters();
        logger.log("PATH:" + pathParameters);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();

        try {
            String possibleId = Optional.ofNullable(pathParameters != null ? pathParameters.get("id") : null)
                    .orElseThrow(() -> {
                        throw new IllegalArgumentException("Missing Path Param 'id'");
                    });
            UUID id = UUID.fromString(possibleId);
            Dish newDish = getObjectMapper().readValue(request.getBody(), Dish.class);

            logger.log("deserialized Dish:" + newDish.toString());
            dishDao.updateExistingDish(newDish, id.toString());
            logger.log("Updating dish with id:"+id);
            MutationResponse creationResult = MutationResponse.builder()
                    .status(Status.SUCCESS)
                    .advice("Dish Update Accepted")
                    .build();

            response.setBody(getObjectMapper().writeValueAsString(creationResult));
            response.setStatusCode(200);
        } catch (JsonMappingException | JsonParseException | DateTimeParseException ex) {
            response.setStatusCode(400);
            MutationResponse mrbody = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.INVALID_BODY_STRUCTURE)
                    .build();
            response.setBody(getObjectMapper().writeValueAsString(mrbody));
        } catch (IllegalArgumentException ex) {
            response.setStatusCode(400);
            MutationResponse mrbody = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.INVALID_PARAM_FORMAT)
                    .build();
            response.setBody(getObjectMapper().writeValueAsString(mrbody));
        } catch (Exception ex) {
            response.setStatusCode(500);
            MutationResponse error = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.SYSTEM_ERROR)
                    .build();
            response.setBody(getObjectMapper().writeValueAsString(error));
        }
        response.setIsBase64Encoded(false);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html");
        response.setHeaders(headers);
        return response;
    }

    private static volatile ObjectMapper mapper;

    private static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            synchronized (ObjectMapper.class) {
                if (mapper == null) {
                    mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                }
            }
        }
        return mapper;
    }


}
