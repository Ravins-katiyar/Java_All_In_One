# Methods

## 1. Main Idea
A **method** is a named block of reusable behaviour attached to a class. Its identity is its
**signature**: the method name plus the **types of its parameters**. The return type is *not*
part of the signature, which is why you cannot overload on return type alone. A method
declaration consists of modifiers, an optional type-parameter list, a return type, a name, a
parameter list, an optional `throws` clause, and a body. When you call a method, the JVM
creates a **stack frame**, copies argument **values** into parameter variables, executes the
bytecode, and returns a value (or `void`). Methods are where Java's polymorphism actually
happens: one call site can run different code depending on the *runtime* type of the receiver.

## 2. Why Does This Exist?
Without methods you would duplicate logic everywhere it is needed, and every bug fix would
have to be applied in many places. Methods give you:
- **Reuse** — write once, call anywhere.
- **Abstraction** — callers see *what*, not *how* (`transfer(...)`, not 40 lines of SQL).
- **Testability** — a unit test calls one method with known inputs and checks the output.
- **Polymorphism** — a base reference or interface can dispatch to the right implementation.
- **Encapsulation** — variables declared inside a method are invisible outside it.

## 3. Prerequisites
- Topic 01 Classes and Objects (instance vs static, references).
- Topic 02 Constructors (a constructor is method-like but is *not* a method).
- Topic 03 `this` and `super` (`this.method()`, `super.method()`).
- Overloading appears briefly here but is fully covered in **topic 05**.

## 4. Core Concepts

### Anatomy Of A Method
```java
public  static  <T>  int  max(T a, T b, Comparator<T> c)  throws IllegalArgumentException  { ... }
|       |       |    |    |   |                  |        |
mods    static  type ret  name params         throws     body
                params
```
| Part | Meaning | Notes |
|---|---|---|
| Modifiers | `public`, `private`, `static`, `final`, `synchronized`, `abstract`, `default`, `strictfp` | Visibility, dispatch and locking |
| Type parameters | `<T>` | Declared before the return type |
| Return type | Any type, or `void` | `void` = no value returned |
| Name | Verb-style camelCase | `calculateTotal`, not `CalculateTotal` |
| Parameter list | Zero or more declarations | `(String id, long amount)` |
| `throws` | Checked exceptions the caller must handle | Part of the API contract |
| Body | Statements in `{ }` | `abstract`/interface-abstract methods end with `;` |

### Signature vs Method Declaration
```java
void  send(String id, long amount)     // full declaration
      \__________________________/
      signature = send(String, long)
```
- **Signature** = name + parameter *types*. `send(String, long)` ≠ `send(long, String)`.
- **Not** in the signature: return type, `throws` clause, parameter names, modifiers,
  type-parameter names.
- Consequence 1: **return type alone cannot overload** a method → compile error.
- Consequence 2: `throws` alone cannot overload a method → compile error.
- Consequence 3: erasure means `m(List<String>)` and `m(List<Integer>)` clash (same erasure `List`).

### Parameters vs Arguments
- **Parameter** = the variable declared by the method (`long amount`).
- **Argument** = the value supplied at the call site (`transfer(id, 1500)`).
- Arguments are always **copied** into parameters — Java is pass-by-value (topic 07).

### Return Values and Control Flow
- `return expr;` must match the declared return type (or be assignable to it).
- `return;` is legal only in a `void` method.
- Every path of a non-`void` method must return a value or throw — the compiler enforces it.
- With `try/finally`, the return value is **computed before** `finally` runs (but the return
  statement itself completes after it) — a classic interview nuance.

### Covariant Return Types
An override may narrow the return type to a subtype (Java 5+):
```java
class Factory        { Number create() { return 1; } }
class IntegerFactory extends Factory {
    @Override Integer create() { return 1; }    // legal: Integer is a Number
}
```

### Static vs Instance Methods
- **Instance method**: has an implicit `this`; can read/write instance fields; dispatched
  virtually (`invokevirtual`), so it can be overridden.
- **Static method**: belongs to the class; no `this`; is **hidden**, not overridden; compiled to
  `invokestatic` and resolved at compile time. Calling it via an instance is legal but
  misleading and tooling flags it.

### `@Override`, Overriding vs Overloading
| | Overloading | Overriding |
|---|---|---|
| Where | Same class (or inherited, visible) | Subclass, same signature, compatible return |
| Chosen | **Compile time** (static resolution) | **Runtime** (dynamic dispatch) |
| Requirement | Different parameter list | Same name + same parameter types |
| `@Override` | Not applicable | Strongly recommended; the compiler verifies it |
| Covered in | Topic 05 | Inheritance/Polymorphism module |

### Method Chaining and Guard Clauses
- A method returning `this` (or another object) supports chaining — the basis of builders.
- **Guard clauses** validate first and return/throw immediately, keeping the happy path flat:
  ```java
  void transfer(String id, long amount) {
      if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
      if (amount <= 0)                throw new IllegalArgumentException("amount must be positive");
      // happy path, no nesting
  }
  ```
<!-- c4-s4-end -->

## 5. Syntax
```java
public class Calculator {

    /** Instance method: uses the receiver's state. */
    public int add(int a, int b) { return a + b; }

    /** void method: performs an action and returns nothing. */
    public void printSum(int a, int b) {
        System.out.println("sum = " + add(a, b));   // methods calling methods
    }

    /** throws clause: the caller must handle or declare it. */
    public int divide(int a, int b) throws ArithmeticException {
        if (b == 0) throw new ArithmeticException("/ by zero");
        return a / b;
    }

    /** Static (class-level) method: no 'this'. */
    public static boolean isEven(int value) { return value % 2 == 0; }

    /** Varargs method (topic 06): accepts zero or more ints. */
    public int sum(int... values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }

    /** Fluent method: returns the receiver so calls can be chained. */
    public Calculator log() { System.out.println("chained"); return this; }
}
```

## 6. Simple Example
See `01-MethodBasics/MethodBasicsDemo.java`: instance vs static methods, parameters vs
arguments, `void` vs value-returning methods, guard clauses, and method chaining.

## 7. Real-Life Analogy
A method is a **kitchen appliance with a labelled button**. The button says "Toast"; the
operator does not need to know about coils, timers or thermostats. The parameters are the
settings you dial in (bread type, browning level), the return value is the toast that comes
out, and the internals are hidden behind the panel. A **static** method is the appliance in
the break room that anyone can use without owning one; an **instance** method is the one in
your own kitchen that only works with your settings.
<!-- c4-s7-end -->

## 8. Real Backend Example
A service method is the unit of transaction, validation and testability:

```java
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final AuditPublisher audit;

    public TransferService(AccountRepository accounts, AuditPublisher audit) {
        this.accounts = Objects.requireNonNull(accounts);
        this.audit = Objects.requireNonNull(audit);
    }

    /** Public API surface: validates, then delegates to small private steps. */
    @Transactional
    public TransferReceipt transfer(String fromId, String toId, BigDecimal amount) {
        validate(fromId, toId, amount);                    // guard clauses
        Account from = load(fromId);                        // one responsibility
        Account to   = load(toId);
        debit(from, amount);
        credit(to, amount);
        TransferReceipt receipt = new TransferReceipt(fromId, toId, amount);
        audit.publish(receipt);                             // side effect at the end
        return receipt;                                     // value-returning method
    }

    private void validate(String fromId, String toId, BigDecimal amount) {
        if (fromId == null || fromId.isBlank()) throw new IllegalArgumentException("fromId required");
        if (toId == null || toId.isBlank())     throw new IllegalArgumentException("toId required");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be positive");
    }

    private Account load(String id) {
        return accounts.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));   // checked/unchecked contract
    }

    private void debit(Account account, BigDecimal amount) { account.withdraw(amount); }
    private void credit(Account account, BigDecimal amount) { account.deposit(amount); }

    /** Static helper: no instance state needed. */
    static boolean isSameAccount(String fromId, String toId) { return fromId.equals(toId); }
}
```

Three production lessons visible in this single class:
- **Small methods with one job** are what make unit tests possible (`validate` can be tested alone).
- **`@Transactional` works through a proxy**: if `transfer()` calls `this.debit()`, that inner call
  bypasses the proxy — the classic *self-invocation* trap. Transaction boundaries must be on the
  method *entered from outside the bean*.
- **`static` helpers** are fine for pure functions (`isSameAccount`) but should not touch state.

## 9. Internal Working
Each method call pushes a **new stack frame** onto the calling thread's JVM stack:

```
Thread stack (per thread)
+-------------------------------+
| main frame                    |  locals: args, calculator, ...
+-------------------------------+
| add(int a, int b) frame       |  locals: a, b   (argument VALUES copied in)
+-------------------------------+
| <- top of stack               |
```

- **Frame contents**: local variable table (slot 0 = `this` for instance methods), operand stack,
  and a reference to the runtime constant pool of its class.
- **Method bytecode** lives in the **Method Area**, not the frame; the frame holds only working data.
- **Invocation bytecodes**:
  | Bytecode | Used for |
  |---|---|
  | `invokevirtual` | normal instance methods (virtual dispatch, overridable) |
  | `invokeinterface` | methods called through an interface reference |
  | `invokespecial` | `super.m()`, `private` methods, constructors (`<init>`) |
  | `invokestatic` | `static` methods |
  | `invokedynamic` | lambdas, string concatenation, some dynamic languages |
- **Return bytecodes**: `ireturn`/`lreturn`/`freturn`/`dreturn`/`areturn` for values, plain
  `return` for `void`.
- **Depth limit**: each frame consumes stack; runaway recursion throws `StackOverflowError`
  (an `Error`, not an `Exception` — catch it only for diagnostics).
- **Optimisation**: the JIT may **inline** hot small methods, removing the call overhead and even
  eliminating the frame entirely. This is why micro-benchmarks must be warmed up.
<!-- c4-s9-end -->

## 10. Important Rules
1. A method's **signature** = name + parameter types. Return type, `throws`, parameter names and
   modifiers are **not** part of it.
2. You cannot overload by return type or by `throws` alone.
3. A non-`void` method must return a value or throw on **every** path — the compiler checks this.
4. `return;` (no value) is only valid in a `void` method.
5. Parameters are **local variables initialised with copies** of the arguments (topic 07).
6. `static` methods: no `this`, no access to instance fields directly, hidden (not overridden).
7. `@Override` is optional but should always be used — it turns a silent "new method instead of
   override" bug into a compile error.
8. An override may **narrow** the return type (covariant) but not widen it, and may narrow the
   `throws` list but not add checked exceptions.
9. Method names should be verbs (`calculateTotal`), and boolean-returning methods usually read as
   `is...`/`has...`.
10. Keep methods small and single-purpose; extract private helpers instead of nesting deeply.

## 11. Common Mistakes
- **Trying to overload on return type**:
  ```java
  void   send(String id) { }
  String send(String id) { return id; }   // compile error: same signature
  ```
- **Assuming `throws` is part of the signature**:
  ```java
  void go() { }
  void go() throws IOException { }        // compile error: same signature
  ```
- **Forgetting a return on one path**:
  ```java
  int classify(int n) { if (n > 0) return 1; }   // compile error: missing return statement
  ```
- **Mutating a parameter and expecting the caller to see it** (primitive case):
  ```java
  void addOne(int n)        { n++; }         // caller's value unchanged
  void addOne(int[] values) { values[0]++; } // caller SEES this (same array object)
  ```
- **Calling a `static` method through an instance** (`calculator.isEven(2)`) — legal but misleading;
  tooling warns.
- **`final` on a parameter misunderstanding**: `void m(final List<String> l)` prevents rebinding `l`
  but does **not** make the list immutable.
- **Making everything `public`** — every public method is an API commitment you must support.
- **Self-invocation breaking `@Transactional`/`@Cacheable`** — internal calls skip the proxy.
- **Big methods** — long parameter lists and deep nesting mean the method does too much.

## 12. Comparison

| Aspect | Method | Constructor |
|---|---|---|
| Name | Any identifier | Must equal the class name |
| Return type | Required (or `void`) | None |
| Inherited / overridden | Yes | No |
| Called by | Explicit call / dispatch | `new`, `this(...)`, `super(...)` |
| Can be `static` | Yes | No |

| Aspect | Overloading (topic 05) | Overriding |
|---|---|---|
| Resolved | Compile time (static) | Runtime (virtual dispatch) |
| Requires | Different parameter list | Same signature, compatible return |
| Across classes? | Can be inherited/visible overloads | Must be a subclass |
| `@Override` | Not used | Recommended |
| Bytecode | The call site is fixed | `invokevirtual`/`invokeinterface` |

| Aspect | Instance method | `static` method |
|---|---|---|
| `this` available | Yes | No |
| Access instance fields | Directly | Only through a passed reference |
| Dispatch | Virtual (`invokevirtual`), overridable | Static (`invokestatic`), hidden |
| When to use | Behaviour depending on object state | Pure functions, factories, utilities |

| Aspect | Return type `void` | Return a value |
|---|---|---|
| Meaning | Performs an action | Produces data |
| Testability | Needs an observable side effect to verify | Assert on the returned value |
| Prefer when | Genuine side effect (log, persist, publish) | A result exists (compute, lookup, transform) |

## 13. Code Examples
- **Beginner** — `01-MethodBasics/MethodBasicsDemo.java`: instance vs static methods, parameters
  vs arguments, `void`, guard clauses, methods calling methods, fluent chaining.
- **Intermediate** — `02-SignatureAndCovariance/CovariantReturnDemo.java`: covariant return types,
  parameter-order overloading, and the (illegal) return-type/`throws`-only overloads as comments.
- **Advanced** — `03-StackFrames/StackFrameDemo.java`: prints the live call stack via
  `Thread.currentThread().getStackTrace()` and measures the recursion depth at which the JVM
  throws `StackOverflowError`.
<!-- c4-s13-end -->

## 14. Practice Questions
1. What exactly is a method signature, and which of these are part of it: return type, `throws`, parameter names, parameter types?
2. Why does `void go() {}` plus `void go() throws IOException {}` fail to compile?
3. Predict: `int classify(int n) { if (n > 0) return 1; }` — compile error or not, and why?
4. What does this print?
   ```java
   static void bump(int n) { n++; }
   public static void main(String[] a) { int x = 5; bump(x); System.out.println(x); }
   ```
5. What does this print, and why is it different from Q4?
   ```java
   static void bump(int[] v) { v[0]++; }
   public static void main(String[] a) { int[] x = {5}; bump(x); System.out.println(x[0]); }
   ```
6. Are parameters and arguments the same thing?
7. Can a `static` method call an instance method of the same class directly? Why not?
8. What is a covariant return type, and is widening the return type allowed in an override?
9. What happens when a method recurses without a base case? Which JVM structure is exhausted?
10. Which bytecode instruction is used for a `static` method, and which for an overridable one?

### Answers
1. Name + parameter types only. Return type, `throws`, parameter names and modifiers are **not** part of it.
2. Same signature — only the `throws` clause differs, which is not part of the signature.
3. Compile error: `missing return statement` — not every path returns an `int`.
4. Prints `5`. The parameter is a copy of the argument; `n++` affects only the copy.
5. Prints `6`. The copy is a copy of the *reference*, so both point to the same `int[]`.
6. No: parameters are the declared variables; arguments are the values passed at the call site.
7. No — a `static` method has no `this`, so there is no object to invoke the instance method on.
8. An override may return a subtype (`Number` → `Integer`); widening is not allowed, and neither is
   changing to an unrelated type.
9. `StackOverflowError`; the thread's JVM stack (its frames) is exhausted.
10. `invokestatic` for static; `invokevirtual` for overridable instance methods (and
    `invokeinterface` through an interface reference).

## 15. Coding Practice
**Easy** — Write `MathUtils` with `static int max(int a, int b)`, `static boolean isEven(int n)` and
`static void printTable(int n)`. Call all three from `main`.
**Medium** — Write `TextProcessor` with guard clauses and single-purpose private helpers:
`normalize(String)`, `isBlank(String)`, `wordCount(String)`, `longestWord(String)`. Return values
instead of printing, so it can be unit-tested.
**Hard** — Write `naiveFibonacci(int n)` and `memoizedFibonacci(int n)` using a `long[]` cache. Measure
both with `System.nanoTime()` for `n = 35`, print the timings and the recursion depth reached
(pass a depth parameter). Then explain why the naive version is exponential and which JVM structure
would be exhausted for a very large `n`.
<!-- c4-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What is a method?**
- *Testing*: vocabulary.
- *Expected*: a named block of behaviour with a signature, optional parameters, a return type and a body, invoked on a class or an object.
- *Wrong*: "A function inside a class."

**Q2. What is a method signature?**
- *Testing*: precision that prevents real bugs.
- *Expected*: name + parameter types. Not the return type, `throws`, parameter names or modifiers.
- *Wrong*: "Name plus parameters plus return type."

**Q3. Difference between a parameter and an argument?**
- *Testing*: terminology.
- *Expected*: parameter = declared variable; argument = value passed at the call site.
- *Wrong*: "They are the same."

**Q4. What does `void` mean?**
- *Testing*: return semantics.
- *Expected*: the method returns no value; `return;` may be used to exit early.
- *Wrong*: "It returns `null`."

**Q5. Difference between a static and an instance method?**
- *Testing*: core Java.
- *Expected*: static belongs to the class, has no `this`, is hidden (not overridden), compiled to `invokestatic`; instance methods use the receiver's state and are dispatched virtually.
- *Wrong*: "Static methods are faster."

### Intermediate
**Q6. Can you overload a method by changing only the return type?**
- *Testing*: the signature rule.
- *Expected*: No — return type is not part of the signature; same signature means duplicate method.
- *Wrong*: "Yes, the compiler uses the assignment context."

**Q7. What is method overriding, and how does it differ from overloading?**
- *Testing*: the classic pairing.
- *Expected*: Overriding = subclass replaces inherited behaviour with the same signature, resolved at runtime; overloading = same name, different parameters, resolved at compile time.
- *Wrong*: "They are the same thing with different names."

**Q8. What does `@Override` do?**
- *Testing*: compiler tooling awareness.
- *Expected*: Nothing at runtime; it makes the compiler verify that a method actually overrides something, catching typos like `tostring()`.
- *Wrong*: "It is required to override."

**Q9. Why does `int classify(int n) { if (n > 0) return 1; }` not compile?**
- *Testing*: definite return analysis.
- *Expected*: A path exists with no return; the compiler requires every path either to return a value or throw.
- *Wrong*: "It compiles and returns 0."

**Q10. What happens to a method's return value if `finally` modifies a variable?**
- *Testing*: control-flow subtlety.
- *Expected*: For a normal `return expr;`, the value is evaluated before `finally` runs, so changing the variable inside `finally` does not change the returned value (a `return` inside `finally` does override it, and that is a bug).
- *Wrong*: "The modified value is returned."
<!-- c4-s16a-end -->

### Advanced
**Q11. Why is self-invocation (`this.method()`) a problem for `@Transactional` and `@Cacheable`?**
- *Testing*: real production experience.
- *Expected*: Those annotations work through a proxy; an internal call happens on the raw object, bypassing the proxy, so the advice never runs. Fixes: call through the injected bean, self-inject, or restructure the boundary.
- *Wrong*: "It works because `this` is the proxy."

**Q12. Which bytecode instruction executes a `static` method and why does that matter for overriding?**
- *Testing*: linking behaviour to JVM semantics.
- *Expected*: `invokestatic`, resolved at compile time — so `static` methods are *hidden*, not overridden. A call through a parent reference runs the parent's `static` method, even if the runtime object is a subclass.
- *Wrong*: "`invokevirtual`, so they are overridden."

**Q13. How can method inlining change measured performance, and what does that imply for benchmarking?**
- *Testing*: JIT awareness.
- *Expected*: The JIT inlines hot small methods, removing call overhead and enabling further optimisations (escape analysis, constant folding); benchmarks must warm up the JIT and use a harness (JMH) rather than `System.currentTimeMillis()` loops.
- *Wrong*: "Inlining does not happen in Java."

**Q14. What is the difference between `throws` on a method and `throw` inside it, and how do the rules differ for overrides?**
- *Testing*: exception contract precision.
- *Expected*: `throws` declares checked exceptions in the contract; `throw` actually raises one. An override may narrow or remove the checked exceptions but may not add broader ones; unchecked exceptions are unrestricted.
- *Wrong*: "An override can throw anything."

**Q15. Why can a lambda-based method reference be used where an interface method is expected, given methods are not values in Java?**
- *Testing*: connects methods to functional interfaces (leads to the Streams module).
- *Expected*: A functional interface with exactly one abstract method can be targeted by a lambda or method reference; the compiler uses `invokedynamic`/LambdaMetafactory to create an implementation, so the method becomes a value without boxing the logic.
- *Wrong*: "Java has first-class functions."

## 17. Production-Level Questions
1. **Transaction bug:** `transfer()` calls `this.debit()` which is annotated `@Transactional(REQUIRES_NEW)`. Nothing happens. Explain precisely why, and give two safe fixes.
2. **API surface:** a service class exposes 40 `public` methods because "someone might need them". What is the maintenance cost, and how would you shrink the surface without changing behaviour?
3. **Performance:** a hot method returns a large `List` that callers immediately copy. How do you redesign the method so that mutability is not shared, without paying an unnecessary copy on every call?
4. **Error handling:** a method returns `null` to signal "not found". What problems does that create at call sites, and what does the modern Java API (`Optional`, `orElseThrow`) change about them?
5. **Debugging:** a production log shows `StackOverflowError` with thousands of identical service frames. What kinds of code produce this pattern, and how would you detect it before release?
<!-- c4-s17-end -->

## 18. What I Should Remember
1. Signature = name + parameter types. Return type and `throws` are **not** part of it.
2. A non-`void` method must return on every path or throw.
3. Parameters are copies of arguments — mutating a primitive parameter never affects the caller.
4. Instance methods dispatch virtually; `static` methods are hidden and resolved at compile time.
5. Constructors are not methods: no return type, not inherited, not overridden.
6. Overriding happens at runtime; overloading happens at compile time (topic 05).
7. Every call pushes a frame; unbounded recursion → `StackOverflowError`.
8. Keep methods small, single-purpose, with guard clauses and meaningful return values.

## 19. Connection To Other Java Topics
```
Topic 01-03 (objects, constructors, this/super)
        ↓
Topic 04 Methods                      <-- you are here
        ↓
Topic 05 Method Overloading (compile-time selection)
Topic 06 Varargs  (the weakest overload, array in disguise)
Topic 07 Pass-by-Value (parameters receive copies)
Topic 08 Recursion (frames + base cases)
        ↓
Inheritance / Polymorphism → Interfaces → Generics → Lambdas & Streams
        ↓
Topic 11 Object contract (equals/hashCode are ordinary overridable methods)
```
The invocation bytecodes you met here (`invokevirtual`, `invokeinterface`, `invokestatic`,
`invokespecial`) are exactly what polymorphic interfaces and lambdas rely on later.

## 20. One-Minute Revision
- Signature = name + parameter types; return type/`throws` are not part of it.
- Every path must return or throw; `return;` only in `void`.
- Parameters are copies; mutating a primitive parameter never affects the caller.
- Instance method = virtual dispatch; `static` = compile-time resolution and no `this`.
- `void` = action, value = result; prefer returning a value when a result exists.
- A call pushes a frame; runaway recursion exhausts the stack.




