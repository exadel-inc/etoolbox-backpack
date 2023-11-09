package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the wrapper for a collection of active paths and broken paths obtained as a result of the search.
 */
public class ResourceRelationships {

    private List<String> actualPaths;
    private List<String> brokenPaths;

    public ResourceRelationships(List<String> actualPaths, List<String> brokenPaths) {
        this.actualPaths = actualPaths != null ? actualPaths : new ArrayList<>();
        this.brokenPaths = brokenPaths != null ? brokenPaths : new ArrayList<>();
    }

    public List<String> getActualPaths() {
        return actualPaths;
    }

    public void setActualPaths(List<String> actualPaths) {
        this.actualPaths = actualPaths != null ? actualPaths : new ArrayList<>();;
    }

    public List<String> getBrokenPaths() {
        return brokenPaths;
    }

    /**
     * Concatenation two ResourceRelationships objects into one
     *
     * @param pathRelationships {@code ResourceRelationships} object
     * @return ResourceRelationships object
     */
    public ResourceRelationships concat(ResourceRelationships pathRelationships) {
        this.actualPaths = concatPaths(this.actualPaths, pathRelationships.getActualPaths());
        this.brokenPaths = concatPaths(this.brokenPaths, pathRelationships.getBrokenPaths());
        return this;
    }

    /**
     * Concatenation two List objects into one with null value filtering
     *
     * @param base {@code List<String>} object
     * @param renewed {@code List<String>} object
     * @return List<String> object
     */
    private List<String> concatPaths(List<String> base, List<String> renewed) {
        return Stream.of(base, renewed)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
