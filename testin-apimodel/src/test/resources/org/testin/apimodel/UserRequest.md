```java
package com.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserRequest {
    @JsonProperty("first-name")
    private String firstName;

    private String mobile;

    private String pin;

    private Address address;

    private List<String> roles;

    private List<CardsItem> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Address {
        private String city;

        private String street;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class CardsItem {
        private String type;

        private Integer limit;
    }
}
```
