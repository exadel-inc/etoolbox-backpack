package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.*;

public class PathInfo {

    private Map<String, List<String>> references;
    private Set<String> liveCopies;
    private Set<String> children;

    public PathInfo() {
        this.references = new HashMap<>();
        this.liveCopies = new HashSet<>();
        this.children = new HashSet<>();
    }

    public Map<String, List<String>> getReferences() {
        return references;
    }

    public Set<String> getLiveCopies() {
        return liveCopies;
    }

    public Set<String> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
