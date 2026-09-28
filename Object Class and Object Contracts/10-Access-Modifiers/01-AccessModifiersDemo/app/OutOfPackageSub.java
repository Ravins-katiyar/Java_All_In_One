package app;

/**
 * Topic 10 - Access Modifiers: a subclass in a DIFFERENT package.
 *
 * The 'protected' rule across packages has a subtlety worth memorising:
 * inside a subclass in another package you may access a protected member only
 * through a reference whose type is the subclass (or a subtype) - NOT through
 * a reference typed as the superclass.
 *
 * Run:
 *   cd 01-AccessModifiersDemo && javac library/*.java app/*.java && java app.AccessModifierDemo
 */
public class OutOfPackageSub extends library.Account {

    public OutOfPackageSub(String id, long balance) {
        super(id, balance);
    }

    /** Works: 'this' has the subclass type, so protected members are reachable. */
    public String describeFromOtherPackageSubclass() {
        balance = balance + 25;                     // protected: OK through 'this'
        long viaThis = this.balance;                // protected: OK
        String publicCall = publicMethod();         // public: OK
        long protectedCall = protectedMethod();     // protected: OK

        // NOT accessible from a different package:
        // String packageField = packageNote;       // package-private: other package
        // String packageCall = packageMethod();    // package-private: other package
        // String id = this.id;                     // private
        // String secret = secretKey();             // private
        // InternalHelper h = new InternalHelper(); // package-private TYPE: not even nameable

        return "other-package subclass -> balance=" + viaThis
                + " | protectedMethod=" + protectedCall
                + " | " + publicCall;
    }

    /**
     * ILLEGAL: through a superclass-typed reference, protected access is denied
     * in a different package. Uncommenting the next line breaks the build.
     */
    public long readProtectedOfAnotherAccount(library.Account other) {
        // return other.balance;
        //     error: balance has protected access in library.Account
        return other.publicValue();
    }
}
