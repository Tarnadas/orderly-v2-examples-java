package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;

import java.math.BigInteger;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jcajce.provider.digest.Keccak;
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
      JSONArray holdings = client.account.getClientHolding();
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
         System.out.println(
               "minting test USDC. Please wait a few minutes before running again to receive your test tokens");
         return;
      }

      JSONObject orders = client.order.getOrders();
      System.out.println("orders: " + orders);

      client.order.createOrder("PERP_ETH_USDC", "MARKET", 0.01, "BUY");

      JSONObject pnl = client.pnl.settlePnL();
      System.out.println("pnl: " + pnl);

      JSONObject withdraw = client.account.withdraw("USDC", "1000000");
      System.out.println("withdraw: " + withdraw);

      Web3j web3j = Web3j.build(new HttpService("https://arbitrum-goerli.publicnode.com"));
      DefaultGasProvider gasProvider = new DefaultGasProvider();

      NativeUSDC USDC = new NativeUSDC("0xfd064a18f3bf249cf1f87fc203e90d8f650f2d63", web3j, credentials, gasProvider);
      BigInteger balance = USDC.balanceOf(credentials.getAddress()).send();
      System.out.println("USDC balance: " + balance);

      String vaultAddress = "0x0c554ddb6a9010ed1fd7e50d92559a06655da482";
      Vault vault = new Vault(vaultAddress, web3j, credentials, gasProvider);

      BigInteger depositAmount = new BigInteger("100000");

      Keccak.Digest256 brokerHash = new Keccak.Digest256();
      byte[] brokerId = client.config.brokerId.getBytes("UTF-8");
      brokerHash.update(brokerId, 0, brokerId.length);

      Keccak.Digest256 tokenHash = new Keccak.Digest256();
      byte[] usdcBytes = "USDC".getBytes("UTF-8");
      tokenHash.update(usdcBytes, 0, usdcBytes.length);

      USDC.approve(vaultAddress, depositAmount).send();
      vault.deposit(
            new Vault.VaultDepositFE(
                  Hex.decodeHex(client.getAccountId().substring(2)),
                  brokerHash.digest(),
                  tokenHash.digest(),
                  depositAmount),
            depositAmount).send();
   }
}
