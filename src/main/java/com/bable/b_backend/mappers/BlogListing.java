package com.bable.b_backend.mappers;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Data;
import lombok.NoArgsConstructor;


// DTO for a blog object in main feed


@Data
public class BlogListing {

    @Data
    @NoArgsConstructor
    public static class Author {
        @Field(targetType = FieldType.OBJECT_ID)
        private String id;

        private String name;
    }

    @Field(targetType = FieldType.OBJECT_ID)
    private String id;

    private String heading;
    private String img_url;
    private Author author;

}
