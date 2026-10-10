```java
package com.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserResponse(
        Integer id,

        @JsonProperty("first-name")
        String firstName,

        String status,

        Double balance,

        @JsonProperty("class")
        String classValue,

        Address address,

        List<CardsItem> cards) {

    public record Address(
            String city) {
    }

    public record CardsItem(
            String last4,

            Boolean frozen) {
    }
}
```
