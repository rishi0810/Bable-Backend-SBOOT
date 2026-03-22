package com.bable.b_backend.mappers;

import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for Profile Page Info

@Data
public class ProfileBody { 
    
    @Data
    @NoArgsConstructor
    public static class BlogItem {
        private String id;
        private String heading;
    }
    
    private String id;
    private String name;
    private String email;
    Date createdAt;
    Date updatedAt;
    
    private List <BlogItem> writtenBlogs;
    private List <BlogItem> storedBlogs;
   
}
