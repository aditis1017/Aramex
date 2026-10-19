package com.customcharge.persistencelayer.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Getter
@Setter
@Component
public class TransactionIdUtil {
    public static final Random RANDOM = new SecureRandom();

    private TransactionIdUtil(){}
    public static String generateRandomId() {
        return String.valueOf(RANDOM.nextInt(999999));
    }

    public static double formatToDecimalPoint(double val,int decimalPoint) {
        var nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumFractionDigits(decimalPoint);
        nf.setMaximumFractionDigits(decimalPoint);
        return Double.parseDouble(nf.format(val));
    }


    public static <T> List<T>  convertList(List<?> sourceList, Class<T> toListof) {
        return sourceList.stream().map(element -> JsonUtils.getObjectMapper().convertValue(element, toListof)).toList();
    }

    public static ObjectMapper getObjectMapper() {
        return JsonUtils.getObjectMapper();
    }

    public static <T> T convertObject(Object object, Class<T> toObjectOf) {
        return JsonUtils.getObjectMapper().convertValue(object,toObjectOf);
    }


    public static String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        emptyNames.add("uuid");
        for(java.beans.PropertyDescriptor pd : pds) {
            if(pd.getName().equals("uuid")) continue;
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null || ObjectUtils.isEmpty(srcValue)) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public static void copy(Object source,Object target) {
        BeanUtils.copyProperties(source,target,getNullPropertyNames(source));
    }

    public static LocalDateTime localDateTimeFormator(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(date, formatter).atStartOfDay();
    }

    public static <T> T readValueString(Object object, Class<T> toObjectOf) {
        try {
            return getObjectMapper().readValue((String) object, toObjectOf);
        } catch (Exception e) {
            return convertObject(object, toObjectOf);
        }
    }

    public static <T> PageImpl<T> createPage(List<T> list, int page, int size) {
        Pageable paging = PageRequest.of(page, size);
        int start = Math.min((int)paging.getOffset(), list.size());
        int end = Math.min((start + paging.getPageSize()),list.size());
        return new PageImpl<>(list.subList(start, end), paging, list.size());
    }

    public static <T> PageImpl<T> createPage(List<T> list, int page, int size, long totalListSize) {
        Pageable paging = PageRequest.of(page, size);
        return new PageImpl<>(list, paging, totalListSize);
    }

    public static  <T> T convert(String string, Class<T> toObjectOf) {
        return JsonUtils.convert(string,toObjectOf);
    }
}