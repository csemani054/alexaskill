package com.providersearch.alexa.exceptions;

public class GeneralSpeechletException extends Exception{
	public GeneralSpeechletException(String message, Exception e) {
        super(message, e);
    }

    public GeneralSpeechletException(String message) {
        super(message);
    }

    public GeneralSpeechletException(Exception e) {
        super(e);
    }
}


