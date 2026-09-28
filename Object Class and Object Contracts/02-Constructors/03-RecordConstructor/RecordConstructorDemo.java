/**
 * Topic 02 - Constructors (Advanced example: records).
 *
 * A record (Java 16+) is a concise immutable data carrier. The compiler generates:
 *   - the canonical constructor (one parameter per component)
 *   - accessor methods with the same name as the component
 *   - equals(), hashCode() and toString() based on ALL components
 *
 * A COMPACT constructor lets you validate and normalise without repeating
 * the parameter list or the field assignments.
 *
 * Run: javac RecordConstructorDemo.java && java RecordConstructorDemo
 */
public class RecordConstructorDemo {

    public static void main(String[] args) {

        Money amount = new Money(1250, "eur");
        System.out.println("record          : " + amount);          // Money[minorUnits=1250, currency=EUR]
        System.out.println("accessor        : " + amount.currency()); // EUR (normalised to upper case)

        // Generated equals(): value-based, compares components.
        Money same = new Money(1250, "EUR");
        System.out.println("record equals   : " + amount.equals(same));   // true

        // The compact constructor rejects invalid state.
        try {
            new Money(-1, "EUR");
        } catch (IllegalArgumentException ex) {
            System.out.println("rejected        : " + ex.getMessage());
        }

        // A normal class must implement the contract itself (topic 11).
        Amount a = new Amount(1250, "EUR");
        Amount b = new Amount(1250, "EUR");
        System.out.println("class equals    : " + a.equals(b)); // false: Object.equals = identity
    }
}

/**
 * Immutable value object as a record.
 * The compact constructor has NO parameter list; it runs before the fields are set
 * and its parameter assignments (e.g. currency = ...) become the field values.
 */
record Money(long minorUnits, String currency) {
    Money {
        if (minorUnits < 0) {
            throw new IllegalArgumentException("minorUnits must not be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        currency = currency.toUpperCase();   // normalisation applied to the component
    }
}

/**
 * The same idea as a plain class, WITHOUT overriding equals/hashCode.
 * Kept here on purpose to contrast with the record above.
 */
class Amount {
    private final long minorUnits;
    private final String currency;

    Amount(long minorUnits, String currency) {
        this.minorUnits = minorUnits;
        this.currency = currency.toUpperCase();
    }

    @Override
    public String toString() {
        return "Amount[" + minorUnits + ", " + currency + "]";
    }
}
