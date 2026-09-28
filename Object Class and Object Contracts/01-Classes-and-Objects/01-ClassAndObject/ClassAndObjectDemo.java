/**
 * Topic 01 - Classes and Objects (Beginner example).
 *
 * Demonstrates:
 *  - one class -> many independent objects (separate heap state)
 *  - reference assignment aliases the object (it does NOT copy it)
 *  - '==' compares references, '.equals()' compares content when overridden
 *
 * Run: javac ClassAndObjectDemo.java && java ClassAndObjectDemo
 */
public class ClassAndObjectDemo {

    public static void main(String[] args) {

        // Two 'new' calls -> two separate objects on the heap.
        BankAccount accountA = new BankAccount("A-101", 500);
        BankAccount accountB = new BankAccount("B-202", 900);

        accountA.deposit(250); // mutates only accountA

        System.out.println("A balance : " + accountA.getBalance()); // 750
        System.out.println("B balance : " + accountB.getBalance()); // 900

        // --- Reference aliasing -------------------------------------------------
        BankAccount alias = accountA;   // copies the REFERENCE, not the object
        alias.deposit(100);             // same object as accountA

        System.out.println("A balance after alias deposit: " + accountA.getBalance()); // 850
        System.out.println("accountA == alias : " + (accountA == alias));               // true

        // --- '==' vs '.equals()' ------------------------------------------------
        BankAccount accountC = new BankAccount("A-101", 850); // same values, different object

        System.out.println("accountA == accountC      : " + (accountA == accountC));      // false
        System.out.println("accountA.equals(accountC) : " + accountA.equals(accountC));  // false (no override here)
    }
}

/**
 * The blueprint. Every BankAccount object gets its own copy of both fields.
 */
class BankAccount {

    // Instance fields: stored inside each object on the heap.
    private String accountNumber;
    private long balance;

    BankAccount(String accountNumber, long balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    void deposit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit must be positive");
        }
        this.balance += amount;
    }

    long getBalance() {
        return balance;
    }

    String getAccountNumber() {
        return accountNumber;
    }

    // Intentionally NOT overriding equals() yet: this topic shows the default
    // reference-equality behaviour. Topic 11 replaces this with a real contract.
    @Override
    public String toString() {
        return "BankAccount{" + accountNumber + ", balance=" + balance + "}";
    }
}
