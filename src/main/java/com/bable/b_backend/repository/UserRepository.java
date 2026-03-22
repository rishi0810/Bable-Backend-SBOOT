package com.bable.b_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.bable.b_backend.models.User;

// Mongo Repo for User queries

@Repository
public interface UserRepository extends MongoRepository<User,String> {
    // Find user by email matching
    Optional<User> findByEmail(String email);

    // Author name view -> {"author" : "name" , "id" : "6aeeh...."}
    @Query(value = "{ '_id': ?0 }", fields = "{ 'name': 1 }")
     Optional<AuthorNameView> findAuthorById(String id);

    // Bulk of Author name view for a list of query ids
    @Query(value = "{ '_id': { $in: ?0 } }", fields = "{ 'name': 1 }")
    List<AuthorNameView> findAuthorsByIdIn(List<String> ids);
      interface AuthorNameView {
          String getId();
          String getName();
      }

    // List of users having a blog id in stored blog array
    List<User> findByStoredBlogsContaining(String id);
}
