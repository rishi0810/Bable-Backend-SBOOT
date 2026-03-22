package com.bable.b_backend.models;

import java.util.List;

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

// DTO for primary interaction with users document

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode (callSuper = true)
public class User extends BaseDocument {
    
    @MongoId(FieldType.OBJECT_ID)
    private String id;
    
    private String name;
    private String email;
    private String password;

    @Field(targetType = FieldType.OBJECT_ID)
    private List<String> storedBlogs;

    @Field(targetType = FieldType.OBJECT_ID)
    private List<String> writtenBlogs;
    
}
