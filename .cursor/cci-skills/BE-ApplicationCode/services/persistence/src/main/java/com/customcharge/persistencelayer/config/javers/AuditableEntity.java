package com.customcharge.persistencelayer.config.javers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableEntity {
    String groupName();

    AuditCommitProperty[] commitProperties() default {};
}
