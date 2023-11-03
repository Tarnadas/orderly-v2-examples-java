package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

public class OrderlyV2ExamplesJava {
   public static void main(String[] args) throws Exception {
      OkHttpClient client = new OkHttpClient();

      Dotenv dotenv = Dotenv.load();
      String pk = dotenv.get("PRIVATE_KEY");
      Credentials credentials = Credentials.create(ECKeyPair.create(Hex.decodeHex(pk)));
      System.out.println("Address: " + credentials.getAddress());

      Request accountReq = new Request.Builder()
            .url(Config.BASE_URL + "/v1/get_account?address=" + credentials.getAddress() + "&broker_id="
                  + Config.BROKER_ID)
            .build();

      String accountRes;
      try (Response response = client.newCall(accountReq).execute()) {
         accountRes = response.body().string();
      }
      JSONObject accountObj = new JSONObject(accountRes);
      System.out.println("get_account response: " + accountObj);

      Register register = new Register(client, credentials);

      String accountId;
      if (accountObj.getBoolean("success")) {
         accountId = accountObj.getJSONObject("data").getString("account_id");
      } else {
         accountId = register.registerAccount();
      }
      System.out.println("accountId: " + accountId);

      String orderlyKey = register.addAccessKey();
      System.out.println("orderlyKey: " + orderlyKey);
   }
}
