package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.HashSet;
import java.util.Set;

public class PathInfo {

    private Set<String> liveCopies;
    private Set<String> children;
    private Set<String> pages;
    private Set<String> tags;
    private Set<String> assets;

    public PathInfo() {
        this.liveCopies = new HashSet<>();
        this.children = new HashSet<>();
        this.pages = new HashSet<>();
        this.tags = new HashSet<>();
        this.assets = new HashSet<>();
    }

    public Set<String> getLiveCopies() {
        return liveCopies;
    }

    public Set<String> getChildren() {
        return children;
    }

    public Set<String> getPages() {
        return pages;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<String> getAssets() {
        return assets;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
