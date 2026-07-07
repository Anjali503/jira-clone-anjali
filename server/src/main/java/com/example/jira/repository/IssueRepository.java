package com.example.jira.repository;

import com.example.jira.model.Issue;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface IssueRepository extends MongoRepository<Issue, ObjectId> {

    List<Issue> findByProjectId(String projectId);

    List<Issue> findBySprintId(String sprintId);

    // Phase 1: Subtask support
    List<Issue> findByParentId(String parentId);

    // Phase 1: Dependency support (used in Phase 2 for notifications)
    List<Issue> findByDependenciesContaining(String dependencyId);

    List<Issue> findByDueDateBetween(Instant start, Instant end);
}

