# Operators, Control Flow, and Modern Switch Expressions

## 1. Main Idea
Operators and control flow statements dictate the execution path and logic of a Java program. **Operators** perform mathematical, logical, relational, and bitwise operations. **Control Flow** statements (`if-else`, `for`, `while`) govern conditional execution and iteration. **Modern Switch Expressions** (fully standardized in Java 14+) provide a concise, expression-oriented alternative to traditional switch statements, supporting arrow syntax (`->`), multiple case labels, and value returns without `break` statements.

## 2. Why Does This Exist?
- **Operators:** Allow computational manipulation, comparison, and boolean evaluation.
- **Control Flow:** Enables software to make decisions, execute loops, and handle business logic conditionally.
- **Modern Switch Expressions:** Eliminate error-prone fall-through behavior, reduce verbosity, and allow switch blocks to return values directly as expressions.

## 3. Prerequisites
- Variables, data types, and basic Java syntax (`01_Java_Execution_Model.md`, `02_Variables_Data_Types.md`).

## 4. Core Concepts

### Operators & Control Flow
- **Operators:** Arithmetic (`+`, `-`, `*`, `/`, `%`), Relational (`==`, `!=`, `>`, `<`), Logical (`&&`, `||`, `!`), and Ternary (`condition ? true : false`).
- **Control Flow:** Conditional (`if`, `if-else`, `switch`) and Loops (`for`, `enhanced for`, `while`).

### Modern Switch Expressions (Java 14+)
- **Arrow Syntax (`->`):** Executes only the matching branch without needing `break`.
- **Switch as an Expression:** Can return a value directly assigned to a variable.
- **Multiple Constants:** Combine case labels cleanly (e.g., `case 1, 2, 3 ->`).
- **`yield` keyword:** Used in block bodies (`{ ... }`) to return a value from a switch expression branch.


## 5. Syntax & Example
```java
public class ControlFlowDemo {
    public static void main(String[] args) {
        int score = 85;
        String grade = (score >= 90) ? "A" : (score >= 80) ? "B" : "C";

        String status = "ACTIVE";
        String response = switch (status) {
            case "ACTIVE", "RUNNING" -> "System is operational";
            case "PENDING" -> "System is starting up";
            default -> "Unknown status";
        };

        System.out.println("Grade: " + grade);
        System.out.println("Status Response: " + response);
    }
}
```

## 6. Real-Life Analogy
A railway switchman at a junction. Depending on the incoming train's destination sign (variable condition), the switchman pulls the lever to direct the train down track A, track B, or track C.

## 7. Real Backend Example
In a Spring Boot e-commerce backend processing order fulfillment status:
```java
public String calculateShippingFee(String orderType, double orderAmount) {
    return switch (orderType) {
        case "EXPRESS" -> orderAmount > 100.0 ? "Free Express" : "$15.00";
        case "STANDARD" -> orderAmount > 50.0 ? "Free Standard" : "$5.00";
        default -> throw new IllegalArgumentException("Invalid order type");
    };
}
```

## 8. Internal Working
- **Branching:** Compiled into bytecode instructions such as `tableswitch` or `lookupswitch`.
- **`tableswitch`:** Used for dense/sequential case values (O(1) jump table lookup).
- **`lookupswitch`:** Used for sparse case values (binary search O(log N)).

## 9. Important Rules & Common Mistakes
1. **Exhaustiveness:** Modern switch expressions used as assignments must cover all possible values (requiring `default` or exhaustive enums).
2. **Fall-through:** Traditional switch statements require `break` to prevent falling through to subsequent cases.

## 10. Interview Questions
- **Q1: What is the difference between traditional switch statements and modern switch expressions?**
  - Traditional switch uses colons (`case 1:`), requires explicit `break`, and cannot return values directly. Modern switch uses arrows (`->`), has no fall-through, and can return values as expressions.
- **Q2: When does the JVM generate a `tableswitch` versus a `lookupswitch`?**
  - `tableswitch` is generated for dense sequential cases (O(1)). `lookupswitch` is generated for sparse cases (O(log N)).
