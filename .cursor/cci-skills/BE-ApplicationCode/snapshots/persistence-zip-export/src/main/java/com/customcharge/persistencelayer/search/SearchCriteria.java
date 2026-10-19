package com.customcharge.persistencelayer.search;

import java.io.Serializable;

public class SearchCriteria implements Serializable {

    private String fieldName;

    private String condition;

    private Object value;

    private Object secondValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public SearchCriteria(String fieldName, String condition, Object value, Object secondValue) {
        this.fieldName = fieldName;
        this.condition = condition;
        this.value = value;
        this.secondValue = secondValue;
    }

    public SearchCriteria(String fieldName, String condition, Object value) {
        this.fieldName = fieldName;
        this.condition = condition;
        this.value = value;
    }

    public SearchCriteria() {
    }

    public Object getSecondValue() {
        return secondValue;
    }

    public void setSecondValue(Object secondValue) {
        this.secondValue = secondValue;
    }
}
