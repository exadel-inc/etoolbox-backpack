package com.exadel.etoolbox.backpack.core.dto.response;

import java.util.List;

public class ResponseWrapper<T> {

    private T data;
    private ResponseStatus status;
    private List<String> logs;

    public ResponseWrapper(T data, ResponseStatus status, List<String> logs) {
        this.data = data;
        this.status = status;
        this.logs = logs;
    }

    public ResponseWrapper(T data, ResponseStatus status) {
        this.data = data;
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public List<String> getLogs() {
        return logs;
    }

    public enum ResponseStatus {
        SUCCESS, ERROR, WARNING
    }
}
