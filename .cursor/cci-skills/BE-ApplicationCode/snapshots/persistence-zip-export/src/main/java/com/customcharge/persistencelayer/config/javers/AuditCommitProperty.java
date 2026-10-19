package com.customcharge.persistencelayer.config.javers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditCommitProperty {
    String key();

    String path();

    AuditValueNormalize normalize() default AuditValueNormalize.NONE;

    boolean required() default true;
}
