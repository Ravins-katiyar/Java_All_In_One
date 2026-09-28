/**
 * Topic 09 - static/final (Advanced example: why mutable static state is dangerous).
 *
 * Demonstrates the hazard behind "static means shared":
 *   1. a static counter incremented by many threads loses updates (count++ is not atomic)
 *   2. the same code with synchronized / AtomicInteger is correct
 *   3. a static final MUTABLE collection is still mutable (final protects the reference only)
 *
 * This is the bridge to the concurrency module: shared mutable state needs coordination.
 *
 * Run: javac SharedStateDangerDemo.java && java SharedStateDangerDemo
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedStateDangerDemo {

    private static final int THREADS = 8;
    private static final int INCREMENTS_PER_THREAD = 100_000;
    private static final int EXPECTED = THREADS * INCREMENTS_PER_THREAD;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("expected total            : " + EXPECTED);

        System.out.println();
        System.out.println("--- 1. plain static int: lost updates ---");
        runAll(UnsafeCounter::increment);
        System.out.println("UnsafeCounter.value       : " + UnsafeCounter.value
                + "   <- usually LESS than expected");

        System.out.println();
        System.out.println("--- 2a. static int guarded by synchronized ---");
        runAll(GuardedCounter::increment);
        System.out.println("GuardedCounter.value      : " + GuardedCounter.value + "   <- always correct");

        System.out.println();
        System.out.println("--- 2b. static AtomicInteger ---");
        runAll(AtomicCounter::increment);
        System.out.println("AtomicCounter.value       : " + AtomicCounter.value + "   <- always correct");

        System.out.println();
        System.out.println("--- 3. static final List is still mutable ---");
        System.out.println("Registry.MODES before     : " + Registry.MODES);
        Registry.register("EXPRESS");                       // legal: only the REFERENCE is final
        System.out.println("Registry.MODES after      : " + Registry.MODES
                + "   <- static final did not protect the contents");
    }

    /** Runs 'work' once per thread, then waits for all threads to finish. */
    private static void runAll(Runnable work) throws InterruptedException {
        List<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    work.run();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();                                  // wait for every worker
        }
    }
}

/** Static mutable state with a non-atomic read-modify-write: count++ is three bytecodes. */
class UnsafeCounter {
    static int value;

    static void increment() {
        value++;                     // getfield, add, putfield -- interleaving loses updates
    }
}

/** The same counter with mutual exclusion: correctness at the cost of contention. */
class GuardedCounter {
    static int value;

    static synchronized void increment() {
        value++;
    }
}

/** Lock-free alternative using an atomic class (java.util.concurrent.atomic). */
class AtomicCounter {
    static final AtomicInteger value = new AtomicInteger();

    static void increment() {
        value.incrementAndGet();
    }
}

/** 'static final' makes the reference constant, NOT the contents. */
class Registry {
    static final List<String> MODES = new ArrayList<>(List.of("FAST", "SAFE"));

    static void register(String mode) {
        MODES.add(mode);             // completely legal
    }
}
