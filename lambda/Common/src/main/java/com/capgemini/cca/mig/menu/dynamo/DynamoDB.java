package com.capgemini.cca.mig.menu.dynamo;


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.capgemini.cca.mig.menu.model.Dish;
import com.capgemini.cca.mig.menu.model.MutationResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;

public class DynamoDB {
    private static volatile DynamoDB instance;

    private static DynamoDBMapper mapper;

    private DynamoDB() {

        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
        mapper = new DynamoDBMapper(client);
    }

    public static DynamoDB instance() {

        if (instance == null) {
            synchronized(DynamoDB.class) {
                if (instance == null)
                    instance = new DynamoDB();
            }
        }

        return instance;
    }

    public List<Dish> findDishes(Optional<LocalDate> dayFrom, Optional<LocalDate> dayTo, int limit) {

        Long startofDay = dayFrom.orElse(LocalDate.now((ZoneOffset.UTC)))
                .atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC);

        Long endofDay = dayTo.orElse(LocalDate.now((ZoneOffset.UTC)))
                .plusDays(1)
                .atStartOfDay()
                .minusMinutes(1)
                .toEpochSecond(ZoneOffset.UTC);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":start", new AttributeValue().withN(String.valueOf(startofDay)));
        eav.put(":end", new AttributeValue().withN(String.valueOf(endofDay)));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("servedOn between :start and :end")
                .withExpressionAttributeValues(eav);

        List<Dish> dishes = mapper.scan(Dish.class, scanExpression);
        dishes.forEach(
                dish ->
                        dish.setServedOn(
                                Instant.ofEpochSecond(dish.getServedOnTime())
                                        .atZone(ZoneOffset.UTC).toLocalDate())
        );
        return dishes;

    }

    public void createNewDish(Dish newDish) {
        // Converting LocalDate to Unix Time
        newDish.setServedOnTime(newDish.getServedOn().toEpochSecond(LocalTime.now(), ZoneOffset.UTC));
        // create new unique ID for dish
        newDish.setId(UUID.randomUUID().toString());
        mapper.save(newDish);

    }

    public void updateExistingDish(Dish newDish) {
        // Converting LocalDate to Unix Time
        newDish.setServedOnTime(newDish.getServedOn().toEpochSecond(LocalTime.now(), ZoneOffset.UTC));

        mapper.save(newDish);

    }

}
