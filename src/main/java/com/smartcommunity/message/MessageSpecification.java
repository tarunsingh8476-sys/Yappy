package com.smartcommunity.message;

import com.smartcommunity.message.dto.MessageSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageSpecification {

    public static Specification<Message> withCriteria(MessageSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getQuery())) {
                predicates.add(cb.like(cb.lower(root.get("content")), "%" + criteria.getQuery().trim().toLowerCase() + "%"));
            }

            if (criteria.getChannelId() != null) {
                predicates.add(cb.equal(root.get("channel").get("id"), criteria.getChannelId()));
            }

            if (criteria.getConversationId() != null) {
                predicates.add(cb.equal(root.get("conversation").get("id"), criteria.getConversationId()));
            }

            if (criteria.getSenderId() != null) {
                predicates.add(cb.equal(root.get("sender").get("id"), criteria.getSenderId()));
            }

            if (criteria.getPinned() != null) {
                predicates.add(cb.equal(root.get("pinned"), criteria.getPinned()));
            }

            if (StringUtils.hasText(criteria.getStatus())) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            } else {
                // By default do not return REMOVED messages
                predicates.add(root.get("status").in("PUBLISHED", "HELD_FOR_REVIEW"));
            }

            if (criteria.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getStartDate()));
            }

            if (criteria.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getEndDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
