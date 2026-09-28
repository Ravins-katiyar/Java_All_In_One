package library;

/**
 * Topic 10 - Access Modifiers: the four visibility levels on one class.
 *
 *   private        : only inside this top-level class (including its nested classes)
 *   (no modifier)  : package-private - any class in the same package
 *   protected      : same package + subclasses in other packages
 *   public         : everywhere (subject to module exports)
 *
 * Run:
 *   cd 01-AccessModifiersDemo && javac library/*.java app/*.java && java app.AccessModifierDemo
 */
public class Account {

    /** private: never visible outside Account. */
    private final String id;

    /** package-private: visible to every class in 'library'. */
    final String packageNote = "packageNote (package-private field)";

    /** protected: visible in 'library' AND in subclasses elsewhere. */
    protected long balance;

    /** public: visible to everyone. */
    public final String label = "label (public field)";

    public Account(String id, long balance) {
        this.id = id;
        this.balance = balance;
    }

    private String secretKey() {
        return "secretKey (private method)";
    }

    String packageMethod() {
        return "packageMethod (package-private method)";
    }

    protected long protectedMethod() {
        return balance;
    }

    public String publicMethod() {
        return "publicMethod (public method)";
    }

    /** The public accessor a caller in another package must use instead of 'balance'. */
    public long publicValue() {
        return balance;
    }

    /**
     * Inside the declaring class EVERYTHING is reachable - this method is the
     * reference point for the access matrix in the README.
     */
    public String describeFromInside() {
        return String.join(" | ",
                id,
                packageNote,
                "balance=" + balance,
                label,
                secretKey(),
                packageMethod(),
                "protectedMethod=" + protectedMethod(),
                publicMethod());
    }

    /** Uses the package-private helper class: legal because it is in the same package. */
    public String withInternalHelper() {
        return "handled by " + InternalHelper.describe();
    }
}
