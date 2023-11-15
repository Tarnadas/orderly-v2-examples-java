package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

public class OrderlyV2ExamplesJava {
   public static void main(String[] args) throws Exception {
      Dotenv dotenv = Dotenv.load();
      String pk = dotenv.get("PRIVATE_KEY");
      Credentials credentials = Credentials.create(ECKeyPair.create(Hex.decodeHex(pk)));
      System.out.println("Address: " + credentials.getAddress());

      OrderlyClient client = new OrderlyClient(Config.testnet(), new OkHttpClient(), credentials);
      client.initialize();
      client.createNewAccessKey();
      JSONArray holdings = client.getClientHolding();
      System.out.println("Holdings: " + holdings);

      boolean hasUSDC = false;
      for (Object holding : holdings) {
         if (((JSONObject) holding).getString("token").equals("USDC")
               && ((JSONObject) holding).getFloat("holding") > 100.) {
            hasUSDC = true;
            break;
         }
      }
      if (!hasUSDC) {
         TestnetUtil.mintTestUSDC(new OkHttpClient(), client.config, credentials);
      }
   }
}
