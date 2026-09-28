# this and super

## 1. Main Idea
`this` is a reference to **the current object** — the instance whose method or constructor
is executing. `super` is *not* a reference to a separate object; it is a keyword that tells
the compiler "resolve this member against the **direct superclass** instead of the current
class". Java has no "parent object" on the heap: a `Child` object contains the parent's
fields inside the *same* memory block, and `super` is just a compile-time instruction for
which declaration to use. Both must appear **only in instance context** — you cannot use
`this` or `super` inside a `static` method, block or field initialiser.

## 2. Why Does This Exist?
- **`this`** solves *shadowing* (a parameter named `balance` hiding the field `balance`),
  enables **constructor chaining** (`this(...)`), enables **fluent APIs** (`return this;`),
  lets an object **pass itself** as an argument, and lets an inner class name its enclosing
  instance (`Outer.this`).
- **`super`** lets a subclass **reuse** parent behaviour instead of duplicating it
  (`super.validate()`), **extend** it (`super.toString() + ", x=" + x`), and **choose**
  which parent constructor runs (`super(id)`). Without it, inheritance would force
  copy-paste.

## 3. Prerequisites
- Topic 01 Classes and Objects (references, instance fields, `new`).
- Topic 02 Constructors (default constructor, `this(...)`, ordering, implicit `super()`).
- Basic `extends` syntax. Full polymorphism comes in the next module.

## 4. Core Concepts

### `this` — Five Uses
| Use | Syntax | Why |
|---|---|---|
| Disambiguate a shadowed field | `this.balance = balance;` | Parameter hides the field |
| Chain constructors | `this("UNASSIGNED", 0L);` | One real initialisation path |
| Return the current object | `return this;` | Fluent / builder-style APIs |
| Pass itself as an argument | `registry.register(this);` | Observer / callback patterns |
| Qualify an outer instance | `Outer.this.count` | Inner class hides the outer field |

### `super` — Three Uses
| Use | Syntax | Why |
|---|---|---|
| Call a parent constructor | `super(id);` | Must be the first statement |
| Call a parent method that is overridden | `super.describe();` | Reuse instead of duplicate |
| Read a hidden parent field | `super.limit` | Only when the child declares a field with the same name |

Note: `super.field` reads the *same memory slot* semantics as `this.field` — there is only
one object. It exists purely to skip the child's declaration during name resolution.

### How `this.method()` and `super.method()` differ
- `this.method()` (or a bare `method()`) compiles to **`invokevirtual`** → **dynamic
  dispatch**: the actual *runtime* class's override is called.
- `super.method()` compiles to **`invokespecial`** → **static** (non-virtual) dispatch: the
  *superclass* version is called, and subclasses of *your* class cannot intercept it.

```
this.method()   -> invokevirtual  -> pick the implementation of the runtime class
super.method()  -> invokespecial  -> pick the parent's implementation, unconditionally
```

### `this` Inside Lambdas and Inner Classes
- A **lambda** does not create a new `this`. `this` inside a lambda refers to the
  *enclosing instance* (unlike an anonymous class, which captures its own but still has an
  enclosing reference).
- An **inner (non-static) class** has its own `this` and also an outer reference,
  written `Outer.this`.
- A **static nested class** has no outer instance, so it cannot use `Outer.this`.

```java
class Counter {
    int count;
    void increment() { count++; }

    Runnable lambda = () -> this.count++;          // 'this' = the Counter instance
    class Inner { void bump() { Counter.this.count++; } }   // outer instance
    static class Nested { /* no Counter.this here */ }
}
```

### `this` Cannot Escape a Constructor Safely
Passing `this` from a constructor (`registry.register(this)`) publishes a **partially
constructed object** to other threads. Don't do it — collect callbacks and publish in a
factory/init method instead.

## 5. Syntax
```java
class Parent {
    protected String id;

    Parent(String id) { this.id = id; }          // 'this' disambiguates the field

    String describe() { return "Parent(" + id + ")"; }
}

class Child extends Parent {

    private String id;                            // hides Parent.id on purpose (bad idea!)

    Child(String id) {
        super(id);                                // must be first: runs Parent(String)
    }

    Child() {
        this("generated");                        // chains to Child(String), which calls super(...)
    }

    @Override
    String describe() {
        return super.describe() + " + Child(" + this.id + ")";   // reuse + own field
    }

    Child self() { return this; }                 // fluent: returns the current object
}
```

## 6. Simple Example
See `01-ThisKeyword/ThisKeywordDemo.java` (shadowing, chaining, `return this`, `Outer.this`)
and `02-SuperKeyword/SuperKeywordDemo.java` (constructor order, `super.method()`,
`invokevirtual` vs `invokespecial` behaviour).

## 7. Real-Life Analogy
`this` is your **own name badge** at a conference: when two people are called "Alex" and
someone says "Alex, present", the badge tells you which Alex is meant. `super` is the
**"ask the person who trained me"** button: when you do not know a policy, you follow the
procedure your mentor (the parent class) defined, even if you have your own updated copy
of the manual (an override).
<!-- c3-s7-end -->

## 8. Real Backend Example
**Auditing base entity** (`@MappedSuperclass`) — every entity inherits audit fields, and the
child calls `super` to reuse the parent's logic instead of duplicating it:

```java
@MappedSuperclass
public abstract class AuditedEntity {

    private Instant createdAt;
    private Instant updatedAt;

    /** Shared hook every subclass reuses. */
    public void touch() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    public String auditSummary() {
        return "created=" + createdAt + ", updated=" + updatedAt;
    }
}

@Entity
public class AccountEntity extends AuditedEntity {

    @Id
    private String accountNumber;
    private long balance;

    protected AccountEntity() { }                   // JPA needs a no-arg constructor

    public AccountEntity(String accountNumber, long balance) {
        super();                                    // implicit; written for clarity
        this.accountNumber = accountNumber;         // 'this' disambiguates the field
        this.balance = balance;
    }

    @Override
    public String auditSummary() {
        return super.auditSummary() + ", account=" + this.accountNumber;   // reuse + extend
    }
}
```

**Fluent builder** — `return this;` produces readable, chainable APIs:

```java
public final class TransferRequestBuilder {

    private String fromAccount;
    private String toAccount;
    private long amountMinorUnits;

    public TransferRequestBuilder from(String account) { this.fromAccount = account; return this; }
    public TransferRequestBuilder to(String account)   { this.toAccount   = account; return this; }
    public TransferRequestBuilder amount(long minor)   { this.amountMinorUnits = minor; return this; }

    public TransferRequest build() {
        return new TransferRequest(fromAccount, toAccount, amountMinorUnits);   // validated inside
    }
}
// usage: new TransferRequestBuilder().from("A-1").to("B-2").amount(1_500).build();
```

**Composite `toString()`** — the most common production use of `super.method()`: subclasses
append their own state to the parent's output instead of reimplementing it. Logging and
proxy layers depend on this shape.

## 9. Internal Working
- At bytecode level `this` **is local variable slot 0** of every instance method and
  constructor — you see `aload_0` (load `this`) all over the place. There is no separate
  hidden "this pointer" parameter.
- A `static` method has no slot 0, which is precisely why `this`/`super` are compile errors there.
- `this.method()` or a bare `method()` → `invokevirtual` (dynamic dispatch on the runtime class).
- `super.method()` → `invokespecial` (non-virtual; the parent's declaration is fixed at compile time).
- `this.field` and `super.field` both compile to `getfield`/`putfield` **on the same object**;
  they differ only in which declaration the compiler resolves against.
- `this(...)`/`super(...)` compile to `invokespecial <init>` **on the same reference** — no
  second object is ever created.

```java
class Child extends Parent { Child() { super(); } }
```
```
javap -c -p Child  ->  Child();
    0: aload_0
    1: invokespecial #1    // Method Parent."<init>":()V     <-- super()
    4: return
```

```
Object layout: ONE allocation for a Child instance
+---------------------------------------------+
| header (mark word + class pointer)          |
| Parent fields  (createdAt, updatedAt)       |
| Child fields   (accountNumber, balance)     |
+---------------------------------------------+
   ^ 'this' and 'super' both address THIS block
```
<!-- c3-s9-end -->

## 10. Important Rules
1. `this` and `super` are valid only in **instance** context: instance methods, constructors,
   instance initialiser blocks, instance field initialisers, and lambdas/inner classes that
   capture an instance. **Never** in `static` context.
2. `this(...)` and `super(...)` must be the **first statement** of a constructor, and only one may appear.
3. `super.super.method()` does not exist — Java cannot skip a level of the hierarchy.
4. `super` is not an object. There is no separate parent instance on the heap; you cannot
   print it, cast it, or store it.
5. `this` cannot be reassigned; `this.x` can be assigned if `x` is not `final`.
6. Never publish `this` from a constructor (it leaks a partially constructed object).
7. Inside a **lambda**, `this` refers to the enclosing instance, not to a new object.
8. A **static nested class** has no outer instance; an **inner class** does, named `Outer.this`.
9. `super.field` only changes meaning when the subclass declares a field with the same name;
   otherwise it behaves exactly like `this.field`.

## 11. Common Mistakes
- **Forgetting `this.` in a setter** — the parameter silently assigns to itself:
  ```java
  void setBalance(long balance) {
      balance = balance;          // compiles fine, does nothing
  }                               // fix: this.balance = balance;
  ```
- **Using `this`/`super` in a `static` method**:
  ```java
  static void log() {
      System.out.println(this);   // compile error: non-static variable this cannot be referenced
  }
  ```
- **Believing `super()` creates a parent object** — it initialises the *parent part of the same object*.
- **Trying `super.super.toString()`** → compile error. Extract a `protected` helper in the middle
  class and call `super.helper()` instead.
- **Publishing `this` from a constructor**:
  ```java
  TransferService(Registry registry) {
      registry.register(this);    // other threads may observe a half-built object
  }
  ```
- **Shadowing a parent field with the same name** (`private String id;` in both classes) — legal,
  but now `this.id` and `super.id` are two different fields and bugs follow.
- **Assuming `this.method()` inside the parent class calls the parent's version** — it does not;
  it dispatches to the child's override (see topic 02's constructor trap).
<!-- c3-s11-end -->

## 12. Comparison

| Aspect | `this` | `super` |
|---|---|---|
| Meaning | The current object | "Resolve against the direct superclass" |
| Runtime object? | Yes — local slot 0 of the frame | No — a compile-time instruction only |
| Allowed in `static` context | No | No |
| Constructor chaining | `this(...)` → same class | `super(...)` → direct parent |
| Method dispatch | `invokevirtual` (polymorphic) | `invokespecial` (fixed) |
| Field use | Disambiguate shadowing | Read a hidden parent field |

| Aspect | `this.method()` / bare `method()` | `super.method()` |
|---|---|---|
| Which implementation | The runtime class's override | The parent's declaration |
| Bytecode | `invokevirtual` | `invokespecial` |
| Affected by later overrides | Yes | No |
| Typical use | Calling own/overridable behaviour | Reusing parent behaviour (`super.toString()`) |

| Aspect | Inner (non-static) class | static nested class |
|---|---|---|
| Has outer reference | Yes → `Outer.this` | No |
| Can access outer instance fields | Yes | Only via a passed reference |
| Created with | `outer.new Inner()` | `new Outer.Nested()` |
| Memory | Holds a hidden `this$0` field | Independent |

## 13. Code Examples
- **Beginner** — `01-ThisKeyword/ThisKeywordDemo.java`: the five uses of `this`, including
  `Outer.this` and the silent `balance = balance` bug with its fix.
- **Intermediate** — `02-SuperKeyword/SuperKeywordDemo.java`: constructor ordering with
  `super(...)`, `super.describe()` reuse, and two same-named fields showing `this.id` vs `super.id`.
- **Advanced** — `03-DispatchDifference/DispatchDifferenceDemo.java`: printed proof that
  `this.method()` (invokevirtual) and `super.method()` (invokespecial) select different
  implementations; inspect with `javap -c -p Base`.
<!-- c3-s13-end -->

## 14. Practice Questions
1. Why is `this` required in `this.balance = balance;` and what happens without it?
2. Compile or not? `static void f() { System.out.println(this); }`
3. How many objects are created by `Child c = new Child();` when `Child extends Parent`?
4. What does `super.describe()` do differently from `this.describe()` inside an override?
5. Can you call `super()` as the second statement of a constructor? Why not?
6. Predict the output:
   ```java
   class A { A() { who(); } void who() { System.out.print("A"); } }
   class B extends A { String n = "B"; void who() { System.out.print(n); } }
   new B();
   ```
7. Why does `super.super.toString()` not compile, and what is the workaround?
8. In a lambda inside an instance method, what does `this` refer to?
9. When is `super.field` different from `this.field`?
10. Why is publishing `this` from a constructor dangerous in a multi-threaded service?

### Answers
1. Without `this.`, `balance = balance;` assigns the parameter to itself — the field never changes.
2. Does **not** compile: `non-static variable this cannot be referenced from a static context`.
3. **One** object. `super()` initialises the parent portion of the same object.
4. `super.describe()` uses `invokespecial` → the parent's implementation, ignoring overrides in
   the current class (and any further subclass). `this.describe()` dispatches polymorphically.
5. No. `this(...)`/`super(...)` must be the first statement, because the object's initialisation
   must be exactly one linear chain.
6. `null` — the parent constructor runs before `B.n` is assigned, but dispatch still reaches `B.who()`.
7. Java has no syntax to skip a level. Workaround: the middle class exposes a `protected` (or
   `final`) helper that calls its own `super.<method>()`, and the leaf calls that helper.
8. The enclosing instance — lambdas do not introduce a new `this`.
9. Only when the subclass declares a field with the same name; then `this.field` is the child's
   field and `super.field` is the parent's.
10. Another thread can obtain a reference to the object before its fields are assigned, so it can
    observe default values or violate invariants; the JVM's `final` field guarantees help but do
    not cover mutable state.

## 15. Coding Practice
**Easy** — Write `Temperature` with a constructor `Temperature(double celsius)` that uses `this.` to
assign the field, plus a `toFahrenheit()` method. Then write a deliberately broken setter and prove
it fails, then fix it.
**Medium** — Write `Vehicle` and `Car extends Vehicle`. `Vehicle` has `start()` and `describe()`;
`Car` overrides `describe()` to call `super.describe()` and add its own state. Print the output of
`new Car("Toyota", "Corolla").describe()`.
**Hard** — Implement a fluent `HttpRequestBuilder` (method, url, header, body) using `return this;`
and a `send()` that validates. Then add a subclass `JsonRequestBuilder extends HttpRequestBuilder`
and explain why fluent methods that return the *base* type break chaining on the subclass, and how
`return this` typed as the base class forces you to choose between fluent-chaining and inheritance.
<!-- c3-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What is `this` in Java?**
- *Testing*: basic vocabulary.
- *Expected*: a reference to the current object; local variable slot 0 in instance methods and constructors.
- *Wrong*: "A keyword that refers to the class."

**Q2. What is `super`?**
- *Testing*: whether you know it is not an object.
- *Expected*: a keyword used to access the parent class's constructor, methods and hidden fields; there is no parent object on the heap.
- *Wrong*: "A reference to the parent object."

**Q3. Why do we write `this.balance = balance;`?**
- *Testing*: shadowing.
- *Expected*: the parameter hides the field; `this.` selects the field.
- *Wrong*: "It is optional style."

**Q4. Can you use `this` in a `static` method?**
- *Testing*: static vs instance context.
- *Expected*: No — compile error; there is no current object.
- *Wrong*: "Yes, it refers to the class."

**Q5. What does `super()` do?**
- *Testing*: constructor chaining.
- *Expected*: invokes the direct parent's no-arg constructor on the *same* object, before the child's fields are initialised.
- *Wrong*: "Creates a new parent object."

### Intermediate
**Q6. `this(...)` vs `super(...)`?**
- *Testing*: chaining rules.
- *Expected*: same class vs direct parent; both must be the first statement; only one; same object.
- *Wrong*: "Both can appear together."

**Q7. What is the difference in dispatch between `this.method()` and `super.method()`?**
- *Testing*: the core of this topic.
- *Expected*: `this.method()` → `invokevirtual` (runtime override wins). `super.method()` → `invokespecial` (parent's version, fixed at compile time).
- *Wrong*: "They always call the same method."

**Q8. Why can a subclass not call `super.super.method()`?**
- *Testing*: language design awareness.
- *Expected*: Java only supports one level of super access; skipping a level would break encapsulation of the intermediate class.
- *Wrong*: "It works if the classes are in the same package."

**Q9. Inside an anonymous class, what does `this` refer to — and inside a lambda?**
- *Testing*: capture semantics.
- *Expected*: anonymous class → its own instance (use `Outer.this` for the enclosing instance). Lambda → the enclosing instance.
- *Wrong*: "The same in both."

**Q10. What does `Outer.this` mean?**
- *Testing*: inner classes.
- *Expected*: explicitly refers to the enclosing instance of an inner (non-static) class, needed when names collide.
- *Wrong*: "It is the same as `Outer.class`."
<!-- c3-s16a-end -->

### Advanced
**Q11. Why are `this` and `super` both unusable in a `static` nested class's methods (and how do you work around it)?**
- *Testing*: static vs inner semantics.
- *Expected*: A static nested class has no enclosing instance, so there is no `this` of the outer type nor an outer to `super` into; pass the outer instance (or its required data) as a parameter.
- *Wrong*: "Make the method non-static" (it would then need an instance to call it on).

**Q12. How does `final` interact with `this` and with `super.method()`?**
- *Testing*: precise modifier semantics.
- *Expected*: `this` cannot be reassigned; a `final` method cannot be overridden, so `super.method()` on a `final` parent method is effectively the same computation as a normal call on that declaration; `final` on a field makes the field assignable only in a constructor/initialiser.
- *Wrong*: "`final` prevents `this` from being used."

**Q13. In bytecode, what is `invokespecial` and when else is it used?**
- *Testing*: JVM instruction knowledge.
- *Expected*: non-virtual dispatch: used for `super.method()`, `private` methods, and constructors (`<init>`). `invokestatic`, `invokevirtual`, `invokeinterface` and `invokedynamic` handle the other cases.
- *Wrong*: "It is the same as `invokevirtual` but faster."

**Q14. A parent constructor calls `this.templateMethod()`. What exactly happens, and how do you make it safe?**
- *Testing*: template-method pattern plus constructor ordering.
- *Expected*: The child override runs before the child's fields are initialised. Make the template method `final` in the parent with a `private`/`protected final` "do" hook, or defer the hook to an explicit `init()` call.
- *Wrong*: "Nothing — it is fine because it is a `this.` call."

**Q15. Why does `System.out.println(super)` not compile?**
- *Testing*: understanding `super` is not an expression.
- *Expected*: `super` is not a value; it is only usable as a receiver qualifier (`super.field`, `super.method()`, `super(...)`). Printing the current object means printing `this`.
- *Wrong*: "It needs a cast."

## 17. Production-Level Questions
1. **Fluent APIs vs inheritance:** a builder returns `this` typed as the base builder. A subclass's extra methods become unreachable mid-chain. How do you solve this (generics-based self-types, or composition instead of inheritance)?
2. **Auditing:** an entity's `update()` calls `super.touch()`, but a later refactoring overrides `touch()` in the child. What breaks, and why is making the parent's method `final` a common fix?
3. **Concurrency:** a constructor registers `this` with a listener. Which memory-visibility guarantees are missing, and what is the minimal safe redesign?
4. **Logging:** `toString()` implementations that concatenate `super.toString()` are convenient but can leak PII into logs. What is the production-safe approach?
5. **Framework debugging:** Hibernate/Spring generate proxy subclasses. Why can `this.getClass()` and `instanceof` behave surprisingly inside such a proxy, and how does that connect to `super`-based calls?
<!-- c3-s17-end -->

## 18. What I Should Remember
1. `this` = current object = local variable slot 0; `super` = a compile-time instruction, not an object.
2. Neither exists in `static` context.
3. `this.field` fixes shadowing; a missing `this.` gives silently broken setters.
4. `this.method()` → `invokevirtual` (polymorphic); `super.method()` → `invokespecial` (fixed).
5. `this(...)` = same class, `super(...)` = direct parent; first statement only, and only one.
6. `super()` initialises the parent part of the **same** object — one object, one allocation.
7. No `super.super`; no printing `super`; `this` cannot be reassigned.
8. Every inner class holds a hidden outer reference; static nested classes do not.
9. Never leak `this` from a constructor.

## 19. Connection To Other Java Topics
```
Topic 02 Constructors (this(...), implicit super(), ordering)
        ↓
Topic 03 this and super            <-- you are here
        ↓
Inheritance → Polymorphism → dynamic dispatch (invokevirtual)
        ↓
Abstract classes and Interfaces → template method pattern
        ↓
equals/hashCode contract (topic 11): equals is a common place to use super.equals() ... or not
```
`invokevirtual` is the mechanism you will use implicitly every time you call a method through
an interface or a parent reference — that machinery starts here.

## 20. One-Minute Revision
- `this` = me; `super` = "the parent's version of that member" — same object, no second allocation.
- Slot 0 = `this`; `static` methods have no slot 0, so `this`/`super` do not compile there.
- `this.balance = balance;` exists purely because parameters shadow fields.
- `this.m()` is polymorphic; `super.m()` is fixed at compile time.
- `this(...)`/`super(...)` first statement, one constructor chain, parent always before child fields.
- Never publish `this` from a constructor.


