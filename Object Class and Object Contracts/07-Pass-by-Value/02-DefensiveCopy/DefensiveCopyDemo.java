/**
 * Topic 07 - Pass-by-Value (Advanced example: defensive copying).
 *
 * Because a method receives a COPY of the reference, any object it stores can be
 * mutated later by the original owner. This example shows:
 *
 *   1. the leak: a constructor that stores the caller's list
 *   2. the fix: List.copyOf (a snapshot, and it rejects nulls)
 *   3. returning internal state: mutable array leak vs clone()
 *   4. view vs snapshot: unmodifiableList vs List.copyOf
 *   5. emulating an out-parameter with a small mutable holder (Java has no 'out')
 *
 * Run: javac DefensiveCopyDemo.java && java DefensiveCopyDemo
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefensiveCopyDemo {

    public static void main(String[] args) {

        System.out.println("--- 1 & 2. storing an argument without copying ---");

        List<String> callerList = new ArrayList<>(List.of("apple"));
        LeakyBasket leaky = new LeakyBasket(callerList);
        callerList.add("attacker-value");                  // caller still owns the same list
        System.out.println("leaky basket      : " + leaky.items() + "   <- internal state changed!");

        List<String> safeList = new ArrayList<>(List.of("apple"));
        SafeBasket safe = new SafeBasket(safeList);
        safeList.add("attacker-value");
        System.out.println("safe basket       : " + safe.items() + "   <- copied, unaffected");

        System.out.println();
        System.out.println("--- 3. leaking internal arrays ---");
        TagHolder leakyHolder = new TagHolder(new String[]{"vip"});
        String[] leaked = leakyHolder.tagsLeaked();
        leaked[0] = "hacked";
        System.out.println("after leak        : " + leakyHolder.describe() + "   <- internal state corrupted");

        TagHolder carefulHolder = new TagHolder(new String[]{"vip"});
        String[] safeCopy = carefulHolder.tagsSafely();
        safeCopy[0] = "hacked";
        System.out.println("after safe copy   : " + carefulHolder.describe() + "   <- unchanged");

        System.out.println();
        System.out.println("--- 4. view vs snapshot ---");
        List<String> live = new ArrayList<>(List.of("one"));
        List<String> view = Collections.unmodifiableList(live);   // view of the SAME list
        List<String> snapshot = List.copyOf(live);                // independent copy
        live.add("two");
        System.out.println("view              : " + view + "     <- reflects later changes");
        System.out.println("snapshot          : " + snapshot + "   <- frozen when created");
        try {
            view.add("three");
        } catch (UnsupportedOperationException ex) {
            System.out.println("view is read-only : UnsupportedOperationException");
        }

        System.out.println();
        System.out.println("--- 5. emulating an out-parameter with a holder ---");
        Counter counter = new Counter();
        increment(counter, 5);                     // Java cannot pass a variable by reference
        increment(counter, 2);
        System.out.println("counter.value     : " + counter.value + "   <- holder was mutated, not the variable");
    }

    /** Java has no out-parameters; a mutable holder is the workaround. */
    static void increment(Counter counter, int by) {
        counter.value += by;                       // mutates the shared holder object
    }
}

/* --------------------------------------------------- 1 & 2: list argument */

class LeakyBasket {
    private final List<String> items;

    LeakyBasket(List<String> items) {
        this.items = items;                        // WRONG: stores the caller's list reference
    }

    List<String> items() {
        return items;                              // WRONG: hands the mutable list straight out
    }
}

class SafeBasket {
    private final List<String> items;

    SafeBasket(List<String> items) {
        this.items = List.copyOf(items);           // snapshot: caller cannot mutate our state
    }

    List<String> items() {
        return items;                              // List.copyOf is already unmodifiable
    }
}

/* --------------------------------------------------------- 3: array access */

class TagHolder {
    private final String[] tags;

    TagHolder(String[] tags) {
        this.tags = tags.clone();                  // copy on the way in
    }

    /** Leaks the internal array: the caller can now mutate our state. */
    String[] tagsLeaked() {
        return tags;
    }

    /** Safe: returns a copy, so mutations affect only the caller's copy. */
    String[] tagsSafely() {
        return tags.clone();
    }

    String describe() {
        return java.util.Arrays.toString(tags);
    }
}

/* ------------------------------------------------------------ 5: the holder */

final class Counter {
    long value;
}
