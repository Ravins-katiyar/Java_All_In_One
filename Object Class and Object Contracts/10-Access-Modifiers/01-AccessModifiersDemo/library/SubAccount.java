package library;

/**
 * Topic 10 - Access Modifiers: a subclass in the SAME package.
 *
 * Being in the same package means it can reach BOTH package-private and
 * protected members - plus, of course, its own inherited private state only
 * through Account's public/protected API.
 *
 * Run:
 * cd 01-AccessModifiersDemo && javac library/*.java app/*.java && java
 * app.AccessModifierDemo
 */
public class SubAccount extends Account {

    public SubAccount(String id, long balance) {
        super(id, balance);
    }

    /** Demonstrates the same-package access matrix. */
    public String describeFromSamePackageSubclass() {
        balance = balance + 10; // protected: OK (same package + subclass)
        String packageField = packageNote; // package-private: OK (same package)
        String packageCall = packageMethod(); // package-private method: OK
        String protectedCall = String.valueOf(protectedMethod()); // protected: OK
        String publicCall = publicMethod(); // public: OK
        String helper = InternalHelper.describe(); // package-private TYPE: OK (same package)
        String helperDetail = InternalHelper.extendedDetail(); // protected member of a
                                                               // package-private type: OK here

        // NOT accessible even from a same-package subclass:
        // String id = this.id; // private in Account
        // String secret = secretKey(); // private in Account

        return "same-package subclass -> balance=" + balance
                + " | " + packageField
                + " | " + packageCall
                + " | protectedMethod=" + protectedCall
                + " | " + publicCall
                + " | " + helper
                + " | " + helperDetail;
    }

    public long balance() {
        return balance;
    }
}
