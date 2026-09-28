# static and final

## 1. Main Idea
`static` means **"belongs to the class, not to an instance"**. A `static` field exists once per
*classloader*, a `static` method has no `this`, a `static` nested class has no outer instance, and a
`static` initialiser block runs once when the class is first used. `final` means **"assigned at most
once"** (for a variable), **"cannot be overridden"** (for a method), or **"cannot be extended"** (for a
class) — and for a **field**, `final` also gives a Java Memory Model guarantee of safe publication. The
two modifiers are independent and often combined (`static final` = a constant), but they solve different
problems: `static` is about **ownership/sharing**, `final` is about **stability**. Crucially, `final` on a
*reference* does not make the referenced object immutable — a `final List` still has a mutable list inside.

## 2. Why Does This Exist?
**`static` avoids per-instance duplication and expresses class-level concepts:**
- Shared constants and configuration (`static final int MAX_RETRIES = 3`).
- Stateless utility methods (`Math.max`, `Collections.sort`) — no object needed.
- Factory methods and singletons (`Integer.valueOf`, `getInstance`).
- One-time initialisation (loading a driver, building a lookup table).
- Grouping a nested type that needs no outer instance (so no hidden outer reference is retained).

**`final` creates guarantees the compiler and JVM can enforce:**
- **Correctness**: values that must never change (an ID, a currency, a tenant code).
- **Safety**: immutable objects made of `final` fields can be shared across threads with no
  synchronisation, and `final` fields are **safely published** under the JMM.
- **Design intent**: preventing subclassing of value/security classes (`String`, `Integer`) and preventing
  an override that would break an invariant.
- **Optimisation**: `final` fields and methods give the JIT stable assumptions (inlining, folding).

## 3. Prerequisites
- Topic 01 Classes and Objects (Method Area, instance fields, `Class` object).
- Topic 02 Constructors (`<clinit>` vs `<init>`, initialisation order).
- Topic 03 `this` and `super` (why `this` cannot exist in a static context).
- Topic 07 Pass-by-Value (`final` reference ≠ immutable object).
- `Java Fundamentals/02-Variables-Data-Types`: wrapper caching, `String` immutability.
<!-- c9-s3-end -->

## 4. Core Concepts

### The Five Uses Of `static`
| Use | Syntax | Notes |
|---|---|---|
| Static field | `static int counter;` | One copy per classloader, shared by all instances |
| Static method | `static int add(int a, int b)` | No `this`; **hidden**, not overridden; `invokestatic` |
| Static nested class | `static class Config { }` | No outer instance, no hidden `this$0` reference |
| Static initialiser | `static { ... }` | Compiled into `<clinit>`; runs once |
| Static import | `import static java.lang.Math.PI;` | Removes the class qualifier; use sparingly |

### Class Initialisation (`<clinit>`)
- Static field initialisers and every `static { }` block are compiled into one method: **`<clinit>`**.
- It runs **once per classloader**, the first time the class is actively used (instance creation, static
  field/method access, reflection, or subclass initialisation).
- Order: superclass `<clinit>` first, then this class's static initialisers and `static` blocks **in source
  order**.
- Failure inside `<clinit>` throws `ExceptionInInitializerError` and leaves the class unusable.
- Because it is per classloader, the same class loaded twice has **two independent sets** of statics — the
  classic application-server duplicate-static trap.

### Static Context Restrictions
Inside a `static` method or block you cannot use `this`/`super`, cannot access instance fields or methods
**directly**, and cannot rely on an enclosing instance. The other direction is always fine: instance code
can access statics freely.

### Hiding vs Overriding
```java
class Parent { static void describe() { System.out.println("Parent"); } }
class Child extends Parent { static void describe() { System.out.println("Child"); } }

Parent p = new Child();
p.describe();          // prints "Parent"  <-- static methods are HIDDEN, chosen at compile time
```
Static methods are chosen by the **static type** (exactly like overload resolution, topic 05) and are not
polymorphic. Narrowing the access modifier while hiding is a compile error, and `@Override` on a static
method is an error too.

### `static final` And Constant Inlining
```java
static final int    MAX_RETRIES = 3;                    // compile-time constant
static final String SERVICE     = "payments";           // compile-time constant
static final List<String> MODES = List.of("A", "B");    // NOT a constant: a runtime value
```
- A `static final` field of a **primitive or `String`** initialised with a **constant expression** is a
  *compile-time constant* (`ConstantValue` attribute), and `javac` **inlines its value at every use site**.
- Consequence: changing such a constant requires recompiling **all dependent classes**, or old copies keep
  the old value — a real deployment bug.

### The Six Uses Of `final`
| Applied to | Meaning |
|---|---|
| Local variable / parameter | Assigned once; cannot be rebound |
| Instance field | Assigned exactly once — in an initialiser or in **every** constructor |
| Static field | Same, at class-initialisation time |
| Method | Cannot be overridden (may still be overloaded) |
| Class | Cannot be extended (methods are effectively final) |
| Captured local (Java 8+) | Must be `final` or **effectively final** for a lambda/inner class |

### `final` Reference vs Immutable Object
```java
final List<String> modes = new ArrayList<>();
modes.add("A");                 // LEGAL: the reference is final, the list is not immutable
// modes = new ArrayList<>();   // compile error: cannot assign a final variable
```

### `final` Fields And Safe Publication (JMM)
A `final` field is guaranteed to be visible with its **correctly constructed value** to any thread that
obtains the reference, provided `this` does not escape during construction. That is why immutable objects
built from `final` fields need no synchronisation — a foundation for the concurrency topics later.

### `final` And Frameworks
- Hibernate/JPA cannot proxy `final` classes or lazily load `final` fields; entities are normally non-final
  with an accessible (often `protected`) no-arg constructor.
- Spring injects `final` dependencies through **constructor injection**, the recommended style (topic 02).
- Interface fields are implicitly `public static final`; `record` components are `private final`.
<!-- c9-s4-end -->

## 5. Syntax
```java
public final class Money {                       // 1. final class: cannot be extended

    public static final String DEFAULT_CURRENCY = "EUR";   // 2. static final constant
    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(Money.class.getName());

    private static int instancesCreated;          // 3. static field: one per classloader

    static {                                      // 4. static initialiser -> <clinit>
        LOG.info("Money class initialised");
    }

    private final long minorUnits;                // 5. final instance field
    private final String currency;

    public Money(long minorUnits, String currency) {
        this.minorUnits = minorUnits;             // blank final assigned in EVERY constructor
        this.currency = currency == null ? DEFAULT_CURRENCY : currency;
        instancesCreated++;
    }

    public Money(long minorUnits) {               // must also assign every final field
        this(minorUnits, DEFAULT_CURRENCY);
    }

    public long minorUnits()   { return minorUnits; }
    public String currency()   { return currency; }

    public static int instancesCreated() {        // 6. static method: no 'this'
        return instancesCreated;
    }

    public static Money zero() {                  // 7. static factory method
        return new Money(0L);
    }

    public final boolean isZero() {               // 8. final method: cannot be overridden
        return minorUnits == 0L;
    }

    public static final class Builder {           // 9. static nested class
        private long minorUnits;
        private String currency = DEFAULT_CURRENCY;

        public Builder minorUnits(long value) { this.minorUnits = value; return this; }
        public Builder currency(String value) { this.currency = value; return this; }
        public Money build()                  { return new Money(minorUnits, currency); }
    }
}
```
Rules visible in that single class: `final` class, `static final` constants, a static counter, a static
initialiser, blank `final` fields assigned in every constructor, a static factory, a `final` method and a
static nested class.

## 6. Simple Example
See `01-StaticDemo/StaticDemo.java` (shared static counter, `<clinit>` ordering, static methods with no
`this`, hiding vs overriding, static nested class, and `static final` constants) and
`02-FinalDemo/FinalDemo.java` (blank finals, `final` reference to a mutable list, `final` method/class,
effectively final in lambdas, and immutability) plus `03-SharedStateDanger/SharedStateDangerDemo.java`,
which shows why mutable `static` state is a production hazard.

## 7. Real-Life Analogy
`static` is the **company whiteboard in the lobby**: everyone who works here sees the same text, and
writing on it changes it for all of them (one copy per office — i.e. per classloader). `final` is a
**laminated sign**: you cannot remove it or replace it. But note the difference between laminating the
*sign* and laminating the *thing the sign points to*: a laminated sign pointing at a whiteboard still lets
people write on the whiteboard. That is `final List<String> list` — the reference is laminated, the list
is not.
<!-- c9-s7-end -->

## 8. Real Backend Example
```java
// 1. The single most common use of static final: a per-class logger.
private static final Logger log = LoggerFactory.getLogger(TransferService.class);

// 2. Utility class: static methods + a private constructor so nobody can instantiate it (topic 02).
public final class AccountIds {
    private AccountIds() { }
    public static String normalize(String raw) { return raw == null ? null : raw.trim().toUpperCase(); }
}

// 3. Eager singleton: one instance per classloader, guaranteed by 'static final' (class init is thread-safe).
public final class RateLimiter {
    private static final RateLimiter INSTANCE = new RateLimiter();   // <clinit> is synchronised by the JVM
    private RateLimiter() { }
    public static RateLimiter getInstance() { return INSTANCE; }
}

// 4. Constant inlining trap across services:
//    "lib-common" publishes  static final String TOPIC = "payments.v1";
//    service A compiled against v1 has "payments.v1" INLINED. Upgrading the library to
//    "payments.v2" without recompiling service A changes nothing at runtime.

// 5. Immutable DTO: final fields + no setters -> safe to share between threads.
public record TransferReceipt(String txnId, String fromId, String toId, long minorUnits) { }

// 6. The production hazard: mutable static state in a Spring singleton.
@Service
public class NotificationService {
    // WRONG: shared across ALL requests and threads, grows without bound, and is not thread-safe.
    private static final Map<String, Long> LAST_SENT = new HashMap<>();

    public void notify(String accountId) {
        LAST_SENT.put(accountId, System.currentTimeMillis());
    }
}
```

Production lessons:
- **A Spring `@Service` is a singleton**, so any mutable instance *or* static field is shared by every
  concurrent request. Prefer method-local state, `final` dependencies, or a properly concurrent
  collection/`@Cacheable` with bounded size.
- **`static final` mutable collections are still mutable** — `static final Map` protects the reference,
  not the map. Return `Map.copyOf(...)` or use a concurrent implementation.
- **Constants belong in the right place**: a "constant interface" (`interface Codes { int OK = 1; }`) is a
  known anti-pattern; use `enum`, `record` or a `final class` with a private constructor.
- **Do not add `final` to JPA entity classes or their lazy fields** — Hibernate needs to subclass entities
  for proxies.

## 9. Internal Working
- **Static fields live with the class metadata** (Metaspace/`Class` object area), not on the heap with
  instances. All instances of the class see the same slots.
- **Load once per classloader**, not once per JVM. Two classloaders = two sets of statics. This is why
  "global counters" behave differently inside an application server or a plugin container.
- **`<clinit>` generation**: static initialisers and `static { }` blocks are merged into one synthetic
  method `<clinit>`; the JVM guarantees it completes before any other use of the class, and its execution is
  effectively serialised per class, so a `static final` singleton is safely published by JLS rules.
- **`ConstantValue` attribute** (verified behaviour): `static final` primitives/`String` become true
  compile-time constants and `javac` replaces uses with `ldc`/`bipush`-style literals — hence the
  recompilation trap.
```
javap -c -p               observed for static and final members:
static void log()   ->    invokestatic      #6   // Method log:()V
final void stop()   ->    invokevirtual     #9   // Method stop:()V   <-- STILL virtual
```
Note the second line: a `final` **method** is still invoked with `invokevirtual`; `final` only removes the
possibility of an override, which lets the JIT **devirtualise** and inline the call. It is not
`invokespecial` (that is reserved for `super.m()`, `private` methods and constructors).
- **`final` fields**: the reference may be assigned only inside an initialiser or constructor; the JVM
  emits a **freeze action** at the end of construction, guaranteeing that other threads which obtain the
  reference see the final fields fully initialised (JLS 17.5).
- **`final` class / method** effects at runtime are promises to the optimiser: no subclass can override, so
  the JIT can inline and remove virtual dispatch (devirtualisation), and sealed/`final` hierarchies help
  escape analysis.
- **Verification**: because a static call is resolved at compile time (`invokestatic`), hiding a static
  method in a subclass cannot change an already-compiled call site.
<!-- c9-s9-end -->

## 10. Important Rules
1. `static` members belong to the **class**, not an instance: no `this`, no `super`, no direct instance access.
2. Static state is **one copy per classloader** — not one per JVM. Statics are effectively global mutable
   state and must be treated as a concurrency hazard.
3. A static initialiser (`<clinit>`) runs **once**, superclass first, then in source order, on first active use.
4. **Static methods are hidden, not overridden.** The call is resolved from the static type at compile time.
5. `static final` on a **primitive/`String` constant** is inlined at compile time — changing it requires
   recompiling all dependants.
6. `final` on a variable means "assigned at most once"; on an instance field it must be assigned in an
   initialiser or in **every** constructor.
7. `final` on a **reference** does not make the object immutable.
8. `final` on a method prevents overriding; on a class it prevents extension.
9. A lambda/inner class may capture only **final or effectively final** locals.
10. `final` fields are **safely published** under the JMM — the basis of immutable-object thread safety.

## 11. Common Mistakes
- **Mutable `static` state in a singleton service** — shared by every request and every thread:
  ```java
  private static final Map<String, Long> LAST_SENT = new HashMap<>();   // unbounded + not thread-safe
  ```
- **Assuming `count++` on a static field is atomic** — verified: 8 threads × 100 000 increments lost
  ~600 000 updates in a run on this machine (read-modify-write with no coordination).
- **`static final` mistaken for immutability**:
  ```java
  static final List<String> MODES = new ArrayList<>();
  MODES.add("EXPRESS");        // legal — 'final' froze the reference, not the list
  ```
- **Calling a static method through an instance** — verified compiler lint warning:
  ```
  warning: [static] static method should be qualified by type name, Shape, instead of by an expression
  ```
- **Trying to `@Override` a static method** → compile error; static methods are hidden, not overridden.
- **Expecting a subclass's static method to be called through a parent reference** — it is not; the
  static type decides.
- **`this` inside a `static` method** → `non-static variable this cannot be referenced from a static context`.
- **Forgetting to assign a blank `final` field in one constructor** → `variable x might not have been initialized`.
- **Reassigning a captured local after using it in a lambda** → `local variables referenced from a lambda expression must be final or effectively final`.
- **Constant inlining across libraries**: updating `public static final String TOPIC = "payments.v2"` in a
  shared library does nothing for a service that is not recompiled.
- **Putting `final` on a JPA entity or its lazy fields** — Hibernate proxies subclasses and needs to
  intercept lazy loading.
- **Static import overuse** (`import static java.util.stream.Collectors.*;`) hiding where a method comes from.
- **Assuming a static field is unique JVM-wide** — two classloaders give two copies (classic cause of
  "the singleton ran twice").
- **Using statics as a substitute for dependency injection** — untestable, hidden coupling, and it breaks
  under parallel tests.

## 12. Comparison

| Aspect | `static` member | Instance member |
|---|---|---|
| Storage | Class metadata (one per classloader) | Each object on the heap |
| Access to `this` | No | Yes |
| Overriding | Hidden, not overridden | Overridden (virtual dispatch) |
| Bytecode | `invokestatic` / `getstatic` | `invokevirtual` / `invokeinterface` / `getfield` |
| Typical use | Constants, utilities, factories, singletons | Per-object state and behaviour |

| Aspect | `final` variable | `final` reference to a mutable object | Fully immutable object |
|---|---|---|---|
| Reassign the variable | No | No | No |
| Change object contents | n/a | **Yes** | No |
| Thread-safe sharing | Yes (value) | **No** | Yes |
| Example | `final int x = 1;` | `final List<String> l = new ArrayList<>();` | `record Money(long minor, String cur) {}` |

| Aspect | Compile-time constant (`static final` primitive/`String`) | Runtime constant |
|---|---|---|
| Inlined by `javac` | Yes (`ConstantValue`) | No |
| Changing it requires recompiling callers | Yes | No |
| Example | `static final int MAX = 3;` | `static final List<String> MODES = List.of(...);` |

| Aspect | `static` nested class | Inner (non-static) class |
|---|---|---|
| Outer instance | Not required | Required (hidden `this$0`) |
| Memory retention risk | None | Can retain the outer object |
| Created with | `new Outer.Nested()` | `outer.new Inner()` |

## 13. Code Examples
- **Beginner/Intermediate** — `01-StaticDemo/StaticDemo.java`: shared static counter, verified `<clinit>`
  order (`static-block-1, field-initialiser, static-block-2`), static methods without `this`, hiding vs
  overriding, static nested class, and `static final` constants.
- **Beginner/Intermediate** — `02-FinalDemo/FinalDemo.java`: final locals, blank finals assigned per
  constructor, a final reference to a mutable list, a final method and a final class, effectively final
  capture in a lambda, and an immutable value class.
- **Advanced** — `03-SharedStateDanger/SharedStateDangerDemo.java`: measured lost updates with a plain
  static `int` (versus correct `synchronized` and `AtomicInteger` versions) and a demonstration that a
  `static final` list is still mutable.
<!-- c9-s13-end -->

## 14. Practice Questions
1. How many copies of a `static` field exist in a JVM? What changes that answer?
2. When exactly does a static initialiser run, and in what order relative to field initialisers?
3. Why can't a `static` method use `this`?
4. What does `Parent p = new Child(); p.staticMethod();` print when both classes declare `staticMethod()`? Why?
5. What is the difference between hiding and overriding?
6. Is `static final List<String> MODES = List.of("A")` immutable? Can you add to it? Can you reassign it?
7. What is the output?
   ```java
   final List<String> l = new ArrayList<>();
   l.add("x");
   System.out.println(l);
   ```
8. Which of these are compile errors, and why?
   ```java
   final class A { }
   class B extends A { }
   class C { final void m() { } }
   class D extends C { @Override void m() { } }
   ```
9. Why must a local variable be final or effectively final to be captured by a lambda?
10. A library changes `public static final int TIMEOUT = 30;` to `60`. Your service already compiled against it. What value does your service see without recompilation?

### Answers
1. **One per classloader.** Loading the same class with two classloaders gives two independent copies.
2. Once, on first active use of the class; superclass `<clinit>` first, then this class's static field
   initialisers and `static` blocks in source order.
3. Because a static method has no instance, so there is no object to refer to; slot 0 is not a `this`.
4. Prints the **parent's** version. Static methods are resolved from the static type at compile time
   (`invokestatic`); there is no dynamic dispatch.
5. Hiding replaces a static method for *new* call sites compiled against the subclass; overriding replaces
   an instance method for *all* calls via virtual dispatch on the runtime type.
6. The reference is final but the list is immutable **because `List.of` returns an immutable list** — not
   because of `final`. Adding throws `UnsupportedOperationException`; reassigning is a compile error. With
   `new ArrayList<>()` instead, adding would succeed.
7. Prints `[x]` — the reference is final, the list contents are not.
8. `B extends A` → error (`A` is final). `D.m()` → error (`C.m()` is final, cannot be overridden).
9. Because the lambda captures a *copy* of the local; allowing reassignment afterwards would make the
   captured value ambiguous, so the compiler requires stability.
10. The **old value (30)**, because `static final` primitives/`String` constants are inlined at compile
    time. Recompilation is required. (If the field were not a compile-time constant, e.g. a `List` or a
    value from a method call, the new value would be read at runtime.)

## 15. Coding Practice
**Easy** — Write a `MathUtil` class with `static final double PI`, `static int add(int, int)` and a
`static` counter that counts how many times `add` was called. Then create two objects and show that the
counter is shared.
**Medium** — Write `Config` with `static` fields for host/port initialised in a `static` block that reads
`System.getProperty(...)` with defaults, plus `static final` limits. Then add a `static` block that fails
deliberately (e.g. `Integer.parseInt("abc")`) and observe `ExceptionInInitializerError`.
**Hard** — Write an immutable `final class Iban` with a `final` field, validation in the constructor,
`equals`/`hashCode` (see topic 11), and a `static` factory `of(String)`. Then create a "broken" variant
with a non-final field, prove that shared instances can be mutated from multiple threads, and fix it.
Finally, demonstrate the constant-inlining trap by compiling two classes against a `static final int`,
recompiling only the class that defines the constant with a new value, and observing the stale value.
<!-- c9-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What does the `static` keyword mean?**
- *Testing*: core definition.
- *Expected*: the member belongs to the class rather than an instance — one copy per classloader, accessible without an object.
- *Wrong*: "It means the value cannot change."

**Q2. What does the `final` keyword mean?**
- *Testing*: the four distinct meanings.
- *Expected*: variable → assigned once; field → assigned once (in an initialiser or every constructor); method → cannot be overridden; class → cannot be extended.
- *Wrong*: "It always means immutable."

**Q3. Can a `static` method access instance variables?**
- *Testing*: static context rules.
- *Expected*: Not directly — there is no `this`. It can if an instance is passed as a parameter.
- *Wrong*: "Yes, Java resolves them automatically."

**Q4. What is a static initialiser block?**
- *Testing*: `<clinit>` awareness.
- *Expected*: a `static { }` block compiled into `<clinit>`, executed exactly once when the class is initialised.
- *Wrong*: "It runs before every constructor call."

**Q5. Is `static final` the same as immutable?**
- *Testing*: the classic misconception.
- *Expected*: No. `final` freezes the reference; the object can still be mutable (`static final List`).
- *Wrong*: "Yes, `static final` fields cannot change."

### Intermediate
**Q6. Why does `Parent p = new Child(); p.staticMethod();` call the parent's method?**
- *Testing*: hiding vs overriding.
- *Expected*: static methods are not virtual; the compiler resolves them from the static type (`invokestatic`).
- *Wrong*: "Because static methods are private."

**Q7. When is a class initialised, and what happens if `<clinit>` throws?**
- *Testing*: JVM lifecycle knowledge.
- *Expected*: on first active use (instance creation, static access, reflection, subclass init); a throw becomes `ExceptionInInitializerError` and the class becomes unusable.
- *Wrong*: "At JVM start-up for every class."

**Q8. Are static variables shared between all instances in the JVM?**
- *Testing*: classloader awareness.
- *Expected*: One copy **per classloader**, so not necessarily JVM-wide — which is why the same class can appear twice in an application server.
- *Wrong*: "Yes, exactly one per JVM."

**Q9. What is the difference between a `final` field and an effectively `final` local?**
- *Testing*: precision.
- *Expected*: `final` is declared and enforced; effectively final is inferred because the variable is never reassigned (needed for lambda/inner-class capture).
- *Wrong*: "They are unrelated concepts."

**Q10. Why is `private static final Map<...> CACHE = new HashMap<>()` a problem in a Spring service?**
- *Testing*: production awareness.
- *Expected*: a singleton bean is shared by all requests/threads, so the map is unsynchronised shared mutable state and can grow unbounded — use a bounded, concurrent cache (`@Cacheable`, Caffeine, `ConcurrentHashMap` with eviction).
- *Wrong*: "It is fine because it is `final`."
<!-- c9-s16a-end -->

### Advanced
**Q11. Explain the JMM guarantee for `final` fields and why immutability is thread-safe.**
- *Testing*: concurrency depth.
- *Expected*: a freeze action at the end of construction ensures any thread that reads the reference after construction sees the final fields correctly initialised (JLS 17.5), provided `this` did not escape. An object with all-final fields and no escaping publishes safely without synchronisation.
- *Wrong*: "`final` fields are volatile."

**Q12. Why is a `final` method still invoked with `invokevirtual`, and how does `final` help performance?**
- *Testing*: bytecode/JIT knowledge.
- *Expected*: verified: `final` methods compile to `invokevirtual`; `final` only removes the possibility of an override, which allows the JIT to devirtualise and inline, enabling further optimisations. `invokespecial` is reserved for `super.m()`, `private` methods and constructors.
- *Wrong*: "`final` methods are called with `invokespecial`."

**Q13. What is the constant-inlining trap with `static final` primitives/`String`?**
- *Testing*: real deployment experience.
- *Expected*: such fields become compile-time constants (`ConstantValue`) and are inlined into every caller; changing the value without recompiling callers leaves stale behaviour. Multi-service deployments are where this bites (shared config library).
- *Wrong*: "All fields are read at runtime."

**Q14. Static state and unit tests — what goes wrong?**
- *Testing*: testability awareness.
- *Expected*: static state persists between tests and between test classes, so tests become order-dependent and cannot run in parallel; static caches/singletons leak state and hide dependencies. Prefer injected instances, and reset state in `@BeforeEach` when unavoidable.
- *Wrong*: "Statics make tests faster, so they are good."

**Q15. Why can't a class be both `final` and `abstract`, and why can't a `final` class be proxied by Hibernate/Spring CGLIB?**
- *Testing*: combination reasoning.
- *Expected*: `final` forbids extension while `abstract` requires extension — mutually exclusive. Proxies work by subclassing (CGLIB) or by interface implementation (JDK proxies), so a `final` class cannot be CGLIB-proxied; use interface-based proxying or remove `final`.
- *Wrong*: "`final abstract` is allowed and means it cannot be instantiated."

## 17. Production-Level Questions
1. **Cache leak:** a `private static final Map` cache in a Spring singleton is the source of a gradual
   OutOfMemoryError. Explain the mechanism and give three fixes with their trade-offs.
2. **Lost updates:** a metrics counter implemented as `private static long count;` is incremented from a
   thread pool and periodically reads low. Which of the three fixes (`synchronized`, `AtomicLong`,
   `LongAdder`) would you choose for high write contention, and why?
3. **Deployment bug:** a config library changes a `public static final int` and the platform team rebuilds
   only the library. Services report the old value. What is the root cause and how would you design the
   constant so this cannot happen?
4. **Testing:** a service uses `static` singletons with mutable state; tests pass individually but fail in
   the suite. What is happening, and how do you refactor without a full rewrite?
5. **Framework friction:** an architect marks all domain classes `final` for immutability, and JPA stops
   lazily loading. How do you reconcile strict immutability with ORM requirements in the same codebase?
<!-- c9-s17-end -->

## 18. What I Should Remember
1. `static` = class-level: one copy **per classloader**, no `this`, hidden not overridden, `<clinit>` once.
2. `final` variable/field = assign once; `final` method = no override; `final` class = no extension.
3. `final` on a reference ≠ immutable object; `static final` on a primitive/`String` is **inlined** by javac.
4. `final` fields give JMM safe publication (freeze action) — the foundation of immutable thread safety.
5. Mutable `static`/`final`-referenced state is shared global mutable state: unsynchronised counters lose
   updates (measured), and unbounded static caches leak.
6. Static methods are resolved at compile time, so hiding is not polymorphism.
7. Frameworks interact with these modifiers: JPA needs non-`final` classes and fields; Spring prefers
   `final` constructor-injected dependencies.

## 19. Connection To Other Java Topics
```
Topic 01 Classes and Objects (Method Area)  →  where static fields live
Topic 02 Constructors (<clinit> vs <init>, blank finals)
Topic 07 Pass-by-Value (final reference ≠ immutable object)
        ↓
Topic 09 static and final                 <-- you are here
        ↓
Topic 10 Access Modifiers (visibility of all of the above)
Topic 11 Object contract (equals/hashCode/hashCode pairs on immutable value objects)
        ↓
Immutability → safe publication → Java Memory Model → threads and locks
        ↓
Enums (static final instances) → constants done right
```
The `static final` counter example is the smallest possible illustration of why the concurrency module
exists: shared mutable state without coordination produces incorrect results.

## 20. One-Minute Revision
- `static` = one per classloader; no `this`; hidden not overridden; `<clinit>` runs once.
- `final` = assign once / cannot override / cannot extend; a `final` reference is not an immutable object.
- `static final` primitives/`String` are compile-time constants → inlined → recompile dependants.
- `final` fields are safely published; all-final fields make a class immutable and thread-safe.
- Shared mutable static state = lost updates and leaks; use `AtomicLong`/`LongAdder` or bounded concurrent caches.
- JPA: non-final classes/fields; Spring: `final` constructor-injected dependencies.





