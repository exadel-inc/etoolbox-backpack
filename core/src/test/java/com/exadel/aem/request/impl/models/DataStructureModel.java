package com.exadel.aem.request.impl.models;

import com.exadel.aem.request.annotations.RequestMapping;
import com.exadel.aem.request.annotations.RequestParam;

import java.util.List;

@RequestMapping
public class DataStructureModel {

    @RequestParam(name = "listOfStrings")
    private List<String> list;

    @RequestParam(name = "arrayOfIntegers")
    private Integer[] integers;

    public List<String> getList() {
        return list;
    }

    public Integer[] getIntegers() {
        return integers;
    }
}
