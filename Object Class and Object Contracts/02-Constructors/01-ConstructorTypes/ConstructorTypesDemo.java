/**
 * Topic 02 - Constructors (Beginner example).
 *
 * Shows every constructor flavour in one place:
 *   - implicit default constructor (class with no constructor)
 *   - explicit no-arg constructor that chains with this(...)
 *   - parameterised constructor that validates
 *   - copy constructor
 *   - private constructor + static factory method
 *
 * Run: javac ConstructorTypesDemo.java && java ConstructorTypesDemo
 */
public class ConstructorTypesDemo {

    public static void main(String[] args) {

        // 1) No-arg constructor -> chains to the validating constructor.
        BankAccount unassigned = new BankAccount();
        System.out.println("no-arg        : " + unassigned);

        // 2) Parameterised constructor.
        BankAccount real = new BankAccount("A-101", 500);
        System.out.println("parameterised : " + real);

        // 3) Copy constructor -> independent object with the same state.
        BankAccount copy = new BankAccount(real);
        copy.deposit(100);
        System.out.println("original      : " + real);
        System.out.println("copy          : " + copy);
        System.out.println("same object?  : " + (real == copy));   // false

        // 4) Private constructor is reachable only through the factory method.
        BankAccount newCustomer = BankAccount.forNewCustomer("C-303");
        System.out.println("via factory   : " + newCustomer);

        // 5) Validation is enforced by the constructor, not by the caller's discipline.
        try {
            new BankAccount("", 0);
        } catch (IllegalArgumentException ex) {
            System.out.println("rejected      : " + ex.getMessage());
        }

        // 6) The classic trap: 'void BankAccount()' is a METHOD, not a constructor.
        Trap defaultConstructed = new Trap();   // uses the implicit default constructor
        defaultConstructed.BankAccount();       // calls an ordinary method
    }
}

class BankAccount {

    private final String accountNumber;
    private long balance;

    /** No-arg constructor: delegates to the validating constructor. */
    BankAccount() {
        this("UNASSIGNED", 0L);          // this(...) MUST be the first statement
    }

    /** The single place where the object's invariant is enforced. */
    BankAccount(String accountNumber, long balance) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    /** Copy constructor: an explicit, readable alternative to Cloneable. */
    BankAccount(BankAccount other) {
        this(other.accountNumber, other.balance);
    }

    /** Private constructor: creation is possible only inside this class. */
    private BankAccount(String accountNumber) {
        this(accountNumber, 0L);
    }

    static BankAccount forNewCustomer(String accountNumber) {
        return new BankAccount(accountNumber);   // allowed: same class
    }

    void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("deposit must be positive");
        }
        balance += amount;
    }

    @Override
    public String toString() {
        return "BankAccount[" + accountNumber + ", balance=" + balance + "]";
    }
}

/** Demonstrates how easy it is to write a method that *looks* like a constructor. */
class Trap {
    // NOT a constructor: it has a return type, so it is just an oddly named method.
    public void BankAccount() {
        System.out.println("Trap.BankAccount() is a METHOD, not a constructor");
    }
}
