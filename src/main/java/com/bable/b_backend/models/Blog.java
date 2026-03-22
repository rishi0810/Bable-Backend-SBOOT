package com.bable.b_backend.models;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.bable.b_backend.utils.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// DTO for primary interaction with blogs document

@Document(collection="blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Blog extends BaseDocument {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    private String heading;
    private String content;

    @Field(targetType = FieldType.OBJECT_ID)
    private String author;

    @Builder.Default
    private int upvotes = 0;
    
    @Field("img_url")
    private String imgUrl;

}
