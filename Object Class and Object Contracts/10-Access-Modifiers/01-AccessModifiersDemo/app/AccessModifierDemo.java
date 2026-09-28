package app;

/**
 * Topic 10 - Access Modifiers: what code in a DIFFERENT package can see.
 *
 * This class lives in 'app', so it can use only the PUBLIC surface of
 * 'library.Account'. Everything else is deliberately out of reach - which is
 * how encapsulation is enforced by the compiler rather than by convention.
 *
 * Run:
 *   cd 01-AccessModifiersDemo && javac library/*.java app/*.java && java app.AccessModifierDemo
 */
public class AccessModifierDemo {

    public static void main(String[] args) {

        library.Account account = new library.Account("A-101", 500);

        System.out.println("--- accessible from another package (public only) ---");
        System.out.println("label            : " + account.label);
        System.out.println("publicMethod()   : " + account.publicMethod());
        System.out.println("describeFromInside() (public method, private internals) :");
        System.out.println("                   " + account.describeFromInside());
        System.out.println("withInternalHelper() : " + account.withInternalHelper());

        System.out.println();
        System.out.println("--- NOT accessible from another package (would not compile) ---");
        System.out.println("account.balance      -> protected: only through a subclass reference");
        System.out.println("account.packageNote  -> package-private: 'library' only");
        System.out.println("account.packageMethod() -> package-private: 'library' only");
        System.out.println("account.secretKey()  -> private: 'Account' only");
        System.out.println("library.InternalHelper -> package-private TYPE: not even nameable here");

        System.out.println();
        System.out.println("--- same-package subclass ('library' package) ---");
        library.SubAccount subAccount = new library.SubAccount("A-202", 1_000);
        System.out.println(subAccount.describeFromSamePackageSubclass());

        System.out.println();
        System.out.println("--- other-package subclass ('app' package) ---");
        OutOfPackageSub outOfPackage = new OutOfPackageSub("A-303", 2_000);
        System.out.println(outOfPackage.describeFromOtherPackageSubclass());
        System.out.println("reading a foreign Account through the superclass type is a compile error:");
        System.out.println("  publicValue() = " + outOfPackage.readProtectedOfAnotherAccount(account));
    }
}
