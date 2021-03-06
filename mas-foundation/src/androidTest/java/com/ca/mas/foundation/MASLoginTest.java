/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.foundation;

import android.content.Context;

import com.ca.mas.GatewayDefaultDispatcher;
import com.ca.mas.MASCallbackFuture;
import com.ca.mas.MASStartTestBase;
import com.ca.mas.core.auth.AuthenticationException;
import com.ca.mas.core.client.ServerClient;
import com.ca.mas.core.token.JWTExpiredException;
import com.ca.mas.core.token.JWTInvalidAUDException;
import com.ca.mas.core.token.JWTInvalidAZPException;
import com.ca.mas.core.token.JWTInvalidSignatureException;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MASLoginTest extends MASStartTestBase {

    //Mock response for device registration
    String cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDCjCCAfKgAwIBAgIIKzRkwk/TRDswDQYJKoZIhvcNAQEMBQAwIzEhMB8GA1UEAxMYYXdpdHJp\n" +
            "c25hLWRlc2t0b3AuY2EuY29tMB4XDTEzMTEyNzE5MzkwOVoXDTE4MTEyNjE5MzkwOVowIzEhMB8G\n" +
            "A1UEAxMYYXdpdHJpc25hLWRlc2t0b3AuY2EuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n" +
            "CgKCAQEAoaCzdLbRhqt3T4ROTgOBD5gizxsJ/vhqmIpagXU+3OPhZocwf0FIVjvbrybkj8ZynTve\n" +
            "p1cJsAmdkuX+w6m8ow2rAR/8BQnIaBD281gNqDCYXAGkguEZBbCQ2TvD4FZYnJZSmrE9PJtIe5pq\n" +
            "DneOqaO0Kqj3sJpYIG11U8djio9UNAqTd0J9q5+fEMVle/QG0X0ro3MR30PaHIA7bpvISpjFZ0zD\n" +
            "54rQc+85bOamg4aJFcfiNSMIaAYaFMi/peJLmW8Q4DZriAQSG6PIBcekMx1mi4tuXkSrr3P3ycKu\n" +
            "bU0ePKnxckxWHygK42bQ5ClLuJeYNPxqHiBapZj2hwmzsQIDAQABo0IwQDAdBgNVHQ4EFgQUZddX\n" +
            "bkxC+asQgSCSIViGKuGS2f4wHwYDVR0jBBgwFoAUZddXbkxC+asQgSCSIViGKuGS2f4wDQYJKoZI\n" +
            "hvcNAQEMBQADggEBAHK/QdXrRROjKjxwU05wo1KZNRmi8jBsKF/ughCTqcUCDmEuskW/x9VCIm/r\n" +
            "ZMFgOA3tou7vT0mX8gBds+95td+aNci1bcBBpiVIwiqOFhBrtbiAhYofgXtbcYchL9SRmIpek/3x\n" +
            "BwBj5CBmaimOZsTLp6wqzLE4gpAdTMaU+RIlwq+uSUmKhQem6fSthGdWx5Ea9gwKuVi8PwSFCs/Q\n" +
            "nwUfNnCvOTP8PtQgvmLsXeaFfy/lYK7iQp1CiwwXYpc3Xivv9A7DH7MqVSQZdtjDrRI2++1/1Yw9\n" +
            "XoYtMDN0dQ5lBNIyJB5rWtCixZgfacHp538bMPMskLePU3dxNdCqhas=\n" +
            "-----END CERTIFICATE-----";


    @After
    public void deregister() throws InterruptedException, ExecutionException {
        if (isSkipped) return;
        MASCallbackFuture<Void> logoutCallback = new MASCallbackFuture<Void>();
        if (MASUser.getCurrentUser() != null) {
            MASUser.getCurrentUser().logout(logoutCallback);
            Assert.assertNull(logoutCallback.get());
        }

        if (MASDevice.getCurrentDevice().isRegistered()) {
            MASCallbackFuture<Void> deregisterCallback = new MASCallbackFuture<Void>();
            MASDevice.getCurrentDevice().deregister(deregisterCallback);
            Assert.assertNull(deregisterCallback.get());
        }
    }

    @Test
    public void testAuthenticationListener() throws JSONException, InterruptedException, URISyntaxException, ExecutionException {

        final boolean[] result = {false};
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        MAS.setAuthenticationListener(new MASAuthenticationListener() {

            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                MASUser.login("test", "test".toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        result[0] = true;
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        result[0] = false;
                        countDownLatch.countDown();
                    }
                });
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {

            }
        });

        MASRequest request = new MASRequest.MASRequestBuilder(new URI("/protected/resource/products?operation=listProducts")).build();
        MASCallbackFuture<MASResponse<JSONObject>> callback = new MASCallbackFuture<>();
        MAS.invoke(request, callback);
        Assert.assertNotNull(callback.get());
        countDownLatch.await();
        Assert.assertTrue(result[0]);

    }

    @Test
    public void testCallbackWithAuthenticateFailed() throws InterruptedException, URISyntaxException, ExecutionException {

        final boolean[] override = {true};
        final int expectedErrorCode = 1000202;
        final String expectedErrorMessage = "{ \"error\":\"invalid_request\", \"error_description\":\"The resource owner could not be authenticated due to missing or invalid credentials\" }";
        final String CONTENT_TYPE = "Content-Type";
        final String CONTENT_TYPE_VALUE = "application/json";

        setDispatcher(new GatewayDefaultDispatcher() {
            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                if (override[0]) {
                    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                            .setHeader("WWW-Authenticate", "Basic realm=\"fake\"")
                            .setHeader("x-ca-err", expectedErrorCode)
                            .setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE).setBody(expectedErrorMessage);
                } else {
                    return super.registerDeviceResponse(request);
                }
            }
        });

        final Throwable[] throwable = new Throwable[1];

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, final long requestId, MASAuthenticationProviders providers) {
                MASUser.login("test", "test".toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser masUser) {
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        throwable[0] = e;
                        override[0] = false;
                        countDownLatch.countDown();
                    }
                });
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {

            }
        });

        MASRequest request = new MASRequest.MASRequestBuilder(new URI("/protected/resource/products?operation=listProducts")).build();
        MASCallbackFuture<MASResponse<JSONObject>> callback = new MASCallbackFuture<>();
        MAS.invoke(request, callback);
        Assert.assertNotNull(callback.get());
        countDownLatch.await();

        assertTrue((throwable[0]).getCause() instanceof AuthenticationException);
        assertTrue(((MASException) throwable[0]).getRootCause() instanceof AuthenticationException);
        AuthenticationException e = (AuthenticationException) (throwable[0]).getCause();
        assertEquals(CONTENT_TYPE_VALUE, e.getContentType());
        assertEquals(expectedErrorCode, e.getErrorCode());
        assertEquals(expectedErrorMessage, e.getMessage());
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, e.getStatus());

    }

    @Test
    public void invalidSignature() throws InterruptedException {

        setDispatcher(new GatewayDefaultDispatcher() {
            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("device-status", "activated")
                        .setHeader("mag-identifier", "test-device")
                        .setHeader("id-token", "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.ewogImV4cCI6IDI0MDA4Nzg1OTEsCiAiYXpwIjogInRlc3QtZGV2aWNlIiwKICJzdWIiOiAieCIsCiAiYXVkIjogImR1bW15IiwKICJpc3MiOiAiaHR0cDovL20ubGF5ZXI3dGVjaC5jb20vY29ubmVjdCIsCiAiaWF0IjogMTQwMDg3ODU5MQp9.zenKvXlhDtpXym_auPCbukBiVqr3rqZrcoeDyfsvftA")
                        .setHeader("id-token-type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .setBody(cert);
            }
        });

        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASUser.login("test", "test".toCharArray(), callback);
        try {
            Assert.assertNotNull(callback.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getCause() instanceof JWTInvalidSignatureException);
            assertTrue(((MASException) e.getCause()).getRootCause() instanceof JWTInvalidSignatureException);
        }
    }

    @Test
    public void invalidAud() throws InterruptedException {

        setDispatcher(new GatewayDefaultDispatcher() {
            @Override
            protected MockResponse initializeResponse() {
                String result = "{\"client_id\":\"8298bc51-f242-4c6d-b547-d1d8e8519cb5\", \"client_secret\":\"dummy\", \"client_expiration\":" + new Date().getTime() + 36000 + "}";
                return new MockResponse().setResponseCode(200).setBody(result);
            }

            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("device-status", "activated")
                        .setHeader("mag-identifier", "test-device")
                        .setHeader("id-token", "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.ewogImV4cCI6IDI0MDA4Nzg1OTEsCiAiYXpwIjogInRlc3QtZGV2aWNlIiwKICJzdWIiOiAieCIsCiAiYXVkIjogImR1bW15IiwKICJpc3MiOiAiaHR0cDovL20ubGF5ZXI3dGVjaC5jb20vY29ubmVjdCIsCiAiaWF0IjogMTQwMDg3ODU5MQp9.zenKvXlhDtpXym_auPCbukBiVqr3rqZrcoeDyfsvftA")
                        .setHeader("id-token-type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .setBody(cert);
            }
        });

        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASUser.login("test", "test".toCharArray(), callback);
        try {
            Assert.assertNotNull(callback.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getCause() instanceof JWTInvalidAUDException);
            assertTrue(((MASException) e.getCause()).getRootCause() instanceof JWTInvalidAUDException);

        }
    }

    @Test
    public void invalidAzp() throws InterruptedException {

        setDispatcher(new GatewayDefaultDispatcher() {

            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("device-status", "activated")
                        .setHeader("mag-identifier", "dummy-device")
                        .setHeader("id-token", "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.ewogImV4cCI6IDI0MDA4Nzg1OTEsCiAiYXpwIjogInRlc3QtZGV2aWNlIiwKICJzdWIiOiAieCIsCiAiYXVkIjogImR1bW15IiwKICJpc3MiOiAiaHR0cDovL20ubGF5ZXI3dGVjaC5jb20vY29ubmVjdCIsCiAiaWF0IjogMTQwMDg3ODU5MQp9.zenKvXlhDtpXym_auPCbukBiVqr3rqZrcoeDyfsvftA")
                        .setHeader("id-token-type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .setBody(cert);
            }
        });
        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASUser.login("test", "test".toCharArray(), callback);
        try {
            Assert.assertNotNull(callback.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getCause() instanceof JWTInvalidAZPException);
            assertTrue(((MASException) e.getCause()).getRootCause() instanceof JWTInvalidAZPException);
        }

    }

    @Test
    public void invalidExp() throws InterruptedException {

        setDispatcher(new GatewayDefaultDispatcher() {

            @Override
            protected MockResponse initializeResponse() {
                String result = "{\"client_id\":\"8298bc51-f242-4c6d-b547-d1d8e8519cb4\", \"client_secret\":\"dummy\", \"client_expiration\":" + new Date().getTime() + 36000 + "}";
                return new MockResponse().setResponseCode(200).setBody(result);
            }

            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("device-status", "activated")
                        .setHeader("mag-identifier", "d9b38b22-14a2-4573-80b7-b95de92b18he")
                        .setHeader("id-token", "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.ewogImV4cCI6IDE0MDA3OTQ2MjEsCiAiYXpwIjogImQ5YjM4YjIyLTE0YTItNDU3My04MGI3LWI5NWRlOTJiMThoZSIsCiAic3ViIjogIngiLAogImF1ZCI6ICI4Mjk4YmM1MS1mMjQyLTRjNmQtYjU0Ny1kMWQ4ZTg1MTljYjQiLAogImlzcyI6ICJodHRwOi8vbS5sYXllcjd0ZWNoLmNvbS9jb25uZWN0IiwKICJpYXQiOiAxNDAwNzk0NjIxCn0.H4Yvz9d-uzoWGWeshgYTFLm110B1M1pb63vrwrJsIIg")
                        .setHeader("id-token-type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .setBody(cert);
            }
        });

        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASUser.login("test", "test".toCharArray(), callback);
        try {
            Assert.assertNotNull(callback.get());
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getCause() instanceof JWTExpiredException);
            assertTrue(((MASException) e.getCause()).getRootCause() instanceof JWTExpiredException);
        }
    }

    @Test
    public void testLoginWithIdToken() throws Exception {
        String expected = "dummy_id_token";
        String expectedType = "dummy_id_token_type";

        //Register with id token
        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASIdToken idToken = new MASIdToken.Builder().value(expected).type(expectedType).build();
        MASUser.login(idToken, callback);
        Assert.assertNotNull(callback.get());
        RecordedRequest registerRequest = getRecordRequest(GatewayDefaultDispatcher.CONNECT_DEVICE_REGISTER);
        Assert.assertEquals(registerRequest.getHeader("authorization"), "Bearer " + expected);
        Assert.assertEquals(registerRequest.getHeader("x-authorization-type"), expectedType);

        //Logout
        MASCallbackFuture<Void> logoutCallback = new MASCallbackFuture<>();
        MASUser.getCurrentUser().logout(logoutCallback);
        logoutCallback.get();

        //invoke token with id token
        MASCallbackFuture<MASUser> loginCallback = new MASCallbackFuture<>();
        MASIdToken newIdToken = new MASIdToken.Builder().value(expected).type(expectedType).build();
        MASUser.login(newIdToken, loginCallback);
        Assert.assertNotNull(loginCallback.get());

        RecordedRequest tokenRequest = getRecordRequest(GatewayDefaultDispatcher.AUTH_OAUTH_V2_TOKEN);
        String body = URLDecoder.decode(tokenRequest.getBody().readUtf8(), "UTF-8");
        assertTrue(body.contains(ServerClient.ASSERTION + "=" + expected));
        assertTrue(body.contains(ServerClient.GRANT_TYPE + "=" + expectedType));
    }

    @Test
    public void testLoginWithIdTokenDefaultType() throws Exception {
        String expected = "dummy_id_token";
        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASIdToken idToken = new MASIdToken.Builder().value(expected).build();
        MASUser.login(idToken, callback);
        Assert.assertNotNull(callback.get());
        RecordedRequest rr = getRecordRequest(GatewayDefaultDispatcher.CONNECT_DEVICE_REGISTER);
        Assert.assertEquals(rr.getHeader("authorization"), "Bearer " + expected);
        Assert.assertEquals(rr.getHeader("x-authorization-type"), MASIdToken.JWT_DEFAULT);
    }

    @Test(expected = NullPointerException.class)
    public void testLoginWithIDTokenNullValue() throws Exception {
        new MASIdToken.Builder().build();
    }

    @Test
    public void testCallbackWithAuthenticateFailedAndCancel() throws JSONException, InterruptedException, URISyntaxException {

        final int expectedErrorCode = 1000202;
        final String expectedErrorMessage = "{ \"error\":\"invalid_request\", \"error_description\":\"The resource owner could not be authenticated due to missing or invalid credentials\" }";
        final String CONTENT_TYPE = "Content-Type";
        final String CONTENT_TYPE_VALUE = "application/json";

        setDispatcher(new GatewayDefaultDispatcher() {

            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED).
                        setHeader("x-ca-err", expectedErrorCode).
                        setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE).setBody(expectedErrorMessage);

            }
        });

        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, final long requestId, MASAuthenticationProviders providers) {

                MASUser.login("admin", "7layer".toCharArray(), new MASCallback<MASUser>() {
                    @Override
                    public void onSuccess(MASUser result) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        MAS.cancelRequest(requestId);
                    }
                });

            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {

            }
        });

        MASRequest request = new MASRequest.MASRequestBuilder(new URI(GatewayDefaultDispatcher.PROTECTED_RESOURCE_PRODUCTS))
                .notifyOnCancel()
                .build();
        MASCallbackFuture<MASResponse<JSONObject>> callback = new MASCallbackFuture<>();
        MAS.invoke(request, callback);
        try {
            callback.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getCause() instanceof MAS.RequestCancelledException);
        }
    }

    @Test
    public void loginTest() throws InterruptedException, ExecutionException {

        MASCallbackFuture<MASUser> callback = new MASCallbackFuture<>();
        MASUser.login("admin", "7layer".toCharArray(), callback);
        assertNotNull(callback.get());
        assertNotNull(MASUser.getCurrentUser());

    }

    @Test
    public void testAuthenticationFail() throws InterruptedException {

        final int expectedErrorCode = 1000202;
        final String expectedErrorMessage = "{ \"error\":\"invalid_request\", \"error_description\":\"The resource owner could not be authenticated due to missing or invalid credentials\" }";
        final String CONTENT_TYPE = "Content-Type";
        final String CONTENT_TYPE_VALUE = "application/json";

        setDispatcher(new GatewayDefaultDispatcher() {

            @Override
            protected MockResponse registerDeviceResponse(RecordedRequest request) {
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                        .setHeader("WWW-Authenticate", "Basic realm=\"fake\"")
                        .setHeader("x-ca-err", expectedErrorCode)
                        .setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE).setBody(expectedErrorMessage);

            }
        });

        MASCallbackFuture<MASUser> callbackFuture = new MASCallbackFuture<>();

        MASUser.login("admin", "invalid".toCharArray(), callbackFuture);

        try {
            callbackFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, ((AuthenticationException) e.getCause().getCause()).getStatus());
        }


    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticateTestWithNullUsername() {
        MASUser.login(null, "7layer".toCharArray(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticateTestWithNullPassword() {
        MASUser.login("admin", (char[]) null, null);
    }

    @Test
    public void testPendingRequest() throws Exception {
        MASRequest request = new MASRequest.MASRequestBuilder(new URI(GatewayDefaultDispatcher.PROTECTED_RESOURCE_PRODUCTS))
                .build();

        final CountDownLatch authenticationListenerCountDownLaunch = new CountDownLatch(10);

        MAS.setAuthenticationListener(new MASAuthenticationListener() {

            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                authenticationListenerCountDownLaunch.countDown();
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {

            }
        });

        final CountDownLatch requestCountDownLaunch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            MAS.invoke(request, new MASCallback<MASResponse<JSONObject>>() {

                @Override
                public void onSuccess(MASResponse<JSONObject> result) {
                    requestCountDownLaunch.countDown();

                }

                @Override
                public void onError(Throwable e) {

                }
            });
        }
        authenticationListenerCountDownLaunch.await();
        //onAuthenticateRequest has been invoked 10 times
        assertTrue(true);

        MASUser.login("admin", "7layer".toCharArray(), null);

        requestCountDownLaunch.await();
        //All Pending request are executed after login
        assertTrue(true);


    }
}
