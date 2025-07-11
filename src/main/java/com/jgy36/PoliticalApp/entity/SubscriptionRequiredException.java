package com.jgy36.PoliticalApp.entity;

public class SubscriptionRequiredException extends RuntimeException {
    public SubscriptionRequiredException(String message) {
        super(message);
    }
}
