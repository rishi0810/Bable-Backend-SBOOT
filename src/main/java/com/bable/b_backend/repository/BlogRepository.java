package com.bable.b_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bable.b_backend.models.Blog;

// Mongo Repo for queries
public interface BlogRepository extends MongoRepository<Blog, String> {
}
