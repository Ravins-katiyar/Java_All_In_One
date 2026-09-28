import java.util.Objects;

/**
 * Topic 11 - Object class and object contracts (Beginner/Intermediate example).
 *
 * java.lang.Object is the root of every class hierarchy. Every type you write
 * already inherits its methods, unless you (implicitly or explicitly) extend
 * something else:
 *
 * public class X { } is really public class X extends Object { }
 *
 * This example shows what the DEFAULT implementations do, so that the
 * equals/hashCode contract can be understood as "what you must replace".
 *
 * Run: javac ObjectClassMethodsDemo.java && java ObjectClassMethodsDemo
 */
public class ObjectClassMethodsDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. every class extends Object (even indirectly) ---");
        System.out.println("Plain superclass        : " + Plain.class.getSuperclass().getName());
        System.out.println("String superclass       : " + String.class.getSuperclass().getName());
        System.out.println("int[] superclass        : " + int[].class.getSuperclass().getName());
        System.out.println("Integer superclass      : " + Integer.class.getSuperclass().getName());

        System.out.println();
        System.out.println("--- 2. default equals() is IDENTITY (same object) ---");
        Plain a = new Plain("x");
        Plain b = new Plain("x");
        Plain sameAsA = a;
        System.out.println("a.equals(b)             : " + a.equals(b) + "   (different objects, same content)");
        System.out.println("a.equals(sameAsA)       : " + a.equals(sameAsA));
        System.out.println("a == b                  : " + (a == b));
        System.out.println("a.equals(null)          : " + a.equals(null) + "   (must never throw - contract)");

        System.out.println();
        System.out.println("--- 3. default hashCode() is identity-based ---");
        System.out.println("a.hashCode()            : " + a.hashCode());
        System.out.println("b.hashCode()            : " + b.hashCode());
        System.out.println("a.hashCode() == identityHashCode(a) : " + (a.hashCode() == System.identityHashCode(a)));
        System.out.println("(HotSpot's default Object.hashCode is derived from identity)");

        System.out.println();
        System.out.println("--- 4. default toString() is ClassName@hexHashCode ---");
        System.out.println("a.toString()            : " + a);
        PlainWithToString c = new PlainWithToString("x");
        System.out.println("overridden toString()   : " + c);

        System.out.println();
        System.out.println("--- 5. getClass(): final, never overridden, runtime type ---");
        Object asObject = c;
        System.out.println("declared type           : Object");
        System.out.println("asObject.getClass()     : " + asObject.getClass().getName());
        System.out.println("asObject instanceof PlainWithToString : " + (asObject instanceof PlainWithToString));

        System.out.println();
        System.out.println("--- 6. java.util.Objects: the null-safe helpers ---");
        System.out.println("Objects.equals(a, b)         : " + Objects.equals(a, b));
        System.out.println("Objects.equals(null, null)   : " + Objects.equals(null, null));
        System.out.println("Objects.hashCode(null)       : " + Objects.hashCode(null));
        System.out.println("Objects.toString(null, \"?\") : " + Objects.toString(null, "?"));
        System.out.println("Objects.hash(\"A-1\", 42)      : " + Objects.hash("A-1", 42));
        try {
            Objects.requireNonNull(null, "accountId is required");
        } catch (NullPointerException e) {
            System.out.println("requireNonNull(null, msg)    : NullPointerException(" + e.getMessage() + ")");
        }

        System.out.println();
        System.out.println("--- 7. clone(): shallow by default and needs Cloneable ---");
        try {
            PlainCloneable original = new PlainCloneable("x", new int[] { 1, 2, 3 });
            PlainCloneable copy = (PlainCloneable) original.clone();
            System.out.println("copy != original               : " + (copy != original));
            System.out.println("copy.values == original.values : " + (copy.values == original.values)
                    + "   <- SHALLOW: the array is shared!");

            PlainNotCloneable bad = new PlainNotCloneable("x");
            try {
                bad.clone();
            } catch (CloneNotSupportedException e) {
                System.out.println("clone() without Cloneable      : CloneNotSupportedException");
            }
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }

        System.out.println();
        System.out.println("--- 8. wait/notify() require the monitor ---");
        Object lock = new Object();
        try {
            lock.wait(1); // no monitor -> IllegalMonitorStateException
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IllegalMonitorStateException e) {
            System.out.println("wait() outside synchronized    : IllegalMonitorStateException");
        }
    }
}

/** The default behaviour, inherited straight from Object. */
class Plain {
    final String id;

    Plain(String id) {
        this.id = id;
    }
}

class PlainWithToString {
    final String id;

    PlainWithToString(String id) {   
        this.id = id;
    }

    @Override
    public String toString() {
        return "PlainWithToString[" + id + "]";
    }
}

/** Implements Cloneable -> clone() succeeds, but it is a SHALLOW copy. */
class PlainCloneable implements Cloneable {
    final String id;
    final int[] values;

    PlainCloneable(String id, int[] values) {
        this.id = id;
        this.values = values;
    }

    @Override 
    public Object clone() throws CloneNotSupportedException {
        return super.clone();                  // Object.clone(): field-by-field (shallow)
    }
}

/** Does NOT implement Cloneable -> Object.clone() refuses. */
class PlainNotCloneable {
    final String id;

    PlainNotCloneable(String id) {
        this.id = id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
 