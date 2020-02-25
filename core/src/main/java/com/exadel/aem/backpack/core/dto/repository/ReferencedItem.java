package com.exadel.aem.backpack.core.dto.repository;

import java.util.Objects;

public class ReferencedItem {
    private String path;

    public ReferencedItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferencedItem that = (ReferencedItem) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
