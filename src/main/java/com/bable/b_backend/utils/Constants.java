package com.bable.b_backend.utils;


// Class of constants for mutability
public class Constants {
    private Constants(){}
    public static final long EXPIRY_TIME = 1000 * 30 * 30 ; 
    public static final long REFRESH_TIME = 5 * 60 * 1000;
    public static final String[] publicRoutes = {
            "/blog/blog-content",
            "/blog/main-feed", 
            "/user/create-user",
            "/user/login-user",
            "/user/details-user"
    };
}
