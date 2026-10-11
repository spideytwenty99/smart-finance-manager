package com.saurabh.financemanager.category;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeywordCleaner {
    public static final int MIN_LENGTH=3;

    public KeywordCleaner(){

    }
    public static String clean(String raw){
        if (raw==null || raw.isBlank()){
            return null;
        }

        Set<String> keywords=new LinkedHashSet<>();

        for (String keyword: raw.split(",")){
            keyword=keyword.trim().toLowerCase();

            if (keyword.isEmpty()){
                continue;
            }
            if (keyword.length()<MIN_LENGTH){
                throw  new IllegalArgumentException("Keyword must have at lest 3 letters "+keyword);
            }
            keywords.add(keyword);
        }
        if (keywords.isEmpty()){
            return null;
        }
        return String.join(",", keywords);
    }
}
