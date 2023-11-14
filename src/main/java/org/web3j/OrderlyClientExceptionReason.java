package org.web3j;

public enum OrderlyClientExceptionReason {
   ACCOUNT_UNINITIALIZED,
   KEY_UNINITIALIZED;

   public OrderlyClientException asException() {
      return switch (this) {
         case ACCOUNT_UNINITIALIZED -> new OrderlyClientException("Account uninitialized");
         case KEY_UNINITIALIZED -> new OrderlyClientException("Key uninitialized");
      };
   }
}
