/**
 * Topic 02 - Constructors (Intermediate example).
 *
 * Prints the EXACT initialisation order for a two-level hierarchy:
 *   static  -> parent instance -> parent ctor -> child instance -> child ctor
 *
 * Run: javac InitializationOrderDemo.java && java InitializationOrderDemo
 */
public class InitializationOrderDemo {

    public static void main(String[] args) {
        System.out.println("--- about to create Child ---");
        new Child();
        System.out.println("--- creating a second Child (static init runs only once) ---");
        new Child();
    }
}

class Parent {

    // 1) Static field initialiser -> compiled into <clinit>, runs once per classloader.
    static int parentStatic = log("1. Parent static field initialiser");

    // 2) Instance field initialiser -> compiled into <init>, after super() call.
    int parentInstance = log("3. Parent instance field initialiser");

    // 3) Instance initialiser block -> also compiled into <init>, in source order.
    {
        log("4. Parent instance initialiser block");
    }

    static int log(String message) {
        System.out.println(message);
        return 0;
    }

    Parent() {
        // An implicit super(); (java.lang.Object) runs before reaching this line.
        log("5. Parent constructor body");
    }
}

class Child extends Parent {

    static int childStatic = Parent.log("2. Child static field initialiser");

    int childInstance = Parent.log("6. Child instance field initialiser");

    {
        Parent.log("7. Child instance initialiser block");
    }

    Child() {
        // An implicit super(); runs first -> that is why Parent() printed before this.
        Parent.log("8. Child constructor body");
    }
}
