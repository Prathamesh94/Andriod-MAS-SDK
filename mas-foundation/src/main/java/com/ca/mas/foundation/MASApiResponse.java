/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.mas.foundation;

import java.util.List;
import java.util.Map;

public class MASApiResponse<T> implements MASResponse<T> {

    private MASResponse<T> response;

    public MASApiResponse(MASResponse<T> response) {
        this.response = response;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return response.getHeaders();
    }

    @Override
    public int getResponseCode() {
        return response.getResponseCode();
    }

    @Override
    public String getResponseMessage() {
        return response.getResponseMessage();
    }

    @Override
    public MASResponseBody<T> getBody() {
        return new MASResponseBody<T>() {
            @Override
            public T getContent() {
                if (response.getBody() == null) {
                    return null;
                }
                return response.getBody().getContent();
            }
        };
    }
}
