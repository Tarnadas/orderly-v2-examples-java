package org.web3j;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Instant;

import org.bitcoinj.base.Base58;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Register {
   private OkHttpClient client;
   private Credentials credentials;

   public Register(OkHttpClient client, Credentials credentials) {
      this.client = client;
      this.credentials = credentials;
   }

   public String registerAccount() throws IOException {
      Request nonceReq = new Request.Builder()
            .url(Config.BASE_URL + "/v1/registration_nonce")
            .build();

      String nonceRes;
      try (Response response = client.newCall(nonceReq).execute()) {
         nonceRes = response.body().string();
      }
      JSONObject nonceObj = new JSONObject(nonceRes);

      String registrationNonce = nonceObj.getJSONObject("data").getString("registration_nonce");

      JSONObject registerMessage = new JSONObject();
      registerMessage.put("brokerId", Config.BROKER_ID);
      registerMessage.put("chainId", Config.CHAIN_ID);
      registerMessage.put("timestamp", Instant.now().toEpochMilli());
      registerMessage.put("registrationNonce", registrationNonce);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("types", Eip712.MESSAGE_TYPES);
      jsonObject.put("primaryType", "Registration");
      jsonObject.put("domain", Eip712.OFF_CHAIN_DOMAIN);
      jsonObject.put("message", registerMessage);

      Sign.SignatureData signature = Sign.signTypedData(jsonObject.toString(), credentials.getEcKeyPair());

      JSONObject jsonBody = new JSONObject();
      jsonBody.put("message", registerMessage);
      jsonBody.put("signature", Util.signatureToHashString(signature));
      jsonBody.put("userAddress", credentials.getAddress());
      RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
      Request registerReq = new Request.Builder()
            .url(Config.BASE_URL + "/v1/register_account")
            .post(body)
            .build();
      String registerRes;
      try (Response response = client.newCall(registerReq).execute()) {
         registerRes = response.body().string();
      }
      System.out.println("register_account response: " + registerRes);
      JSONObject registerObj = new JSONObject(registerRes);

      return registerObj.getJSONObject("data").getString("account_id");
   }

   public String addAccessKey()
         throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
      ECKeyPair key = Keys.createEcKeyPair();
      System.out.println(key.getPublicKey().toByteArray().length);
      System.out.println(key.getPrivateKey().toByteArray().length);
      // I know this doesn't make sense. You should normally send the public key
      // base58 encoded, but for some reason the private key has (almost always) 32
      // bytes and the public key has 64 bytes, if generated with Web3J even though it
      // should be the other way around.
      String orderlyKey = "ed25519:" + Base58.encode(key.getPrivateKey().toByteArray());
      // System.out.println(orderlyKey);

      JSONObject addKeyMessage = new JSONObject();
      long timestamp = Instant.now().toEpochMilli();
      addKeyMessage.put("brokerId", Config.BROKER_ID);
      addKeyMessage.put("chainId", Config.CHAIN_ID);
      addKeyMessage.put("scope", "trading");
      addKeyMessage.put("orderlyKey", orderlyKey);
      addKeyMessage.put("timestamp", timestamp);
      addKeyMessage.put("expiration", timestamp + 1_000 * 60 * 60 * 24 * 365); // 1 year

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("types", Eip712.MESSAGE_TYPES);
      jsonObject.put("primaryType", "AddOrderlyKey");
      jsonObject.put("domain", Eip712.OFF_CHAIN_DOMAIN);
      jsonObject.put("message", addKeyMessage);

      Sign.SignatureData signature = Sign.signTypedData(jsonObject.toString(), credentials.getEcKeyPair());

      JSONObject jsonBody = new JSONObject();
      jsonBody.put("message", addKeyMessage);
      jsonBody.put("signature", Util.signatureToHashString(signature));
      jsonBody.put("userAddress", credentials.getAddress());
      System.out.println(jsonBody.toString(2));
      RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
      Request addKeyReq = new Request.Builder()
            .url(Config.BASE_URL + "/v1/orderly_key")
            .post(body)
            .build();
      String addKeyRes;
      try (Response response = client.newCall(addKeyReq).execute()) {
         addKeyRes = response.body().string();
      }
      System.out.println("orderly_key response: " + addKeyRes);
      JSONObject addKeyObj = new JSONObject(addKeyRes);

      return addKeyObj.getJSONObject("data").getString("orderly_key");
   }
}
