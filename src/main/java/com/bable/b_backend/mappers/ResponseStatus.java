package com.bable.b_backend.mappers;

import lombok.Data;

// DTO for String Response Routes

@Data
public class ResponseStatus {
    private int code;
    private boolean success;
    private String stringBody;
    
}
