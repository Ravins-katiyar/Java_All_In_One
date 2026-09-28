/**
 * Topic 03 - this and super (Advanced example: dispatch difference).
 *
 * Proves the bytecode-level difference between:
 *   this.method()  -> invokevirtual  -> dynamic dispatch, runtime class wins
 *   super.method() -> invokespecial  -> non-virtual, parent declaration wins
 *   Bare method()  -> invokevirtual  -> same as this.method()
 *
 * Run: javac DispatchDifferenceDemo.java && java DispatchDifferenceDemo
 * Then inspect: javap -c -p Base
 */
public class DispatchDifferenceDemo {

    public static void main(String[] args) {

        Base base = new Derived();
        System.out.println("polymorphic call  : " + base.name());   // Derived (invokevirtual)

        // Inside Derived.ownName() the call is a normal virtual call -> Derived.name()
        System.out.println("this.name()       : " + ((Derived) base).ownName());

        // Inside Derived.parentName() the call is invokespecial -> Base.name()
        System.out.println("super.name()      : " + ((Derived) base).parentName());

        // A further subclass cannot intercept a super.method() call made by Derived.
        Base second = new GrandChild();
        System.out.println("grandchild        : " + ((GrandChild) second).parentName());
    }
}

class Base {
    String name() {
        return "Base";
    }

    /** A 'final' helper is a safe alternative when you need fixed, non-virtual behaviour. */
    final String fixedName() {
        return "Base-fixed";
    }
}

class Derived extends Base {

    @Override
    String name() {
        return "Derived";
    }

    /** bare call == this.name() == invokevirtual -> dispatches to the runtime class. */
    String ownName() {
        return name();
    }

    /** invokespecial -> always Base's implementation, regardless of further overrides. */
    String parentName() {
        return super.name();
    }
}

/** Overrides name() again; Derived.parentName() still prints "Base". */
class GrandChild extends Derived {

    @Override
    String name() {
        return "GrandChild";
    }
}
