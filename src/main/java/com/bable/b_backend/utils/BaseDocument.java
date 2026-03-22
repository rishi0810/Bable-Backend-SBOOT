package com.bable.b_backend.utils;

import java.util.Date;

import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Getter;
import lombok.Setter;

// Abstract DTO for adding createdAt | updatedAt

@Getter
@Setter
@AccessType(Type.PROPERTY)
public abstract class BaseDocument {
    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
    
}
