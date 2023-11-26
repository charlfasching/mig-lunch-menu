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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GetDishHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final DynamoDB dishDao = DynamoDB.instance();


    public GetDishHandler() {
        getObjectMapper();
    }

    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: " + request.getClass().toString());
        logger.log("BODY:" + request.getBody());
        Map<String, String> qsp = Optional.ofNullable(
                request.getQueryStringParameters()
        ).orElse(new HashMap<>());
        logger.log("QSP:" + qsp);

        Optional<String> fromDay = Optional.ofNullable(qsp.get("fromDay"));
        Optional<String> toDay = Optional.ofNullable(qsp.get("toDay"));
        Optional<String> limit = Optional.ofNullable(qsp.get("limit"));

        LocalDate fromDateParsed = fromDay.map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)).orElseGet(LocalDate::now);
        LocalDate fromToParsed = toDay.map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)).orElseGet(LocalDate::now);
        int limitParsed = limit.map(Integer::parseInt).orElse(10);
        logger.log("fromDay-Parsed:" + fromDateParsed);
        logger.log("toDay-Parsed:" + fromToParsed);
        logger.log("limit-Parsed:" + limitParsed);

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setIsBase64Encoded(false);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html");
        response.setHeaders(headers);

        try {
            List<Dish> dishes = retrieveDishes(fromDateParsed, fromToParsed, limitParsed);
            response.setBody(serialize(dishes));
            response.setStatusCode(200);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            response.setStatusCode(400);
            MutationResponse mrbody = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.INVALID_BODY_STRUCTURE)
                    .build();

            response.setBody(serialize(mrbody));

        } catch (Exception ex) {
            response.setStatusCode(500);
            MutationResponse error = MutationResponse.builder()
                    .advice(ex.getMessage())
                    .status(Status.SYSTEM_ERROR)
                    .build();
            response.setBody(serialize(error));

        }
        return response;
    }

    protected List<Dish> retrieveDishes(LocalDate fromDate, LocalDate toDate, int limit ) {

        return dishDao.findDishes(Optional.ofNullable(fromDate), Optional.ofNullable(toDate), limit);

    }

    private String serialize(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
