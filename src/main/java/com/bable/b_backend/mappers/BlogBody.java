package com.bable.b_backend.mappers;

import lombok.Data;


// DTO for Blog Body for Routes

@Data
public class BlogBody {
    private String heading;
    private String content;
    private int upvotes;
    private String img_url;

}
