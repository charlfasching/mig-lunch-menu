package com.capgemini.cca.mig.menu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dish implements Serializable {

    String id;
    String name;
    LocalDate servedOn;
    Float price;
    String description;
    Integer rating;
    String image;
    List<String> allergies;
}

