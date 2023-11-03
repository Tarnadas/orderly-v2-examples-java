package org.web3j;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public abstract class Util {
   public static String signatureToHashString(Sign.SignatureData signature) {
      byte[] retval = new byte[65];
      System.arraycopy(signature.getR(), 0, retval, 0, 32);
      System.arraycopy(signature.getS(), 0, retval, 32, 32);
      System.arraycopy(signature.getV(), 0, retval, 64, 1);
      return Numeric.toHexString(retval);
   }
}
