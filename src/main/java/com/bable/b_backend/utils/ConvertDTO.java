package com.bable.b_backend.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bable.b_backend.mappers.BlogDisplay;
import com.bable.b_backend.mappers.BlogDisplay.AuthorItem;
import com.bable.b_backend.mappers.JWTBody;
import com.bable.b_backend.mappers.ProfileBody;
import com.bable.b_backend.mappers.ProfileBody.BlogItem;
import com.bable.b_backend.models.Blog;
import com.bable.b_backend.models.User;
import com.bable.b_backend.repository.BlogRepository;

@Component
public class ConvertDTO {

    @Autowired
    private BlogRepository blogRepo;

    public List<BlogItem> mapBlogItems(List<String> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Create Unique hashmap for objects
        Map<String, BlogItem> blogItemsById = new HashMap<>();

        // Parallel find by Id call
        blogRepo
                .findAllById(blogIds)
                .forEach(blog -> {
                    // New object in the list
                    BlogItem item = new BlogItem();
                    item.setId(blog.getId());
                    item.setHeading(blog.getHeading());
                    blogItemsById.put(blog.getId(), item);
                });

        return blogIds
                .stream()
                .map(blogId -> {
                    BlogItem item = blogItemsById.get(blogId);
                    if (item != null) {
                        return item;
                    }

                    BlogItem unresolvedItem = new BlogItem();
                    unresolvedItem.setId(blogId);
                    return unresolvedItem;
                })
                .collect(Collectors.toList());
    }

    public ProfileBody convertUserToProfileBody(User entity) {

        // Extract list of blogs of both arrays
        List<BlogItem> writtenBlogItems = mapBlogItems(
                entity.getWrittenBlogs()
        );
        List<BlogItem> storedBlogItems = mapBlogItems(
                entity.getStoredBlogs()
        );

        // Create the profile object to send to handler
        ProfileBody userDetails = new ProfileBody();
        userDetails.setId(entity.getId());
        userDetails.setName(entity.getName());
        userDetails.setEmail(entity.getEmail());
        userDetails.setWrittenBlogs(writtenBlogItems);
        userDetails.setStoredBlogs(storedBlogItems);
        userDetails.setUpdatedAt(entity.getUpdatedAt());
        userDetails.setCreatedAt(entity.getCreatedAt());

        return userDetails;

    }

    public BlogDisplay convertBlogToBlogDisplay(Blog entity, JWTBody currentAuthor) {

        BlogDisplay targetBlog = new BlogDisplay();

        // Convert Blog item to display item
        targetBlog.setId(entity.getId());
        targetBlog.setImg_url(entity.getImgUrl());
        targetBlog.setContent(entity.getContent());
        targetBlog.setHeading(entity.getHeading());
        targetBlog.setUpvotes(0);
        targetBlog.setCreatedAt(entity.getCreatedAt());
        targetBlog.setUpdatedAt(entity.getUpdatedAt());

        // Set Author item
        BlogDisplay.AuthorItem curAuthor = new AuthorItem();
        curAuthor.setId(currentAuthor.getId());
        curAuthor.setName(currentAuthor.getName());
        targetBlog.setAuthor(curAuthor);

        return targetBlog;
    }
}
