/**
 * Topic 08 - Recursion (Advanced example: backtracking and the stack limit).
 *
 * Shows the three things that matter when recursion meets production:
 *   1. backtracking = choose / explore / un-choose (permutations)
 *   2. the stack is finite: unbounded recursion throws StackOverflowError (measured)
 *   3. the same traversal with an EXPLICIT stack on the heap is not depth-limited
 *
 * Run: javac BacktrackingAndStackDemo.java && java BacktrackingAndStackDemo
 */
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BacktrackingAndStackDemo {

    private static int depth;

    public static void main(String[] args) {

        System.out.println("--- 1. backtracking: all permutations of [A, B, C] ---");
        System.out.println(permutations(List.of("A", "B", "C")));

        System.out.println();
        System.out.println("--- 2. unbounded recursion exhausts the thread stack ---");
        System.out.println("deepest frame reached : " + deepestFrame());
        System.out.println("(platform dependent; never design code around this number)");

        System.out.println();
        System.out.println("--- 3. same problem, explicit stack on the HEAP ---");
        // A degenerate 200 000-node chain would overflow a recursive traversal,
        // but an explicit ArrayDeque grows on the heap instead.
        ChainNode chainTail = new ChainNode(0, null);
        ChainNode chainRoot = chainTail;
        for (int i = 1; i <= 200_000; i++) {
            chainRoot = new ChainNode(i, chainRoot);
        }
        System.out.println("recursive count of 200 001-node chain : would throw StackOverflowError");
        System.out.println("iterative count with explicit stack   : " + countIteratively(chainRoot));
    }

    /* ------------------------------------------------------------- backtracking */

    static List<List<String>> permutations(List<String> items) {
        List<List<String>> results = new ArrayList<>();
        permute(new ArrayList<>(items), 0, results);
        return results;
    }

    /** Classic in-place backtracking: swap into position, recurse, swap back. */
    private static void permute(List<String> items, int position, List<List<String>> results) {
        if (position == items.size()) {                 // base case: one full arrangement
            results.add(new ArrayList<>(items));        // copy, because 'items' keeps changing
            return;
        }
        for (int i = position; i < items.size(); i++) {
            swap(items, position, i);                   // choose
            permute(items, position + 1, results);      // explore
            swap(items, position, i);                   // un-choose (backtrack)
        }
    }

    private static void swap(List<String> items, int i, int j) {
        String tmp = items.get(i);
        items.set(i, items.get(j));
        items.set(j, tmp);
    }

    /* -------------------------------------------------------- the stack limit */

    /** No base case on purpose: the JVM must eventually refuse a new frame. */
    static int grow(int frameNumber) {
        depth = frameNumber;
        return grow(frameNumber + 1);
    }

    static int deepestFrame() {
        try {
            grow(0);
            return -1;
        } catch (StackOverflowError error) {            // java.lang.Error, not Exception
            return depth;
        }
    }

    /* ----------------------------------------- the same walk without recursion */

    /**
     * Iterative pre-order count using an explicit stack (LIFO).
     * Stack depth is now bounded by the HEAP, not by the thread stack,
     * so a degenerate tree of millions of nodes is fine.
     */
    static long countIteratively(ChainNode root) {
        if (root == null) {
            return 0;
        }
        long count = 0;
        Deque<ChainNode> pending = new ArrayDeque<>();       // explicit stack: heap memory
        pending.push(root);
        while (!pending.isEmpty()) {
            ChainNode current = pending.pop();
            count++;
            if (current.left != null)  pending.push(current.left);
            if (current.right != null) pending.push(current.right);
        }
        return count;
    }
}

class ChainNode {
    final int value;
    final ChainNode left;
    final ChainNode right;

    ChainNode(int value, ChainNode left) {
        this(value, left, null);
    }

    ChainNode(int value, ChainNode left, ChainNode right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }
}
