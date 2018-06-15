package com.providersearch.alexa;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public class ProviderSearchSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

	private static final Set<String> supportedApplicationIds = new HashSet<String>();
	static {
		/*
		 * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
		 * Alexa Skill and put the relevant Application Ids in this Set.
	      	       supportedApplicationIds.add("amzn1.ask.skill.1d323a1c-89ef-4917-8549-f3b5e5794c5d");
		 */
		supportedApplicationIds.add("amzn1.ask.skill.ba6aefe3-37dc-4c51-91a5-df68526a6e7c");
	}

	public ProviderSearchSpeechletRequestStreamHandler() {
		super(new ProviderSearchSpeechlet(), supportedApplicationIds);
	}
}