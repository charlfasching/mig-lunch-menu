package com.capgemini.cca.mig.menu.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "dishes")
public class Dish implements Serializable {

    // this is converted into Unix timestamp below
    @DynamoDBIgnore
    LocalDate servedOn;

    @DynamoDBHashKey(attributeName = "servedOn")
    @JsonIgnore
    Long servedOnTime;

    @DynamoDBRangeKey
    String id;

    @DynamoDBAttribute
    String name;

    @DynamoDBAttribute
    String price;

    @DynamoDBAttribute
    String description;

    @DynamoDBAttribute
    Integer rating;

    @DynamoDBAttribute
    String image;

    @DynamoDBAttribute
    Set<String> allergies;

    public Dish merge(Dish newValues) {
        if (!this.name.equals(newValues.name)) {
            this.setName(newValues.getName());
        }
        if (!this.price.equals(newValues.price)) {
            this.setPrice(newValues.getPrice());
        }
        if (!this.description.equals(newValues.description)) {
            this.setDescription(newValues.getDescription());
        }
        if (!this.rating.equals(newValues.rating)) {
            this.setRating(newValues.getRating());
        }
        if (!this.image.equals(newValues.image)) {
            this.setImage(newValues.getImage());
        }
        if (!this.allergies.containsAll(newValues.allergies)) {
            this.setAllergies(newValues.getAllergies());
        }
        return this;
    }
}

