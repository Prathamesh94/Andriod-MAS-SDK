/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.core.request.internal;

import android.util.Log;

import com.ca.mas.core.context.MssoContext;
import com.ca.mas.foundation.MASResponse;
import com.ca.mas.foundation.MASResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.ca.mas.foundation.MAS.DEBUG;
import static com.ca.mas.foundation.MAS.TAG;

/**
 * A request to retrieve the the current state of the SDK
 */
public class StateRequest extends MAGRequestProxy implements LocalRequest {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String CLIENT_ID = "client_id";
    public static final String MAG_IDENTIFIER = "mag_identifier";

    public StateRequest() {
        request = new MASRequestBuilder((URL) null).password().build();
    }

    @Override
    public MASResponse send(final MssoContext context) {

        return new MASResponse<JSONObject>() {

            @Override
            public Map<String, List<String>> getHeaders() {
                return null;
            }

            @Override
            public int getResponseCode() {
                return HttpURLConnection.HTTP_OK;
            }

            @Override
            public String getResponseMessage() {
                return null;
            }

            @Override
            public MASResponseBody<JSONObject> getBody() {
                return new MASResponseBody<JSONObject>() {
                    @Override
                    public JSONObject getContent() {
                        JSONObject entity = new JSONObject();
                        try {
                            entity.put(ACCESS_TOKEN, context.getAccessToken());
                            entity.put(CLIENT_ID, context.getClientId());
                            entity.put(MAG_IDENTIFIER, context.getTokenManager().getMagIdentifier());
                        } catch (JSONException e) {
                            if (DEBUG) Log.i(TAG, e.getMessage(), e);
                            return new JSONObject();
                        }
                        return entity;
                    }
                };
            }
        };
    }
}
