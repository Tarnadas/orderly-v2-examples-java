package org.web3j;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.time.Instant;
import java.util.Base64;

import org.json.JSONObject;

import net.i2p.crypto.eddsa.EdDSAEngine;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class Signer {
   public final Config config;

   public String accountId;
   public KeyPair keyPair;

   public Signer(Config config) {
      this.config = config;
   }

   public Signer(Config config, String accountId) {
      this.config = config;
      this.accountId = accountId;
   }

   public Signer(Config config, String accountId, KeyPair keyPair) {
      this.config = config;
      this.accountId = accountId;
      this.keyPair = keyPair;
   }

   public Request createSignedRequest(String url)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException, OrderlyClientException {
      checkIsInitialized();
      return createSignedRequest(url, "GET", null);
   }

   public Request createSignedRequest(String url, String method, JSONObject json)
         throws OrderlyClientException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
      checkIsInitialized();
      if (method == "GET") {
         return createSignedGetRequest(url);
      } else if (method == "POST" && json != null) {
         return createSignedPostRequest(url, json);
      } else {
         throw new IllegalArgumentException();
      }
   }

   private void checkIsInitialized() throws OrderlyClientException {
      if (accountId == null) {
         throw OrderlyClientExceptionReason.ACCOUNT_UNINITIALIZED.asException();
      }
      if (keyPair == null) {
         throw OrderlyClientExceptionReason.KEYPAIR_UNINITIALIZED.asException();
      }
   }

   private Request createSignedGetRequest(String url)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException {
      long timestamp = Instant.now().toEpochMilli();
      String message = "" + timestamp + "GET" + url;

      EdDSAEngine signature = new EdDSAEngine();
      signature.initSign(keyPair.getPrivate());
      byte[] orderlySignature = signature.signOneShot(message.getBytes("UTF-8"));

      return new Request.Builder()
            .url(config.baseUrl + url)
            .addHeader("Content-Type", "x-www-form-urlencoded")
            .addHeader("orderly-timestamp", "" + timestamp)
            .addHeader("orderly-account-id", accountId)
            .addHeader("orderly-key", Util.encodePublicKey(keyPair))
            .addHeader("orderly-signature", Base64.getUrlEncoder().encodeToString(orderlySignature))
            .get()
            .build();
   }

   private Request createSignedPostRequest(String url, JSONObject json)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException {
      RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));

      long timestamp = Instant.now().toEpochMilli();
      String message = "" + timestamp + "POST" + url + json.toString();

      EdDSAEngine signature = new EdDSAEngine();
      signature.initSign(keyPair.getPrivate());
      byte[] orderlySignature = signature.signOneShot(message.getBytes("UTF-8"));

      return new Request.Builder()
            .url(config.baseUrl + url)
            .addHeader("Content-Type", "application/json")
            .addHeader("orderly-timestamp", "" + timestamp)
            .addHeader("orderly-account-id", accountId)
            .addHeader("orderly-key", Util.encodePublicKey(keyPair))
            .addHeader("orderly-signature", Base64.getUrlEncoder().encodeToString(orderlySignature))
            .post(body)
            .build();
   }
}
