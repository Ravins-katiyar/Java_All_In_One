/**
 * Topic 04 - Methods (Intermediate example: signature and covariance).
 *
 * Demonstrates two rules that are frequently asked in interviews:
 *   1. The return type is NOT part of the method signature.
 *      -> you cannot overload a method by changing only the return type.
 *      (The illegal attempt is shown as a comment; uncommenting breaks compilation.)
 *   2. Covariant return types ARE allowed when overriding:
 *      an override may return a subtype of the original return type.
 *
 * Run: javac CovariantReturnDemo.java && java CovariantReturnDemo
 */
public class CovariantReturnDemo {

    public static void main(String[] args) {

        // Covariant return: the subclass's narrowed return type is used directly.
        IntegerFactory factory = new IntegerFactory();
        Integer produced = factory.create();            // no cast needed
        System.out.println("covariant return : " + produced + " (" + produced.getClass().getSimpleName() + ")");

        // Through the parent reference, the declared type is used.
        Factory asFactory = factory;
        Number asNumber = asFactory.create();           // still the same object at runtime
        System.out.println("declared type    : " + asNumber.getClass().getSimpleName());

        // Overloading uses parameter TYPES only, so the order matters.
        Printer printer = new Printer();
        printer.print("A-1", 100);                      // print(String, int)
        printer.print(100, "A-1");                      // print(int, String)
        printer.print("A-1");                           // print(String)
    }
}

class Factory {
    Number create() {
        return 1;
    }
}

class IntegerFactory extends Factory {
    /**
     * Covariant return type: Integer is a subtype of Number, so this is a valid override.
     */
    @Override
    Integer create() {
        return 1;
    }
}

class Printer {

    void print(String id, int amount) {
        System.out.println("print(String,int) : " + id + "/" + amount);
    }

    void print(int amount, String id) {
        System.out.println("print(int,String) : " + amount + "/" + id);
    }

    void print(String id) {
        System.out.println("print(String)     : " + id);
    }

    // ILLEGAL: same signature (name + parameter types) - only the return type differs.
    // int print(String id) { return 0; }
    //
    // ILLEGAL: 'throws' is not part of the signature either.
    // void print(String id) throws java.io.IOException { }
}
