package org.web3j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.time.Instant;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.*;

import net.i2p.crypto.eddsa.EdDSAEngine;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderlyClient {

   public final Config config;
   private OkHttpClient client;
   private Credentials credentials;

   private String accountId;
   private KeyPair keyPair;

   private Register registerClient;

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.registerClient = new Register(config, client, credentials);
   }

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials, String accountId) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.accountId = accountId;

      this.registerClient = new Register(config, client, credentials);
   }

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials, KeyPair keyPair) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.keyPair = keyPair;

      this.registerClient = new Register(config, client, credentials);
   }

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials, String accountId,
         KeyPair keyPair) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.accountId = accountId;
      this.keyPair = keyPair;

      this.registerClient = new Register(config, client, credentials);
   }

   /**
    * Initializes client.
    * Fetches account ID and registers account, if not yet registered.
    * 
    * @throws IOException
    */
   public void initialize() throws IOException {
      Request req = new Request.Builder()
            .url(config.baseUrl + "/v1/get_account?address=" + credentials.getAddress() + "&broker_id="
                  + config.brokerId)
            .build();

      String res;
      try (Response response = client.newCall(req).execute()) {
         res = response.body().string();
      }
      JSONObject accountObj = new JSONObject(res);
      System.out.println("get_account response: " + accountObj);

      registerClient = new Register(config, client, credentials);

      if (accountObj.getBoolean("success")) {
         accountId = accountObj.getJSONObject("data").getString("account_id");
      } else {
         accountId = registerClient.registerAccount();
      }
      System.out.println("accountId: " + accountId);
   }

   /**
    * Create new randomly generated orderly key and stores it in the client.
    *
    * @throws InvalidAlgorithmParameterException
    * @throws NoSuchAlgorithmException
    * @throws NoSuchProviderException
    * @throws IOException
    */
   public void createNewAccessKey()
         throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
      keyPair = this.registerClient.addAccessKey();
   }

   /**
    * Get the current summary of user token holdings.
    * 
    * @throws NoSuchAlgorithmException
    * @throws InvalidKeyException
    * @throws SignatureException
    * @throws IOException
    * @throws InvalidAlgorithmParameterException
    * @throws OrderlyClientException
    */
   public JSONArray getClientHolding()
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException,
         InvalidAlgorithmParameterException, OrderlyClientException {
      checkKeyPairPresent();

      Request req = createSignedRequest("/v1/client/holding");
      String res;
      try (Response response = client.newCall(req).execute()) {
         res = response.body().string();
      }
      System.out.println("client holding response: " + res);
      JSONObject obj = new JSONObject(res);
      return obj.getJSONObject("data").getJSONArray("holding");
   }

   public Request createSignedRequest(String url)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException {
      return createSignedRequest(url, "GET", null);
   }

   public Request createSignedRequest(String url, String method, JSONObject json)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException, IllegalArgumentException {
      if (method == "GET") {
         return createSignedGetRequest(url);
      } else if (method == "POST") {
         return createSignedPostRequest(url, json);
      } else {
         throw new IllegalArgumentException();
      }
   }

   private void checkKeyPairPresent() throws OrderlyClientException {
      if (keyPair == null) {
         throw OrderlyClientExceptionReason.KEY_UNINITIALIZED.asException();
      }
   }

   private Request createSignedGetRequest(String url)
         throws SignatureException, UnsupportedEncodingException, InvalidKeyException {
      long timestamp = Instant.now().toEpochMilli();
      String message = "" + timestamp + "GET" + url;

      EdDSAEngine signature = new EdDSAEngine();
      signature.initSign(this.keyPair.getPrivate());
      byte[] orderlySignature = signature.signOneShot(message.getBytes("UTF-8"));

      return new Request.Builder()
            .url(config.baseUrl + url)
            .addHeader("orderly-timestamp", "" + timestamp)
            .addHeader("orderly-account-id", this.accountId)
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
      signature.initSign(this.keyPair.getPrivate());
      byte[] orderlySignature = signature.signOneShot(message.getBytes("UTF-8"));

      return new Request.Builder()
            .url(config.baseUrl + url)
            .addHeader("orderly-timestamp", "" + timestamp)
            .addHeader("orderly-account-id", this.accountId)
            .addHeader("orderly-key", Util.encodePublicKey(keyPair))
            .addHeader("orderly-signature", Base64.getUrlEncoder().encodeToString(orderlySignature))
            .post(body)
            .build();
   }
}
