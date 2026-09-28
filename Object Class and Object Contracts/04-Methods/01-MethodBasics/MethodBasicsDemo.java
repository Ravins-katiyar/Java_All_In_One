/**
 * Topic 04 - Methods (Beginner example).
 *
 * Shows:
 *   - instance vs static methods
 *   - parameters vs arguments
 *   - void vs value-returning methods
 *   - guard clauses instead of nested if/else
 *   - methods calling methods
 *   - fluent method chaining
 *
 * Run: javac MethodBasicsDemo.java && java MethodBasicsDemo
 */
public class MethodBasicsDemo {

    public static void main(String[] args) {

        Calculator calculator = new Calculator();

        // Arguments: 7 and 5 are the ARGUMENTS; a and b inside add(int,int) are the PARAMETERS.
        System.out.println("add              : " + calculator.add(7, 5));

        calculator.printSum(7, 5);                       // void method

        System.out.println("divide           : " + calculator.divide(10, 4));

        // Static method: called on the class, not on an object.
        System.out.println("isEven(10)       : " + Calculator.isEven(10));

        // Varargs: 0..n values (topic 06).
        System.out.println("sum()            : " + calculator.sum());
        System.out.println("sum(1,2,3)       : " + calculator.sum(1, 2, 3));

        // Fluent chaining: each call returns 'this'.
        calculator.log().log().log();

        // Guard clause: invalid input is rejected at the top of the method.
        try {
            calculator.transfer("", 100);
        } catch (IllegalArgumentException ex) {
            System.out.println("rejected         : " + ex.getMessage());
        }
    }
}

class Calculator {

    /** Instance method using the receiver. */
    int add(int a, int b) {
        return a + b;
    }

    /** void: performs an action, returns nothing. */
    void printSum(int a, int b) {
        System.out.println("printSum         : " + add(a, b));   // calls add(a, b)
    }

    /** Non-void method: every path returns a value or throws. */
    int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("division by zero");
        }
        return a / b;
    }

    /** Static: no 'this', cannot touch instance fields, resolved at compile time. */
    static boolean isEven(int value) {
        return value % 2 == 0;
    }

    /** Varargs method: the parameter is really an int[]. */
    int sum(int... values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    /** Fluent method: returns the current object so calls can be chained. */
    Calculator log() {
        System.out.println("log()            : chained call");
        return this;
    }

    /** Guard clauses: validate early, then keep the happy path un-nested. */
    void transfer(String accountId, long amount) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        System.out.println("transfer         : " + amount + " to " + accountId);
    }
}
