package com.alibaba.dubbo.container.page;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Page {

    private final String navigation;
    
    private final String title;

    private final List<String> columns;

    private final List<List<String>> rows;
    
    public Page(String navigation) {
        this(navigation, (String) null, (String[]) null, (List<List<String>>) null);
    }

    public Page(String navigation, String title,
                       String column, String row) {
        this(navigation, title, column == null ? null : Arrays.asList(new String[]{column}), row == null ? null : stringToList(row));
    }
    
    private static List<List<String>> stringToList(String str) {
        List<List<String>> rows = Lists.newArrayList();
        List<String> row =  Lists.newArrayList();
        row.add(str);
        rows.add(row);
        return rows;
    }

    public Page(String navigation, String title,
                       String[] columns, List<List<String>> rows) {
        this(navigation, title, columns == null ? null : Arrays.asList(columns), rows);
    }

    public Page(String navigation, String title,
                       List<String> columns, List<List<String>> rows) {
        this.navigation = navigation;
        this.title = title;
        this.columns = columns;
        this.rows = rows;
    }

    public String getNavigation() {
        return navigation;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }

}