/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.core.clientcredentials;

import com.ca.mas.core.error.MAGServerException;

/**
 * This exception is thrown when an error occurs while accessing the MAG Server initialize endpoint
 */
public class ClientCredentialsServerException extends MAGServerException{

    public ClientCredentialsServerException(int errorCode, int status, String contentType, String detailMessage) {
        super(errorCode, status, contentType, detailMessage);
    }

    public ClientCredentialsServerException(int errorCode, int status, String contentType, String detailMessage, Throwable throwable) {
        super(errorCode, status, contentType, detailMessage, throwable);
    }

    public ClientCredentialsServerException(MAGServerException e) {
        super(e);
    }
}
