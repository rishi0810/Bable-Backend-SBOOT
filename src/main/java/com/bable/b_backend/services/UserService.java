package com.bable.b_backend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bable.b_backend.mappers.JWTBody;
import com.bable.b_backend.mappers.ProfileBody;
import com.bable.b_backend.mappers.ProfileBody.BlogItem;
import com.bable.b_backend.mappers.ResponseStatus;
import com.bable.b_backend.mappers.UserBody;
import com.bable.b_backend.models.User;
import com.bable.b_backend.repository.BlogRepository;
import com.bable.b_backend.repository.UserRepository;
import com.bable.b_backend.security.ArgonConfig;
import com.bable.b_backend.security.AuthContext;
import com.bable.b_backend.security.JWTConfig;
import com.bable.b_backend.utils.ResponseMapper;

@Service
public class UserService {

    // Auto Injection of User Repository to interact with Users Document
    @Autowired
    private UserRepository userRepo;

    // Auto Injection of User Repository to interact with Blogs Document
    @Autowired
    private BlogRepository blogRepo;

    // Auto Injection of Authorization Context -> Current User
    @Autowired
    private AuthContext authContext;


    // Creation of JWT Config instance
    @Autowired
    private JWTConfig config;


    // Helper function to create profile page's written and saved blogs per person
    private List<BlogItem> mapBlogItems(List<String> blogIds) {
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

    // Function to add new User
    public ResponseStatus handleAddUser(UserBody entity) {
        // Check for existing user -> dedupe                    
        Optional<User> existingUser = userRepo.findByEmail(entity.getEmail());
        if (existingUser.isPresent()) {
            return ResponseMapper.error(409, "User Already Exists");
        }

        try {
            // Hash password and create new user object in document
            String dbPass = ArgonConfig.hashPassword(entity.getPassword());
            User newUser = new User();
            newUser.setEmail(entity.getEmail());
            newUser.setName(entity.getName());
            newUser.setPassword(dbPass);
            newUser.setStoredBlogs(new ArrayList<>());
            newUser.setWrittenBlogs(new ArrayList<>());
            User savedUser = userRepo.save(newUser);

            return ResponseMapper.success(
                201,
                "User Created | ID : " + savedUser.getId()
            );
        } catch (Exception e) {
            return ResponseMapper.error(500, "Internal Server Error");
        }
    }

    // Function to handle user login
    public ResponseStatus handleLoginUser(UserBody entity) {
        // Check for user existence
        Optional<User> existingUser = userRepo.findByEmail(entity.getEmail());
        if (existingUser.isEmpty()) {
            return ResponseMapper.error(404, "No User Found");
        }

        // Get the user in DB and validate password
        User user = existingUser.get();
        boolean validatePassword = ArgonConfig.comparePassword(
            entity.getPassword(),
            user.getPassword()
        );

        // If wrong password -> Throw incorrect creds
        if (validatePassword == false) {
            return ResponseMapper.error(401, "Unauthorized");
        }
        // Create a new JWT Body from the User entity in request body
        JWTBody body = new JWTBody();
        body.setEmail(user.getEmail());
        body.setId(user.getId());
        body.setName(user.getName());

        // Create a new JWT token for the validated user and send to handler
        String jwtToken = config.generateJWTToken(body);
        return ResponseMapper.success(200, jwtToken);
        
        
        
    }

    // Function for profile page info
    public ProfileBody handleUserDetails(String Id) {
        // Check for existing user
        Optional<User> existingUser = userRepo.findById(Id);
        if (existingUser.isEmpty()) {
            return null;
        }

        // Extract list of blogs of both arrays
        List<BlogItem> writtenBlogItems = mapBlogItems(
            existingUser.get().getWrittenBlogs()
        );
        List<BlogItem> storedBlogItems = mapBlogItems(
            existingUser.get().getStoredBlogs()
        );

        // Create the profile object to send to handler
        ProfileBody currentUserDetails = new ProfileBody();
        currentUserDetails.setId(existingUser.get().getId());
        currentUserDetails.setName(existingUser.get().getName());
        currentUserDetails.setEmail(existingUser.get().getEmail());
        currentUserDetails.setWrittenBlogs(writtenBlogItems);
        currentUserDetails.setStoredBlogs(storedBlogItems);
        currentUserDetails.setUpdatedAt(existingUser.get().getUpdatedAt());
        currentUserDetails.setCreatedAt(existingUser.get().getCreatedAt());

        return currentUserDetails;
    }

    // Function to update user detail
    public ResponseStatus handleUserUpdate(User user) {
        JWTBody currentUser = authContext.getCurrentUser();

        // No logged in user -> unauthenticated
        if(currentUser == null) return ResponseMapper.error(401, "Unauthorized");

        // check if the request body's user ID matches the token's userID
        if (!currentUser.getId().equals(user.getId())) {
            return ResponseMapper.error(403, "Forbidden | Incorrect User");
        }

        // check for existing user via Token's User
        Optional<User> existingUser = userRepo.findById(currentUser.getId());
        if (existingUser.isEmpty()) {
            return ResponseMapper.error(404, "User Not Found");
        }

        // replace the content for the current user -> upsert to DB
        User dbUser = existingUser.get();
        dbUser.setName(user.getName());
        dbUser.setEmail(user.getEmail());
        userRepo.save(dbUser);

        return ResponseMapper.success(200, "Entity Updated");
    }
    
    
}
