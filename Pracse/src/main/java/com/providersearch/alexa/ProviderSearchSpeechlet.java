package com.providersearch.alexa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.slu.entityresolution.Resolution;
import com.amazon.speech.speechlet.Context;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Permissions;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.system.SystemInterface;
import com.amazon.speech.speechlet.interfaces.system.SystemState;
import com.amazon.speech.ui.AskForPermissionsConsentCard;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.providersearch.alexa.exceptions.DeviceAddressClientException;
import com.providersearch.alexa.exceptions.UnauthorizedException;
import com.providersearch.alexa.Address;


public class ProviderSearchSpeechlet implements SpeechletV2 {

	private static final Logger log = LoggerFactory.getLogger(ProviderSearchSpeechlet.class);
	
	/**
     * This is the default title that this skill will be using for cards.
     */
    private static final String ADDRESS_CARD_TITLE = "Device Address";
    
    /**
     * The key to get the item from the intent.
     */
    private static final String ADDRESS_DECIDER_SLOT = "addressDeciderSlot";

    /**
     * The permissions that this skill relies on for retrieving addresses. If the consent token isn't
     * available or invalid, we will request the user to grant us the following permission
     * via a permission card.
     *
     * Another Possible value if you only want permissions for the country and postal code is:
     * read::alexa:device:all:address:country_and_postal_code
     * Be sure to check your permissions settings for your skill on https://developer.amazon.com/
     */
    private static final String ALL_ADDRESS_PERMISSION = "read::alexa:device:all:address";

    private static final String ADDRESS_ERROR_TEXT = "There was an error with the requested device address.";
    
    private static final String MANUAL_ADDRESS_INPUT_TEXT = "User can search now with manual address input. You can tell me your postal code";
    
	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope){
		
		IntentRequest request = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();
		
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());

		Intent intent = request.getIntent();
		String intentName = intent.getName();

		log.info("Intent received: {}", intentName);

		if ("GetProviderSearchIntent".equals(intentName)) {
			
			return getProviderTypeLoadRequest(session);
		} else if ("GetProviderPhyIntent".equals(intentName)) {
			
			return getProviderPhysicianRequest(speechletRequestEnvelope);
		} else if ("DecideAddressIntent".equals(intentName)) {
			Slot addressDeciderSlot = intent.getSlot(ADDRESS_DECIDER_SLOT);
			
			if (addressDeciderSlot != null && addressDeciderSlot.getValue() != null) {
	            String addressDeciderValue = addressDeciderSlot.getValue();
	            List<Resolution> addressResolution =  addressDeciderSlot.getResolutions().getResolutionsPerAuthority();
	            String slotId = addressResolution.get(0).getValueWrappers().get(0).getValue().getId();		
	            // Create the plain text output.
				String speechOutput = "Address acknowledgement Intent requested with Slot ID , "+slotId;
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(speechOutput));
			}else {
				// Create the plain text output.
				String speechOutput = "Address decider is not working properly";
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(speechOutput));
			}
			
		} /*else if ("GetAppointementStatusIntent".equals(intentName)) {
			
			return handlePrescriptionRefillRequest(session);
		} */else if ("AMAZON.HelpIntent".equals(intentName)) {

			// Create the plain text output.
			String speechOutput = "Welcome to the Walgreens Rx, What information you want from your walgreens account?";
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(speechOutput));
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			//Stop Intent message
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech("Goodbye"));
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			//Cancel Intent Message
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech("Goodbye"));
		} else {
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech("Invalid Intent Requested"));
		}
	}

	/**
     * When our skill session is started, a launch event will be triggered. In the case of this
     * sample skill, we will return a welcome message, however the sky is the limit.
     * @param speechletRequestEnvelope container for the speechlet request.
     * @return SpeechletResponse our welcome message
     */
    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> speechletRequestEnvelope) {
        LaunchRequest launchRequest = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();

        log.info("onLaunch requestId={}, sessionId={}", launchRequest.getRequestId(),
            session.getSessionId());

        return getWelcomeResponse();
    }

    
	/**
     * Similar to onSessionStarted, this method will be fired when the skill session has been closed.
     * @param speechletRequestEnvelope container for the speechlet request.
     */
    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> speechletRequestEnvelope) {
        SessionEndedRequest sessionEndedRequest = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();

        log.info("onSessionEnded requestId={}, sessionId={}", sessionEndedRequest.getRequestId(),
            session.getSessionId());

    }

	/**
     * This is fired when a session is started. Here we could potentially initialize a session in
     * our own service for the user, or update a record in a table, etc.
     * @param speechletRequestEnvelope container for the speechlet request.
     */
    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> speechletRequestEnvelope) {
        SessionStartedRequest sessionStartedRequest = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();

        log.info("onSessionStarted requestId={}, sessionId={}", sessionStartedRequest.getRequestId(),
            session.getSessionId());
    }

	/**
	 * Function to handle the onLaunch skill behavior.
	 * 
	 * @return SpeechletResponse object with voice/card response to return to the user
	 */
	private SpeechletResponse getWelcomeResponse() {

		StringBuilder speechText = new StringBuilder ("Welcome to Centene's Provider Search, How can I help you? ");
		speechText.append("You can ask for Search Provider or Appointment Status.");

		SimpleCard card = getSimpleCard("Centene's Practitioner Search", speechText.toString());

        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText.toString());
        
		return SpeechletResponse.newAskResponse(speech, getReprompt(speech), card);
	}
	
	/**
	 * Function to handle the onLaunch skill behavior.
	 * 
	 * @return SpeechletResponse object with voice/card response to return to the user
	 */
	private SpeechletResponse getProviderPhysicianRequest(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {

		StringBuilder speechText = new StringBuilder (" You are searching for a physician. Please provide some more information to serve you better to find a specific physician for you. ");
		
		return getRequestClientAddress(speechText, requestEnvelope, requestEnvelope.getSession());

	}

	/**
	 * Get the claim status for the user from his payer account and return the details in both
	 * speech and SimpleCard format.
	 * 
	 * @param intent
	 *            the intent object which contains the date slot
	 * @param session
	 *            the session object
	 * @return SpeechletResponse object with voice/card response to return to the user
	 */
	private SpeechletResponse getProviderTypeLoadRequest(Session session) {

		StringBuilder speechText = new StringBuilder ("Please tell me what kind of provider you want to search about? ");
		speechText.append("You can ask for a Hospital or a Clinic or a Medical Practitioner.");

		
		SimpleCard card = getSimpleCard("Provider Type Load", speechText.toString());

        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText.toString());
        
		return SpeechletResponse.newAskResponse(speech, getReprompt(speech), card);
	}

	private SpeechletResponse getRequestClientAddress(StringBuilder speechText, SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope, Session session) {
		
		Permissions permissions = session.getUser().getPermissions();
        if (permissions == null) {
            log.info("The user hasn't authorized the skill. Sending a permissions card.");
            return getPermissionsResponse(speechText);
        }
        try {
            SystemState systemState = getSystemState(speechletRequestEnvelope.getContext());
            String apiAccessToken = systemState.getApiAccessToken();
            String deviceId = systemState.getDevice().getDeviceId();
            String apiEndpoint = systemState.getApiEndpoint();

            AlexaDeviceAddressClient alexaDeviceAddressClient = new AlexaDeviceAddressClient(
                deviceId, apiAccessToken, apiEndpoint);

            Address addressObject = alexaDeviceAddressClient.getFullAddress();

            if (addressObject == null) {
            	speechText.append(ADDRESS_ERROR_TEXT);
            	speechText.append(MANUAL_ADDRESS_INPUT_TEXT);
            	
            	SimpleCard card = getSimpleCard(ADDRESS_CARD_TITLE, speechText.toString());
            	
            	String addressRepromt = "Please tell me your postal code.";
            	Reprompt reprompt = getReprompt(getPlainTextOutputSpeech(addressRepromt));
            	
                return SpeechletResponse.newAskResponse(getPlainTextOutputSpeech(speechText.toString()), reprompt, card);
            }

            return getAddressResponse(speechText, 
                addressObject.getAddressLine1(),
                addressObject.getStateOrRegion(),
                addressObject.getPostalCode());
        } catch (UnauthorizedException e) {
            return getPermissionsResponse(speechText);
        } catch (DeviceAddressClientException e) {
            log.error("Device Address Client failed to successfully return the address.", e);
            speechText.append(ADDRESS_ERROR_TEXT);
            speechText.append(MANUAL_ADDRESS_INPUT_TEXT);
            
            SimpleCard card = getSimpleCard(ADDRESS_CARD_TITLE, speechText.toString());
        	
        	String addressRepromt = "Please tell me your postal code.";
        	Reprompt reprompt = getReprompt(getPlainTextOutputSpeech(addressRepromt));
        	
            return SpeechletResponse.newAskResponse(getPlainTextOutputSpeech(speechText.toString()), reprompt, card);
        }
	}
	
	/**
     * Creates a {@code SpeechletResponse} for the GetAddress intent.
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getAddressResponse(StringBuilder speechText, String streetName, String state, String zipCode) {
    	
        speechText.append("Your search address is defaulted with your device address as, " + streetName + " " + state + ", " + zipCode+".");
        speechText.append("Do you want to proceed search with the device address?");
        
        SimpleCard card = getSimpleCard(ADDRESS_CARD_TITLE, speechText.toString());

        String addressRepromt = "Do you want proceed the search with device address?";
    	Reprompt reprompt = getReprompt(getPlainTextOutputSpeech(addressRepromt));
    	
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText.toString());

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
	
	/**
     * Creates a {@code SpeechletResponse} for permission requests.
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getPermissionsResponse(StringBuilder speechText) {
        speechText.append("You have not given this skill permissions to access your device address. ");
        speechText.append("Please give this skill permissions to access your address.");

        // Create the permission card content.
        // The differences between a permissions card and a simple card is that the
        // permissions card includes additional indicators for a user to enable permissions if needed.
        AskForPermissionsConsentCard card = new AskForPermissionsConsentCard();
        card.setTitle(ADDRESS_CARD_TITLE);

        Set<String> permissions = new HashSet<>();
        permissions.add(ALL_ADDRESS_PERMISSION);
        card.setPermissions(permissions);

        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText.toString());

        return SpeechletResponse.newTellResponse(speech, card);
    }
    
    /**
     * Helper method that creates a card object.
     * @param title title of the card
     * @param content body of the card
     * @return SimpleCard the display card to be sent along with the voice response.
     */
    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    /**
     * Helper method that will get the intent name from a provided Intent object. If a name does not
     * exist then this method will return null.
     * @param intent intent object provided from a skill request.
     * @return intent name or null.
     */
    private String getIntentName(Intent intent) {
        return (intent != null) ? intent.getName() : null;
    }

    /**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    /**
     * Helper method that returns a reprompt object. This is used in Ask responses where you want
     * the user to be able to respond to your speech.
     * @param outputSpeech The OutputSpeech object that will be said once and repeated if necessary.
     * @return Reprompt instance.
     */
    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    /**
     * Helper method that retrieves the system state from the request context.
     * @param context request context.
     * @return SystemState the systemState
     */
    private SystemState getSystemState(Context context) {
        return context.getState(SystemInterface.class, SystemState.class);
    }
    
    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     * @param cardTitle Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    private SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}
