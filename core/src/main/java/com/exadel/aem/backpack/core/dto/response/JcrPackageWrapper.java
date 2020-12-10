package com.exadel.aem.backpack.core.dto.response;

import com.google.gson.Gson;
import org.apache.jackrabbit.vault.packaging.JcrPackage;

/**
 * Represents the package modification info.
 */
public class JcrPackageWrapper {

    private transient JcrPackage jcrPackage;
    private String message;
    private int statusCode;

    public void setJcrPackage(final JcrPackage jcrPackage) {
        this.jcrPackage = jcrPackage;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public JcrPackage getJcrPackage() {
        return jcrPackage;
    }

    /**
     * Gets error msg
     *
     * @return String
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets error status code
     *
     * @return int
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Checks jcr-package object is exist
     *
     * @return int
     */
    public boolean isExist() {
        return jcrPackage != null;
    }

    /**
     * Gets JSON representation of current object
     *
     * @return String
     */
    public String getJson() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }
}
