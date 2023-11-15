package org.web3j;

import java.io.IOException;
import java.security.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrderlyClient {

   public final Config config;
   private OkHttpClient client;
   private Credentials credentials;

   private String accountId;
   private Signer signer;

   private Register registerClient;

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.registerClient = new Register(config, client, credentials);
   }

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials,
         String accountId) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.accountId = accountId;

      this.registerClient = new Register(config, client, credentials);
   }

   public OrderlyClient(Config config, OkHttpClient client, Credentials credentials,
         String accountId,
         KeyPair keyPair) {
      this.config = config;
      this.client = client;
      this.credentials = credentials;

      this.accountId = accountId;
      this.signer = new Signer(config, accountId, keyPair);

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
      signer = new Signer(config, accountId, this.registerClient.addAccessKey());
   }

   /**
    * Get the current summary of user token holdings.
    * 
    * @throws OrderlyClientException
    * @throws InvalidKeyException
    * @throws SignatureException
    * @throws IOException
    */
   public JSONArray getClientHolding()
         throws OrderlyClientException, InvalidKeyException, SignatureException, IOException {
      Request req = signer.createSignedRequest("/v1/client/holding");
      String res;
      try (Response response = client.newCall(req).execute()) {
         res = response.body().string();
      }
      System.out.println("client holding response: " + res);
      JSONObject obj = new JSONObject(res);
      return obj.getJSONObject("data").getJSONArray("holding");
   }
}
