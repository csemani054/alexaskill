package com.providersearch.alexa.exceptions;

public class SearchClientException extends Exception  {

    public SearchClientException(String message, Exception e) {
        super(message, e);
    }

    public SearchClientException(String message) {
        super(message);
    }

    public SearchClientException(Exception e) {
        super(e);
    }
}


