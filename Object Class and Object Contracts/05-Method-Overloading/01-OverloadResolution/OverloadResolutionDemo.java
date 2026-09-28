/**
 * Topic 05 - Method Overloading (Beginner/Intermediate example).
 *
 * Demonstrates the three phases of overload resolution (JLS 15.12.2) and the
 * "most specific method" rule, exactly as compiled and run on JDK 21:
 *
 *   phase 1 strict  : widening primitive / widening reference (no boxing, no varargs)
 *   phase 2 loose   : + boxing / unboxing (no varargs)
 *   phase 3 varargs : + variable arity methods
 *
 * The compiler stops at the first phase that produces applicable methods.
 *
 * Run: javac OverloadResolutionDemo.java && java OverloadResolutionDemo
 */
public class OverloadResolutionDemo {

    public static void main(String[] args) {

        System.out.println("--- phase 1 beats phase 2 (widening beats boxing) ---");
        new WideningVsBoxing().store(1);          // int -> long (widening), not Integer

        System.out.println("--- phase 2 beats phase 3 (boxing beats varargs) ---");
        new BoxingVsVarargs().store(1);           // int -> Object (boxing), not int...

        System.out.println("--- most specific method wins ---");
        new MostSpecific().describe("x");         // String, not Object

        System.out.println("--- fixed arity beats varargs ---");
        new FixedVsVarargs().total(1, 2);         // f(int,int), not f(int...)
        new FixedVsVarargs().total(1);            // f(int), not f(int...)

        System.out.println("--- parameter order changes the overload ---");
        new OrderMatters().send("A-1", 100);      // send(String,int)
        new OrderMatters().send(100, "A-1");      // send(int,String)
    }
}

class WideningVsBoxing {
    void store(long value)   { System.out.println("store(long)     <- phase 1: widening primitive"); }
    void store(Integer value){ System.out.println("store(Integer)  <- phase 2: boxing"); }
}

class BoxingVsVarargs {
    void store(Object value) { System.out.println("store(Object)   <- phase 2: boxing + widening reference"); }
    void store(int... value) { System.out.println("store(int...)   <- phase 3: variable arity"); }
}

class MostSpecific {
    void describe(String value) { System.out.println("describe(String) <- most specific"); }
    void describe(Object value) { System.out.println("describe(Object) <- less specific"); }
}

class FixedVsVarargs {
    void total(int a, int b) { System.out.println("total(int,int)  <- fixed arity"); }
    void total(int... v)     { System.out.println("total(int...)   <- variable arity"); }
}

class OrderMatters {
    void send(String id, int amount) { System.out.println("send(String,int) <- parameter types differ"); }
    void send(int amount, String id) { System.out.println("send(int,String) <- parameter ORDER differs"); }
}
