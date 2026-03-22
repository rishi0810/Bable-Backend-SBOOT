package com.bable.b_backend.utils;

import com.bable.b_backend.mappers.ResponseStatus;


// Helper function to create universal Response Statuses

public class ResponseMapper {
    private ResponseMapper() {
    }

    // Function to create a new Status with code, success boolean and message
    public static ResponseStatus create(int code, boolean success, String stringBody) {
        ResponseStatus response = new ResponseStatus();
        response.setCode(code);
        response.setSuccess(success);
        response.setStringBody(stringBody);
        return response;
    }

    // Function for error
    public static ResponseStatus error(int code, String message) {
        return create(code, false, message);
    }

    // Function for success
    public static ResponseStatus success(int code, String body) {
        return create(code, true, body);
    }
}
