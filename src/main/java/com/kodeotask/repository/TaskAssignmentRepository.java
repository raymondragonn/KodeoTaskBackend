package com.kodeotask.repository;

import com.kodeotask.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByTaskId(Long taskId);
    List<TaskAssignment> findByUserId(Long userId);
    void deleteByTaskId(Long taskId);
    void deleteByTaskIdAndUserId(Long taskId, Long userId);
}


