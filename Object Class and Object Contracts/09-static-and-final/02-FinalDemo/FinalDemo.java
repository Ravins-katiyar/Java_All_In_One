/**
 * Topic 09 - final (Beginner/Intermediate example).
 *
 * Shows every use of 'final':
 *   1. final local variable and final parameter
 *   2. blank final instance field (assigned in every constructor)
 *   3. final REFERENCE to a MUTABLE object (the reference is frozen, the object is not)
 *   4. final method (cannot be overridden) and final class (cannot be extended)
 *   5. effectively final capture in a lambda (Java 8+)
 *   6. a fully immutable class built from final fields
 *
 * Run: javac FinalDemo.java && java FinalDemo
 */
import java.util.ArrayList;
import java.util.List;

public class FinalDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. final local variable ---");
        final int maxRetries = 3;
        // maxRetries = 4;            // compile error: cannot assign a final variable
        System.out.println("maxRetries        : " + maxRetries);

        System.out.println();
        System.out.println("--- 2. blank final fields, assigned per constructor ---");
        System.out.println("Point(3, 4)       : " + new ImmutablePoint(3, 4));
        System.out.println("Point(5)          : " + new ImmutablePoint(5));   // chains with this(...)

        System.out.println();
        System.out.println("--- 3. final reference, mutable object ---");
        final List<String> modes = new ArrayList<>();
        modes.add("FAST");                          // LEGAL: the list contents may change
        modes.add("SAFE");
        // modes = new ArrayList<>();               // compile error: cannot assign a final variable
        System.out.println("modes             : " + modes + "   <- reference is final, list is not");

        System.out.println();
        System.out.println("--- 4. final method and final class ---");
        System.out.println("Base.describe()   : " + new Base().describe());
        System.out.println("Derived.describe(): " + new Derived().describe());
        System.out.println("(uncommenting an @Override of describe() is a compile error)");

        System.out.println();
        System.out.println("--- 5. effectively final capture in a lambda ---");
        String tenant = "acme";                     // never reassigned -> effectively final
        Runnable task = () -> System.out.println("lambda sees tenant: " + tenant);
        task.run();
        // tenant = "other";  // would make the lambda stop compiling

        System.out.println();
        System.out.println("--- 6. fully immutable value class (all fields final) ---");
        AccountId id = new AccountId("ACC-1");
        System.out.println("id                : " + id);
        System.out.println("id.value()        : " + id.value());
    }
}

/** 2 + 6: every field is final, so the object is immutable and safely shareable. */
final class ImmutablePoint {

    private final int x;
    private final int y;

    ImmutablePoint(int x, int y) {
        this.x = x;                                 // blank final assigned here
        this.y = y;
    }

    ImmutablePoint(int x) {
        this(x, 0);                                 // chaining still assigns both finals once
    }

    @Override
    public String toString() {
        return "ImmutablePoint(" + x + ", " + y + ")";
    }
}

/** 4: a final method cannot be overridden. */
class Base {
    final String describe() {
        return "Base.describe (final: cannot be overridden)";
    }
}

class Derived extends Base {
    String describe2() {                             // a NEW method, allowed
        return "Derived adds a different method";
    }

    // @Override String describe() { return "nope"; }
    // compile error: describe() in Derived cannot override describe() in Base;
    //               overridden method is final
}

/** 4: a final class cannot be extended. */
final class SealedValue {
    private final String value;

    SealedValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SealedValue[" + value + "]";
    }
}

// class Sub extends SealedValue { }
// compile error: cannot inherit from final SealedValue

/** 6: an immutable identifier: final class + final field + no mutators. */
final class AccountId {

    private final String value;

    AccountId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value required");
        }
        this.value = value;
    }

    String value() {
        return value;
    }

    @Override
    public String toString() {
        return "AccountId[" + value + "]";
    }
}
