/**
 * Topic 06 - Varargs (Beginner/Intermediate example).
 *
 * Demonstrates that 'Type... name' is compiler sugar for a 'Type[] name' parameter:
 *   - zero, one or many arguments are wrapped into a NEW array at the call site
 *   - passing an existing array does NOT copy it (the method receives YOUR array)
 *   - mutation of the varargs array is therefore visible to the caller
 *   - a null array vs a one-element array containing null
 *   - fixed-arity overload beats varargs (phase 2 before phase 3, topic 05)
 *
 * Run: javac VarargsBasicsDemo.java && java VarargsBasicsDemo
 */
import java.util.Arrays;

public class VarargsBasicsDemo {

    public static void main(String[] args) {

        System.out.println("--- zero / one / many arguments ---");
        Printer.log("no extras:");
        Printer.log("one extra:", "A");
        Printer.log("three extras:", "A", "B", "C");
        System.out.println("total()          : " + Printer.total());
        System.out.println("total(1)         : " + Printer.total(1));
        System.out.println("total(1,2,3,4)   : " + Printer.total(1, 2, 3, 4));

        System.out.println();
        System.out.println("--- an existing array is passed by reference (no copy) ---");
        int[] numbers = {1, 2, 3};
        Printer.mutate(numbers);                    // the SAME array reaches the method
        System.out.println("caller's array   : " + Arrays.toString(numbers) + "  <- changed!");

        System.out.println();
        System.out.println("--- making a defensive copy keeps the caller safe ---");
        int[] safeNumbers = {1, 2, 3};
        Printer.mutateSafely(safeNumbers);
        System.out.println("caller's array   : " + Arrays.toString(safeNumbers) + "  <- unchanged");

        System.out.println();
        System.out.println("--- null: an explicit null array vs one element holding null ---");
        Printer.log("null array:", (Object[]) null);   // args == null
        Printer.log("null element:", (Object) null);   // args.length == 1 and args[0] == null

        System.out.println();
        System.out.println("--- fixed arity wins over varargs ---");
        new Chooser().pick("x");                    // pick(String) - fixed arity
        new Chooser().pick("x", "y");               // pick(String, String...) - varargs required
    }
}

class Printer {

    /** 'Object... args' is really 'Object[] args'. */
    static void log(String label, Object... args) {
        if (args == null) {
            System.out.println(label + " args array itself is null");
        } else {
            System.out.println(label + " args.length=" + args.length + " -> " + Arrays.toString(args));
        }
    }

    static int total(int... values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }

    /** DANGEROUS if the array is meant to be kept: it mutates the caller's own array. */
    static void mutate(int... values) {
        if (values.length > 0) {
            values[0] = 99;
        }
    }

    /** Safe version: copy before touching, so the caller's array is never modified. */
    static void mutateSafely(int... values) {
        int[] copy = Arrays.copyOf(values, values.length);
        if (copy.length > 0) {
            copy[0] = 99;
        }
    }
}

class Chooser {
    void pick(String value) {
        System.out.println("pick(String)                 <- fixed arity wins");
    }

    void pick(String first, String... rest) {
        System.out.println("pick(String, String...)      <- varargs used for " + rest.length + " rest value(s)");
    }
}
