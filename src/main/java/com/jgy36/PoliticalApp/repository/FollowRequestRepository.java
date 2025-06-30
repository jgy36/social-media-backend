// PoliticalApp/src/main/java/com/jgy36/PoliticalApp/repository/FollowRequestRepository.java
package com.jgy36.PoliticalApp.repository;

import com.jgy36.PoliticalApp.entity.FollowRequest;
import com.jgy36.PoliticalApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {

    /**
     * Find pending requests for a target user
     */
    List<FollowRequest> findByTargetAndStatus(User target, FollowRequest.RequestStatus status);

    /**
     * Find requests between two users with a specific status
     */
    Optional<FollowRequest> findByRequesterAndTargetAndStatus(
            User requester,
            User target,
            FollowRequest.RequestStatus status);

    /**
     * Count pending requests for a user
     */
    long countByTargetAndStatus(User target, FollowRequest.RequestStatus status);

    /**
     * Find all requests by requester
     */
    List<FollowRequest> findByRequester(User requester);

    /**
     * Find all requests for target
     */
    List<FollowRequest> findByTarget(User target);
}
