# Access Modifiers

## 1. Main Idea
Access modifiers control **who is allowed to see a type or a member**. Java has four levels, from most
restrictive to most open: `private`, **package-private** (no modifier), `protected`, and `public`. They
are enforced by the **compiler** (and re-checked by the JVM, which can throw `IllegalAccessError` if class
files change incompatibly). `package-private` is a real and useful level — "visible to everyone in this
package" — which is why package structure is part of your API design. Only two levels are legal for a
**top-level type** (`public` or package-private); all four are legal for **members** and **nested
types**. `protected` is the subtlest level: it means "same package **or** a subclass", but in a subclass
in another package you may reach a protected member only through a reference whose type is that subclass
(or a subtype) — verified below with the exact compiler error.

## 2. Why Does This Exist?
- **Encapsulation** — protect invariants: if `balance` were `public`, any class could set it negative and
  bypass every business rule.
- **A small, honest API surface** — public members are promises you must keep for years; everything else
  can be refactored without breaking callers.
- **Implementation hiding** — helper classes should be package-private so `javac` prevents them from
  leaking into the published API.
- **Extensibility control** — `protected` methods hand subclasses deliberate extension points while
  keeping everything else closed.
- **Security and modularity** — with JPMS (`exports` in `module-info.java`), visibility defines trust
  boundaries; without it any code in the JVM could call anything.

## 3. Prerequisites
- Topic 01 Classes and Objects, Topic 02 Constructors (fields, accessors).
- Topic 03 `this` and `super` (subclass references — needed for the `protected` rule).
- Topic 04 Methods (visibility and its effect on overriding).
- Topic 09 `static` and `final` (visibility applies to static members too).
- Basic `package`/`import` syntax from `Java Fundamentals/01-Java-Execution-Model`.

## 4. Core Concepts

### The Access Matrix (memorise this table)
| Where the access happens | `private` | package-private | `protected` | `public` |
|---|---|---|---|---|
| Same class | Yes | Yes | Yes | Yes |
| Same package (any class) | No | Yes | Yes | Yes |
| Subclass in the **same** package | No | Yes | Yes | Yes |
| Subclass in **another** package | No | No | Yes — **only** through a subclass-typed reference | Yes |
| Unrelated class in another package | No | No | No | Yes |

### What Can Take Which Modifier
| Element | Allowed levels |
|---|---|
| Top-level class / interface / enum / record | `public` or package-private **only** (`private`/`protected` are compile errors) |
| Nested class / interface / enum / record | All four |
| Field, method, constructor | All four |
| Local variable, method parameter | None (only `final`) |
| Interface members | Fields/methods implicitly `public` (fields also `static final`); `private` methods and fields allowed since Java 9 |
| `record` components | `private final` fields + `public` accessors (cannot be weakened) |
| Enum constants | Implicitly `public static final` |
| Sealed type's permitted subclasses | Must be `public` if the sealed type is public and the subclass is in another package |

### The `protected` Subtlety, Verified
```java
// library/P.java
package library;
public class P { protected long balance; public long value() { return balance; } }

// app/Q.java  (subclass in another package)
package app;
public class Q extends library.P {
    long viaThis()                 { return this.balance; }    // OK
    long viaOther(library.P other) { return other.balance; }   // ERROR
}
```
Verified compiler error:
```
app/Q.java:4: error: balance has protected access in P
  long viaOther(library.P other) { return other.balance; }
                                               ^
```
An unrelated class in another package gets the same error. A package-private **type** produces:
```
app/S.java:2: error: Hidden is not public in library; cannot be accessed from outside package
public class S { library.Hidden h; }
```

### Package-Private Is A Design Tool
- A package is a **cohesion boundary**: keep implementation classes, helpers and internal DTOs in the same
  package as their public entry point and leave them unmodified (package-private).
- Because `javac` forbids other packages from naming them, they can be refactored freely.
- Tests usually live in the **same package** (same `package` declaration, different source root) so they
  can exercise package-private members — a common reason a class is not `public`.

### Encapsulation: Fields Should Be `private`
```java
public class Account {
    private long balance;                       // state hidden
    public long balance()       { return balance; }
    public void deposit(long a) { if (a <= 0) throw new IllegalArgumentException(); balance += a; }
}
```
Accessors are not ceremony: they are the only place invariants can be enforced and the only seam for
validation, logging, lazy loading or instrumentation.

### Visibility, Overriding And Hiding
- An override may **not reduce** visibility (`protected` → `public` is fine; `public` → `protected` is a
  compile error).
- A static method hiding a parent static method cannot reduce visibility either.
- `abstract` and `private` are mutually exclusive; a subclass must be able to see what it implements.

### Visibility vs Reflection And Modules
- `private` is not a sandbox: `setAccessible(true)` plus module access (`opens`, `--add-opens`) can reach
  it. Encapsulation is a **design contract**, not a security boundary.
- With JPMS (Java 9+), even a `public` type is reachable only if its package is exported:
  `module library { exports library; }`.
<!-- c10-s4-end -->

## 5. Syntax
```java
package library;                                  // the unit that package-private refers to

public final class Account {                      // public type: the published API

    private final String id;                      // private: only Account (and its nests)
    final String packageNote = "internal";        // package-private: whole 'library' package
    protected long balance;                       // protected: package + subclasses
    public static final int MAX_DEPOSIT = 1_000;  // public: everyone

    public Account(String id) { this.id = id; }

    private   String secret()       { return "only Account"; }
              String packageScope() { return "only library"; }        // no modifier
    protected long   balance()      { return balance; }
    public    String id()           { return id; }

    /** A protected method is a deliberate extension point for subclasses. */
    protected void validate(long amount) {
        if (amount <= 0 || amount > MAX_DEPOSIT) throw new IllegalArgumentException("invalid amount");
    }

    /** Final public entry point calling the overridable protected hook (template method). */
    public final void deposit(long amount) {
        validate(amount);                         // subclasses may customise validation
        balance += amount;
    }
}

class AccountValidator { }                        // package-private top-level type: 'library' only
```

## 6. Simple Example
The runnable example in `01-AccessModifiersDemo` is a **multi-package** program, because access modifiers
only make sense across packages:

| File | Package | Shows |
|---|---|---|
| `library/Account.java` | `library` | all four levels on one class; everything reachable from inside |
| `library/InternalHelper.java` | `library` | a **package-private top-level type** (invisible to `app`) |
| `library/SubAccount.java` | `library` | a same-package subclass: sees package-private **and** protected |
| `app/OutOfPackageSub.java` | `app` | a subclass in another package: `this.balance` works, `other.balance` does not |
| `app/AccessModifierDemo.java` | `app` | only the `public` surface is visible |

```bash
cd 01-AccessModifiersDemo
javac library/*.java app/*.java
java app.AccessModifierDemo
```
Verified output includes: `same-package subclass -> balance=1010 | ...` (package-private **and** protected
work), `other-package subclass -> balance=2025 | ...` (protected via `this` works) and
`publicValue() = 500` (the public accessor is the only route in).

## 7. Real-Life Analogy
Access modifiers are the **security rings of an office building**:
- `public` = the lobby, open to any visitor.
- `package-private` = the floor your team works on: colleagues walk in freely, other floors cannot.
- `protected` = a **family pass**: your team plus anyone trained by your team (subclasses) may enter — but
  a family member may only use *their own* pass at *their own* desk, not a stranger's (the
  "subclass-typed reference" rule).
- `private` = your own locked desk drawer: nobody at all, not even others on your floor.
<!-- c10-s7-end -->

## 8. Real Backend Example
```java
// 1. package-by-feature: one package holds the public entry point and its internals.
com.acme.transfer/
    TransferController.java     // public    - HTTP entry point
    TransferService.java        // public    - the feature's API for other features
    TransferMapper.java         // package-private helper
    TransferValidator.java      // package-private helper
    TransferRequest.java        // public    - part of the API contract

// 2. Entity fields stay private; only behaviour is public.
@Entity
public class AccountEntity {
    @Id private String id;                        // private state
    private long balance;

    protected AccountEntity() { }                 // protected: framework-only constructor (topic 02)

    public void deposit(long amount) {            // public: the only way to change balance
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        balance += amount;
    }
}

// 3. protected extension points in a framework-style base class.
public abstract class AbstractJob {
    public final void run() {                     // public, final: fixed algorithm
        try { execute(); }                        // subclasses override only this step
        catch (Exception e) { onFailure(e); }
        finally { cleanup(); }
    }
    protected abstract void execute();
    protected void onFailure(Exception e) { }
    protected void cleanup() { }
}

// 4. A JSON-serialised DTO must be public; its setter can stay package-private.
public class TransferReceipt {
    private String txnId;
    public String txnId() { return txnId; }
    void txnId(String id) { this.txnId = id; }     // package-private: only the mapper can set it
}
```

Production lessons:
- **Make the API as small as possible.** Every `public` method is a compatibility contract; every
  package-private class is free to change. "Public by default" is the most expensive habit in a codebase.
- **Prefer `protected` over `public` for extension points**, and keep the public entry point `final` so the
  algorithm cannot be bypassed (the *template method* pattern).
- **Spring beans can be package-private classes** — the framework instantiates them reflectively, so
  visibility need not be `public`. Prefer package-private for services used only inside their package.
- **Do not add getters/setters by reflex** — that is not encapsulation, it is a public field with extra
  steps. Expose *behaviour* (`deposit`) rather than *state* (`setBalance`).

## 9. Internal Working
- Access levels are stored as **flags in the class file** and enforced in two places:
  - `javac` rejects illegal access at compile time (the errors quoted above).
  - The JVM re-checks during class **linking**, throwing `IllegalAccessError` if a class file references a
    member it may not access at runtime — which is what happens when only part of a system is recompiled
    after a visibility reduction.
- **Class-file flags**: `ACC_PRIVATE`, `ACC_PROTECTED`, `ACC_PUBLIC` (none of the three = package-private),
  plus `ACC_STATIC`, `ACC_FINAL`, `ACC_SYNTHETIC`, etc. `javap -p` shows them.
- **Nestmates (Java 11+)**: before Java 11, an outer class touching a nested class's private members
  required synthetic bridges (`access$000`). Since Java 11 the JVM recognises *nestmates* via the
  `NestHost`/`NestMembers` attributes, so private access inside one top-level class is direct and no
  bridges are generated.
- **`protected` at runtime**: the JVM's cross-package rule mirrors the compiler's — the accessing code must
  be in a subclass and the receiver must be of the accessing class's type or below. That is why
  `other.balance` fails instead of silently working.
- **Reflection** goes through `AccessibleObject.checkAccess`; `setAccessible(true)` sets the `override`
  flag, permitted only when the module opens the package (`opens` / `--add-opens`); otherwise
  `InaccessibleObjectException` is thrown.
- **JPMS ordering**: module readability and package `exports`/`opens` are checked *before* the class-level
  access flags, so a `public` class in a non-exported package is unreachable from another module.
<!-- c10-s9-end -->

## 10. Important Rules
1. Four levels only: `private` < package-private (no modifier) < `protected` < `public`.
2. **Top-level types** can be only `public` or package-private. `private`/`protected` top-level types do
   not compile.
3. The "default" when you write no modifier is **package-private**, not `private` and not `public`.
4. `protected` = same package **or** subclass; across packages it requires a reference of the subclass
   type (or a subtype).
5. An override may **widen** visibility but never narrow it (`public` → `protected` is an error).
6. Interface fields/methods are implicitly `public`; interface fields are also `static final`.
7. `record` components generate `private final` fields with `public` accessors — you cannot weaken them.
8. Visibility is checked by the compiler and re-checked by the JVM at link time; JPMS exports are checked
   even earlier.
9. `private` is a design contract, not a security sandbox — reflection with `setAccessible(true)` and an
   `opens` directive can bypass it.
10. Prefer `private` fields with behaviour-oriented public methods; prefer package-private for anything not
    meant to leave the package.

## 11. Common Mistakes
- **Writing `private`/`protected` on a top-level type**:
  ```
  error: modifier private not allowed here
  ```
- **Assuming no-modifier means `public`** — it means package-private, so another package cannot see it.
- **Reaching for a protected member of a foreign object in another package** — verified error:
  ```
  error: balance has protected access in P
  ```
- **Trying to use a package-private type from another package** — verified error:
  ```
  error: Hidden is not public in library; cannot be accessed from outside package
  ```
- **Narrowing visibility in an override**:
  ```java
  public class A { public void m() { } }
  class B extends A { void m() { } }   // error: attempting to assign weaker access privileges
  ```
- **Making fields public for convenience** — you lose every chance to validate, and a later accessor
  cannot be introduced without breaking binary compatibility.
- **Making everything `public` in a library** — you now own those signatures for ever.
- **Assuming Spring needs `public` beans** — it does not; package-private `@Component`s work.
- **Believing `private` fields are unreadable by frameworks** — Jackson, Hibernate and Spring use
  reflection/`setAccessible`; design visibility for *your* code, not to "block" frameworks.
- **Confusing `final` with visibility** — `final` is about assignment, not who can see the member.
- **Putting test-only accessors in production code** to avoid moving the test into the same package.
- **Forgetting that `protected` members are also part of your API** — subclasses in other packages become
  consumers you must keep compatible.

## 12. Comparison

| Aspect | `private` | package-private | `protected` | `public` |
|---|---|---|---|---|
| Same class | Yes | Yes | Yes | Yes |
| Same package | No | Yes | Yes | Yes |
| Subclass, other package | No | No | Yes (subclass-typed reference only) | Yes |
| Other package | No | No | No | Yes |
| Typical use | State, helpers | Package internals, tests | Extension points | Published API |
| API commitment | None | Package-wide | Subclasses | Everyone, for ever |

| Aspect | Access modifier | `final` | `static` |
|---|---|---|---|
| Controls | Who can see/use the member | Whether it can be reassigned/overridden/extended | Whether it belongs to the class or an instance |
| Checked | Compile time (+ link time) | Compile time | Compile time |
| Can be combined with the others | Yes | Yes | Yes |
| Affects subclass API | Yes (visibility) | Yes (override/extension) | Yes (hiding, not overriding) |

| Aspect | Package-private class | `public` class |
|---|---|---|
| Usable by other packages | No | Yes |
| Refactoring freedom | High | Low (compatibility contract) |
| Typical use | Implementation helpers, internal DTOs | Entry points, published contracts |
| Test access | Same-package tests | Public tests |

## 13. Code Examples
- **Beginner/Intermediate** — `01-AccessModifiersDemo/` (multi-package): `library/Account.java` declares
  all four levels; `library/InternalHelper.java` is a package-private type; `library/SubAccount.java`
  shows same-package subclass access (package-private **and** protected);
  `app/OutOfPackageSub.java` shows the protected cross-package rule (`this.balance` works,
  `other.balance` does not); `app/AccessModifierDemo.java` shows that only the public surface is reachable
  from another package.
  ```bash
  cd 01-AccessModifiersDemo && javac library/*.java app/*.java && java app.AccessModifierDemo
  ```
- **Advanced** — the verified compiler errors quoted in section 4 and section 11 are the same experiment:
  remove the `public` from a type, or read `balance` through a superclass-typed reference from another
  package, and the build fails with exactly those messages.
<!-- c10-s13-end -->

## 14. Practice Questions
1. List the four access levels from most to least restrictive.
2. Which access levels may be applied to a **top-level** class?
3. What does no modifier mean?
4. From another package, which members of a `public` class can you use?
5. A subclass in another package wants to read a `protected` field. When does `other.balance` fail even
   though `this.balance` succeeds?
6. Can an override reduce visibility? What is the compiler error?
7. Are interface fields `public`? Are they `static final`?
8. What happens if a class file is compiled against a `public` method that is later made package-private,
   and only the declaring class is recompiled?
9. Why can tests in the same package access package-private members, and why does that matter?
10. In a Spring application, does a `@Service` class have to be `public`?

### Answers
1. `private` → package-private (no modifier) → `protected` → `public`.
2. Only `public` and package-private; the others are compile errors (`modifier private not allowed here`).
3. Package-private: visible to every class in the same package, and to no other package.
4. Only the `public` members (plus inherited ones visible through the public API).
5. Cross-package `protected` access requires the receiver's **static type** to be the accessing subclass
   (or below). `other` is typed as the superclass, so access is denied: `balance has protected access in P`.
6. No. The compiler reports something like `attempting to assign weaker access privileges; was public`.
7. Yes — implicitly `public`, and also `static final`.
8. The class file still references it; at runtime the JVM's access check fails with `IllegalAccessError`.
   Recompiling the caller turns it into a compile-time error instead.
9. Package-private members are visible to the whole package, and a test in the same package (same
   `package` declaration, different source root) is inside that boundary — so internals can be tested
   without making them public.
10. No. Spring instantiates beans reflectively; package-private `@Component`/`@Service` classes work and
    keep the API surface small.

## 15. Coding Practice
**Easy** — Create two packages, `library` and `app`. Put a class with one field at each access level in
`library`, then try to read all four from `app`. Note which lines fail to compile and why.
**Medium** — Write a `library.Stack` class with `private` storage, a `public push/pop/isEmpty` API, a
`package-private` `capacity()` used by `library` helpers, and a `protected` `onOverflow()` hook. Then
write `library.SubStack` (same package) and `app.SubStack` (other package) and show which members each can
reach.
**Hard** — Design a small library with a `public` interface, a package-private implementation class and a
`public` factory. Write tests in the same package that exercise the package-private implementation
directly, then add a `module-info.java` with only the API package exported and prove that the
implementation package is no longer reachable from another module.
<!-- c10-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What are the four access modifiers in Java?**
- *Testing*: core vocabulary.
- *Expected*: `private`, package-private (default), `protected`, `public`.
- *Wrong*: "public, private, protected and static."

**Q2. What is the default access level when you write no modifier?**
- *Testing*: the most commonly mixed-up detail.
- *Expected*: package-private — visible to all classes in the same package only.
- *Wrong*: "public" or "private".

**Q3. Can a top-level class be `private`?**
- *Testing*: syntax precision.
- *Expected*: No; top-level types can only be `public` or package-private.
- *Wrong*: "Yes, so only its own file can use it."

**Q4. What does `protected` mean?**
- *Testing*: the subtlest of the four.
- *Expected*: visible in the same package and to subclasses (in any package) — subject to the
  subclass-typed-reference rule across packages.
- *Wrong*: "Visible only to subclasses."

**Q5. Why should fields normally be `private`?**
- *Testing*: encapsulation understanding.
- *Expected*: so invariants can be enforced and the class can evolve; public fields expose state with no
  validation and no compatibility seam.
- *Wrong*: "Java requires it."

### Intermediate
**Q6. You are in `app` and want to read `library.Account.balance` (protected) from a subclass. When is it legal?**
- *Testing*: the cross-package rule.
- *Expected*: Only through a reference whose type is your subclass (or a subtype) — `this.balance` works,
  `other.balance` where `other` is typed `Account` fails with `balance has protected access in Account`.
- *Wrong*: "Always legal inside a subclass."

**Q7. Can an override change visibility?**
- *Testing*: override rules.
- *Expected*: It may widen but never narrow; narrowing produces
  `attempting to assign weaker access privileges`.
- *Wrong*: "Yes, any visibility is allowed."

**Q8. What happens at runtime if a `public` method becomes package-private and a caller is not recompiled?**
- *Testing*: link-time access checks.
- *Expected*: `IllegalAccessError` when the stale caller is first executed; recompiling would have caught it.
- *Wrong*: "Nothing, it still runs."

**Q9. Why might you deliberately make a class package-private?**
- *Testing*: API design.
- *Expected*: to keep implementation details out of the published API, keep refactoring freedom, and allow
  same-package tests to exercise internals (a public factory/interface remains the entry point).
- *Wrong*: "Because it is faster."

**Q10. Does Spring need `public` bean classes?**
- *Testing*: framework reality.
- *Expected*: No; beans are created reflectively, so package-private works. Keep the surface minimal.
- *Wrong*: "Yes, otherwise injection fails."
<!-- c10-s16a-end -->

### Advanced
**Q11. Which access levels are allowed on interface and record members, and how do they differ from classes?**
- *Testing*: modern language detail.
- *Expected*: interface fields/methods are implicitly `public` (fields also `static final`); `private`
  methods and `default`/`static` methods are allowed (Java 9+ for private). `record` components become
  `private final` fields with `public` accessors and cannot be weakened; the canonical constructor's
  visibility can be changed but cannot be more restrictive than the record itself.
- *Wrong*: "Interfaces can declare private fields."

**Q12. Why does `other.balance` fail while `this.balance` compiles in a subclass in another package?**
- *Testing*: deep reading of the JLS/JVM rules.
- *Expected*: JLS 6.6.2 restricts cross-package protected access to code in a subclass accessing a member of
  an object whose type is that subclass (or a subtype). It prevents a subclass from "opening up" the
  superclass's protected state for arbitrary instances of the superclass.
- *Wrong*: "It is a compiler bug."

**Q13. How does JPMS change visibility?**
- *Testing*: modules.
- *Expected*: adds a level above access flags: a package must be `exports`-ed for `public` types to be
  reachable, and `opens` is needed for deep reflection; `--add-opens` bypasses temporarily. Otherwise the
  class is unreachable despite being `public`.
- *Wrong*: "Modules do not affect visibility."

**Q14. Are `private` members truly private?**
- *Testing*: realistic threat model.
- *Expected*: Not from a determined caller: reflection with `setAccessible(true)` (plus module `opens`) can
  reach them; `Unsafe`/serialisation/JVM agents can go further. Treat visibility as an API/design contract,
  and never store secrets in private fields as a security measure.
- *Wrong*: "Yes, it is enforced by the JVM and cannot be bypassed."

**Q15. Why did Java 11 introduce nestmates, and what did it replace?**
- *Testing*: bytecode history.
- *Expected*: to let a top-level class and its nested types share private access efficiently. Before Java 11
  the compiler generated synthetic accessor bridges (`access$000`) because the JVM's access rules treated
  nested classes as separate classes; nestmate attributes remove the overhead and make the semantics direct.
- *Wrong*: "To allow private methods in interfaces."

## 17. Production-Level Questions
1. **API bloat:** a shared library marks everything `public`. What concrete costs appear over time, and how
   would you reduce the surface without breaking existing consumers?
2. **Breaking change:** a library team changes a method from `public` to package-private in a patch release.
   Tests pass locally, and one downstream service starts throwing `IllegalAccessError` in production. Why did
   nothing catch it at build time?
3. **Spring visibility:** a team makes every bean class package-private and an integration test in another
   package can no longer inject it. Is that a good outcome? How do you structure the tests instead?
4. **protected leakage:** a base class exposes protected mutable fields that 12 subclasses write directly.
   What design problems does this cause, and what is the refactor?
5. **Package-by-layer vs package-by-feature:** how do access modifiers behave differently in the two
   structures, and why does package-by-feature usually produce a smaller public API?
<!-- c10-s17-end -->

## 18. What I Should Remember
1. Four levels: `private` < package-private (no modifier) < `protected` < `public`.
2. Top-level types: only `public` or package-private.
3. No modifier = **package-private**, not public.
4. `protected` across packages requires a **subclass-typed** reference.
5. An override may widen visibility, never narrow it.
6. Visibility is checked by `javac`, re-checked by the JVM (`IllegalAccessError`), and gated by JPMS
   `exports`/`opens`.
7. `private` is a design contract; reflection (`setAccessible` + `opens`) can bypass it.
8. Keep the public surface minimal; package-private is your refactoring freedom, and same-package tests can
   still reach it.

## 19. Connection To Other Java Topics
```
Topic 01 Classes and Objects  →  Topic 02/03 Constructors, this/super
Topic 04 Methods  →  Topic 05 Overloading  →  Topic 09 static and final
        ↓
Topic 10 Access Modifiers                <-- you are here
        ↓
Topic 11 Object class and object contracts (the members you expose are part of the contract)
        ↓
Interfaces & abstract classes (visibility of the contract) → Packages & JPMS
        ↓
API design, library evolution, module boundaries, framework integration (Spring/JPA)
```
Everything learned so far is now controlled by visibility: encapsulation (private fields + behaviour),
extension points (protected + final), shared state (static) and constant placement (`static final`) are all
access-modifier decisions.

## 20. One-Minute Revision
- `private` → package-private → `protected` → `public`; no modifier means package-private.
- Top-level types are `public` or package-private only.
- Same package sees package-private + protected; other packages see protected only through a subclass-typed
  reference.
- Overrides widen visibility, never narrow it.
- Enforced by `javac`, then the JVM at link time, then JPMS exports.
- Small public surface = cheap refactoring; package-private is a feature, not a limitation.





