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

import com.bable.b_backend.config.RedisConfig;
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
import com.bable.b_backend.utils.ConvertDTO;
import com.bable.b_backend.utils.Constants;
import com.bable.b_backend.utils.ResponseMapper;

@Service
public class BlogService {

    // Auto Injection of Blog Repository to connect to Blog Document
    @Autowired
    private BlogRepository blogRepo;

    // Auto Injection of User Repository for connection to User Document
    @Autowired
    private UserRepository userRepo;

    // Auto Injection of Authentication Context Handler
    @Autowired
    private AuthContext authContext;    

    // Auto Injection of Image Link external API
    @Autowired
    private ExternalApiService postApi;

    // Auto Injection of Redis Configuration
    @Autowired
    private RedisConfig.RedisClient redis;

    // Auto Injection of DTO converter
    @Autowired
    private ConvertDTO converter;

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

            // create redis cache for the new blog display on website
            redis.setObject(Constants.blogDisplayCacheKey(savedBlog.getId()), converter.convertBlogToBlogDisplay(savedBlog, currentAuthor));

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
            User redisUser = userRepo.save(modifiedUser);

            // check redis cache for existing user profile page
            if (redis.exists(Constants.profileCacheKey(modifiedUser.getId()))) {
                // if present -> remove the current user profile page cache
                redis.del(Constants.profileCacheKey(modifiedUser.getId()));
            }

            // Save profile body to redis for future calls
            redis.setObject(Constants.profileCacheKey(redisUser.getId()), converter.convertUserToProfileBody(redisUser));

            // refresh the main feed below
            refreshMainFeedCache();

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
                User changedUser = userRepo.save(modifiedUser);

                // check if a redis cache exists for the user profile page
                if (redis.exists(Constants.profileCacheKey(changedUser.getId()))){
                    // if there, delete
                    redis.del(Constants.profileCacheKey(changedUser.getId()));
                }

                // Create new profile body and save 
                redis.setObject(Constants.profileCacheKey(changedUser.getId()), converter.convertUserToProfileBody(changedUser));

                // check if the user exists in redis cache
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
                    
                    // For all users profile body existence
                    if(redis.exists(Constants.profileCacheKey(user.getId()))){
                        // delete no rebuild
                        redis.del(Constants.profileCacheKey(user.getId()));
                    }
                }

                blogRepo.deleteById(id);

                // remove the blog's display cache
                redis.del(Constants.blogDisplayCacheKey(id));
                
                // refresh the main feed below
                refreshMainFeedCache();

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

        // If the blog info exists in redis cache, send from there only
        if(redis.exists(Constants.blogDisplayCacheKey(id))){
            return redis.getObject(Constants.blogDisplayCacheKey(id), BlogDisplay.class);
        }


        // Create JWT Body and blog body to send to converter
        Blog toShowBlog = existingBlog.get();
        // Fetch the target blog author
        User blogAuthor = userRepo.findById(toShowBlog.getAuthor()).orElse(null);
        JWTBody temp = new JWTBody();
        // Create JWT DTO body
        temp.setId(blogAuthor.getId());
        temp.setEmail(blogAuthor.getEmail());
        temp.setName(blogAuthor.getName());
        // Create Blog Display body
        BlogDisplay targetBlog = converter.convertBlogToBlogDisplay(toShowBlog, temp);
        
        // Create redis cache for the blog display & return
        redis.setObject(Constants.blogDisplayCacheKey(id) , targetBlog);
        return targetBlog;
    }

    // Function to create main feed
    public List<BlogListing> getBlogFeed() {
        // check if main feed is cached
        if (redis.exists(Constants.MAIN_FEED_CACHE_KEY)) {
            // build a class from list of blogListings
            Class<BlogListing> cacheType = BlogListing.class;
            // if the cached feed exists, serve directly
            List<BlogListing> cachedFeed = redis.getList(Constants.MAIN_FEED_CACHE_KEY, cacheType);
            if (cachedFeed != null) {
                return cachedFeed;
            }
        }

        // build main feed and create new cache 
        List<BlogListing> finalList = buildMainFeed();
        redis.setList(Constants.MAIN_FEED_CACHE_KEY, finalList);
        return finalList;
    }

    // refresh main feed cache on call, for changes to any of the blog (add | remove)
    private void refreshMainFeedCache() {
        redis.del(Constants.MAIN_FEED_CACHE_KEY);
        redis.setList(Constants.MAIN_FEED_CACHE_KEY, buildMainFeed());
    }  

    // DB - based cache build
    private List<BlogListing> buildMainFeed() {
        List<Blog> allBlogs = blogRepo.findAll();
        if (allBlogs == null) {
            return new ArrayList<>();
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
            User changedUser = userRepo.save(modifiedUser);

            // check if redis cache exists for the user profile
            if (redis.exists(Constants.profileCacheKey(changedUser.getId()))){
                // clear the cache
                redis.del(Constants.profileCacheKey(changedUser.getId()));
            }

            // create new user profile cache
            redis.setObject(Constants.profileCacheKey(changedUser.getId()), converter.convertUserToProfileBody(changedUser));

            return ResponseMapper.success(200, "Blog Saved | ID " + id);
        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }

    }

    // Function to remove a blog from saved
    public ResponseStatus removeBlogFromSave(String id) {
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

        try {
            Optional<User> dbUser = userRepo.findById(currentUser.getId());
            if (dbUser.isEmpty()) {
                return ResponseMapper.error(404, "User Not Found");
            }

            User modifiedUser = dbUser.get();
            // Build new stored array without the removal
            List<String> currentStoredBlogs = modifiedUser.getStoredBlogs() == null ? new ArrayList<>() : new ArrayList<>(modifiedUser.getStoredBlogs());
            currentStoredBlogs.remove(id);
            modifiedUser.setStoredBlogs(currentStoredBlogs);
            // Upsert the information
            User changedUser = userRepo.save(modifiedUser);

            // check if redis cache exists for the user profile
            if (redis.exists(Constants.profileCacheKey(changedUser.getId()))){
                // clear the cache
                redis.del(Constants.profileCacheKey(changedUser.getId()));
            }

            // create new user profile cache
            redis.setObject(Constants.profileCacheKey(changedUser.getId()), converter.convertUserToProfileBody(changedUser));

            return ResponseMapper.success(200, "Blog Deleted From Save | ID " + id);

        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }

    }

}
