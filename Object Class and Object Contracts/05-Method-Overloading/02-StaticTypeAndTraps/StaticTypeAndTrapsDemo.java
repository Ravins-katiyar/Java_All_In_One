/**
 * Topic 05 - Method Overloading (Advanced example: the traps).
 *
 * Two traps that appear in real production code:
 *
 *   TRAP 1 - Static type decides.
 *            Overloads are chosen with the STATIC type of the receiver/arguments,
 *            so a parent-typed reference cannot reach a child's narrower overload.
 *
 *   TRAP 2 - equals(MyType) OVERLOADS Object.equals instead of OVERRIDING it.
 *            HashSet/HashMap call equals(Object), so lookups silently fail.
 *            This is the most common real-world overloading bug.
 *
 * Run: javac StaticTypeAndTrapsDemo.java && java StaticTypeAndTrapsDemo
 */
import java.util.HashSet;
import java.util.Set;

public class StaticTypeAndTrapsDemo {

    public static void main(String[] args) {

        System.out.println("--- TRAP 1: static type decides the overload ---");
        Parent parentReference = new Child();
        parentReference.handle("payload");     // Parent.handle(Object): compiled from the static type
        ((Child) parentReference).handle("payload");   // Child.handle(String)

        System.out.println();
        System.out.println("--- TRAP 2: equals(MyType) overloads instead of overrides ---");

        Account a1 = new Account("A-1");
        Account a2 = new Account("A-1");

        // This call has both arguments typed Account -> the overload equals(Account) is chosen.
        System.out.println("a1.equals(a2)            : " + a1.equals(a2));   // true (wrong reason)

        // HashSet stores/compares through the Object contract -> Object.equals -> identity.
        Object asObject = a2;
        System.out.println("a1.equals((Object) a2)   : " + a1.equals(asObject));   // false

        Set<Account> accounts = new HashSet<>();
        accounts.add(a1);
        System.out.println("set.contains(same id)    : " + accounts.contains(a2)); // false - BUG
        System.out.println("set.size()               : " + accounts.size());

        System.out.println();
        System.out.println("--- the same test with the CORRECT equals(Object) ---");
        FixedAccount f1 = new FixedAccount("A-1");
        FixedAccount f2 = new FixedAccount("A-1");
        Set<FixedAccount> fixedSet = new HashSet<>();
        fixedSet.add(f1);
        System.out.println("fixedSet.contains(...)   : " + fixedSet.contains(f2)); // true
        System.out.println("f1.equals((Object) f2)   : " + f1.equals((Object) f2)); // true

        System.out.println();
        System.out.println("--- null and the most specific overload ---");
        new NullChooser().find((String) null);   // find(String): most specific of String/Object
    }
}

/* ------------------------------------------------------------------ TRAP 1 */

class Parent {
    void handle(Object payload) {
        System.out.println("Parent.handle(Object)  <- chosen because the reference is typed Parent");
    }
}

class Child extends Parent {
    void handle(String payload) {                 // OVERLOAD, not an override
        System.out.println("Child.handle(String)");
    }
}

/* ------------------------------------------------------------------ TRAP 2 */

/**
 * BROKEN: declares equals(Account) which is a DIFFERENT method from equals(Object).
 * Object.equals is still the identity comparison, so collections misbehave.
 */
class Account {

    private final String id;

    Account(String id) {
        this.id = id;
    }

    // Not an override! Signature is equals(Account), not equals(Object).
    public boolean equals(Account other) {
        return other != null && id.equals(other.id);
    }
    // hashCode() deliberately not overridden either.
}

/** CORRECT: the parameter type is Object, so this really does override the contract. */
class FixedAccount {

    private final String id;

    FixedAccount(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;                                  // reflexivity + fast path
        }
        if (!(other instanceof FixedAccount account)) {    // pattern matching (Java 16+)
            return false;                                  // also handles null
        }
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();                              // must agree with equals
    }
}

/* ------------------------------------------------------------------ null */

class NullChooser {
    void find(String id) { System.out.println("find(String)  <- most specific for null"); }
    void find(Object id) { System.out.println("find(Object)"); }

    // AMBIGUOUS (does not compile): two equally specific unrelated types
    // void find(Integer id) { }
    //   -> error: reference to find is ambiguous
    //      both method find(String) in NullChooser and method find(Integer) in NullChooser match
}
