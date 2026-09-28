/**
 * Topic 09 - static (Beginner/Intermediate example).
 *
 * Shows what "belongs to the class, not to an instance" means in practice:
 *   1. a static field is ONE copy shared by every instance
 *   2. static initialisers (<clinit>) run once, in source order, after the superclass
 *   3. static methods have no 'this' and cannot touch instance state directly
 *   4. static methods are HIDDEN, not overridden (resolved from the static type)
 *   5. static nested classes have no enclosing instance
 *   6. static final constants, including why compile-time constants are inlined
 *
 * Run: javac StaticDemo.java && java StaticDemo
 */
public class StaticDemo {

    public static void main(String[] args) {

        System.out.println("--- 2. class initialisation (<clinit>) runs once ---");
        System.out.println("clinit order so far: " + InitTrace.steps);
        InitTrace.steps.add("main-called-init");
        InitTrace.touch();
        System.out.println("clinit order after second touch (no new entries): " + InitTrace.steps);

        System.out.println();
        System.out.println("--- 1. a static field is shared by all instances ---");
        Counter a = new Counter();
        Counter b = new Counter();
        a.increment();
        a.increment();
        b.increment();
        System.out.println("a.instanceCount=" + a.instanceCount + ", b.instanceCount=" + b.instanceCount);
        System.out.println("Counter.created : " + Counter.created + "  <- one shared class-level value");

        System.out.println();
        System.out.println("--- 3. static methods cannot use instance state ---");
        System.out.println("Counter.describe() : " + Counter.describe());
        // a.instanceCount is fine, but inside describe() there is no 'this':
        //   int bad() { return instanceCount; }   // compile error in a static method

        System.out.println();
        System.out.println("--- 4. static methods are hidden, not overridden ---");
        Shape shape = new Circle();
        shape.label();                     // Shape.label(): chosen from the STATIC type
        new Circle().label();               // Circle.label()

        System.out.println();
        System.out.println("--- 5. static nested classes need no outer instance ---");
        Config config = new Config("payments", 3);     // no Outer instance required
        System.out.println("nested class       : " + config);

        System.out.println();
        System.out.println("--- 6. static final constants ---");
        System.out.println("Retries.MAX_ATTEMPTS : " + Retries.MAX_ATTEMPTS + " (inlined by javac)");
        System.out.println("Retries.MODES        : " + Retries.MODES + " (runtime value, not inlined)");
    }
}

/** 1 + 3: static counter shared by every instance, static helper method. */
class Counter {

    /** ONE copy, shared by every instance of Counter (per classloader). */
    static int created;

    /** Per-object state. */
    int instanceCount;

    Counter() {
        created++;                          // visible to all instances
    }

    void increment() {
        instanceCount++;                    // per-object state
    }

    /** No 'this' available here: only static state can be touched directly. */
    static String describe() {
        return "Counter created " + created + " instance(s)";
    }
}

/** 2: static initialisers run once, in source order. */
class InitTrace {

    static java.util.List<String> steps = new java.util.ArrayList<>();

    static {
        steps.add("static-block-1");
    }

    static String label = init();

    static {
        steps.add("static-block-2");
    }

    private static String init() {
        steps.add("field-initialiser");
        return "ready";
    }

    static void touch() {
        steps.add("touch");
    }
}

/** 4: static methods are hidden by a subclass, not overridden. */
class Shape {
    static void label() {
        System.out.println("Shape.label()  <- resolved from the STATIC type (no polymorphism)");
    }
}

class Circle extends Shape {
    static void label() {
        System.out.println("Circle.label()");
    }
}

/** 5: a static nested class has no hidden reference to an enclosing instance. */
class Config {

    private final String service;
    private final int retries;

    Config(String service, int retries) {
        this.service = service;
        this.retries = retries;
    }

    @Override
    public String toString() {
        return "Config[" + service + ", retries=" + retries + "]";
    }
}

/** 6: static final constants - and the difference between compiled-in and runtime values. */
class Retries {

    /** Compile-time constant: javac substitutes 3 at every use site. */
    static final int MAX_ATTEMPTS = 3;

    /** Runtime value: the call site reads the field, so changes are picked up. */
    static final java.util.List<String> MODES = java.util.List.of("FAST", "SAFE");
}
