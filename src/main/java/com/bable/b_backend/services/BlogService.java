package com.bable.b_backend.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bable.b_backend.mappers.BlogBody;
import com.bable.b_backend.mappers.BlogDisplay;
import com.bable.b_backend.mappers.BlogListing;
import com.bable.b_backend.mappers.JWTBody;
import com.bable.b_backend.mappers.ResponseStatus;
import com.bable.b_backend.models.Blog;
import com.bable.b_backend.models.User;
import com.bable.b_backend.repository.BlogRepository;
import com.bable.b_backend.repository.UserRepository;
import com.bable.b_backend.security.AuthContext;
import com.bable.b_backend.utils.ResponseMapper;

@Service
public class BlogService {

    // Auto Injection of Blog Repository to connect to Blog Document
    @Autowired
    private BlogRepository blogRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private ExternalApiService postApi;

    // Function to create a new blog
    public ResponseStatus createNewBlog(BlogBody entity) {
        // Get current saved user
        JWTBody currentAuthor = (JWTBody) authContext.getCurrentUser();

        // If not, then return unauthorized
        if (currentAuthor == null) {
            return ResponseMapper.error(401, "Unauthorized");
        }

        try {
            // New blog object
            Blog newBlog = new Blog();
            newBlog.setHeading(entity.getHeading());
            newBlog.setAuthor(currentAuthor.getId());
            newBlog.setContent(entity.getContent());
            newBlog.setImgUrl(postApi.uploadImage(entity.getImg_url()));
            // Upsert Blog
            Blog savedBlog = blogRepo.save(newBlog);

            Optional<User> dbUser = userRepo.findById(currentAuthor.getId());;
            if (dbUser.isEmpty()) {
                return ResponseMapper.error(404, "User Not Found");
            }
            User modifiedUser = dbUser.get();

            // Add the blog id to the written blogs
            List<String> currentWrittenBlogs = modifiedUser.getWrittenBlogs() == null ? new ArrayList<>() : new ArrayList<>(modifiedUser.getWrittenBlogs());
            currentWrittenBlogs.add(savedBlog.getId());
            modifiedUser.setWrittenBlogs(currentWrittenBlogs);
            // Upsert the information
            userRepo.save(modifiedUser);

            return ResponseMapper.success(201, "Blog Created | " + savedBlog.getId());

        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }

    }

    // Function to delete a blog
    public ResponseStatus deleteBlogByID(String id) {
        // Find existing blog by id
        Optional<Blog> existingBlog = blogRepo.findById(id);
        if (existingBlog.isEmpty()) {
            return ResponseMapper.error(404, "Blog Not Found");
        }

        // Get current signed in user by context
        JWTBody currentUser = (JWTBody) authContext.getCurrentUser();

        if (currentUser == null) {
            return ResponseMapper.error(403, "Lack of Access");
        }

        Blog toBeDeletedBlog = existingBlog.get();
        try {
            // Check if the user is the author -> if yes allow 
            if (toBeDeletedBlog.getAuthor().equals(currentUser.getId())) {
                Optional<User> dbUser = userRepo.findById(currentUser.getId());
                if (dbUser.isEmpty()) {
                    return ResponseMapper.error(404, "User Not Found");
                }

                // Build updated written blog -> removal of the blog from written blog
                User modifiedUser = dbUser.get();
                List<String> currentWrittenBlogs = modifiedUser.getWrittenBlogs() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(modifiedUser.getWrittenBlogs());
                currentWrittenBlogs.remove(id);
                modifiedUser.setWrittenBlogs(currentWrittenBlogs);

                // Step -1 -> remove from the author's saved if they have saved their written blog
                List<String> currentStoredBlogs = modifiedUser.getStoredBlogs() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(modifiedUser.getStoredBlogs());
                currentStoredBlogs.remove(id);
                modifiedUser.setStoredBlogs(currentStoredBlogs);

                // Safely Upsert the Modified User
                userRepo.save(modifiedUser);

                // Step - 2 -> remove the blog from every user's saved list
                List<User> usersWhoSavedTheBlog = userRepo.findByStoredBlogsContaining(id);
                // Parse each user -> remove from list and upsert
                for (User user : usersWhoSavedTheBlog) {
                    List<String> updatedStoredBlogs = user.getStoredBlogs() == null
                            ? new ArrayList<>()
                            : new ArrayList<>(user.getStoredBlogs());
                    updatedStoredBlogs.remove(id);
                    user.setStoredBlogs(updatedStoredBlogs);
                    userRepo.save(user);
                }

                blogRepo.deleteById(id);

                return ResponseMapper.success(200, "Blog Deleted | Id " + id);
            } // Reject if not the owner
            else {
                return ResponseMapper.error(401, "Unauthorized");
            }
        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }

    }

    // Function for website blog display
    public BlogDisplay getDisplayBlog(String id) {
        // Check for existence 
        Optional<Blog> existingBlog = blogRepo.findById(id);
        if (existingBlog.isEmpty()) {
            return null;
        }

        Blog toShowBlog = existingBlog.get();

        BlogDisplay targetBlog = new BlogDisplay();

        // Convert Blog item to display item
        targetBlog.setId(id);
        targetBlog.setImg_url(toShowBlog.getImgUrl());
        targetBlog.setContent(toShowBlog.getContent());
        targetBlog.setHeading(toShowBlog.getHeading());
        targetBlog.setUpvotes(0);
        targetBlog.setCreatedAt(toShowBlog.getCreatedAt());
        targetBlog.setUpdatedAt(toShowBlog.getUpdatedAt());

        // Find the author of the blog by repo function
        BlogDisplay.AuthorItem author = new BlogDisplay.AuthorItem();
        String authorName = userRepo.findAuthorById(toShowBlog.getAuthor())
                .map(UserRepository.AuthorNameView::getName)
                .orElse(null);

        author.setId(toShowBlog.getAuthor());
        author.setName(authorName);
        
        // Response object created
        targetBlog.setAuthor(author);

        return targetBlog;
    }

    // Function to create main feed
    public List<BlogListing> getBlogFeed() {
        List<Blog> allBlogs = blogRepo.findAll();

        if (allBlogs == null) {
            return null;
        }

        // Extract all author ids from all blogs
        Set<String> authorIds = allBlogs.stream()
                .map(Blog::getAuthor)
                .filter(authorId -> authorId != null && !authorId.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Extract all names according to id by userRepo funcction
        Map<String, String> authorNamesById = authorIds.isEmpty()
                ? Collections.emptyMap()
                : userRepo.findAuthorsByIdIn(List.copyOf(authorIds)).stream()
                        .collect(Collectors.toMap(
                                UserRepository.AuthorNameView::getId,
                                UserRepository.AuthorNameView::getName,
                                (first, second) -> first));

        // Create a stream -> filter based on creating temp object -> appending to list
        return allBlogs.stream()
                .map(blog -> {
                    BlogListing temp = new BlogListing();
                    temp.setId(blog.getId());
                    temp.setHeading(blog.getHeading());
                    temp.setImg_url(blog.getImgUrl());

                    BlogListing.Author blogAuthor = new BlogListing.Author();
                    blogAuthor.setId(blog.getAuthor());
                    blogAuthor.setName(authorNamesById.get(blog.getAuthor()));

                    temp.setAuthor(blogAuthor);
                    return temp;
                })
                .collect(Collectors.toList());

    }

    // Function to add blog to saved
    public ResponseStatus addBlogToSave(String id) {
        try {
            // Get current logged in user
            JWTBody currentUser = (JWTBody) authContext.getCurrentUser();

            // If lack of authentication
            if (currentUser == null) {
                return ResponseMapper.error(401, "Unauthenticated");
            }

            Optional<Blog> existingBlog = blogRepo.findById(id);
            if (existingBlog.isEmpty()) {
                return ResponseMapper.error(404, "Blog Not Found");
            }

            // Fetch user object for current user from DB
            Optional<User> dbUser = userRepo.findById(currentUser.getId());
            if (dbUser.isEmpty()) {
                return ResponseMapper.error(404, "User Not Found");
            }

            User modifiedUser = dbUser.get();

            List<String> currentSavedBlogs = modifiedUser.getStoredBlogs() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(modifiedUser.getStoredBlogs());

            if (currentSavedBlogs.contains(id)) {
                return ResponseMapper.success(200, "Blog Already Saved | ID " + id);
            }

            currentSavedBlogs.add(id);
            modifiedUser.setStoredBlogs(currentSavedBlogs);

            // Save the full user document so unrelated fields are preserved
            userRepo.save(modifiedUser);

            return ResponseMapper.success(200, "Blog Saved | ID " + id);
        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }

    }

    // Function to remove a blog from saved
    public ResponseStatus removeBlogFromSave(String id){
         // Find existing blog by id
        Optional<Blog> existingBlog = blogRepo.findById(id);
        if (existingBlog.isEmpty()) {
            return ResponseMapper.error(404, "Blog Not Found");
        }

        // Get current signed in user by context
        JWTBody currentUser = (JWTBody) authContext.getCurrentUser();

        if (currentUser == null) return ResponseMapper.error(403, "Lack of Access");


        try {
         Optional <User> dbUser = userRepo.findById(currentUser.getId());
         if(dbUser.isEmpty()) return ResponseMapper.error(404, "User Not Found");

         User modifiedUser = dbUser.get();
         // Build new stored array without the removal
         List<String> currentStoredBlogs = modifiedUser.getStoredBlogs() == null ? new ArrayList<>() : new ArrayList<>(modifiedUser.getStoredBlogs());
         currentStoredBlogs.remove(id);
         modifiedUser.setStoredBlogs(currentStoredBlogs);
         // Upsert the information
         userRepo.save(modifiedUser);

        return ResponseMapper.success(200, "Blog Deleted From Save | ID " + id);
   
        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }
        
        
    }

}
