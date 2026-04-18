package com.bable.b_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bable.b_backend.mappers.ProfileBody;
import com.bable.b_backend.mappers.ResponseStatus;
import com.bable.b_backend.mappers.UserBody;
import com.bable.b_backend.mappers.UserInfo;
import com.bable.b_backend.mappers.UserLoginInfo;
import com.bable.b_backend.models.User;
import com.bable.b_backend.services.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    
    // Auto Injection of User Service Logic
    @Autowired
    private UserService userServe;


    // Route for Creating New Account
    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody UserBody entity) {
        ResponseStatus responseMessage = userServe.handleAddUser(entity);
        return ResponseEntity.status(responseMessage.getCode()).body(responseMessage.getStringBody());

    }

    // Route for Login User
    @PostMapping("/login-user")
    public ResponseEntity<UserInfo> loginUser(@RequestBody UserBody entity) {

        // Returns JWT Token on User Validation via String Body
        UserLoginInfo currInfo = userServe.handleLoginUser(entity);


        // If User was found -> return JWT in Authorization header for bearer auth
        if (currInfo.isLoggedIn()) {
            UserInfo authUser = new UserInfo();
            authUser.setName(currInfo.getName());
            authUser.setEmail(currInfo.getEmail());
            authUser.setId(currInfo.getId());

            return ResponseEntity
                    .status(currInfo.getCode())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + currInfo.getToken())
                    .body(authUser);

        }
        UserInfo errorAuthUser = new UserInfo();
            errorAuthUser.setName("Null");
            errorAuthUser.setEmail("Null");
            errorAuthUser.setId("Null");

        // Any kind of Error with status Code
        return ResponseEntity.status(currInfo.getCode()).body(errorAuthUser);

    }
    
    // Route for User Detail, for Profile Page
    @GetMapping ("/details-user")
    public ResponseEntity <ProfileBody> getCurrentUser (@RequestParam String id){
        ProfileBody currentUser = userServe.handleUserDetails(id);
        if (currentUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(currentUser);
        
    } 
    
    // Route for User Details Updation -> Behind Authorized Wall
    @PostMapping ("/update-user")
    public ResponseEntity <String> updateUser (@RequestBody User user){
        ResponseStatus status = userServe.handleUserUpdate(user);
        return ResponseEntity.status(status.getCode()).body(status.getStringBody());
    }
}
