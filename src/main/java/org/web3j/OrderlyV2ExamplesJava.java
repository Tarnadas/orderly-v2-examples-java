package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Hex;

public class OrderlyV2ExamplesJava {
   public static void main(String[] args) throws Exception {
      Dotenv dotenv = Dotenv.load();
      String pk = dotenv.get("PRIVATE_KEY");
      Credentials credentials = Credentials.create(ECKeyPair.create(Hex.decodeHex(pk)));
      System.out.println("Address: " + credentials.getAddress());

      OrderlyClient client = new OrderlyClient(Config.testnet(), new OkHttpClient(), credentials);
      client.initialize();
      client.createNewAccessKey();
      client.getClientHolding();
   }
}
