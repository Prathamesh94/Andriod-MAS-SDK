/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.sample.testapp.tests.instrumentation.storage;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.test.ApplicationTestCase;
import android.util.Base64;
import android.util.Log;

import com.ca.mas.storage.MASEncryptionProvider;
import com.ca.mas.storage.DefaultEncryptionProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

public class EncryptionProviderTest extends ApplicationTestCase<Application> {

    private static final String TAG="EncryptionProviderTest";

    /**
     * The Encryption provider reference
     */
    MASEncryptionProvider encryptionProvider;
    public EncryptionProviderTest(Class<Application> applicationClass) {
        super(applicationClass);
    }

    public EncryptionProviderTest() {
        super(Application.class);
    }

    private static final String SYM_KEY="secret";
    private static final String ASYM_KEY_ALIAS = "ASYM_KEY";
    public static final String PREFS_NAME = "SECRET_PREFS";

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Log.i(TAG,"inside tearDown");
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if(ks.containsAlias(SYM_KEY)){
            ks.deleteEntry(SYM_KEY);
            Log.i(TAG, "deleted SYM_KEY from KS");
        }
        if(ks.containsAlias(ASYM_KEY_ALIAS)){
            ks.deleteEntry(ASYM_KEY_ALIAS);
            Log.i(TAG, "deleted ASYM_KEY_ALIAS from KS");
        }

        SharedPreferences sp=getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if(sp.contains(SYM_KEY)){
            sp.edit().remove(SYM_KEY).apply();
            Log.i(TAG, "deleted SYM_KEY from SP");
        }
        if(sp.contains(ASYM_KEY_ALIAS)){
            sp.edit().remove(ASYM_KEY_ALIAS).apply();
            Log.i(TAG, "deleted ASYM_KEY_ALIAS from SP");
        }
    }

    public void testEncryptDecryptOperation(){

        encryptionProvider=new DefaultEncryptionProvider(getContext());
        try {
            String dataString="CA Technologies";
            byte[] data=dataString.getBytes("UTF-8");
            byte[] encryptedData=encryptionProvider.encrypt(data);
            Log.i(TAG,"Encrypted Data: "+ Base64.encodeToString(encryptedData, Base64.DEFAULT));
            byte[] decryptedData=encryptionProvider.decrypt(encryptedData);
            String result=new  String(decryptedData,"UTF-8");
            assertEquals("Decrypting"+dataString+": ",dataString,result);
        } catch (Exception e) {
            Log.e(TAG, "Error while encrypting/decrypting data", e);
            fail("Error while encrypting/decrypting");
        }
    }

    /**
     * PLEASE RUN THIS TEST ONLY IN ANDROID M OR HIGHER
     * The upgrade test case is a very special test case in which we mock the conditions of less than Android M on a Android M or higher OS
     * and the then run the test on a Android M or higher.
     * Please make sure that the pin is set before running.
     */

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void testUpgradeScenario(){

        Log.i(TAG,"Testing when an device upgrades from less than Android M to Android M or higher");
        String ASYM_KEY_ALIAS="ASYM_KEY";
        String PREFS_NAME = "SECRET_PREFS";
        String SYM_KEY_ALIAS="secret";


        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            boolean val=ks.containsAlias(SYM_KEY_ALIAS);
            assertEquals("Symmetric key is not present in keyStore ", false, val);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            Log.e(TAG,"Error while getting data keystore",e);
            fail("Error while getting data keystore");
        }


        //---------------Generate a symmetric key---------------
        javax.crypto.KeyGenerator kg = null;
        try {
            kg = javax.crypto.KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            fail("Error while instantiating KeyGenerator");
        }
        kg.init(256);
        SecretKey sk= kg.generateKey();

        //---------------End of Generate a symmetric key-------------

        //--------Generate an asymmetric key and store in the Keystore--------

        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            fail("Error while instantiating KeyGenerator");
        }
        Calendar notAfter = Calendar.getInstance();
        notAfter.add(Calendar.YEAR, 1);

        try {
            keyPairGenerator.initialize(new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(ASYM_KEY_ALIAS)
                    .setEncryptionRequired()
                    .setSubject(
                            new X500Principal(String.format("CN=%s, OU=%s", ASYM_KEY_ALIAS, "com.ca")))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(new Date())
                    .setEndDate(notAfter.getTime())
                    .build());
        } catch (InvalidAlgorithmParameterException e) {
            fail("Error while instantiating KeyGenerator");
        }
        KeyPair keyPair=keyPairGenerator.genKeyPair();

        //--------End of Generate an asymmetric key and store in the Keystore--------

        //------ Retrieve the public key part of the asymmetric data and encrypt the symmetric key -------


        KeyStore.Entry entry = null;
        PublicKey publicKey = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            entry = ks.getEntry(ASYM_KEY_ALIAS, null);
            publicKey=((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableEntryException e) {
            fail("Error in keystore operation");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            fail("Error in cipher operation");
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            fail("Error in cipher operation");
        }
        byte[] keyEncrypt=null;

        try {
            keyEncrypt= cipher.doFinal(sk.getEncoded());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            fail("Error in cipher operation");
        }
        //------ End of Retrieve the public key part of the asymmetric data and encrypt the symmetric key -------

        //-----Store the symmetric key in the local storage----------

        SharedPreferences sharedpreferences=getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        String ss = Base64.encodeToString(keyEncrypt, Base64.DEFAULT);
        editor.putString(SYM_KEY_ALIAS, ss);
        editor.commit();

         //-----End of Store the symmetric key in the local storage ----------


         encryptionProvider=new DefaultEncryptionProvider(getContext());



        //Now test assertions----------

        try {
            String dataString="CA Technologies";
            byte[] data=dataString.getBytes("UTF-8");
            byte[] encryptedData=encryptionProvider.encrypt(data);
            Log.i(TAG,"Encrypted Data: "+ Base64.encodeToString(encryptedData, Base64.DEFAULT));

            //Check if the key is now present in the Keystore or not
            try {
                boolean val= ks.containsAlias(SYM_KEY_ALIAS);
                assertEquals("Symmetric key is present in keyStore",true,val);
            } catch (KeyStoreException e) {
                Log.e(TAG, "Error while getting data keystore", e);
                fail("Error while getting data keystore");
            }


            byte[] decryptedData=encryptionProvider.decrypt(encryptedData);
            String result=new  String(decryptedData,"UTF-8");
            assertEquals("Decrypting"+dataString+": ",dataString,result);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error while encrypting/decrypting data", e);
            fail("Error while encrypting/decrypting data");
        }
    }
}