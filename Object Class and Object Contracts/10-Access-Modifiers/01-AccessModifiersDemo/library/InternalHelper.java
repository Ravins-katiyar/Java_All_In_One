package library;

/**
 * Topic 10 - Access Modifiers: a PACKAGE-PRIVATE top-level class.
 *
 * There is no modifier, so this class is visible only inside the 'library' package.
 * Code in 'app' cannot even name it, which is exactly the point: implementation
 * details stay internal while 'Account' (public) is the published API.
 *
 * Also note: only 'public' and package-private are allowed for TOP-LEVEL types.
 * A top-level class can never be 'private' or 'protected'.
 */
class InternalHelper {

    static String describe() {
        return "InternalHelper (package-private type, invisible outside 'library')";
    }

    /** A package-private type may be exposed to subclasses through a protected method. */
    protected static String extendedDetail() {
        return "extendedDetail (protected static method on a package-private type)";
    }
}
