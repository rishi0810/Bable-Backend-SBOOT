package com.bable.b_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bable.b_backend.mappers.BlogBody;
import com.bable.b_backend.mappers.BlogDisplay;
import com.bable.b_backend.mappers.BlogListing;
import com.bable.b_backend.mappers.ResponseStatus;
import com.bable.b_backend.services.BlogService;


@RestController
@RequestMapping("/blog")
public class BlogController {

    // Auto Bean Injection of Blogging Service
    @Autowired
    private BlogService blogServe;

    // @Autowired 
    // private AuthContext authContext;

    // Route to get the main feed for explore page
    @GetMapping("/main-feed")
    public ResponseEntity<List<BlogListing>> getBlogFeed() {
        List<BlogListing> feed = blogServe.getBlogFeed();
        if (feed == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(feed);
    }

    // Route to create a new blog
    @PostMapping("/create-blog")
    public ResponseEntity <String> createNewBlog(@RequestBody BlogBody entity) {
        ResponseStatus status = blogServe.createNewBlog(entity);
        return ResponseEntity.status(status.getCode()).body(status.getStringBody());
        
    }

    // Route to delete a blog
    @DeleteMapping("/delete-blog")
    public ResponseEntity <String> deleteBlogByID(@RequestParam String id) {
        ResponseStatus result = blogServe.deleteBlogByID(id);
        return ResponseEntity.status(result.getCode()).body(result.getStringBody());
    }  

    // Route to get the displaying content for a blog
    @GetMapping("/blog-content")
    public ResponseEntity <BlogDisplay> getBlogContent(@RequestParam String id) {
        BlogDisplay thisBlog = blogServe.getDisplayBlog(id);
        if (thisBlog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(thisBlog);


    }

    // Route to save a blog to stored blog
    @GetMapping("/save-blog")
    public ResponseEntity <String> saveToStored(@RequestParam String id) {
        ResponseStatus result = blogServe.addBlogToSave(id);
        return ResponseEntity.status(result.getCode()).body(result.getStringBody());
    }

    // Route to remove a blog from saved blog
    @GetMapping("/delete-save")
    public ResponseEntity <String> deleteFromSaved(@RequestParam String id) {
        ResponseStatus result = blogServe.removeBlogFromSave(id);
        return ResponseEntity.status(result.getCode()).body(result.getStringBody());
    }

    
    
    
    
    
}
