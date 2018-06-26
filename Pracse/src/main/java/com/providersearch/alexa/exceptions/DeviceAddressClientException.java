package com.providersearch.alexa.exceptions;

import com.providersearch.alexa.AlexaDeviceAddressClient;

/**
 * This is an exception thrown from the {@link AlexaDeviceAddressClient} that indicates that a failure occurred.
 */
public class DeviceAddressClientException extends Exception {

    public DeviceAddressClientException(String message, Exception e) {
        super(message, e);
    }

    public DeviceAddressClientException(String message) {
        super(message);
    }

    public DeviceAddressClientException(Exception e) {
        super(e);
    }

}
