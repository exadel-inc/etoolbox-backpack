package com.exadel.aem.request.validator;

import java.util.ArrayList;
import java.util.List;

public class ValidatorResponse<T> {

    private boolean valid;
    private List<String> log = new ArrayList<>();
    private T model;

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public List<String> getLog() {
        return log;
    }

    public void setLog(final List<String> log) {
        this.log = log;
    }

    public T getModel() {
        return model;
    }

    public void setModel(final T model) {
        this.model = model;
    }
}
