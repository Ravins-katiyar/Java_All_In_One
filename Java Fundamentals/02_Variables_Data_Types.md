# Variables, Data Types, Primitive vs Reference Types, Wrapper Classes, Autoboxing, Unboxing, and Type Casting

## 1. Main Idea
Java is a strictly statically-typed language, meaning every variable must be declared with a data type known at compile time. Data types are categorized into two fundamental groups: **Primitive Types** (which store raw numerical or boolean values directly in memory) and **Reference Types** (which store memory addresses pointing to objects on the heap). **Wrapper Classes** (`Integer`, `Double`, `Boolean`, etc.) encapsulate primitive values into objects, allowing primitives to participate in object-oriented structures like Collections. **Autoboxing** and **unboxing** provide automatic conversion between primitives and their corresponding wrapper classes. **Type casting** allows converting a value from one data type to another, either implicitly (widening) or explicitly (narrowing).

## 2. Why Does This Exist?
- **Statically-Typed Variables:** Catch type errors at compile time rather than production runtime, ensuring type safety and optimal memory allocation.
- **Primitives vs References:** Primitives maximize CPU performance and memory efficiency for basic values, while reference types enable complex object graphs, polymorphism, and OOP design.
- **Wrapper Classes & Autoboxing:** Java collections (`ArrayList`, `HashMap`) store objects, not raw primitives. Wrapper classes bridge this gap seamlessly without requiring manual object creation boilerplate.
- **Type Casting:** Enables safe numerical promotions and conversions when interacting with mixed data types or APIs.

## 3. Prerequisites
- Basic Java syntax and execution model (`01_Java_Execution_Model.md`).

## 4. Core Concepts

### Variables and Data Types
- A variable is a named container for storing data values.
- **Primitive Types (8 total):**
  - Integer types: `byte`, `short`, `int`, `long`
  - Floating-point types: `float`, `double`
  - Character type: `char`
  - Boolean type: `boolean`
- **Reference Types:** Class instances, interfaces, arrays, and enums. Store memory references pointing to heap memory.

### Wrapper Classes & Autoboxing/Unboxing
- **Wrapper Classes:** Object equivalents for primitives (`Integer`, `Double`, `Boolean`, etc.). Cached for frequently used values (`-128` to `127`).
- **Autoboxing:** Automatic conversion of a primitive type into its corresponding wrapper object (e.g., `Integer num = 5;`).
- **Unboxing:** Automatic conversion of a wrapper object back into its primitive value (e.g., `int val = num;`).

### Type Casting
- **Widening Casting (Implicit):** Converting a smaller data type to a larger type (`int` $\rightarrow$ `long` $\rightarrow$ `double`). Safe, no data loss.

## 5. Syntax & Example
```java
public class TypesDemo {
    public static void main(String[] args) {
        int age = 30;                  // Primitive
        Integer boxedAge = age;        // Autoboxing (int -> Integer)
        int unboxedAge = boxedAge;     // Unboxing (Integer -> int)

        double salary = 75000.50;
        int roundedSalary = (int) salary; // Narrowing cast (double -> int) -> 75000

        System.out.println("Age: " + unboxedAge);
        System.out.println("Salary: " + roundedSalary);
    }
}
```

## 6. Real-Life Analogy
- **Primitive Type:** A physical coin in your pocket—fast and direct.
- **Reference Type:** A claim check or key to a safety deposit box pointing to where the actual object resides in the bank vault.

## 7. Real Backend Example
In a Spring Boot REST API processing user data:
- Database IDs or counts use boxed wrapper types (`Long userId`, `Integer retryCount`) in JPA entities to gracefully handle `null` values when database records are missing.
- Mathematical calculations or metrics aggregation use raw primitives (`long latency`, `double cpuUsage`) for high-performance processing without object allocation overhead.

## 8. Internal Working
- **Stack Memory:** Stores primitive variable values directly and reference variables (holding memory addresses pointing to the heap).
- **Heap Memory:** Stores actual object instances.
- **Integer Caching:** Java caches wrapper objects between `-128` and `127`. Comparing cached wrapper objects with `==` returns `true`, whereas comparing values outside this range compares memory addresses.

## 9. Important Rules & Common Mistakes
1. **`==` vs `.equals()` on Wrappers:** Never use `==` to compare wrapper object values; use `.equals()`.
2. **Null Pointer Exception during Unboxing:** Unboxing a `null` wrapper object throws a `NullPointerException`.

## 10. Interview Questions
- **Q1: What is the difference between primitive types and reference types in memory?**
  - Primitives store raw values directly on the stack. Reference types store a memory reference on the stack pointing to an object on the heap.
- **Q2: Why does `Integer.valueOf(100) == Integer.valueOf(100)` evaluate to `true`, but `Integer.valueOf(200) == Integer.valueOf(200)` evaluate to `false`?**
  - Java caches integer objects from `-128` to `127`. For `100`, references point to the same cached instance (`==` is true). For `200`, separate heap objects are created (`==` is false).

- **Narrowing Casting (Explicit):** Converting a larger data type to a smaller type (`double` $\rightarrow$ `int`). Requires explicit cast `(int)`.
