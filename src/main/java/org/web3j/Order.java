package org.web3j;

import java.io.IOException;
import java.security.*;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Order {

  public final Config config;

  private OkHttpClient client;
  private Signer signer;

  public Order(Config config, OkHttpClient client, Signer signer) {
    this.config = config;
    this.client = client;
    this.signer = signer;
  }

  public JSONObject getOrders() throws OrderlyClientException, InvalidKeyException, SignatureException,
      IOException {
    Request req = signer.createSignedRequest("/v1/orders");
    String res;
    try (Response response = client.newCall(req).execute()) {
      res = response.body().string();
    }
    System.out.println("client holding response: " + res);
    JSONObject obj = new JSONObject(res);
    return obj.getJSONObject("data");
  }
}
