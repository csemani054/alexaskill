package com.providersearch.alexa;

import java.util.ArrayList;
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
import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.IntentRequest.DialogState;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Permissions;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.dialog.directives.DelegateDirective;
import com.amazon.speech.speechlet.dialog.directives.DialogIntent;
import com.amazon.speech.speechlet.interfaces.system.SystemInterface;
import com.amazon.speech.speechlet.interfaces.system.SystemState;
import com.amazon.speech.ui.AskForPermissionsConsentCard;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.providersearch.alexa.exceptions.DeviceAddressClientException;
import com.providersearch.alexa.exceptions.SearchClientException;
import com.providersearch.alexa.exceptions.UnauthorizedException;


public class ProviderSearchSpeechlet implements SpeechletV2 {

	private static final Logger log = LoggerFactory.getLogger(ProviderSearchSpeechlet.class);
	
    /**
     * Check whether the call is from Provider Search Generic Intent or Specific Provider Intent
     */
    private static final String GENERIC_INTENT_FLAG = "genericIntentFlag";
    
    /**
     * Slots which gets User Inputs
     */
    private static final String PROVIDER_TYPE_SLOT = "providerType";
    private static final String ADDRESS_DECIDER_SLOT = "addressDecider";
    private static final String PINCODE_SLOT = "manualPincode";
    private static final String MILES_VALUE_SLOT = "milesValue";
    private static final String TRICARE_PLAN_SLOT = "tricarePlan";
    private static final String GENDER_TYPE_SLOT = "genderType";
    private static final String SPECIALITY_TYPE_SLOT = "specialityType";
    private static final String FIRSTNAME_SLOT = "firstName";
    
    
    /**
     * Generic Message Strings
     */
    private static final String GENERIC_INTENT_MAPPING_ERROR = "Your speech response ended with a wrong request. Kindly give a proper response next time, thanks.";
    private static final String GENERIC_INTENT_NOT_FOUND_ERROR = "Requested response not found, Still I am learning with the new things, Please try again.";
    private static final String PROVIDER_DETAILS_YESNO_QUES = "PROVIDER_DETAILS_YESNO_QUES";
    private static final String MANUAL_ADDRESS = "MANUAL_ADDRESS";
    private static final String PROVIDER_TYPE_DESC = "PROVIDER_TYPE_DESC";
    private static final String PLAN_TYPE_DESC = "PLAN_TYPE_DESC";
    private static final String YESNO_INTENT_QUES_TYPE = "YESNO_INTENT_QUES_TYPE";
    private static final String SEARCH_YESNO_QUES = "SEARCH_YESNO_QUES";
    
    
    
    /**
     * Plain or SSML text for VUI and Card
     */
    private static final String ADDRESS_ERROR_TEXT = "There was an error with the requested device address";
    
    private static final String DEVICE_ADDRESS_SEARCH = "Search continued with your Device address, ";
    
    private static final String MILES_AROUND = "Please tell me the radius of your search <prosody rate=\"medium\"> in miles </prosody>";
    
    private static final String MANUAL_ADDRESS_INPUT_TEXT = "User can search now with manual address input. <s> You can tell me your postal code</s> <break time=\"0.1s\" /> <s>For Example, my postal code is <say-as interpret-as=\"digits\"> 90125 </say-as> </s>";
    
    private static final String TRICARE_PLAN_TYPE_TEXT = "You can now, tell me your <prosody rate=\"slow\"> Tricare Plan type </prosody> which you belongs to <s> For example Tricare Prime, Tricare Prime Remote </s>";
    
    private static final String PROVIDER_DETAILS_INPUT_TEXT = "<s>Please share the details of the provider such as provider name, gender and speciality.</s>";    
    
    private static final String PROVIDER_DETAILS_REPROMT_TEXT = "<prosody rate=\"medium\"> <s> You can ask for provider filter to add information.</s> <break time=\"0.1s\" /> <s>In that case, you dont want to add any of the "
    		+ "filter, just say skip or <break time=\"0.1s\" /> don't know </s> <break time=\"0.1s\" /> <s> Kindly note, if you skip the filter, it would search for all the possibility. </s> </prosody>";
    
    private static final String NO_SEARCH_TEXT = "<s>Thanks for using Centene's provider search, voice interface.</s> <s> Please come again.</s>";
    
    /**
     * Card Title
     */
    private static final String ADDRESS_CARD_TITLE = "Device Address";
    
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
    
	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope){
		
		IntentRequest request = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();
		
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());

		Intent intent = request.getIntent();
		String intentName = intent.getName();
		//Get Dialog State
        
		log.info("Intent received: {}", intentName);

		if ("GetProviderSearchIntent".equals(intentName)) {
			
			session.setAttribute(GENERIC_INTENT_FLAG, "TRUE");
			return getDialogDirective(speechletRequestEnvelope);
		} else if ("GetProviderPhyIntent".equals(intentName)) {
			
			///// --------------------  Setting Session Values started ----------------- //////
			session.setAttribute(PROVIDER_TYPE_SLOT, "P");
			session.setAttribute(PROVIDER_TYPE_DESC, "Physician");
			///// --------------------  Setting Session Values ended ----------------- //////
			
			return getProviderPhysicianRequest(speechletRequestEnvelope);
		} else if ("DecideAddressIntent".equals(intentName)) {
			
			return getDecideAddressRequest(intent);
		} else if ("GetManualAddressInput".equals(intentName)) {
			
			return getDialogDirective(speechletRequestEnvelope);
		} else if ("GetMilesIntent".equals(intentName)) {
			
			return getDialogDirective(speechletRequestEnvelope);
		} else if("GetTricarePlanIntent".equals(intentName)) {
			
			return getDialogDirective(speechletRequestEnvelope);
		} else if("GetProviderFilterIntent".equals(intentName)) {
			
			return getDialogDirective(speechletRequestEnvelope);
		} else if("AMAZON.YesIntent".equals(intentName)) {
			
			//Getting YESNO question type
			String yesNoQuesType = (String)session.getAttribute(YESNO_INTENT_QUES_TYPE);
			
			///// --------------------  Setting Session Values started ----------------- //////
			session.setAttribute(YESNO_INTENT_QUES_TYPE, "");
			///// --------------------  Setting Session Values ended ----------------- //////
			
			if(yesNoQuesType != null && !yesNoQuesType.trim().equals("")) {
				
				switch(yesNoQuesType) {
					case PROVIDER_DETAILS_YESNO_QUES:
						
						return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(PROVIDER_DETAILS_INPUT_TEXT+PROVIDER_DETAILS_REPROMT_TEXT), getReprompt(getSSMLOutputSpeech(PROVIDER_DETAILS_REPROMT_TEXT)), getSimpleCard("Provider Details Filter - Input", "Filter to get provider details"));
					
					case SEARCH_YESNO_QUES:
						
					try {
						return getProviderSearch();
					} catch (UnauthorizedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SearchClientException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						
					default:
						//Intent Mapping Error Message
						return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_MAPPING_ERROR));
				}
				
			}else {
				//Intent Mapping Error Message
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_MAPPING_ERROR));
			}
		} else if("AMAZON.NoIntent".equals(intentName)) {
			
			//Getting YESNO question type
			String yesNoQuesType = (String)session.getAttribute(YESNO_INTENT_QUES_TYPE);
			
			///// --------------------  Setting Session Values started ----------------- //////
			session.setAttribute(YESNO_INTENT_QUES_TYPE, "");
			///// --------------------  Setting Session Values ended ----------------- //////
			
			if(yesNoQuesType != null && !yesNoQuesType.trim().equals("")) {
				
				switch(yesNoQuesType) {
					case PROVIDER_DETAILS_YESNO_QUES:
					
						try {
							return getProviderSearch();
						} catch (UnauthorizedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SearchClientException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					case SEARCH_YESNO_QUES:
						
						return SpeechletResponse.newTellResponse(getSSMLOutputSpeech(NO_SEARCH_TEXT));
					default:
						//Intent Mapping Error Message
						return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_MAPPING_ERROR));
				}
				
			}else {
				//Intent Mapping Error Message
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_MAPPING_ERROR));
			}
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			
			// Create the plain text output.
			String speechOutput = "Welcome to the Centene Helpdesk, What kind of information you want?";
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(speechOutput));
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			
			//Stop Intent message
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech("Goodbye"));
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			
			//Cancel Intent Message
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech("Goodbye"));
		} else {
			
			return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_NOT_FOUND_ERROR));
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
	 * Welcome voice text
	 * @return SpeechletResponse
	 */
	private SpeechletResponse getWelcomeResponse() {

		StringBuilder speechText = new StringBuilder ("Welcome to Centene's Provider Search, How can I help you? ");
		speechText.append("You can ask, Search for Specific Provider Type or Appointment Status. For Example, Search for physicians, find nearby hospitals, practitioner, hospitals, clinics and so on.");

		SimpleCard card = getSimpleCard("Centene's Practitioner Search", speechText.toString());

        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText.toString());
        
		return SpeechletResponse.newAskResponse(speech, getReprompt(speech), card);
	}
	
	/**
	 * Function to handle the Specific Provider Type - Physician Indent
	 * @param requestEnvelope
	 * @return SpeechletResponse
	 */
	private SpeechletResponse getProviderPhysicianRequest(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {

		StringBuilder speechText = new StringBuilder (" You are searching for a physician. Please provide some more information to serve you better to find a specific physician for you. ");
		
		return getRequestClientAddress(speechText, requestEnvelope, requestEnvelope.getSession());

	}
	
	/**
	 * Function to decide the user to go for either Manual or Device address
	 * @param intent
	 * @return SpeechletResponse
	 */
	private SpeechletResponse getDecideAddressRequest(Intent intent) {
		//Defaulted address to DEVICE_ADDRESS
		String speechText = DEVICE_ADDRESS_SEARCH+MILES_AROUND;
        SimpleCard card = getSimpleCard("Device Address - Search", speechText.toString());
		Slot addressDeciderSlot = intent.getSlot(ADDRESS_DECIDER_SLOT);
		if (addressDeciderSlot != null && addressDeciderSlot.getValue() != null) {
            List<Resolution> addressResolution =  addressDeciderSlot.getResolutions().getResolutionsPerAuthority();
            String slotId = addressResolution.get(0).getValueWrappers().get(0).getValue().getId();
            log.info("Address acknowledgement Intent requested with Slot ID {}", slotId);
            //MANUAL_ADDRESS
            if(slotId != null && slotId.equals(MANUAL_ADDRESS)) {
            	speechText = MANUAL_ADDRESS_INPUT_TEXT;
            	card = getSimpleCard("Manual Address - Input", speechText.toString());
            }
		}			
		return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(speechText), getReprompt(getSSMLOutputSpeech(speechText)), card);
	}
	
	/**
	 * Dialog Directives to control the more conversational dialogs
	 * @param speechletRequestEnvelope
	 * @return SpeechletResponse
	 */
	private SpeechletResponse getDialogDirective(SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope) {
		
		IntentRequest request = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();
		Intent intent = request.getIntent();
		//Get Dialog State
        DialogState dialogueState = request.getDialogState();
        
		// If the IntentRequest dialog state is STARTED
		// This is where you can pre-fill slot values with defaults
		if (dialogueState == IntentRequest.DialogState.STARTED)
		{
			// 1.
			DialogIntent dialogIntent = new DialogIntent(intent);

			// 2.
			DelegateDirective dd = new DelegateDirective();
			dd.setUpdatedIntent(dialogIntent);

			List<Directive> directiveList = new ArrayList<Directive>();
			directiveList.add((Directive) dd);

			SpeechletResponse speechletResp = new SpeechletResponse();
			speechletResp.setDirectives((List<com.amazon.speech.speechlet.Directive>) directiveList);
			// 3.
			speechletResp.setShouldEndSession(false);
			return speechletResp;
		}
		else if (dialogueState == IntentRequest.DialogState.COMPLETED)
		{
			if("GetManualAddressInput".equals(intent.getName())) {
				Slot manualPinCode = intent.getSlot(PINCODE_SLOT);
				if(manualPinCode != null && manualPinCode.getValue() != null) {
					String pinCode = manualPinCode.getValue();
					log.info("Entered Pincode is {}", pinCode); 
					
					///// --------------------  Setting Session Values started ----------------- //////
					session.setAttribute(PINCODE_SLOT, pinCode);
					///// --------------------  Setting Session Values ended ----------------- //////
					
					if("TRUE".equals(session.getAttribute(GENERIC_INTENT_FLAG))) {
						SimpleCard card = getSimpleCard("Generic Intent - Tricare Plan Select", TRICARE_PLAN_TYPE_TEXT);
			        	return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(TRICARE_PLAN_TYPE_TEXT), getReprompt(getSSMLOutputSpeech(TRICARE_PLAN_TYPE_TEXT)), card);
					}else {
						SimpleCard card = getSimpleCard("Manual Address - Search", MILES_AROUND);
						return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(MILES_AROUND), getReprompt(getSSMLOutputSpeech(MILES_AROUND)), card);
					}
				}
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(" Manual Address Input error, Please try again later "));
			}
			else if("GetTricarePlanIntent".equals(intent.getName())) {
				Slot tricarePlanSlot = intent.getSlot(TRICARE_PLAN_SLOT);
				if (tricarePlanSlot != null && tricarePlanSlot.getValue() != null) {
					List<Resolution> tricarePlanResolution =  tricarePlanSlot.getResolutions().getResolutionsPerAuthority();
					String slotId = tricarePlanResolution.get(0).getValueWrappers().get(0).getValue().getId();
					log.info("Tricare Care Plan Type Slot Id : {}", slotId);
					
					///// --------------------  Setting Session Values started ----------------- //////
					session.setAttribute(TRICARE_PLAN_SLOT, slotId);
					session.setAttribute(PLAN_TYPE_DESC, tricarePlanSlot.getValue());
					///// --------------------  Setting Session Values ended ----------------- //////
					
					StringBuilder speechText = new StringBuilder("<prosody rate=\"medium\">I'm saving you words as, you are belongs to "+ session.getAttribute(PLAN_TYPE_DESC)+", "); 
					speechText.append(" and searching for "+ session.getAttribute(PROVIDER_TYPE_DESC) +", ");
					speechText.append(" with postal code <say-as interpret-as=\"digits\"> "+session.getAttribute(PINCODE_SLOT)+"</say-as>, ");
					speechText.append(" and search around "+ session.getAttribute(MILES_VALUE_SLOT));
					speechText.append(" miles radius</prosody>");
					
					speechText.append("<break time=\"0.2s\" />Do you want to add some more details about the searching provider ? ");
					
					///// --------------------  Setting Session Values started ----------------- //////
					session.setAttribute(YESNO_INTENT_QUES_TYPE, PROVIDER_DETAILS_YESNO_QUES);
					///// --------------------  Setting Session Values ended ----------------- //////
					
					
					SimpleCard card = getSimpleCard("User Details - Acknowledgement", speechText.toString());
					return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(speechText.toString()), getReprompt(getSSMLOutputSpeech(speechText.toString())), card);
				}			
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(" Tricare Plan Input Select error, Please try again later "));
			} 
			else if("GetMilesIntent".equals(intent.getName())) {
				
				Slot milesValueSlot = intent.getSlot(MILES_VALUE_SLOT);
				SimpleCard card = new SimpleCard();
				if (milesValueSlot != null && milesValueSlot.getValue() != null) {
					
					String milesValue = milesValueSlot.getValue();
					
					///// --------------------  Setting Session Values started ----------------- //////
					session.setAttribute(MILES_VALUE_SLOT, milesValue);
					///// --------------------  Setting Session Values ended ----------------- //////
					
					log.info("Miles value : {}", milesValue);
					card = getSimpleCard("Miles value - User value", "Search radius is changed as "+ milesValue+" miles as per user request ");
				}			
				return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(TRICARE_PLAN_TYPE_TEXT), getReprompt(getSSMLOutputSpeech(TRICARE_PLAN_TYPE_TEXT)), card);
			}
			else if("GetProviderSearchIntent".equals(intent.getName())) {
				
				//Below 3 slots are mandatory to fill the Indent
				Slot providerTypeSlot = intent.getSlot(PROVIDER_TYPE_SLOT);
				Slot milesValueSlot = intent.getSlot(MILES_VALUE_SLOT);
				Slot addressDeciderSlot = intent.getSlot(ADDRESS_DECIDER_SLOT);
				
				///// --------------------  Setting Session Values started ----------------- //////
				String providerTypeId = providerTypeSlot.getResolutions().getResolutionsPerAuthority().get(0).getValueWrappers().get(0).getValue().getId();
				session.setAttribute(PROVIDER_TYPE_SLOT, providerTypeId);
				log.info(" Provider Type Desc : {}", providerTypeSlot.getValue());
				session.setAttribute(PROVIDER_TYPE_DESC, providerTypeSlot.getValue());
				session.setAttribute(MILES_VALUE_SLOT, milesValueSlot.getValue());
				///// --------------------  Setting Session Values ended ----------------- //////
				
				SimpleCard card = new SimpleCard();
	            List<Resolution> addressResolution =  addressDeciderSlot.getResolutions().getResolutionsPerAuthority();
	            String slotId = addressResolution.get(0).getValueWrappers().get(0).getValue().getId();
	            log.info("Address acknowledgement Intent requested with Slot ID {}", slotId);
	            //MANUAL_ADDRESS
	            if(slotId != null && slotId.equals(MANUAL_ADDRESS)) {
	            	card = getSimpleCard("Manual Address - Input", MANUAL_ADDRESS_INPUT_TEXT);
	            	return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(MANUAL_ADDRESS_INPUT_TEXT), getReprompt(getSSMLOutputSpeech(MANUAL_ADDRESS_INPUT_TEXT)), card);
	            } else {
	            	return getRequestClientAddress(new StringBuilder(), speechletRequestEnvelope, session);
	            }
			}
			else if("GetProviderFilterIntent".equals(intent.getName())) {
				
				String genderId 	= "";
				String specalityId 	= "";
				
				//Below 3 slots are mandatory to fill the Indent
				Slot specilaityTypeSlot = intent.getSlot(SPECIALITY_TYPE_SLOT);
				Slot genderTypeSlot = intent.getSlot(GENDER_TYPE_SLOT);
				Slot firstNameSlot = intent.getSlot(FIRSTNAME_SLOT);
				
				if(genderTypeSlot != null && genderTypeSlot.getValue() != null) {
					genderId = genderTypeSlot.getResolutions().getResolutionsPerAuthority().get(0).getValueWrappers().get(0).getValue().getId();
				}
				if(specilaityTypeSlot != null && specilaityTypeSlot.getValue() != null) {
					specalityId = specilaityTypeSlot.getResolutions().getResolutionsPerAuthority().get(0).getValueWrappers().get(0).getValue().getId();
				}
				
				///// --------------------  Setting Session Values started ----------------- //////
				session.setAttribute(SPECIALITY_TYPE_SLOT, specalityId.equals("ALL")?specalityId:specilaityTypeSlot.getValue());
				session.setAttribute(GENDER_TYPE_SLOT, genderId.equals("ALL")?genderId:genderTypeSlot.getValue());
				session.setAttribute(FIRSTNAME_SLOT, firstNameSlot.getValue());
				///// --------------------  Setting Session Values ended ----------------- //////
				
				StringBuilder speechText = new StringBuilder("<prosody rate=\"medium\">I have marked your provider filter as follows, <break time=\"0.2s\" /> ");
				
				int skipCount = 0;
				
				if(firstNameSlot.getValue() != null && !("ALL").equals(firstNameSlot.getValue())) {
					speechText.append(" First name "+session.getAttribute(FIRSTNAME_SLOT)+", ");
				} else {
					skipCount++;
				}
				
				if(genderId != null && !("ALL").equals(genderId)) {
					speechText.append(" Gender "+session.getAttribute(GENDER_TYPE_SLOT)+ ", ");
				} else {
					skipCount++;
				}
				
				if(specalityId != null && !("ALL").equals(specalityId)) {
					speechText.append(" Speciality "+session.getAttribute(SPECIALITY_TYPE_SLOT)+ ", ");
				} else {
					skipCount++;
				}
				
				if(skipCount > 0 && skipCount < 3) {
					speechText.append(" and you are skipped one or more filters ");
				}else if(skipCount == 3){
					speechText.append(" <s>You are skipped all the filters and you requested the search with all possibilites</s> ");
				}
				
				speechText.append("</prosody>");
				speechText.append("<break time=\"0.2s\" />Shall i begin the search? ");
				
				///// --------------------  Setting Session Values started ----------------- //////
				session.setAttribute(YESNO_INTENT_QUES_TYPE, SEARCH_YESNO_QUES);
				///// --------------------  Setting Session Values ended ----------------- //////
				
				return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(speechText.toString()), getReprompt(getSSMLOutputSpeech(speechText.toString())), getSimpleCard("Provider Filter Details", speechText.toString()));
				
			}
			else {
				//Invalid Intent Request Error
				return SpeechletResponse.newTellResponse(getPlainTextOutputSpeech(GENERIC_INTENT_NOT_FOUND_ERROR));
			}
		}
		else
		{
			//This is executed when the dialog is in state e.g. IN_PROGESS. If there is only one slot this shouldn't be called
			DelegateDirective dd = new DelegateDirective();

			List<Directive> directiveList = new ArrayList<Directive>();
			directiveList.add((Directive) dd);

			SpeechletResponse speechletResp = new SpeechletResponse();
			speechletResp.setDirectives((List<com.amazon.speech.speechlet.Directive>) directiveList);
			speechletResp.setShouldEndSession(false);
			return speechletResp;
		}
	}

	/**
	 * Get Device Address with the requested device details
	 * @param speechText
	 * @param speechletRequestEnvelope
	 * @param session
	 * @return SpeechletResponse
	 */
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

            return getAddressResponse(session, speechText, 
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
	 * Device address API call
	 * @param session
	 * @param speechText
	 * @param streetName
	 * @param state
	 * @param zipCode
	 * @return SpeechletResponse
	 */
    private SpeechletResponse getAddressResponse(Session session, StringBuilder speechText, String streetName, String state, String zipCode) {
    	
        speechText.append("Your search address is defaulted with your device address as, " + streetName + " <break time=\"0.2s\" /> " + state + ",<break time=\"0.2s\" /> <say-as interpret-as=\"digits\">" + zipCode + "</say-as>.<break time=\"0.2s\" />");
        
        ///// --------------------  Setting Session Values started ----------------- //////
        session.setAttribute(PINCODE_SLOT, zipCode);
        ///// --------------------  Setting Session Values ended ----------------- //////
        
        if("TRUE".equals(session.getAttribute(GENERIC_INTENT_FLAG))){
        	SimpleCard card = getSimpleCard("Generic Intent - Tricare Plan Select", speechText.toString()+ TRICARE_PLAN_TYPE_TEXT);
        	return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(speechText.toString()+TRICARE_PLAN_TYPE_TEXT), getReprompt(getSSMLOutputSpeech(speechText.toString()+TRICARE_PLAN_TYPE_TEXT)), card);
        } else {
        	speechText.append("Shall i proceed the search with, device address or some other address?");
        	SimpleCard card = getSimpleCard(ADDRESS_CARD_TITLE, speechText.toString());
            String addressRepromt = "You can tell me, Shall i continue with, device address or some other address?";

            return SpeechletResponse.newAskResponse(getSSMLOutputSpeech(speechText.toString()), getReprompt(getPlainTextOutputSpeech(addressRepromt)), card);
        }
    }
    
    private SpeechletResponse getProviderSearch() throws UnauthorizedException, SearchClientException {
    	
    	StringBuilder speechText = new StringBuilder();
    	
    	//Setting search filters
    	List<ProviderSearch> searchList = SearchClient.getProviderSearch();
    	speechText.append("<s>I have found "+searchList.size()+" records for you.</s> <break time=\"0.1s\" /> ");
    	
    	if(searchList != null && searchList.size() > 0) {
    		for(ProviderSearch provDetails : searchList) {
    			log.info("provDetails {}",provDetails.getFirstName());
    			speechText.append("<s>"+provDetails.getFirstName()+provDetails.getLastName()+" from "+provDetails.getAddress().getAddressLine1()+", "+provDetails.getAddress().getCity()+"</s><break time=\"0.1s\" />");
    		}
    	}
    	
    	return SpeechletResponse.newTellResponse(getSSMLOutputSpeech(speechText.toString()+NO_SEARCH_TEXT));
    }
	
	/**
	 * Return the voice to the user about skill address access permission is not granted
	 * @param speechText
	 * @return SpeechletResponse
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
     * SSML language text build
     * @param speechText
     * @return SsmlOutputSpeech
     */
    private SsmlOutputSpeech getSSMLOutputSpeech(String speechText) {
    	SsmlOutputSpeech speech = new SsmlOutputSpeech();
    	speech.setSsml("<speak>" + speechText.toString() + "</speak>");
    	
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
