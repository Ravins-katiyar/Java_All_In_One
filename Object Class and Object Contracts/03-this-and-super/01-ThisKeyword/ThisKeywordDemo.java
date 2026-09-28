/**
 * Topic 03 - this and super (Beginner example: 'this').
 *
 * Shows the five practical uses of 'this':
 *   1. disambiguating a shadowed field
 *   2. constructor chaining with this(...)
 *   3. returning the current object (fluent API)
 *   4. passing the current object as an argument
 *   5. qualifying the outer instance from an inner class (Outer.this)
 *
 * Run: javac ThisKeywordDemo.java && java ThisKeywordDemo
 */
public class ThisKeywordDemo {

    public static void main(String[] args) {

        // 1 + 2: shadowing is resolved by this.x, chaining reuses one validation path
        BankAccount account = new BankAccount("A-101", 500);
        System.out.println("1/2 shadowing + chain : " + account);

        // 3: fluent API - every setter returns 'this'
        BankAccount fluent = new BankAccount()
                .withHolder("Ravins")
                .withBalance(1_000);
        System.out.println("3   fluent chain      : " + fluent);

        // 4: passing 'this' as an argument
        Registry registry = new Registry();
        registry.register(account);
        registry.register(fluent);
        System.out.println("4   registered        : " + registry.size());

        // 5: inner class naming the outer instance explicitly
        Scheduler scheduler = new Scheduler("nightly-settlement");
        System.out.println("5   outer from inner  : " + scheduler.new Job().describe());

        // The silent self-assignment bug, shown safely
        BuggySetter buggy = new BuggySetter();
        buggy.setBalanceBroken(999);
        buggy.setBalanceFixed(999);
        System.out.println("bug  broken setter    : balance=" + buggy.balance + " (parameter assigned to itself)");
    }
}

class BankAccount {

    private String accountNumber;
    private String holder;
    private long balance;

    /** No-arg constructor chains to the real one via this(...). */
    BankAccount() {
        this("UNASSIGNED", 0L);
    }

    BankAccount(String accountNumber, long balance) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber required");
        }
        this.accountNumber = accountNumber;   // 'this' = the object being constructed
        this.balance = balance;               // parameter shadows the field
    }

    // Fluent setters: return the current object so calls can be chained.
    BankAccount withHolder(String holder) {
        this.holder = holder;
        return this;                          // no new object is created
    }

    BankAccount withBalance(long balance) {
        this.balance = balance;
        return this;
    }

    @Override
    public String toString() {
        return "BankAccount[" + accountNumber + ", holder=" + holder + ", balance=" + balance + "]";
    }
}

/** Receives 'this' from BankAccount instances. */
class Registry {
    private int count;

    void register(BankAccount account) {
        count++;
    }

    int size() {
        return count;
    }
}

/** An inner (non-static) class can name its enclosing instance as Outer.this. */
class Scheduler {

    private final String name;
    private int runs;

    Scheduler(String name) {
        this.name = name;
    }

    class Job {
        String describe() {
            Scheduler.this.runs++;              // the outer instance, explicitly
            return "job of " + Scheduler.this.name + " (run #" + Scheduler.this.runs + ")";
        }
    }
}

/** Demonstrates the classic 'x = x' self-assignment bug and its fix. */
class BuggySetter {

    long balance;

    void setBalanceBroken(long balance) {
        balance = balance;                      // assigns the parameter to itself: compiles, does nothing
    }

    void setBalanceFixed(long balance) {
        this.balance = balance;                 // correct: assigns the parameter to the field
    }
}
