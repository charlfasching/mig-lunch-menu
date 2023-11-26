package com.capgemini.cca.mig.menu.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.capgemini.cca.mig.menu.dynamo.DynamoDB;
import com.capgemini.cca.mig.menu.model.Dish;
import com.capgemini.cca.mig.menu.model.MutationResponse;
import com.capgemini.cca.mig.menu.model.Status;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.time.format.DateTimeParseException;
import java.util.HashMap;

public class NewDishHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final DynamoDB dishDao = DynamoDB.instance();

    public NewDishHandler() {
        getObjectMapper();
    }

    @SneakyThrows
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: " + request.getClass().toString());
        logger.log("BODY:" + request.getBody());

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();

        try {
            Dish newDish = getObjectMapper().readValue(request.getBody(), Dish.class);
            logger.log("deserialized Dish:" + newDish.toString());
            dishDao.createNewDish(newDish);
            MutationResponse creationResult = MutationResponse.builder()
                            .status(Status.SUCCESS)
                            .advice("New Dish Accepted")
                            .build();
            response.setBody(getObjectMapper().writeValueAsString(creationResult));
            response.setStatusCode(202);
        } catch (JsonMappingException | DateTimeParseException | IllegalArgumentException ex) {
            response.setStatusCode(400);
            MutationResponse mrbody = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.INVALID_BODY_STRUCTURE)
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

    private MutationResponse createDish(Dish newDish) {


        return MutationResponse.builder()
                .status(Status.SUCCESS)
                .build();
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
