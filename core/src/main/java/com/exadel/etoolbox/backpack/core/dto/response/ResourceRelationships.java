package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the wrapper for a collection of active paths and broken paths obtained as a result of the search
 */
public class ResourceRelationships {

    private List<String> validPaths;
    private List<String> brokenPaths;

    public ResourceRelationships(List<String> validPaths, List<String> brokenPaths) {
        this.validPaths = validPaths != null ? validPaths : new ArrayList<>();
        this.brokenPaths = brokenPaths != null ? brokenPaths : new ArrayList<>();
    }

    public List<String> getValidPaths() {
        return validPaths;
    }

    public void setValidPaths(List<String> validPaths) {
        this.validPaths = validPaths != null ? validPaths : new ArrayList<>();;
    }

    public List<String> getBrokenPaths() {
        return brokenPaths;
    }

    /**
     * Appends the provided {@link ResourceRelationships} object to the current one
     *
     * @param pathRelationships {@code ResourceRelationships} object
     * @return ResourceRelationships object
     */
    public ResourceRelationships concat(ResourceRelationships pathRelationships) {
        this.validPaths = concatPaths(this.validPaths, pathRelationships.getValidPaths());
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
