package com.providersearch.alexa.exceptions;

import com.providersearch.alexa.exceptions.DeviceAddressClientException;

/**
 * This exception indicates that the AlexaDeviceAddressClient failed because of a permission specific
 * reason. This is an exception aside from {@link DeviceAddressClientException} because permission related
 * issues should be handled separately from a generic failure.
 *
 * Refer to {@link DeviceAddressSpeechlet} to see how permission related errors are handled.
 */
public class UnauthorizedException extends DeviceAddressClientException {

    public UnauthorizedException(String message, Exception e) {
        super(message, e);
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}