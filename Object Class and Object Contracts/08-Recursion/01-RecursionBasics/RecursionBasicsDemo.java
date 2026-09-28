/**
 * Topic 08 - Recursion (Beginner/Intermediate example).
 *
 * Every recursive method needs:
 *   1. a base case      (stops the recursion)
 *   2. a recursive case (calls itself)
 *   3. progress         (argument moves toward the base case)
 *
 * This file shows linear recursion (factorial, digits, gcd, reverse),
 * tree recursion without memoisation (slow Fibonacci) and with memoisation (fast),
 * and the equivalent iterative versions for comparison.
 *
 * Run: javac RecursionBasicsDemo.java && java RecursionBasicsDemo
 */
public class RecursionBasicsDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. linear recursion: factorial ---");
        System.out.println("factorial(5) recursive : " + factorial(5));
        System.out.println("factorial(5) iterative : " + factorialIterative(5));

        System.out.println();
        System.out.println("--- 2. unwinding: countdown ---");
        countdown(3);
        System.out.println();

        System.out.println("--- 3. sum of digits (progress by n / 10) ---");
        System.out.println("digitSum(9876)         : " + digitSum(9876));

        System.out.println();
        System.out.println("--- 4. Euclid's algorithm (mutual progress) ---");
        System.out.println("gcd(1071, 462)         : " + gcd(1071, 462));

        System.out.println();
        System.out.println("--- 5. reversing a string (base case = empty) ---");
        System.out.println("reverse(\"recursion\")  : " + reverse("recursion"));

        System.out.println();
        System.out.println("--- 6. tree recursion: exponential without memoisation ---");
        long start = System.nanoTime();
        long slow = fibNaive(30);                       // ~1.3 million calls for n = 30
        long slowMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        long fast = fibMemo(30);                         // cache makes it O(n)
        long fastMillis = (System.nanoTime() - start) / 1_000_000;

        System.out.println("fibNaive(30)           : " + slow + "   (" + slowMillis + " ms)");
        System.out.println("fibMemo(30)            : " + fast + "   (" + fastMillis + " ms)");
        System.out.println("same answer?           : " + (slow == fast));

        System.out.println();
        System.out.println("--- 7. tail-recursive methods still grow the stack in Java ---");
        System.out.println("countDownTail(5)       : " + countDownTail(5));
        System.out.println("countDownLoop(5)       : " + countDownLoop(5));
    }

    /* ------------------------------------------------------- linear recursion */

    static long factorial(int n) {
        if (n <= 1) {
            return 1;                       // base case
        }
        return n * factorial(n - 1);        // recursive case + progress
    }

    static long factorialIterative(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /** Prints on the way IN, then prints on the way OUT (unwinding). */
    static void countdown(int n) {
        if (n == 0) {
            System.out.println("  liftoff!");
            return;                          // base case
        }
        System.out.println("  descending " + n);
        countdown(n - 1);
        System.out.println("  unwinding " + n);   // runs AFTER the recursive call returns
    }

    static int digitSum(int n) {
        if (n < 10) {
            return n;                        // base case: single digit
        }
        return (n % 10) + digitSum(n / 10);  // progress: the number gets shorter
    }

    static int gcd(int a, int b) {
        if (b == 0) {
            return a;                        // base case
        }
        return gcd(b, a % b);                // progress: the remainder shrinks
    }

    static String reverse(String text) {
        if (text.isEmpty()) {
            return "";                       // base case
        }
        return reverse(text.substring(1)) + text.charAt(0);   // progress: shorter substring
    }

    /* ------------------------------- tree recursion: naive vs memoised */

    /** O(2^n): each call spawns two more; the same subproblem is recomputed many times. */
    static long fibNaive(int n) {
        if (n <= 1) {
            return n;
        }
        return fibNaive(n - 1) + fibNaive(n - 2);
    }

    /** O(n): the same recursion, but each subproblem is computed at most once. */
    static long fibMemo(int n) {
        long[] memo = new long[n + 2];       // index up to n+1 so n-1 never overflows
        return fibMemo(n, memo);
    }

    private static long fibMemo(int n, long[] memo) {
        if (n <= 1) {
            return n;
        }
        if (memo[n] != 0) {                  // cache hit
            return memo[n];
        }
        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }

    /* ---------------------------------------- tail recursion vs iteration */

    /** A tail call - but HotSpot does NOT eliminate the frame. */
    static int countDownTail(int n) {
        if (n == 0) {
            return 0;
        }
        return countDownTail(n - 1);
    }

    /** The iterative equivalent: constant stack usage, no depth limit. */
    static int countDownLoop(int n) {
        int result = 0;
        while (n > 0) {
            result = 0;
            n--;
        }
        return result;
    }
}
