# Constructors

## 1. Main Idea
A **constructor** is a special block that runs immediately after an object's memory is
allocated, whose job is to put the object into a **valid initial state**. It has the same
name as the class, has no return type (not even `void`), and is invoked only by `new`
(or by `this(...)`/`super(...)` from another constructor). If you write no constructor,
the compiler inserts a **default constructor** with an empty body that calls `super()`.
Constructors are **not inherited** and **not overridden** — they are only *invoked* by a
subclass through `super(...)`. Constructors can be overloaded (topic 05) but cannot be
`static`, `final`, `abstract` or `synchronized`.

## 2. Why Does This Exist?
`new` gives you memory full of **default** values (`null`, `0`, `false`). A `BankAccount`
with `null` for `accountNumber` and `0` for balance is structurally valid but logically
broken — nothing stops `deposit()` from being called on it. Constructors exist to make it
impossible to create a half-built object: all required invariants are established before
any other method can touch the object.

## 3. Prerequisites
- Topic 01 Classes and Objects (heap allocation, instance fields, defaults).
- Topic 03 `this` and `super` is *used* here (`this(...)`, `super(...)`) — read the
  constructor parts of 03 side by side with this topic.

## 4. Core Concepts

### The Default Constructor (the one the compiler writes)
- Created **only if the class declares no constructor at all**.
- Its visibility matches the class visibility.
- It contains exactly one thing: an implicit `super();` call.
- Declaring *any* constructor removes it — including a private one.

```java
class A { }                       // compiler generates: A() { super(); }
class B { B(int x) { } }          // NO default constructor -> new B() will not compile
```

### Kinds of Constructor
| Kind | Example | Purpose |
|---|---|---|
| Default (implicit) | `A()` generated | Only when none is declared |
| No-arg (explicit) | `A() { super(); }` | Frameworks/JPA need it; explicit initialisation |
| Parameterised | `A(int x) { }` | Enforce required state |
| Overloaded | `A()`, `A(int x)`, `A(int x, String s)` | Convenience variants |
| Copy | `A(A other) { ... }` | Explicit copy (normal constructor, not `Cloneable`) |
| Private | `private A() { }` | Utility class / singleton / factory-only creation |
| `record` compact | `record P(int x) { P { } }` | Validate + normalise record components |

### Constructor Chaining
- `this(args...)` calls another constructor **of the same class**.
- `super(args...)` calls a constructor **of the direct superclass**.
- Either may appear **only as the first statement**, and you may use only one of them.
- If you write neither, the compiler inserts `super();` (the no-arg parent constructor).

### Initialisation Order (memorise this; it is a favourite interview question)
For `new Child()` where `Child extends Parent`:

```
1. Class loading / static init for Parent   (once per JVM, on first use)
2. Class loading / static init for Child    (once per JVM, on first use)
3. Parent: field default values (0 / null)
4. Parent: field initialisers + instance initialiser blocks, in source order
5. Parent: constructor body
6. Child : field default values (0 / null)
7. Child : field initialisers + instance initialiser blocks, in source order
8. Child : constructor body
```
Key consequence: **the parent is fully constructed before the child's fields are
initialised**, so calling an overridden method from a parent constructor can observe the
child's fields as `0`/`null` (see Common Mistakes).

### Why frameworks want a no-arg constructor
Reflection-based frameworks (JPA/Hibernate, older Spring XML, Jackson with default
deserialisation) create the object reflectively with
`Class.getDeclaredConstructor().newInstance()` and then set fields. That requires a
no-arg constructor (it may be `protected`/package-private for JPA, but usually not
`private`), and the class must not rely on constructor logic for validity.

## 5. Syntax
```java
public class BankAccount {

    private String accountNumber;      // required -> must come from a constructor
    private long balance;              // optional -> has a sensible default

    // 1) no-arg constructor: chains to the "full" constructor
    public BankAccount() {
        this("UNASSIGNED", 0L);        // this(...) must be the first statement
    }

    // 2) parameterised constructor: enforces the invariant
    public BankAccount(String accountNumber, long balance) {
        if (accountNumber == null || accountNumber.isBlank())
            throw new IllegalArgumentException("accountNumber required");
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    // 3) copy constructor (not Cloneable): an explicit, readable copy operation
    public BankAccount(BankAccount other) {
        this(other.accountNumber, other.balance);
    }

    // 4) private constructor: creation only through a factory
    private BankAccount(String accountNumber) {
        this.accountNumber = accountNumber;
        this.balance = 0L;
    }

    public static BankAccount forNewCustomer(String accountNumber) {
        return new BankAccount(accountNumber);   // can call the private constructor
    }
}
```

## 6. Simple Example
See `01-ConstructorTypes/ConstructorTypesDemo.java` for all constructor flavours, and
`02-InitializationOrder/InitializationOrderDemo.java` for the exact order of static
initialisers, instance initialisers, field initialisers and constructor bodies.

## 7. Real-Life Analogy
A constructor is a **car factory's pre-delivery checklist**. The chassis may be built
(the allocated, zeroed memory), but the car is not handed to the customer until the
engine is installed, fluids are filled and the odometer is reset. The customer can never
receive a car with no brakes — because the checklist runs *before* handover.
<!-- c2-s7-end -->

## 8. Real Backend Example
**JPA/Hibernate entity** — the framework needs a no-arg constructor because it instantiates
the class reflectively and then populates fields from the result set:

```java
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private String accountNumber;

    private long balance;

    /** Required by JPA. Never call this from business code. */
    protected AccountEntity() { }

    /** The only constructor business code should use. */
    public AccountEntity(String accountNumber, long openingBalance) {
        if (accountNumber == null || accountNumber.isBlank())
            throw new IllegalArgumentException("accountNumber required");
        this.accountNumber = accountNumber;
        this.balance = openingBalance;
    }
}
```

**Spring constructor injection** — preferred over field injection because it makes
dependencies `final` and guarantees the bean cannot exist half-initialised:

```java
@Service
public class TransferService {

    private final AccountRepository accounts;    // final: set exactly once
    private final AuditPublisher audit;

    public TransferService(AccountRepository accounts, AuditPublisher audit) {
        this.accounts = Objects.requireNonNull(accounts);   // fail fast at startup
        this.audit = Objects.requireNonNull(audit);
    }
}
```

**Records** validate and normalise in a compact constructor and get a canonical
constructor for free (Java 16+):

```java
public record Money(long minorUnits, String currency) {
    public Money {                              // compact constructor: no parameter list
        if (minorUnits < 0) throw new IllegalArgumentException("negative amount");
        currency = currency.toUpperCase();      // reassigning the parameter normalises the field
    }
}
```

**Builder** — use it when the constructor would need 10+ parameters (a known code smell):
`new Order.Builder().buyer(id).lines(lines).currency("EUR").build();`

## 9. Internal Working
- A constructor compiles into an instance method named **`<init>`** (the JVM permits `<` in
  names; you cannot write that name yourself). `javap -c` shows `AccountEntity.<init>`.
- `new` → `dup` → `invokespecial <init>` is the bytecode shape of `new AccountEntity(...)`.
- The child's `<init>` *starts* with `invokespecial <Parent>.<init>` — that is the injected
  or explicit `super()` call.
- Field initialisers and instance initialiser blocks are compiled **into** `<init>`, in
  source order, **after** the `super()` call and **before** your constructor statements.
- Static fields and static blocks go into a separate method, **`<clinit>`**, which runs once
  per classloader the first time the class is actively used.

```java
class Demo {
    static int S = initStatic();          // goes into <clinit>
    int x = initInstance();               // goes into <init>, before the ctor body
    Demo() {                              // the rest of <init>
        System.out.println("ctor body");
    }
}
```
```
javap -c -p Demo   ->   static {};  Demo();
                       inside Demo(): invokespecial java/lang/Object.<init>()
```
<!-- c2-s9-end -->

## 10. Important Rules
1. Name = class name; **no return type**. Writing `void Demo()` makes it a *method*, not a constructor.
2. A constructor cannot be `static`, `final`, `abstract`, `synchronized`, `native` or `default`.
3. Constructors are **not inherited**. A subclass reaches a parent constructor only via `super(...)`.
4. No constructor declared → compiler adds a default one. Any constructor declared → no default one.
5. A constructor not starting with `this(...)`/`super(...)` gets an implicit `super();`. If the
   parent has no accessible no-arg constructor, compilation fails.
6. `this(...)` and `super(...)` must be the **first statement**, and only one may be used.
7. `super()` completes **before** the child's field initialisers run.
8. A `private` constructor blocks external instantiation. To make a utility class truly
   non-instantiable *and* non-subclassable, add `final` (otherwise a subclass can still call a
   public/protected parent constructor).
9. Constructors should be cheap and side-effect free — no I/O, no queries, no `start()`ing threads.

## 11. Common Mistakes
- **Accidentally writing a method instead of a constructor**:
  ```java
  public void BankAccount() { }        // legal Java: a METHOD named BankAccount with void return
  BankAccount a = new BankAccount();    // still compiles, using the default constructor
  ```
- **Calling an overridable method from a constructor** — the classic trap:
  ```java
  class Parent {
      Parent() { print(); }              // executes BEFORE Child's fields are initialised
      void print() { System.out.println("parent"); }
  }
  class Child extends Parent {
      String name = "child";            // not set yet when Parent() runs
      @Override void print() { System.out.println("child:" + name); }  // prints child:null
  }
  ```
  Fix: never call overridable methods from a constructor — make them `private`, `final` or `static`.
- **Putting a statement before `this(...)`/`super(...)`** → compile error
  `call to super must be first statement in constructor`.
- **Accidentally deleting the default constructor** by adding a parameterised one; existing
  `new BankAccount()` call sites and reflective frameworks that need a no-arg constructor break.
- **Not validating constructor arguments** → objects that exist but are logically invalid.
- **Doing heavy work in a constructor** (opening connections, loading files) — constructor
  injection is preferable.

## 12. Comparison

| Aspect | Constructor | Method |
|---|---|---|
| Name | Must equal the class name | Any legal identifier |
| Return type | None | Required (possibly `void`) |
| Invoked by | `new`, `this(...)`, `super(...)`, reflection | Explicit call / virtual dispatch |
| Inherited / overridden | No | Yes |
| Allowed modifiers | Visibility only | Most modifiers |
| Polymorphic | No — resolved statically | Yes |

| Aspect | Constructor | Factory method (`static X of(...)`) |
|---|---|---|
| Name | Fixed = class name | Descriptive (`of`, `valueOf`, `forNewCustomer`) |
| Returns | Exactly the new object | Any subtype, or a **cached** instance |
| Subclass-friendly | Needs `super(...)` | Can choose the implementation |
| Use when | Required state is obvious | Caching, subtype choice, clearer naming |

| Aspect | `this(...)` | `super(...)` |
|---|---|---|
| Target | Another constructor of the **same** class | A constructor of the **direct parent** |
| Position | First statement | First statement |
| Effect | Continues initialising the **same** object | Initialises the **parent part** of the same object |
| Creates a new object? | No | No |

## 13. Code Examples
- **Beginner** — `01-ConstructorTypes/ConstructorTypesDemo.java`: default vs no-arg vs
  parameterised vs copy constructors, plus the `void BankAccount()` trap demonstrated safely.
- **Intermediate** — `02-InitializationOrder/InitializationOrderDemo.java`: the exact order of
  static init, instance init, field initialisers and parent-then-child constructor bodies.
- **Advanced** — `03-RecordConstructor/RecordConstructorDemo.java`: compact constructor
  validation/normalisation, canonical constructor, and `record` vs class equality (links to topic 11).
<!-- c2-s13-end -->

## 14. Practice Questions
1. Does `class A { }` have a constructor? Who wrote it, and what does it contain?
2. `class B { B(int x) {} }` — why does `new B()` fail to compile?
3. In `class C { C() { this(1); } C(int x) {} }`, which constructor runs first, and why does it matter?
4. Predict the output:
   ```java
   class P { P() { System.out.print("P "); } }
   class C extends P { C() { System.out.print("C "); } }
   new C();
   ```
5. What is wrong with `public void Account() { }` inside `class Account`?
6. Why does calling an overridable method from a parent constructor produce `null` values?
7. `class D { private D() {} }` — list two ways code outside `D` can still obtain a `D`.
8. Where do field initialisers live in bytecode, and where do static initialisers live?
9. A class has constructors `A(int)` and `A(String)`. Does it have a no-arg constructor? What breaks?
10. Why does `record Point(int x, int y)` not need you to write `equals`, `hashCode` or `toString`?

### Answers
1. Yes — the compiler-generated **default constructor**: accessible the same as the class, body is `super();`.
2. Declaring `B(int)` suppresses the default constructor, so no no-arg constructor exists.
3. `this(1)` runs the `C(int)` constructor first, then returns to finish `C()`. It matters because
   the "real" initialisation happens once, in one place (avoiding duplicated validation code).
4. `P C` — the parent constructor body always runs before the child's.
5. It is a **method** (it has a `void` return type), not a constructor. The class then still has a
   default constructor, so `new Account()` quietly compiles — confusing and easy to miss.
6. Because the parent constructor runs before the child's field initialisers. Overridden dispatch
   still reaches the child's method, which reads fields that are still `0`/`null`.
7. (a) A `static` factory/property inside `D`; (b) reflection
   (`getDeclaredConstructor().setAccessible(true)`), respecting module access rules.
8. Field initialisers go into `<init>` (after the `super(...)` call); static initialisers and
   static field assignments go into `<clinit>`.
9. No no-arg constructor. `new A()` fails and reflective frameworks requiring one fail.
10. For records the compiler generates a **canonical constructor** plus `equals`, `hashCode` and
    `toString` based on the components (`java.lang.Record` declares them `abstract`).

## 15. Coding Practice
**Easy** — Write `Rectangle(width, height)` that rejects non-positive values, plus a no-arg
constructor defaulting to `1 x 1`. Add `area()`.
**Medium** — Write `Order` with constructors `(String id)`, `(String id, List<String> lines)` and a
copy constructor. Make the list defensively copied in both directions so callers cannot mutate
internal state (that is constructor injection of immutability).
**Hard** — Take a class with a constructor of 12 parameters. Refactor it into a nested `Builder`.
Prove with a `main` method that the old call sites can be rewritten, and that `build()` throws when a
required field is missing. Then explain when a Builder is *not* worth it.
<!-- c2-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What is a constructor and what is it for?**
- *Testing*: basic vocabulary plus *intent*.
- *Expected*: a block named like the class, no return type, run right after allocation, whose purpose is to establish a valid initial state.
- *Wrong*: "It allocates the object."

**Q2. What is the default constructor?**
- *Testing*: whether you know the compiler generates it.
- *Expected*: inserted only when the class declares no constructor; same visibility as the class; body is `super();`.
- *Wrong*: "It sets fields to default values" (the JVM zeroing does that).

**Q3. Can a constructor return a value?**
- *Testing*: syntax precision.
- *Expected*: It has no return type at all — not even `void`. Adding a return type turns it into a method.
- *Wrong*: "It returns `void`."

**Q4. Are constructors inherited?**
- *Testing*: a common misconception.
- *Expected*: No. A subclass must call a parent constructor via `super(...)`.
- *Wrong*: "Yes, that is why `new Child()` works."

**Q5. Can a constructor be `private`? Why would you do that?**
- *Testing*: design patterns.
- *Expected*: Yes — for utility classes, singletons and factory-only creation.
- *Wrong*: "No, then the class cannot be instantiated at all."

### Intermediate
**Q6. Explain the exact order of initialisation for `new Child()` where `Child extends Parent`.**
- *Testing*: the most-asked constructor question.
- *Expected*: Parent static → Child static → Parent field defaults → Parent field initialisers + instance blocks → Parent ctor body → Child field defaults → Child field initialisers + instance blocks → Child ctor body.
- *Wrong*: "Child's constructor then Parent's."

**Q7. Why is `this(...)` restricted to the first statement?**
- *Testing*: understanding that an object is initialised exactly once.
- *Expected*: Because the delegating constructor performs the real initialisation; allowing it mid-body would leave the object partially initialised and allow conflicting initialisations.
- *Wrong*: "It is just a compiler limitation."

**Q8. What is the output, and why?**
```java
class P { P() { print(); } void print() { System.out.print("P"); } }
class C extends P { String s = "C"; void print() { System.out.print(s); } }
new C();
```
- *Testing*: overridable call from a constructor.
- *Expected*: `null` — the parent constructor runs before `C.s` is assigned, yet dispatch still reaches `C.print()`.
- *Wrong*: "`P`" or "`C`".

**Q9. `super()` vs `this()` — differences and constraints?**
- *Testing*: chaining rules.
- *Expected*: `this(...)` targets the same class, `super(...)` the direct parent; both must be the first statement; only one may be used; both keep the same object.
- *Wrong*: "`super()` creates a parent object."

**Q10. What happens if the parent has only `Parent(int)` and the child's constructor does not call `super(...)` explicitly?**
- *Testing*: the implicit-super rule.
- *Expected*: compile error — the compiler inserts `super();` and there is no accessible no-arg constructor.
- *Wrong*: "It calls `Parent(int)` automatically."
<!-- c2-s16a-end -->

### Advanced
**Q11. Why is calling an overridable method from a constructor dangerous beyond the `null` case?**
- *Testing*: deeper reasoning about initialisation order.
- *Expected*: The subclass has not run its own validation yet, so the parent can observe an object that violates subclass invariants; if the method stores `this` anywhere, a partially constructed object escapes.
- *Wrong*: "Only `null` values are affected."

**Q12. How does a `record`'s compact constructor work, and where does normalisation go?**
- *Testing*: Java 16+ knowledge.
- *Expected*: no parameter list; runs before fields are assigned; assignments to its parameters become the field values; the canonical constructor is still generated.
- *Wrong*: "It is syntax sugar with no body."

**Q13. A class with a private constructor and static factory methods — trade-offs?**
- *Testing*: API design.
- *Expected*: Pros — caching, subtype choice, expressive names, easier evolution. Cons — cannot be subclassed, less obvious for `new`-oriented developers, reflection/serialisation caveats.
- *Wrong*: "It is always better than `new`."

**Q14. Why do JPA entities need a no-arg constructor, and what visibility?**
- *Testing*: production framework knowledge.
- *Expected*: Hibernate instantiates reflectively and then populates fields, so a no-arg constructor is required; `protected` stops business code abusing it. It also forces a mutable (non-final-field) entity.
- *Wrong*: "It must be `public`."

**Q15. Constructors `A(int, String)` and `A(String, int)` — good design?**
- *Testing*: API judgement (links to topic 05).
- *Expected*: Legal, but a reliable source of swapped-argument bugs; prefer named factories, builders or distinct parameter types.
- *Wrong*: "Impossible — that is a compile error."

## 17. Production-Level Questions
1. **Startup failure:** constructor injection makes a Spring bean fail at startup when a dependency is missing. Why is that better than failing at request time with field injection?
2. **Reflection:** a mapper library calls `getDeclaredConstructor().newInstance()` on your DTO and throws `NoSuchMethodException`. What does its design assume, and how do you fix it without weakening your invariants?
3. **Serialisation:** a `record` normalises currency to upper case in its compact constructor, yet a JSON-stored value is lower case. Where was the constructor bypassed, and why?
4. **Performance:** a hot-path constructor calls `String.format` and copies a `List` into an unmodifiable view. What is the real cost, and when is lazy validation better?
5. **Thread safety:** a constructor publishes `this` into a shared collection. What can another thread observe, and how do `final` fields change that?
<!-- c2-s17-end -->

## 18. What I Should Remember
1. A constructor runs **after** allocation and exists to make the object valid; it has no return type.
2. Default constructor exists only if you declared no constructor; it is just `super();`.
3. Constructors are not inherited — subclasses reach the parent through `super(...)`.
4. Order: parent static → child static → parent fields/blocks → parent ctor body → child fields/blocks → child ctor body.
5. `this(...)` and `super(...)` must be the first statement; only one.
6. Never call an overridable method from a constructor.
7. Constructors compile to `<init>`; static initialisers to `<clinit>`.
8. Use a copy constructor and/or a Builder instead of 10-parameter constructors.

## 19. Connection To Other Java Topics
```
Topic 01 Classes and Objects (heap allocation, defaults, references)
        ↓
Topic 02 Constructors            <-- you are here
        ↓
Topic 03 this and super (chaining, dispatch)
        ↓
Inheritance and Polymorphism → abstract classes → interfaces
        ↓
equals/hashCode contract (topic 11): a correct equals() must be symmetric across subclasses
```
Immutability, defensive copying and `final` fields — all decided in constructors — are the
foundation for the concurrency topics later (safe publication without locks).

## 20. One-Minute Revision
- Constructor = class name, no return type, called by `new`; not inherited, not overridden.
- `void Demo()` is a *method* — a classic silent bug.
- Default constructor appears only when no constructor is declared.
- Order for `new Child()`: Parent static → Child static → Parent fields/blocks → Parent ctor → Child fields/blocks → Child ctor.
- `this(...)` = same class, `super(...)` = direct parent, both first statement, only one allowed.
- `<init>` per constructor, `<clinit>` for static initialisation.



