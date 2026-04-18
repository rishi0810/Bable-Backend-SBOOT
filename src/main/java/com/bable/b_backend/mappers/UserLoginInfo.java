package com.bable.b_backend.mappers;

import lombok.Data;

// DTO for login information of user to be saved on client side

@Data
public class UserLoginInfo {
    private String id;
    private String name;
    private String email;
    private String token;
    private boolean isLoggedIn;
    private String message;
    private int code;
}


