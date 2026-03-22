package com.bable.b_backend.mappers;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.bable.b_backend.utils.BaseDocument;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// DTO for Displaying the Blog on Website

@Data
@EqualsAndHashCode(callSuper=true)
public class BlogDisplay extends BaseDocument { 
    
    @Data
    @NoArgsConstructor
    public static class AuthorItem {

        @Field(targetType = FieldType.OBJECT_ID)
        private String id;

        private String name;
    }

    @Field(targetType = FieldType.OBJECT_ID)
    private String id;

    private String heading;
    private String content;
    private int upvotes;
    private String img_url;

    private AuthorItem author;
   
}
