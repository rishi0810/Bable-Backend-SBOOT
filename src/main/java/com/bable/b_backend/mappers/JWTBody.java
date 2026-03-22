package com.bable.b_backend.mappers;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Data;

// DTO for creation | validation of JWT Token

@Data
public class JWTBody {

    @Field(targetType = FieldType.OBJECT_ID)
    private String id;

    private String name;
    private String email;
}
