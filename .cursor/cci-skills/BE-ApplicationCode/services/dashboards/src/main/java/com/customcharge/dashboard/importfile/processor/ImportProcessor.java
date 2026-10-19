package com.customcharge.dashboard.importfile.processor;

public interface ImportProcessor {

    String screenName();

    ImportResult process(ImportContext context);
}
