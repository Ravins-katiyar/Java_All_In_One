/**
 * Topic 06 - Varargs (Advanced example: heap pollution and @SafeVarargs).
 *
 * A generic varargs parameter is NOT reifiable: at runtime its array is just Object[].
 * That allows code to put a wrong-typed element into it without any error at the write,
 * and the failure only appears later as a ClassCastException - "heap pollution".
 *
 *   - @SafeVarargs : the promise that the method does not pollute the array
 *   - the real ClassCastException caused by pollution, reproduced below
 *   - the defensive-copy rule when an array (or the varargs parameter) is stored
 *
 * Run: javac VarargsCleanDemo.java && java VarargsCleanDemo
 *      javac HeapPollutionDemo.java && java HeapPollutionDemo
 */
public class HeapPollutionDemo {

    public static void main(String[] args) {
        System.out.println("--- unsafe generic varargs: heap pollution in action ---");
        demonstrateHeapPollution();

        System.out.println();
        System.out.println("--- safe generic varargs with @SafeVarargs ---");
        System.out.println("SafeBoxes.of(\"a\", \"b\") : " + SafeBoxes.of("a", "b"));
    }

    /**
     * The pollution source: a generic varargs parameter.
     * At runtime 'lists' is Object[], so writing into it is unchecked.
     */
    @SuppressWarnings("unchecked")
    static void pollute(java.util.List<String>... lists) {
        Object[] raw = lists;                             // the array is really Object[]
        raw[0] = java.util.List.of(42);                   // an Integer list inside a List<String>[] !
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void demonstrateHeapPollution() {
        java.util.List<String>[] trusted = new java.util.List[] { java.util.List.of("original") };

        pollute(trusted);                                 // no error here...
        System.out.println("array[0] now holds  : " + trusted[0]);

        try {
            String first = trusted[0].get(0);             // ...the failure appears HERE
            System.out.println("never printed: " + first);
        } catch (ClassCastException ex) {
            System.out.println("ClassCastException   : " + ex.getMessage());
            System.out.println("^ the write above was unchecked; the read below blew up");
        }
    }
}

/** The safe pattern: promise nothing is stored into the varargs array. */
class SafeBoxes {

    /**
     * @SafeVarargs is a promise that this method does not pollute the array.
     * Note: forwarding a non-reifiable array straight into ANOTHER varargs method
     * (e.g. List.of(items)) still produces
     *   warning: [varargs] Varargs method could cause heap pollution from non-reifiable varargs parameter
     * so we copy the elements explicitly here instead.
     */
    @SafeVarargs
    static <T> java.util.List<T> of(T... items) {
        java.util.List<T> result = new java.util.ArrayList<>(items.length);
        for (T item : items) {
            result.add(item);
        }
        return java.util.List.copyOf(result);          // immutable view; never stores 'items'
    }
}
