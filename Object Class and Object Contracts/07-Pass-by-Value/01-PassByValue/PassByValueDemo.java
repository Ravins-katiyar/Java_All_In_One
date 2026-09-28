/**
 * Topic 07 - Pass-by-Value (Beginner/Intermediate example).
 *
 * Java is ALWAYS pass-by-value. For objects, the VALUE that is copied is the
 * REFERENCE, so:
 *
 *   1. primitive parameter reassignment   -> NOT visible to the caller
 *   2. reference parameter reassignment   -> NOT visible to the caller
 *   3. mutating the shared object         -> VISIBLE to the caller
 *   4. mutating a shared array element    -> VISIBLE to the caller
 *   5. swap(a, b) can never work for references
 *   6. String vs StringBuilder exposes why (immutability, not pass-by-reference)
 *
 * Run: javac PassByValueDemo.java && java PassByValueDemo
 */
public class PassByValueDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. reassigning a primitive parameter ---");
        int count = 5;
        bumpInt(count);
        System.out.println("caller count            : " + count + "   (unchanged: the COPY was incremented)");

        System.out.println();
        System.out.println("--- 2. reassigning a reference parameter ---");
        Wallet wallet = new Wallet("W-1", 100);
        replaceWallet(wallet);
        System.out.println("caller wallet.id        : " + wallet.id + "   (unchanged: only the copied arrow moved)");

        System.out.println();
        System.out.println("--- 3. mutating the SHARED object ---");
        deposit(wallet, 900);
        System.out.println("caller wallet.amount    : " + wallet.amount + "  (changed: same object on the heap)");

        System.out.println();
        System.out.println("--- 4. mutating a shared array element ---");
        long[] balances = {10, 20, 30};
        zeroFirst(balances);
        System.out.println("caller balances[0]      : " + balances[0] + "    (changed: same array object)");

        System.out.println();
        System.out.println("--- 5. swap can never work ---");
        Wallet x = new Wallet("X", 1);
        Wallet y = new Wallet("Y", 2);
        swap(x, y);
        System.out.println("after swap: x.id=" + x.id + ", y.id=" + y.id + "  (unchanged: only locals swapped)");
        Wallet[] swapped = swapAndReturn(x, y);
        System.out.println("returning works: x.id=" + swapped[0].id + ", y.id=" + swapped[1].id);

        System.out.println();
        System.out.println("--- 6. String (immutable) vs StringBuilder (mutable) ---");
        String text = "hello";
        appendBangString(text);
        System.out.println("String caller value     : " + text + "        (unchanged)");

        StringBuilder builder = new StringBuilder("hello");
        appendBangBuilder(builder);
        System.out.println("StringBuilder caller    : " + builder + "        (changed)");
    }

    static void bumpInt(int n) {
        n++;                                  // modifies the local copy only
    }

    static void replaceWallet(Wallet incoming) {
        incoming = new Wallet("REPLACED", -1); // rebinds the local copy only
    }

    static void deposit(Wallet incoming, long amount) {
        incoming.amount += amount;             // dereferences the shared reference
    }

    static void zeroFirst(long[] values) {
        values[0] = 0;                         // shared array object is modified
    }

    static void swap(Wallet a, Wallet b) {
        Wallet temp = a;
        a = b;
        b = temp;                              // only the two local slots change
    }

    /** The idiomatic fix: return the result instead of trying to mutate the caller's variables. */
    static Wallet[] swapAndReturn(Wallet a, Wallet b) {
        return new Wallet[]{b, a};
    }

    static void appendBangString(String s) {
        s = s + "!";                           // rebinding + immutability: invisible outside
    }

    static void appendBangBuilder(StringBuilder s) {
        s.append("!");                         // mutating the shared, mutable object
    }
}

/** A simple mutable object: its FIELDS can be changed through a copied reference. */
class Wallet {

    String id;
    long amount;

    Wallet(String id, long amount) {
        this.id = id;
        this.amount = amount;
    }
}
