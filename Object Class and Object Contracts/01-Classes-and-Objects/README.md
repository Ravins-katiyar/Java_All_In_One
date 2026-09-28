# Classes and Objects

## 1. Main Idea
A **class** is a compile-time blueprint: it declares what fields (state) and methods
(behaviour) every instance will have. An **object** is a runtime instance of that
blueprint, allocated on the heap by `new`, and reached through a **reference** that
lives on the stack (or inside another object). The class exists once in the JVM's
method area as a `Class` object; the instances are created on demand. Two objects of the
same class have the same shape but independent state. In one line: *a class is the
cookie cutter, an object is a cookie, the reference is the note telling you where the
cookie is.*

## 2. Why Does This Exist?
Procedural code groups data and the functions that operate on it separately, so any
function can corrupt any variable. A class bundles state and behaviour so that state can
be protected (topic 10: `private`), reused (many objects from one definition), and
specialised (topic 03: inheritance). Without objects you cannot model real backend
concepts like `Order`, `User`, or `BankAccount` as single units of code.

## 3. Prerequisites
- Module 01 Heap/Stack/Method Area (`Java Fundamentals/01-Java-Execution-Model`).
- Primitives vs reference types and `==` behaviour (`02-Variables-Data-Types`).
- Nothing else. This is the first topic of the module.

## 4. Core Concepts

### Class vs Object
- **Class** = declaration (`class BankAccount { ... }`). One per classloader in the JVM.
- **Object (instance)** = `new BankAccount("A-101", 500)`. Each has its own copies of instance fields.
- **Reference variable** = the `BankAccount myAccount` part. Holds an address, not the object.

### Instance Fields vs Local Variables
| | Instance field | Local variable |
|---|---|---|
| Stored in | Heap, inside the object | Stack, inside the frame |
| Lifetime | Until the object is garbage collected | Until the method returns |
| Default value | Yes (`0`, `false`, `null`) | No — must be assigned before use |
| Shared with other objects? | No (unless `static`, topic 09) | No |

### Reference Semantics (The Part That Bites People)
- `accountA = accountB;` copies the **reference**, so both names point to one object.
- `accountA == accountB` compares references (addresses), not content.
- `accountA.equals(accountB)` compares content — but only if you override `equals()` (topic 11).

### Object Lifecycle
```
class file  --ClassLoader-->  Class object in Method Area
                                        |
   new BankAccount(...)  -->  memory reserved on Heap
                                        |
                          fields set to default values (0 / null)
                                        |
                          constructor runs (topic 02)
                                        |
                     reference returned and stored on Stack
                                        |
                     no more reachable references  -->  GC eligible
```

## 5. Syntax
```java
class BankAccount {                 // the class / blueprint
    private String accountNumber;   // instance field (per object)
    private long balance;           // instance field (per object)

    BankAccount(String accountNumber, long balance) {   // constructor (topic 02)
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    void deposit(long amount) {     // instance method (topic 04)
        this.balance += amount;
    }

    long getBalance() { return balance; }
}

public class Demo {
    public static void main(String[] args) {
        BankAccount a = new BankAccount("A-101", 500);  // object #1
        BankAccount b = new BankAccount("B-202", 900);  // object #2
        a.deposit(250);                                  // affects only a
        System.out.println(a.getBalance() + " / " + b.getBalance()); // 750 / 900
    }
}
```
<!-- section-5-end -->

## 6. Simple Example
See `01-ClassAndObject/ClassAndObjectDemo.java` — creates two accounts, shows that
mutating one does not affect the other, then shows reference aliasing with `==` and the
default `.equals()` behaviour.

## 7. Real-Life Analogy
A class is an **architect's floor plan**; an object is an **actual house built from it**.
One plan can produce hundreds of houses. Two houses may be identical on paper but contain
different furniture (state). Writing the plan's address on paper is a **reference** — the
paper is not the house, and tearing up the paper does not demolish it (only the garbage
collector does, and only when nobody holds any reference).

## 8. Real Backend Example
A payment service defines one `PaymentTransaction` class and creates millions of objects
per day. Each object is the in-memory form of a DB row, not a shared record:

```java
public class PaymentTransaction {
    private final String txnId;
    private final String accountId;
    private final BigDecimal amount;
    private PaymentStatus status;   // mutable on purpose

    public PaymentTransaction(String txnId, String accountId, BigDecimal amount) {
        this.txnId = txnId;
        this.accountId = accountId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void markSettled() { this.status = PaymentStatus.SETTLED; }
    public boolean isSettled() { return status == PaymentStatus.SETTLED; }
}
```
Why this matters in production: `PaymentTransaction` objects end up as keys/values in
`HashMap`/`HashSet` and in Spring caches, so `equals`/`hashCode` design (topic 11) is a
**correctness** issue. And because one request thread creates the object and another
thread reads it, field `final`ness is what makes publication safe without locks.

## 9. Internal Working
1. `new BankAccount("A-101", 500)` compiles to `new` + `dup` + `invokespecial` bytecode.
2. The JVM computes the object's size from class metadata in the Method Area.
3. Heap memory is allocated and **zeroed** — that is where default values come from.
4. The object header is written (mark word + class pointer used by `getClass()`).
5. Initialisers and the constructor run on that memory (order: topic 02).
6. `invokespecial` leaves the reference on the operand stack; it is stored into the
   stack frame's local variable slot.

```
Stack frame (main)          Heap
+------------------+        +--------------------------------+
| a -> 0x7f3a1000  |------> | BankAccount (header)           |
+------------------+        | accountNumber = "A-101"        |
                            | balance       = 750            |
                            +--------------------------------+
```
<!-- section-9-end -->

## 10. Important Rules
1. Instance fields get default values; local variables do not.
2. `new` never returns `null` — it returns a valid reference or throws `OutOfMemoryError`.
3. `==` compares references; `.equals()` compares content (only when overridden — topic 11).
4. Assigning a reference does **not** copy the object. Use a copy constructor or `clone()` for a copy.
5. A file may contain at most one `public` top-level class, and the file name must match it.
6. An object becomes GC-eligible when no live reference chain reaches it; `null` assignment helps only that one variable.

## 11. Common Mistakes
- **Assuming assignment copies**:
  ```java
  BankAccount a = new BankAccount("A-101", 500);
  BankAccount b = a;        // same object, not a copy
  b.deposit(100);
  System.out.println(a.getBalance()); // 600
  ```
- **Comparing objects with `==`**:
  ```java
  String x = "abc";
  String y = new String("abc");
  System.out.println(x == y);        // false (two different objects)
  System.out.println(x.equals(y));   // true  (same content)
  ```
- **Multiple public classes in one file** → `class B is public, should be declared in a file named B.java`.
- **Using an uninitialised local reference** → compile error `variable x might not have been initialized`.
- **Naming a class with a lowercase letter** (`class bankAccount`) — legal, but breaks Java conventions (classes are `PascalCase`).
- **Believing scope controls GC** — a local going out of scope is not what frees memory; unreachability is.

## 12. Comparison

| Aspect | Class | Object |
|---|---|---|
| Exists at | Compile time (+ one runtime `Class` object) | Runtime only |
| Memory | Method Area (metadata) | Heap |
| Created by | `class` declaration | `new` |
| How many | One per classloader | Unlimited |
| Inspect with | — | `obj.getClass()` |

| Aspect | Instance field | `static` field (topic 09) |
|---|---|---|
| Copies | One per object | One per classloader |
| Accessed via | `obj.field` | `ClassName.field` |
| Good for | Object state | Config / counters / caches |

| Aspect | `==` on objects | `.equals()` on objects |
|---|---|---|
| Compares | Reference (address) | Logical content |
| Override? | No | Yes |
| Default behaviour | Identity | Identity (`Object.equals`) |

## 13. Code Examples
- **Beginner** — `01-ClassAndObject/ClassAndObjectDemo.java`: two objects, independent
  state, aliasing, `==` vs `.equals()` present but unoverridden.
- **Intermediate** — `02-ReferenceVsObject/ReferenceVsObjectDemo.java`: mutating a passed
  object works, re-pointing a passed reference does not (sets up topic 07).
- **Advanced** — topic 11 `EqualsHashCodeContractDemo.java`: what happens when these
  objects are used as `HashMap` keys.
<!-- section-13-end -->

## 14. Practice Questions
1. How many objects are created by `BankAccount a = new BankAccount(...); BankAccount b = a;`?
2. What does `a == b` print for the two variables in Q1, and does it depend on the field values?
3. If `b = a` and then `a.deposit(100); b.deposit(50);`, what is `a.getBalance()`?
4. What happens when another class tries `System.out.println(account.balance)` while `balance` is `private`?
5. Why do instance fields have defaults while local variables do not?
6. Which of these live in the Method Area: instance fields, method bytecode, `static` fields, object headers?
7. `Demo.java` contains `class A {}` and `public class B {}`. Why does compilation fail, and what are the two fixes?
8. An object's only reference is set to `null`. Is the memory freed immediately? Explain.
9. Is a class a runtime entity or a compile-time entity? What *is* a runtime entity here?
10. You need two `Order` objects with the same `orderId` but independent mutable `status`. Is that one object or two? Why?

### Answers
1. **One object**, two references.
2. `true` — `==` compares references; both point to the same object. Field values are irrelevant.
3. `a.getBalance()` grew by 150 (both deposits hit the same object).
4. Compile error — `balance` is not visible outside the class (topic 10).
5. Heap memory is zeroed on allocation, so a defined default exists for fields; a stack frame is not zeroed for you, so the compiler forces explicit assignment for locals.
6. Method bytecode and `static` fields. (Instance fields live in each heap object; object headers live in the heap object itself.)
7. Only one top-level type may be `public` per file, matched by file name. Fixes: move `A` to `A.java`, or make `A` non-public.
8. No. It becomes *eligible* for GC; collection happens later at the collector's discretion.
9. The source-level class is compile-time; the runtime entity is the object (and the `Class` object representing the class).
10. Two objects — per-object state must be independent.

## 15. Coding Practice
**Easy** — Model a `Point` with `x`, `y`, a constructor, `move(dx, dy)` and `distanceTo(Point other)`. Create two points and print the distance.
**Medium** — Model `Wallet` holding a `List<Long> transactions`. Add `add(long)`, `min()`, `max()`, `average()`. Prove that two wallets never share the list.
**Hard** — Write `static Wallet deepCopy(Wallet src)`. Prove (with your own assertions) that mutating the copy leaves the original unchanged, then explain why a shallow `List` copy is dangerous when both objects are handed to different threads.
<!-- section-15-end -->

## 16. Interview Questions

### Beginner
**Q1. Difference between a class and an object?**
- *Testing*: core OOP vocabulary.
- *Expected*: class = blueprint/type; object = runtime instance on the heap with its own state.
- *Wrong*: "A class is a file, an object is a variable."

**Q2. What does `new` do (step by step)?**
- *Testing*: whether you know allocation is separate from construction.
- *Expected*: allocate heap memory → zero it (defaults) → set header → run initialisers + constructor → return the reference.
- *Wrong*: "It calls the constructor."

**Q3. Where do objects live and where do references live?**
- *Testing*: memory model.
- *Expected*: objects on the heap; references are values inside stack frames, other objects, or static fields.
- *Wrong*: "Objects are on the stack."

**Q4. Are instance fields automatically initialised?**
- *Testing*: default values.
- *Expected*: yes — `0`, `0.0`, `false`, `'\u0000'`, `null`. Locals are not.
- *Wrong*: "No, the constructor always sets them."

**Q5. When is an object garbage collected?**
- *Testing*: reachability.
- *Expected*: when no live thread can reach it via any reference chain; timing is the GC's choice.
- *Wrong*: "When it goes out of scope" / "when you call `System.gc()`".

### Intermediate
**Q6. Why does this print `600`?**
```java
BankAccount a = new BankAccount("A-101", 500);
BankAccount b = a;
b.deposit(100);
System.out.println(a.getBalance());
```
- *Testing*: reference aliasing.
- *Expected*: `b` holds the same reference as `a`; there is only one object.
- *Wrong*: "Each variable keeps its own copy."

**Q7. How do you copy an object, and why is `clone()` tricky?**
- *Testing*: shallow vs deep copy (leads into topic 11).
- *Expected*: implement `Cloneable`, override `clone()`; it is shallow, so nested mutable objects are shared. A copy constructor is usually clearer.
- *Wrong*: "`a = b` copies it."

**Q8. Two objects have identical field values. Is `a.equals(b)` true?**
- *Testing*: default `Object.equals` semantics.
- *Expected*: only if the class overrides `equals`; otherwise it is reference equality.
- *Wrong*: "Yes, because the fields match."

**Q9. Where is method bytecode stored and where are local variables stored?**
- *Testing*: Method Area vs Stack.
- *Expected*: bytecode and class metadata in the Method Area; locals in per-thread stack frames.
- *Wrong*: "Both in the heap."

**Q10. Why can't one `.java` file declare two `public` top-level classes?**
- *Testing*: compilation units.
- *Expected*: the file name must match the single public type; use separate files or nested classes.
- *Wrong*: "Java allows it."
<!-- section-16a-end -->

### Advanced
**Q11. Can an object exist with no reference variable pointing at it?**
- *Testing*: anonymous objects and reachability.
- *Expected*: yes — `new Service().run()` or `list.add(new Object())` creates an object reachable via another structure; "anonymous" does not mean "unreachable".
- *Wrong*: "Then it is deleted immediately."

**Q12. Does `getClass()` use the declared type or the real type?**
- *Testing*: object headers / polymorphism.
- *Expected*: it reads the class pointer in the object header, so it returns the real runtime class; it is `final` and cannot be overridden.
- *Wrong*: "It returns the declared type."

**Q13. A `static` method takes an object parameter and mutates its fields. Is that still OOP?**
- *Testing*: design judgement.
- *Expected*: legal but moves behaviour out of the object and weakens encapsulation; fine for utilities, poor for domain logic.
- *Wrong*: "`static` methods cannot touch instance fields" (they cannot touch them *directly*; they can via a reference).

**Q14. Two objects reference each other but nothing else references them. Leak?**
- *Testing*: tracing GC vs reference counting.
- *Expected*: no leak — a tracing collector marks from GC roots, so the isolated cycle is garbage.
- *Wrong*: "Cycles cause memory leaks in Java."

**Q15. Why might adding an `int` field not increase an object's size?**
- *Testing*: layout/padding awareness vs cargo-culting.
- *Expected*: object sizes are padded (commonly to 8-byte boundaries); a 4-byte field may fit in existing padding. Verify with `jol`/measurement rather than assuming.
- *Wrong*: "Every field adds exactly 4 bytes."

## 17. Production-Level Questions
1. **Cache identity bug:** you key a cache by `Wallet` objects. Two wallets with the same `walletId` produce two cache entries. Which design decision caused it, and which topic fixes it?
2. **Allocation pressure:** a hot endpoint creates 50 000 short-lived `PaymentTransaction` objects per request. Which JVM mechanism handles them, and what would you measure before optimising (allocation rate, GC pauses)?
3. **Visibility:** a `PaymentTransaction` is created on a request thread and read by a settlement thread. Which fields must be `final`, and what does `final` guarantee under the Java Memory Model?
4. **Classic leak:** a `static Map<String, Wallet>` grows without bound in a Spring singleton. Why is this the archetypal leak shape, and how do you bound it?
5. **Over-abstraction:** a team wraps every `String` in its own class. Where is the line between a useful value object and needless allocation?

## 18. What I Should Remember
1. Class = blueprint (Method Area); object = instance (Heap); reference = address value (Stack).
2. `new` allocates → zeroes → initialises → returns a reference.
3. Instance fields have defaults; locals do not.
4. Assignment copies the reference, never the object.
5. `==` = same object; `.equals()` = same content *only if overridden*.
6. GC is about reachability — never scope, never `System.gc()`.
7. One `public` top-level class per file, named after the file.

## 19. Connection To Other Java Topics
```
Java Fundamentals 01-03 (Stack/Heap, references, ==)
        ↓
Classes and Objects            <-- you are here
        ↓
Constructors (02)  →  this/super (03)  →  Inheritance  →  Polymorphism
        ↓
Methods (04) → Overloading (05) / Varargs (06) / Pass-by-Value (07)
        ↓
equals + hashCode contract (11)  →  HashMap / HashSet internals
```
This topic is *why* `HashMap` internals are learnable later: a `HashMap` is buckets of
references that rely entirely on `hashCode()` and `equals()`.

## 20. One-Minute Revision
- Class = cookie cutter, object = cookie, reference = where the cookie is.
- Object header holds the class pointer used by `getClass()`.
- `new` = allocate → zero (defaults) → initialise → return reference.
- Assignment aliases; `==` compares addresses; `.equals()` compares content when overridden.
- Fields default, locals do not. GC = unreachable, not out of scope.

