package com.jgy36.PoliticalApp.annotation;

import com.jgy36.PoliticalApp.entity.SubscriptionTier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSubscription {
    SubscriptionTier tier() default SubscriptionTier.ESSENTIAL;
    String feature() default "";
    String message() default "This feature requires a subscription upgrade";
}
