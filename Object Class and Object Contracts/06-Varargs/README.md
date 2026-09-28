# Varargs

## 1. Main Idea
**Varargs** (`Type... name`) lets a method accept **zero or more** arguments of one type.
It is pure compiler sugar: `void log(String fmt, Object... args)` is compiled as
`void log(String fmt, Object[] args)`, and every call site wraps its trailing arguments into a
**newly allocated array**. At most **one** varargs parameter is allowed per method and it must be
**last**. Varargs is the weakest overload: a call only falls back to a varargs method after the
compiler has failed to find a fixed-arity match (phase 3 of overload resolution, topic 05).
Because generics are non-reifiable, using a generic varargs parameter produces an
**unchecked warning** that must be silenced with `@SafeVarargs` — and only when the method truly
does not store anything into the array.

## 2. Why Does This Exist?
- **Convenience**: `String.format(fmt, a, b, c)`, `List.of(x)`, `List.of(x, y, z)` — one method
  instead of an overload ladder.
- **Backward compatibility**: `printf`/`format` and `List.of` could not have been added later
  without varargs, because both already have an `Object[]` form in the JDK.
- **Compatibility with arrays**: an existing `Object[]` argument can be passed directly to a
  varargs parameter *without* copying, which makes the transition from array parameters painless.
- **Readable call sites** for logging, builders and DSL-ish APIs.

The trade-off is real: varargs **allocates an array on every call** and can hide the intent of an
API that should have used an explicit collection.

## 3. Prerequisites
- Topic 04 Methods (parameter lists, `void`, signature).
- Topic 05 Method Overloading (phase 3, why varargs is last).
- Arrays and generics basics: array covariance, erasure, and *heap pollution*
  (`Java Fundamentals/02-Variables-Data-Types` covers arrays; erasure is covered where generics are).

## 4. Core Concepts

### Syntax And Desugaring
```java
// What you write
static int total(int... values) { ... }

// What the compiler actually sees (identical binary signature)
static int total(int[] values) { ... }
```
- `m(int... v)` and `m(int[] v)` have the **same signature** — they clash if both are declared.
- `int... v` and `int ...v` and `int v[]`-style spacing are equivalent; `int... v` is conventional.

### Call-Site Behaviour
```java
Printer p = new Printer();
p.log("a");                    // compiles to log("a", new String[0])
p.log("a", "b");               // compiles to log("a", new String[]{"b"})
p.log("a", new String[]{"b"}); // NO new array: the array is passed as-is
```
Consequences:
- Each varargs call allocates an array → measurable cost on hot paths.
- If the caller passes an **array**, the method may mutate the caller's array. If the method
  needs to store it, **defensive-copy** it first.
- Passing `null` is ambiguous: `p.log("a", (String[]) null)` is an explicit `null` array
  (`length` throws NPE when accessed), whereas `p.log("a", (String) null)` is a one-element
  array containing `null`.

### Overload Resolution With Varargs (phase 3)
```java
void log(String s)              // fixed arity
void log(String s, String... more)  // varargs, phase 3
log("x");            // fixed-arity wins
log("x", "y");       // varargs required here
```
And the trap from topic 05:
```java
void store(Object o)   void store(int... v)
store(1);              // -> store(Object): phase 2 (boxing) beats phase 3 (varargs)
```

### `@SafeVarargs`
A generic varargs parameter can cause **heap pollution**: the array's runtime type is not the
component type the compiler believes.
```java
@SafeVarargs                       // promise: this method does not store into 'list'
static <T> List<T> of(T... items) { return List.of(items); }
```
Rules:
- `@SafeVarargs` is allowed on `static`, `final` and (Java 9+) `private` methods and on constructors.
- Using a generic varargs parameter **without** it yields
  `warning: [unchecked] Possible heap pollution from parameterized vararg type`.
- Never annotate a method that publishes the array (or its contents) to a caller, because the
  caller's type assumption can be violated.

### Heap Pollution Demonstrated
```java
static <T> void unsafe(List<T>... lists) { }     // unchecked warning
static void bad() {
    List<String>[] strings = (List<String>[]) new List<?>[1];  // suppressed, unsafe
    // A caller could now place a List<Integer> in a List<String>[] variable,
    // and the failure appears later as ClassCastException far from the cause.
}
```
The safe rules: prefer `List<List<T>>` (a collection parameter) over a generic array, avoid
casting to a generic array type, and never assign to the varargs array inside the method.

### Varargs Cannot Be
| Attempt | Result |
|---|---|
| More than one varargs parameter | compile error |
| Varargs not in the last position | compile error |
| `m(int... a, int b)` | compile error |
| Overload only by `T...` element type | erasure clash for generics (`m(T...)` vs `m(String...)` is fine, but `m(List<String>...)` vs `m(List<Integer>...)` is not) |
| Making `@SafeVarargs` on a non-`static`/`final`/`private` instance method | compile error (before Java 9 it was restricted) |
<!-- c6-s4-end -->

## 5. Syntax
```java
public class Printer {

    // The varargs parameter must be last, and there can be only one.
    public void log(String format, Object... args) {
        System.out.println(format + " " + java.util.Arrays.toString(args));
    }

    // Zero extra arguments is allowed.
    public static int total(int... values) {
        int sum = 0;
        for (int v : values) sum += v;
        return sum;
    }

    // Generic varargs: needs @SafeVarargs (static/final/private, Java 9+ for private)
    @SafeVarargs
    public static <T> java.util.List<T> listOf(T... items) {
        return java.util.List.of(items);         // does NOT store the array: safe
    }

    // ILLEGAL: varargs is not last
    // public void bad(int... values, String name) { }

    // ILLEGAL: two varargs parameters
    // public void worse(int... a, int... b) { }
}
```

## 6. Simple Example
See `01-VarargsBasics/VarargsBasicsDemo.java` (zero/one/many arguments, array pass-through,
mutation visibility, the `null` ambiguity, and fixed-arity beating varargs) and
`02-HeapPollutionAndSafeVarargs/HeapPollutionDemo.java` (why `@SafeVarargs` exists, with a real
`ClassCastException` caused by heap pollution).

## 7. Real-Life Analogy
A varargs parameter is a **shopping bag at the till**. The cashier's procedure says "give me the
items" — you may hand over nothing, one item, or twenty, and the shop quietly wraps whatever you
hand over into one bag before the cashier sees it. If you already own a bag and hand *that* over,
the shop does **not** re-bag it — you have handed over your own bag, and anything the cashier does
to the bag happens to your bag. That is exactly the array pass-through behaviour, and it is why
methods defensively copy when they intend to keep the array.
<!-- c6-s7-end -->

## 8. Real Backend Example
Varargs is everywhere in backend Java. Knowing where it allocates and where it defers work matters.

```java
// 1. Structured logging: SLF4J defers toString() until the level is enabled.
//    The varargs array is still allocated, but the expensive formatting is skipped.
log.debug("transfer {} -> {} amount {}", fromId, toId, amount);

// 2. JDBC templates: SQL parameters are varargs.
jdbcTemplate.update(
        "update accounts set balance = balance - ? where id = ? and balance >= ?",
        amount, fromId, amount);

// 3. A helper that must NOT keep the caller's array.
static String joinPath(String base, String... segments) {
    StringBuilder sb = new StringBuilder(base);
    for (String segment : segments) {
        if (segment != null && !segment.isEmpty()) {
            sb.append('/').append(stripSlashes(segment));
        }
    }
    return sb.toString();
}

// 4. A helper that DOES keep the array: defensive copy required.
final class AuditRecord {
    private final String[] tags;

    @SafeVarargs                                  // constructor allowed since Java 9
    AuditRecord(String... tags) {
        this.tags = tags.clone();                 // defensive copy: the caller keeps its array
    }

    String[] tags() { return tags.clone(); }      // and protect on the way out too
}
```

Production lessons:
- **Logging is the common hot path.** `log.debug("...", a, b, c)` allocates an `Object[]` even when
  debug is disabled. Prefer `if (log.isDebugEnabled())` or the `Supplier`-based overloads when the
  arguments are expensive to build.
- **Never store the varargs array** without cloning, otherwise the caller can change your object's
  state after construction.
- **`List.of(...)` vs `List.of(array)`**: `List.of(array)` treats the array as *one element* if the
  array is not typed `E[]`; this is a well-known varargs gotcha
  (`List.of(new String[]{"a"})` gives a `List<String>` only because of the array's type; with
  `Object[]` you get `List<Object[]>`). Be explicit.

## 9. Internal Working
- Varargs is **only** a compile-time feature. The JVM has no concept of varargs.
```
Source                                   Class file signature
log(String fmt, Object... args)  --->    log(Ljava/lang/String;[Ljava/lang/Object;)V
total(int... values)             --->    total([I)I
```
- The **call site** emits the array creation:
  ```
  invokestatic  Printer.total:([I)I
  =  iconst_3 / newarray int / dup / ...astore...   <-- one array allocation per call
  ```
  Pass an existing array and no allocation happens — the reference is used directly.
- **Cost**: one array allocation per call; the JIT's escape analysis *may* eliminate it when the
  array does not escape (measure, do not assume).
- **Generic varargs** is non-reifiable, so the compiler accepts it but warns (verified text):
  ```
  warning: [unchecked] Possible heap pollution from parameterized vararg type List<T>
  ```
  `@SafeVarargs` is the promise that the method does not store anything into (or return) the array
  in a way that breaks the caller's type assumptions. Putting `@SafeVarargs` on an instance method
  that is neither `final` nor `private` is a compile error (verified):
  ```
  error: Invalid SafeVarargs annotation. Instance method store(List<String>...) is neither final nor private.
  ```
- **Heap pollution mechanism**: the varargs parameter of a generic method has the runtime type
  `Object[]`, so an assignment into it is not checked against `T`. The mistake only shows up later
  as a `ClassCastException` at an unrelated line — which is why it is called *pollution*.
- **Binary compatibility**: because the JVM signature is the array form, **adding varargs to an
  existing method is binary-compatible** with code that already passes an array.
<!-- c6-s9-end -->

## 10. Important Rules
1. At most **one** varargs parameter per method, and it must be **last**.
2. `m(T... x)` and `m(T[] x)` have the **same signature** — you cannot declare both.
3. Zero varargs arguments are valid; the parameter is then an **empty array** (never `null`).
4. The array is created at the **call site** — one allocation per call, unless the caller passes an
   existing array.
5. Passing an array does **not** copy it; a method that stores the array must `clone()` it.
6. A **generic** varargs parameter produces an unchecked warning unless annotated `@SafeVarargs`.
7. `@SafeVarargs` is only allowed on `static`, `final`, `private` methods and on constructors
   (`private` and constructors since Java 9).
8. Varargs is the **weakest** overload: fixed-arity and boxing matches win (topic 05).
9. Prefer an explicit `List<T>`/collection parameter for public APIs that genuinely take a variable
   number of elements and need immutability.

## 11. Common Mistakes
- **Putting varargs anywhere but last** → `error: varargs parameter must be the last parameter`.
- **Declaring two varargs parameters** → compile error.
- **Assuming `null` is passed as an empty array**:
  ```java
  void log(Object... args) { }
  log((Object[]) null);      // args == null  -> NullPointerException on args.length
  log();                     // args == new Object[0]
  ```
- **Storing the varargs array without a copy**:
  ```java
  class Tags {
      private final String[] tags;
      Tags(String... tags) { this.tags = tags; }   // caller can mutate your state afterwards
  }
  ```
- **Annotating a *polluting* method with `@SafeVarargs`** — the annotation is a promise, not a fix;
  it only suppresses the warning. If the method writes into or returns the array, the promise is a lie.
- **Non-`static`/`final`/`private` instance method with `@SafeVarargs`** — verified compile error:
  ```
  error: Invalid SafeVarargs annotation. Instance method store(List<String>...) is neither final nor private.
  ```
- **Forwarding a non-reifiable array to another varargs method** — verified warning:
  ```
  warning: [varargs] Varargs method could cause heap pollution from non-reifiable varargs parameter
  ```
- **`List.of(existingArray)`** — if the array's static type is not the component type, you get a list
  containing the array as a single element. Use `List.of(array[0], array[1], ...)` or
  `Arrays.asList(array)` deliberately.
- **Assuming varargs is free** on a hot path (every call allocates).
- **Using varargs where an overload ladder is clearer**: `f()`, `f(int)`, `f(int, int)` communicates
  "up to two" much better than `f(int...)`.

## 12. Comparison

| Aspect | `T... values` | `T[] values` |
|---|---|---|
| Call sites | `m()`, `m(a)`, `m(a,b)`, `m(array)` | `m(array)` only |
| Signature | same as `T[]` | — |
| Array created | at the call site (unless an array is passed) | by the caller |
| `null` handling | ambiguous (`(T[]) null` vs `(T) null`) | explicitly `null` |
| Generic form | unchecked warning unless `@SafeVarargs` | explicit array, no warning if constructed safely |
| Best for | Convenience APIs, formatting, logging | Genuine array semantics / performance-critical code |

| Aspect | Varargs (phase 3) | Fixed arity (phase 1/2) |
|---|---|---|
| Chosen when | No fixed-arity match exists | Any match exists |
| Overhead | One array allocation | None |
| Ambiguity risk | Low (last resort) | Higher (most-specific rules apply) |

| Aspect | `List<T>` parameter | `T... values` parameter |
|---|---|---|
| Call-site sugar | No (`List.of(...)` needed) | Yes |
| Immutability | Natural (`List.copyOf`) | Must clone manually |
| Overload clarity | Clear | Can be ambiguous with arrays |
| Recommendation | Public APIs with variable size | Convenience/formatting methods |

## 13. Code Examples
- **Beginner/Intermediate** — `01-VarargsBasics/VarargsBasicsDemo.java`: zero/one/many arguments,
  array pass-through and mutation visibility, defensive copying, `null` array vs `null` element, and
  fixed arity beating varargs.
- **Advanced** — `02-HeapPollutionAndSafeVarargs/HeapPollutionDemo.java`: a real heap pollution
  `ClassCastException`, the `@SafeVarargs` promise, and a warning-free safe generic implementation.
  Verified class-file descriptors: `total([I)I` and `log(Ljava/lang/String;[Ljava/lang/Object;)V`.
<!-- c6-s13-end -->

## 14. Practice Questions
1. How many varargs parameters may a method have, and where must it appear in the parameter list?
2. Do `m(int... v)` and `m(int[] v)` have the same signature? Can both be declared?
3. What is `args` inside the method when the caller writes `m()` and when the caller writes
   `m((int[]) null)`?
4. What is the difference between `log("x", (Object) null)` and `log("x", (Object[]) null)`?
5. Given `store(Object)` and `store(int...)`, which does `store(1)` call and why?
6. Why does `void total(int... values, String name)` not compile?
7. What does `@SafeVarargs` actually do — fix the code or suppress the warning?
8. Why can `@SafeVarargs` not be placed on a plain (non-`final`, non-`private`) instance method?
9. What happens if a method stores its varargs array in a field without copying?
10. What is the compiled descriptor of `void log(String s, Object... args)`?

### Answers
1. At most one, and it must be the **last** parameter.
2. Yes, identical signature (the JVM only knows `int[]`) — declaring both is a compile error.
3. `m()` → a **new empty array** (`length == 0`). `m((int[]) null)` → `null`, and any use of
   `values.length` throws `NullPointerException`.
4. `(Object) null` creates a one-element array `[null]`; `(Object[]) null` passes a `null` array
   reference, so `args` itself is `null`.
5. `store(Object)` — boxing is a phase 2 conversion, tried before phase 3 varargs.
6. Varargs must be the last parameter; `error: varargs parameter must be the last parameter`.
7. It **suppresses** the unchecked warning and documents a promise. It does not change behaviour and
   cannot fix real pollution.
8. Because a non-`final` instance method can be overridden, and the override could pollute the array;
   the annotation is only safe where the implementation is fixed (`static`, `final`, `private`) or in
   a constructor.
9. The caller retains the same array reference, so it can mutate the object's state after
   construction (and across threads). Always `clone()` in and out.
10. `(Ljava/lang/String;[Ljava/lang/Object;)V`.

## 15. Coding Practice
**Easy** — Write `static int max(int... values)` that throws `IllegalArgumentException` for zero
arguments, plus `static int sum(int... values)`. Call both with 0, 1 and 5 arguments.
**Medium** — Write `static String join(String delimiter, String... parts)` that skips `null` and empty
parts. Then write a `CsvRow` class whose constructor takes `String... cells`, defensively copies, and
exposes an unmodifiable `List<String>` view.
**Hard** — Write `@SafeVarargs static <T> Set<T> union(Set<T>... sets)`, proving it compiles with
`-Xlint:all` and no warnings. Then write a deliberately unsafe version that pollutes the array,
reproduce the `ClassCastException`, and rewrite it so the pollution is impossible (copying elements
into a new collection). Explain why the compiler cannot catch the original bug.
<!-- c6-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What are varargs?**
- *Testing*: basic definition.
- *Expected*: a parameter `Type... name` that accepts zero or more arguments of one type; compiler sugar for `Type[]`.
- *Wrong*: "It accepts any type of argument" (that would be `Object...`).

**Q2. How many varargs parameters can a method have, and where?**
- *Testing*: syntax rule.
- *Expected*: one, and it must be last.
- *Wrong*: "As many as you like."

**Q3. What is the type of the varargs parameter inside the method?**
- *Testing*: desugaring.
- *Expected*: an array (`int[]`, `Object[]`) — created at the call site.
- *Wrong*: "A `List`."

**Q4. Can you call a varargs method with no arguments?**
- *Testing*: edge case.
- *Expected*: Yes; the parameter is a new empty array.
- *Wrong*: "No, at least one is required."

**Q5. Give an example of a varargs method from the JDK.**
- *Testing*: practical familiarity.
- *Expected*: `String.format(String, Object...)`, `System.out.printf`, `List.of(E...)`, `SLF4J`'s logging methods.
- *Wrong*: `String.substring(int, int)`.

### Intermediate
**Q6. What is the difference between `m(int... v)` and `m(int[] v)`?**
- *Testing*: source vs binary view.
- *Expected*: identical signature and descriptor; only the call-site convenience differs.
- *Wrong*: "They are completely different methods."

**Q7. If a method takes `Object... args`, what does `args.length` do when the caller passes `(Object[]) null`?**
- *Testing*: the `null` array trap.
- *Expected*: `NullPointerException` — the parameter itself is `null`, not an empty array.
- *Wrong*: "Returns 0."

**Q8. `store(Object)` and `store(int...)` exist. What does `store(1)` call?**
- *Testing*: overload phases.
- *Expected*: `store(Object)` — phase 2 boxing beats phase 3 varargs.
- *Wrong*: "`store(int...)`, it matches more closely."

**Q9. Why does a varargs method that stores its array in a field need `clone()`?**
- *Testing*: aliasing/encapsulation.
- *Expected*: if the caller passed an array, it is the *same* array; the caller could mutate the object's state afterwards (and unsafely across threads).
- *Wrong*: "Because varargs makes a copy anyway."

**Q10. What does `@SafeVarargs` mean, and when can you use it?**
- *Testing*: generics warnings.
- *Expected*: a promise that the method does not pollute the array; allowed on `static`, `final`, `private` methods and constructors. It suppresses (documents) the unchecked warning — it does not fix bugs.
- *Wrong*: "It makes generic arrays legal."
<!-- c6-s16a-end -->

### Advanced
**Q11. What is heap pollution, and why is it called *pollution*?**
- *Testing*: generics + runtime type knowledge.
- *Expected*: a variable of a parameterised type referring to an object that is not of that type; for generic varargs the array's runtime type is `Object[]`, so unchecked writes can place wrong-typed elements in it, and the failure surfaces later as `ClassCastException` at an unrelated line.
- *Wrong*: "A memory leak."

**Q12. Why does `warning: [varargs] Varargs method could cause heap pollution from non-reifiable varargs parameter` appear even in a method annotated `@SafeVarargs`?**
- *Testing*: understanding the warning's trigger.
- *Expected*: Because the method forwards a non-reifiable array into *another* varargs method; the annotation covers *this* method's body, not the callee. Copy elements explicitly instead of forwarding.
- *Wrong*: "`@SafeVarargs` removes all warnings automatically."

**Q13. Why is adding varargs to an existing method binary compatible?**
- *Testing*: binary compatibility knowledge.
- *Expected*: the JVM descriptor is already the array form, so compiled callers passing an array still link; only recompiled callers gain the sugar.
- *Wrong*: "It breaks binary compatibility."

**Q14. Varargs and performance: what happens on a hot logging path, and how do you avoid it?**
- *Testing*: production profiling awareness.
- *Expected*: each call allocates an array (and may box); escape analysis may eliminate it but cannot be relied on. Use level guards (`if (log.isDebugEnabled())`), supplier-based overloads, or fixed-arity overloads for the hot cases.
- *Wrong*: "The JIT always removes it."

**Q15. When should a public API take `List<T>` instead of `T...`?**
- *Testing*: API design judgement.
- *Expected*: when elements are already a collection, when immutability matters, when the list may be very large (avoid array copies), when overload ambiguity with arrays/lambdas is a risk, or when a named parameter conveys meaning. Keep varargs for small, fixed-shape convenience methods.
- *Wrong*: "Always use varargs, it is shorter."

## 17. Production-Level Questions
1. **Hot logging:** debug logging is disabled in production but the application shows allocation pressure on hot paths. How can varargs contribute, and what is the fix while keeping the code readable?
2. **State corruption:** an immutable-looking `@Value` class takes `String... roles`, stores the array and returns it. Describe a sequence of calls that mutates the object after construction, and the two-line fix.
3. **Library evolution:** a widely used library changes `void send(Object[] args)` to `void send(Object... args)`. Is that source-compatible, binary-compatible, or both? What breaks in each case?
4. **Cache keys:** `cache.get(k1, k2...)` builds a key from a varargs array that is then used as a `HashMap` key. What bug appears, and how do you build a correct key?
5. **Code review:** a service has `process(Task...)` receiving thousands of tasks. What are the memory and GC implications, and how would you redesign the signature?
<!-- c6-s17-end -->

## 18. What I Should Remember
1. `Type... name` is compiler sugar for a `Type[] name` parameter — the JVM has no varargs.
2. One varargs parameter, last position only; `m(T...)` and `m(T[])` share a signature.
3. Zero arguments → a new empty array; `(T[]) null` → the parameter itself is `null`.
4. The array is created at the **call site** (one allocation per call) and is **not copied** when you
   pass an array — clone before storing.
5. Varargs is the weakest overload: fixed arity and boxing win (topic 05 phases).
6. Generic varargs needs `@SafeVarargs` on `static`/`final`/`private` methods or constructors, and the
   annotation is a promise, not a fix.
7. Verified descriptors: `([I)I`, `(Ljava/lang/String;[Ljava/lang/Object;)V`.
8. Prefer `List<T>` when the API genuinely handles a variable-size collection.

## 19. Connection To Other Java Topics
```
Topic 05 Method Overloading (phases; varargs is phase 3)
        ↓
Topic 06 Varargs                      <-- you are here
        ↓
Topic 07 Pass-by-Value (arrays/references are passed by value)
        ↓
Collections (List.of, Arrays.asList are varargs-based)
        ↓
Generics & Type Erasure (non-reifiable types, heap pollution)
        ↓
Streams & Optional (Supplier-based varargs-like APIs avoid eager work)
```
Varargs is the first place you meet **type erasure's practical consequences** — and those
consequences return in full when you study generics and collections.

## 20. One-Minute Revision
- `T...` = `T[]` with automatic wrapping at the call site; last parameter, only one.
- The call site allocates the array; passing an array means no copy → clone before storing.
- Zero args = empty array; `(T[]) null` = a `null` array.
- Overload phases: fixed arity and boxing beat varargs.
- Generic varargs warns; `@SafeVarargs` is a promise limited to `static`/`final`/`private`/constructor.
- Heap pollution = unchecked write into a non-reifiable array, failing later as `ClassCastException`.




