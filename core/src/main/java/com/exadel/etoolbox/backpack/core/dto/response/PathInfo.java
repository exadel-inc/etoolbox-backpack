package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.HashSet;
import java.util.Set;

public class PathInfo {

    private Set<String> references;
    private Set<String> liveCopies;
    private Set<String> children;

    public PathInfo() {
        this.references = new HashSet<>();
        this.liveCopies = new HashSet<>();
        this.children = new HashSet<>();
    }

    public Set<String> getLiveCopies() {
        return liveCopies;
    }

    public Set<String> getChildren() {
        return children;
    }

    public Set<String> getReferences() {
        return references;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
