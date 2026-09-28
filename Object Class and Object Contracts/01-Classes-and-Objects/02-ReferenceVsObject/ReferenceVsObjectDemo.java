/**
 * Topic 01 - Classes and Objects (Intermediate example).
 *
 * Demonstrates the single most important rule of Java references:
 * a method receives the VALUE of the reference, so it can mutate the
 * object the reference points to, but it can never re-point the
 * caller's variable.
 *
 * Run: javac ReferenceVsObjectDemo.java && java ReferenceVsObjectDemo
 */
public class ReferenceVsObjectDemo {

    public static void main(String[] args) {

        Wallet wallet = new Wallet("W-1", 100);

        changeAmount(wallet);       // mutates the SAME object -> visible to caller
        System.out.println("After changeAmount : " + wallet.amount); // 999

        replaceWallet(wallet);      // only rebinds the local parameter -> NOT visible
        System.out.println("After replaceWallet: " + wallet.amount); // still 999
        System.out.println("Caller still refers to  : " + wallet.id);        // W-1
    }

    /** Mutates the object that the copied reference points to. */
    static void changeAmount(Wallet incoming) {
        incoming.amount = 999;
    }

    /**
     * Only reassigns the LOCAL parameter variable.
     * The caller's 'wallet' variable is untouched (pass-by-value, topic 07).
     */
    static void replaceWallet(Wallet incoming) {
        incoming = new Wallet("W-999", 0);
        incoming.amount = -1;
    }
}

class Wallet {
    final String id;
    long amount;

    Wallet(String id, long amount) {
        this.id = id;
        this.amount = amount;
    }
}
