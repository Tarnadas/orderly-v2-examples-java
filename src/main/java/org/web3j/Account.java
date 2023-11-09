package org.web3j;

import java.io.IOException;
import java.security.*;
import java.time.Instant;
import java.util.Base64;

import net.i2p.crypto.eddsa.EdDSAEngine;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Account {

   private OkHttpClient client;
   private String accountId;
   private KeyPair keyPair;

   public Account(OkHttpClient client, String accountId, KeyPair keyPair) {
      this.client = client;
      this.accountId = accountId;
      this.keyPair = keyPair;
   }

   public void getClientHolding()
         throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException,
         InvalidAlgorithmParameterException {

      long timestamp = Instant.now().toEpochMilli();
      String message = "" + timestamp + "GET/v1/client/holding";

      EdDSAEngine signature = new EdDSAEngine();
      signature.initSign(this.keyPair.getPrivate());
      byte[] orderlySignature = signature.signOneShot(message.getBytes("UTF-8"));

      Request clientHoldingReq = new Request.Builder()
            .url(Config.BASE_URL + "/v1/client/holding")
            .addHeader("orderly-timestamp", "" + timestamp)
            .addHeader("orderly-account-id", this.accountId)
            .addHeader("orderly-key", Util.encodePublicKey(keyPair))
            .addHeader("orderly-signature", Base64.getUrlEncoder().encodeToString(orderlySignature))
            .get()
            .build();
      String clientHoldingRes;
      try (Response response = client.newCall(clientHoldingReq).execute()) {
         clientHoldingRes = response.body().string();
      }
      System.out.println("client holding response: " + clientHoldingRes);
   }
}
