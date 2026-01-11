package com.communitcation.rest.lock;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReqeustLock {
    String sessionKey() default "#session.id";
    long timeout() default 10L;
}
