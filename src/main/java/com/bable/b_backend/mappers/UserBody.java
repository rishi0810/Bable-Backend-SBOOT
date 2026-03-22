package com.bable.b_backend.mappers;

import lombok.Data;

// DTO for User Registration | Login request Body

@Data
public class UserBody {
    private String name;
    private String email;
    private String password;
    
}
