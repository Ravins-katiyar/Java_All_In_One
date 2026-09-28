import java.util.HashSet;
import java.util.Set;

/**
 * Topic 11 - Records and the object contract (the modern shortcut).
 *
 * A record (Java 16+) is a transparent carrier for immutable data. The compiler
 * generates, based on the components:
 *
 *   - a canonical constructor
 *   - public accessor methods named after the components
 *   - equals(), hashCode() and toString()   <-- the contract, for free
 *
 * Verified with javap: the generated methods delegate through invokedynamic to
 * java.lang.runtime.ObjectMethods, so the semantics are the documented ones.
 *
 * Run: javac RecordsContractDemo.java && java RecordsContractDemo
 */
public class RecordsContractDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. record gets the whole contract for free ---");
        Money a = new Money(1250, "eur");
        Money b = new Money(1250, "EUR");            // normalised by the compact constructor
        System.out.println("toString        : " + a);                     // Money[minorUnits=1250, currency=EUR]
        System.out.println("accessors       : " + a.minorUnits() + " / " + a.currency());
        System.out.println("a.equals(b)     : " + a.equals(b));
        System.out.println("hashCode equal? : " + (a.hashCode() == b.hashCode()));

        Set<Money> set = new HashSet<>();
        set.add(a);
        set.add(b);
        System.out.println("HashSet size    : " + set.size() + "   <- deduplicated by the generated contract");

        System.out.println();
        System.out.println("--- 2. the same data as a plain class: no contract ---");
        PlainAmount p1 = new PlainAmount(1250, "EUR");
        PlainAmount p2 = new PlainAmount(1250, "EUR");
        System.out.println("p1.equals(p2)   : " + p1.equals(p2) + "   <- Object.equals: identity only");
        Set<PlainAmount> plainSet = new HashSet<>();
        plainSet.add(p1);
        plainSet.add(p2);
        System.out.println("HashSet size    : " + plainSet.size() + "   <- no deduplication");

        System.out.println();
        System.out.println("--- 3. a record's superclass is java.lang.Record ---");
        System.out.println("Money superclass: " + Money.class.getSuperclass().getName());
        System.out.println("Money is final  : " + java.lang.reflect.Modifier.isFinal(Money.class.getModifiers()));

        System.out.println();
        System.out.println("--- 4. every class, including records, is an Object ---");
        Object asObject = a;
        System.out.println("getClass()      : " + asObject.getClass().getName());
        System.out.println("hashCode()      : " + asObject.hashCode());
        System.out.println("equals(itself)  : " + asObject.equals(a));
        System.out.println("equals(null)    : " + asObject.equals(null));
        System.out.println("toString()      : " + asObject);

        System.out.println();
        System.out.println("--- 5. GOTCHA: an array component uses reference equality ---");
        IntArrayHolder h1 = new IntArrayHolder(new int[]{1, 2, 3});
        IntArrayHolder h2 = new IntArrayHolder(new int[]{1, 2, 3});
        System.out.println("equal arrays, equal records? : " + h1.equals(h2)
                + "   <- FALSE: the component is compared with Object.equals");
        System.out.println("same array, equal records?   : " + h1.equals(new IntArrayHolder(h1.values())));
        System.out.println("(use List<Integer> components or a custom equals for array data)");

        System.out.println();
        System.out.println("--- 6. records are immutable: no setters, final components ---");
        System.out.println("record components are private final; the only way to 'change' one is to make a new record");
        Money larger = new Money(a.minorUnits() + 750, a.currency());
        System.out.println("a       : " + a);
        System.out.println("larger  : " + larger);
        System.out.println("a unchanged: " + (a.minorUnits() == 1250));
    }
}

/**
 * A value object as a record.
 * The compact constructor validates and normalises; the contract methods are generated.
 */
record Money(long minorUnits, String currency) {

    Money {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("minorUnits must not be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        currency = currency.toUpperCase();            // assignment to the parameter becomes the field value
    }
}

/** The same data as a plain class, WITHOUT overriding equals/hashCode (for contrast). */
class PlainAmount {

    private final long minorUnits;
    private final String currency;

    PlainAmount(long minorUnits, String currency) {
        this.minorUnits = minorUnits;
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "PlainAmount[" + minorUnits + ", " + currency + "]";
    }
    // No equals / hashCode -> identity semantics.
}

/** A record with an ARRAY component: the generated equals uses reference equality for the array. */
record IntArrayHolder(int[] values) { }
