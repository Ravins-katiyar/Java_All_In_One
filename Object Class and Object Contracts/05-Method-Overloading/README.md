# Method Overloading

## 1. Main Idea
**Overloading** means declaring several methods with the **same name** but **different
parameter lists** in the same class (or in a class hierarchy). It exists so that one concept
can be expressed with one name (`print`, `send`, `of`) while accepting different inputs. The
compiler chooses the version to call at **compile time**, using only the **static (declared)
types** of the arguments — never the runtime types. This is called **static resolution** or
*early binding*. Because the choice is made by the compiler, overloading has nothing to do
with polymorphism: a `Parent`-typed reference calling `m("x")` will pick `Parent`'s overload
even when the object is a `Child`. The compiler tries three phases in order, and picks the
**most specific** applicable method within the earliest phase that produced any match.

## 2. Why Does This Exist?
- **One intuitive name** for one concept: `Math.max(int,int)`, `max(long,long)`,
  `max(double,double)` — callers do not need `maxInt`, `maxLong`, `maxDouble`.
- **Backward compatibility**: `System.out.println` has ~10 overloads so adding a new type
  does not break existing code. (`System.out` is a `PrintStream`.)
- **Convenience defaults**: `substring(int)` and `substring(int, int)`.
- **API ergonomics**: `List.of()`, `List.of(e1)`, `List.of(e1, e2)`, `List.of(e1, e2, e3)`,
  `List.of(E...)`.
- **Boxing bridge**: `f(int)` and `f(Integer)` can coexist so both primitives and wrappers work.

## 3. Prerequisites
- Topic 04 Methods — especially that the **signature = name + parameter types**, and that the
  return type is *not* part of it.
- Topic 02/03 (inheritance and `super` are needed for the "overloads across a hierarchy" case).
- Widening vs boxing conversions from `Java Fundamentals/02-Variables-Data-Types`.

## 4. Core Concepts

### The Three Phases Of Overload Resolution (JLS 15.12.2)
The compiler searches in this order and **stops at the first phase that finds a match**:

| Phase | Name | Conversions allowed | Typical outcome |
|---|---|---|---|
| 1 | Strict invocation | widening primitive, widening reference, **no** boxing, **no** varargs | `int` arg → `f(long)` |
| 2 | Loose invocation | **+ boxing/unboxing** (still no varargs) | `int` arg → `f(Object)` / `f(Integer)` |
| 3 | Variable arity | **+ varargs** (`T...`) | `int` arg → `f(int...)` |

Verified behaviour:
```java
f(Object o)  vs  f(int... v)   -> f(1)      calls f(Object)    // phase 2 beats phase 3
f(long l)    vs  f(Integer i)  -> f(1)      calls f(long)      // phase 1 beats phase 2
f(int,int)   vs  f(int... v)   -> f(1, 2)   calls f(int,int)   // fixed arity beats varargs
f(int a)     vs  f(int... v)   -> f(1)      calls f(int)       // same reason
```

### Applicability and "Most Specific"
Within the chosen phase, several methods may be applicable. The compiler then selects the
**most specific** one: every parameter type of the winner must be a subtype of (or convertible
to without loss over) the corresponding parameter of the loser.

Verified behaviour:
```java
f(String)  vs  f(Object)   -> f("x")   calls f(String)   // String is more specific
```
If no method is more specific than the others, the call is **ambiguous** → compile error.

### Static Type Decides, Not the Runtime Type
```java
class Parent { void m(Object o) { System.out.println("Parent"); } }
class Child extends Parent { void m(String s) { System.out.println("Child"); } }

Parent p = new Child();
p.m("x");     // prints "Parent"  <-- compile-time choice from the STATIC type
((Child) p).m("x");   // prints "Child"
```
This is the single most important overloading fact. Overloading and overriding are *opposites*:
overriding uses the runtime type, overloading uses the static type.

### What Cannot Be Used For Overloading
| Attempt | Result |
|---|---|
| Change only the **return type** | compile error — `method is already defined` |
| Change only the **`throws`** clause | compile error — `method is already defined` |
| Change only a **parameter name** | compile error — same signature |
| Change only **`static`/`final`/visibility** | compile error — same signature |
| Overload using **generics** with the same erasure | compile error — `have the same erasure` |

Verified: `f(List<String>)` and `f(List<Integer>)` both erase to `f(List)`:
```
error: name clash: f(List<Integer>) and f(List<String>) have the same erasure
```

### Overloads Across A Hierarchy
Overload sets are **not** merged in the way overriding is. A subclass's `m(String)` does not
remove the inherited `m(Object)`; both remain candidates, but the choice uses the **static type
of the receiver**, so a parent-typed reference cannot see the child's narrower overload.

### Overloading And `null`
`null` is assignable to every reference type, so it frequently creates ambiguity:
```java
f(String s) vs f(Integer i)   ->  f(null)  // ambiguous -> compile error
f(String s) vs f(Object o)    ->  f(null)  // f(String): most specific
```

### Overloading Is Decided At Compile Time Only
The compiled bytecode contains the **exact target method** in the constant pool
(e.g. `invokevirtual f:(Ljava/lang/String;)V`). There is no runtime "overload lookup", no
performance cost, and no way to add an overload at runtime.
<!-- c5-s4-end -->

## 5. Syntax
```java
public class Printer {

    // Same name, different parameter lists = legal overloads
    public void print(String text)                 { System.out.println("[text] " + text); }
    public void print(int number)                  { System.out.println("[int]  " + number); }
    public void print(String text, int copies)     { System.out.println("[both] " + text + " x" + copies); }
    public void print(String text, int... extras)  { System.out.println("[var]  " + text + extras.length); }

    // Widening ladder: int -> long -> double is preferred over boxing to Integer
    public void store(long value)    { }
    public void store(Double value)  { }

    // Boxed overload for nullability
    public void send(Integer amount) { }
    public void send(String amount)  { }
}
```
Warning: `print(String, int)` and `print(String, int...)` are legal together, but a call like
`print("a", 1)` prefers the fixed-arity version — a common source of surprising behaviour.

## 6. Simple Example
See `01-OverloadResolution/OverloadResolutionDemo.java`, which prints which overload the compiler
chose for each of the verified cases above, and `02-StaticTypeAndTraps/StaticTypeAndTrapsDemo.java`
for the static-type trap, `null` ambiguity notes, and the `equals(MyType)` disaster.

## 7. Real-Life Analogy
Overloading is a **hotel front desk that only has one phrase: "Do you have a reservation?"** The
clerk can serve "a name", "a name plus a company", "a confirmation number", or "nothing at all"
(the walk-in case), and answers appropriately each time. The guest never learns extra vocabulary.
Overload resolution is you picking the fastest queue that accepts your particular case — and the
queue is chosen by what you are *holding at the door* (the static type), not by who you really are
(the runtime type).

## 8. Real Backend Example
Framework APIs are built on overloads; so are the bugs around them.

```java
@Repository
public class AccountJdbcRepository {

    // Intuitive single name for a look-up, with three shapes of input.
    public Optional<Account> find(String accountId)                     { return find(accountId, false); }
    public Optional<Account> find(String accountId, boolean forUpdate)   { /* SQL ... */ }
    public List<Account>     find(List<String> accountIds)               { /* IN (...) */ }

    public void save(Account account)                     { }
    public void save(List<Account> accounts)              { }   // batch
}
```

```java
// Spring's own API design, for reference
String body = restTemplate.getForObject(url, String.class);
List<Account> all = jdbcTemplate.query("select * from accounts", rowMapper);
```

**The production bug that overloading causes:**
```java
public class Account {

    private final String id;

    public Account(String id) { this.id = id; }

    // WRONG: this OVERLOADS Object.equals instead of OVERRIDING it.
    public boolean equals(Account other) {
        return other != null && id.equals(other.id);
    }
    // hashCode() not overridden either.

    // ...
}

Set<Account> accounts = new HashSet<>();
accounts.add(new Account("A-1"));
System.out.println(accounts.contains(new Account("A-1")));   // false!
```
`HashSet`/`HashMap` call `equals(Object)` through the `Object` contract. Because the class declared
`equals(Account)` — a *different* signature — `Object.equals` is still the identity version, so
lookups fail. **Always write `public boolean equals(Object o)` and annotate it `@Override`.** The
compiler would have rejected `@Override` on `equals(Account)`, which is exactly why `@Override`
should be used everywhere (topic 11 covers the full contract).

A second real trap: an overloaded setter that shadows the intended one.
```java
void update(long status)   { /* fine */ }
void update(String status) { /* fine */ }
update(1);          // picks update(long)  -> maybe you meant a status code
update(1L);         // same, explicit
update("1");        // picks update(String)
```
Ambiguity appears as soon as two overloads are equally specific, e.g. `find(long)` vs
`find(String)` with `find(null)`.

## 9. Internal Working
- Overload resolution is a **compile-time** algorithm (JLS 15.12). The class file constant pool
  records the **exact target**: `Methodref AccountJdbcRepository.find:(Ljava/lang/String;)Ljava/util/Optional;`.
- Nothing about the chosen overload survives at runtime except that resolved reference, so there is
  **no runtime search cost** and **no way to extend the overload set dynamically**.
- Because the static type decides, `javac` uses the *declared* type of the receiver and the
  *declared* types of the arguments, exactly as written at the call site.
- A consequence for error messages: `error: reference to f is ambiguous` names both candidates;
  `error: no suitable method found` lists what was considered. Read those messages — they state the
  phase reasoning that failed.
- If you need choice at runtime, you need **overriding** (virtual dispatch) or a
  `switch`/`instanceof` pattern — not overloading.
- Bridge methods exist for *generics/inheritance* (covariant overrides), **not** for overloading:
  overloads produce separate methods with separate descriptors.
```
Compile time                          Runtime class file
f("x")  ---> f(String)   ---------->  invokevirtual Printer.print:(Ljava/lang/String;)V
f(1)    ---> f(int)      ---------->  invokevirtual Printer.print:(I)V
                                        (fixed for ever; no lookup)
```
<!-- c5-s9-end -->

## 10. Important Rules
1. Overloads must differ in the **parameter list** (types, count, order) — nothing else counts.
2. **Return type, `throws`, parameter names, modifiers and type-parameter names are not part of the
   signature**, so changing only those is a compile error.
3. Two methods that erase to the same signature clash (`f(List<String>)` vs `f(List<Integer>)`).
4. Resolution order: **phase 1 strict → phase 2 loose (boxing) → phase 3 varargs**, stopping at the
   first phase with a match.
5. Within a phase, the **most specific** applicable method wins; otherwise the call is **ambiguous**.
6. The **static type** of the receiver and arguments decides — never the runtime type.
7. Overload resolution happens entirely at **compile time**; it is not polymorphism.
8. Prefer widening over boxing when both are possible (phase 1 before phase 2).
9. Avoid overloading across nullable reference types with unrelated types (`String` vs `Integer`) —
   `f(null)` becomes ambiguous.
10. Never "overload" `equals`, `hashCode`, `compareTo` or `toString` by mistake — those are
    **contracts** and must be overridden with the exact parameter types (topic 11).

## 11. Common Mistakes
- **The `equals(MyType)` disaster** (the classic):
  ```java
  public boolean equals(Account other) { ... }   // OVERLOAD: collections never call it
  ```
  Fix: `public boolean equals(Object other)` with `@Override` — the annotation would have caught it.
- **Expecting the runtime type to pick the overload**:
  ```java
  Parent p = new Child();
  p.m("x");                       // Parent's overload; NOT Child's
  ```
  Fix: use overriding (same signature) or explicit dispatch.
- **Changing only the return type** → `error: method send(String) is already defined`.
- **Overloading with two unrelated reference types and passing `null`**:
  ```
  error: reference to find is ambiguous
    both method find(String) in NullChooser and method find(Integer) in NullChooser match
  ```
- **The two-parameter swap trap** — verified ambiguous:
  ```
  error: reference to f is ambiguous
    both method f(int,long) in Amb1 and method f(long,int) in Amb1 match
  ```
- **Erasure clash** — verified error:
  ```
  error: name clash: f(List<Integer>) and f(List<String>) have the same erasure
  ```
- **Accidentally creating an overload instead of an override**: dropping a parameter
  (`update(long)` vs `update(long, boolean)`) looks similar at the call site but compiles to a
  different method; `@Override` cannot be used, which is the only signal you get.
- **Overloading to simulate optional parameters** (`m()`, `m(int)`, `m(int,String)`, ...) — a Builder
  or named factory is clearer once the count grows.
- **Assuming `f(int...)` will be chosen** when a boxing-friendly `f(Object)` exists — it will not
  (phase 2 beats phase 3).

## 12. Comparison

| Aspect | Overloading | Overriding |
|---|---|---|
| Where | Same class / inherited overload set | Subclass |
| Requirement | Different parameter list | Same signature, compatible return |
| Chosen at | **Compile time** (static types) | **Runtime** (runtime type) |
| Method name | Same | Same |
| Return type | Free to differ | Must be the same or covariant |
| Polymorphic | No | Yes |
| `@Override` | Not applicable | Should always be used |
| Bytecode | Exact target in the constant pool | `invokevirtual`/`invokeinterface` |

| Aspect | Widening conversion (phase 1) | Boxing conversion (phase 2) |
|---|---|---|
| Example | `int` → `long` | `int` → `Integer` |
| Preferred? | Yes — wins over boxing | Only if no widening match exists |
| Cost | None (numeric conversion) | May allocate (outside the Integer cache) |
| Combined form | — | Boxing + widening reference (`int` → `Object`) |

| Aspect | Fixed arity | Varargs |
|---|---|---|
| Phase | 1 or 2 | 3 (last resort) |
| Overhead | None | Allocates an array at the call site |
| Signature clash | `f(int,int)` and `f(int...)` can coexist | — |

## 13. Code Examples
- **Beginner/Intermediate** — `01-OverloadResolution/OverloadResolutionDemo.java`: prints which
  overload wins for each verified rule (widening vs boxing, boxing vs varargs, most specific,
  fixed arity vs varargs, parameter order).
- **Advanced** — `02-StaticTypeAndTraps/StaticTypeAndTrapsDemo.java`: the static-type trap, the
  `equals(MyType)` disaster demonstrated with `HashSet`, the corrected `equals(Object)` +
  `hashCode()` version, and `null` overload selection with the ambiguity case documented.
<!-- c5-s13-end -->

## 14. Practice Questions
1. Can two methods differ only in return type? Why or why not?
2. Given `f(long)` and `f(Integer)`, which is called by `f(1)` and which resolution phase is used?
3. Given `f(Object)` and `f(int...)`, which is called by `f(1)`?
4. Given `f(String)` and `f(Object)`, which is called by `f("x")`? And by `f(null)`?
5. Given `f(String)` and `f(Integer)`, what happens with `f(null)`?
6. What does this print, and why is it *not* polymorphism?
   ```java
   class P { void m(Object o) { System.out.println("P"); } }
   class C extends P { void m(String s) { System.out.println("C"); } }
   P p = new C(); p.m("x");
   ```
7. Why do `f(List<String>)` and `f(List<Integer>)` not compile together?
8. What happens with `f(int, long)` and `f(long, int)` when called as `f(1, 2)`?
9. Why does `Set.contains(new Account("A-1"))` return `false` when the class declares
   `boolean equals(Account other)`?
10. Is overload resolution performed at compile time or runtime? What appears in the class file?

### Answers
1. No. The signature is name + parameter types; the return type is not part of it, so this is a
   duplicate method: `method is already defined`.
2. `f(long)` — widening is a **phase 1 (strict)** conversion; boxing to `Integer` is phase 2.
3. `f(Object)` — boxing plus widening reference is **phase 2**, which is tried before
   **phase 3 (varargs)**.
4. `f(String)` for `"x"` (most specific). `f(null)` also picks `f(String)` because it is more
   specific than `Object`.
5. Compile error: `reference to f is ambiguous` — `String` and `Integer` are unrelated.
6. Prints `P`. The overload is chosen from the **static** type of the receiver (`P`), not the runtime
   type; it is overloading, so no virtual dispatch happens.
7. Both erase to `f(List)` — `name clash ... have the same erasure`.
8. Compile error: `reference to f is ambiguous`; neither is more specific than the other.
9. `HashSet` compares through `equals(Object)`. The declared `equals(Account)` is an **overload**, so
   `Object.equals` (identity) is still used and the new object never matches.
10. Compile time. The class file constant pool contains the exact `Methodref`
    (e.g. `f:(I)V`), so there is no runtime lookup.

## 15. Coding Practice
**Easy** — Write a `Converter` with overloads `toCelsius(double f)`, `toCelsius(int f)` and a
`toCelsius(String f)` that parses text. Call each and print which one ran.
**Medium** — Write an `ApiLogger` with `info(String)`, `info(String, Object...)` and
`info(Throwable)`. Log from all three and explain which overload a call `info(msg, ex)` picks when
`ex` is a `Throwable` (careful: `Object...` vs `Throwable`).
**Hard** — Reproduce the `equals(MyType)` bug in a small program: put two "equal" objects into a
`HashSet`, print `contains`, `size`; then fix the class with a correct `equals(Object)` +
`hashCode()` pair annotated `@Override`, and show that the same test now passes. Add a third
`equals(Account, boolean)`-style overload and explain why it changes nothing for the collections.
<!-- c5-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What is method overloading?**
- *Testing*: definition plus the key constraint.
- *Expected*: same name, different parameter lists, in the same class or hierarchy; chosen at compile time.
- *Wrong*: "Two methods with the same name in a parent and child."

**Q2. Can you overload by changing only the return type?**
- *Testing*: the signature rule.
- *Expected*: No — the return type is not part of the signature; it is a duplicate definition.
- *Wrong*: "Yes, if the assignment context makes it clear."

**Q3. Is overloading resolved at compile time or runtime?**
- *Testing*: static vs dynamic binding.
- *Expected*: Compile time, from the static types of the receiver and arguments; the bytecode holds the exact target.
- *Wrong*: "At runtime, based on the actual argument types."

**Q4. What are the three phases of overload resolution?**
- *Testing*: depth beyond "the compiler figures it out".
- *Expected*: phase 1 strict (widening, no boxing/varargs) → phase 2 loose (boxing) → phase 3 varargs.
- *Wrong*: "It picks the first one that compiles."

**Q5. Give a real example of overloading in the JDK.**
- *Testing*: practical awareness.
- *Expected*: `System.out.println(...)` with ~10 overloads; `Math.max`; `String.substring(int)` and `substring(int,int)`; `List.of(...)`.
- *Wrong*: "Overriding in `Object`."

### Intermediate
**Q6. `f(long)` and `f(Integer)` both exist. What does `f(1)` call, and why?**
- *Testing*: phase ordering.
- *Expected*: `f(long)` — widening (`int`→`long`) is phase 1; boxing to `Integer` is phase 2.
- *Wrong*: "`f(Integer)`, because autoboxing is used."

**Q7. `f(Object)` and `f(int...)` both exist. What does `f(1)` call?**
- *Testing*: boxing vs varargs precedence.
- *Expected*: `f(Object)` — boxing plus widening reference is phase 2, tried before phase 3 varargs.
- *Wrong*: "`f(int...)`, because it matches exactly."

**Q8. `f(String)` and `f(Object)` exist, and you call `f(null)`. Which runs?**
- *Testing*: most-specific rule with `null`.
- *Expected*: `f(String)` — both are applicable, `String` is more specific. (With `f(Integer)` instead, it would be ambiguous.)
- *Wrong*: "Compile error."

**Q9. Why does a `Parent`-typed reference not reach a `Child`-only overload?**
- *Testing*: static vs runtime type.
- *Expected*: Overloads are chosen from the static type of the receiver; the child's method is not in the parent-typed receiver's overload set.
- *Wrong*: "Because the child's method is private."

**Q10. Two overloads `f(int, long)` and `f(long, int)` — what happens with `f(1, 2)`?**
- *Testing*: most-specific comparison.
- *Expected*: `reference to f is ambiguous` — each is more specific in one position and less in the other.
- *Wrong*: "It picks the first declared."
<!-- c5-s16a-end -->

### Advanced
**Q11. How does overloading interact with generics and erasure?**
- *Testing*: generics knowledge.
- *Expected*: You cannot overload methods whose parameter lists differ only in type arguments (`f(List<String>)` vs `f(List<Integer>)`) — they have the same erasure. Also, overloads can produce surprising results with generic methods and inference, so prefer distinct names or bounds.
- *Wrong*: "Generics make overloads work perfectly."

**Q12. Why is `equals(MyType)` dangerous, and how does `@Override` prevent it?**
- *Testing*: the most common real overloading bug (bridges to topic 11).
- *Expected*: `equals(MyType)` is an overload, so `Object.equals` (identity) remains in force; `HashMap`/`HashSet`/`List.contains` silently fail. `@Override` on it fails compilation because it does not override anything.
- *Wrong*: "It works because the compiler picks the closest match."

**Q13. Why can overloading make an API harder to evolve?**
- *Testing*: API design judgement.
- *Expected*: Adding an overload can silently change which method an existing call resolves to (e.g. adding `f(String)` starts capturing calls that previously went to `f(Object)`), and it can introduce ambiguity for `null` or lambda arguments. This is a source/binary behavioural change with no compile error.
- *Wrong*: "Adding overloads is always safe."

**Q14. Overloads combined with lambdas: what is the risk?**
- *Testing*: functional-interface inference knowledge.
- *Expected*: Multiple functional-interface parameter overloads make a lambda argument ambiguous, or `Runnable` vs `Callable` style overloads force an explicit cast; the inferred type can also pick an unintended overload. Often a distinct method name is the fix.
- *Wrong*: "Lambdas make overloading easier."

**Q15. How would you implement runtime "overload-like" behaviour when the static type is not enough?**
- *Testing*: design patterns connected to dispatch.
- *Expected*: Use a polymorphic method on the domain types (double dispatch / visitor pattern), a `switch` on sealed types with pattern matching, or a strategy/factory registry — not overloading. Overloading cannot become runtime dispatch.
- *Wrong*: "Reflection is the normal solution."

## 17. Production-Level Questions
1. **Silent cache misses:** a Spring cache keyed by a value object never hits. `equals` is declared as
   `equals(MyType)`. Explain the exact call path that fails, and how you would detect the bug in a test.
2. **Overload added in a patch release:** a library adds `find(String)`. Existing code called
   `find(Object)` and now resolves differently. Was that a breaking change without a compile error, and
   how should the library have released it?
3. **Ambiguity from a framework:** your repository has `findById(String)` and `findById(long)`; a call
   passes an `Integer` and you get surprising results (or an ambiguity). What is the design fix?
4. **Performance:** an overload set has `log(String)` and `log(String, Object...)`. Which is cheaper and
   why does it matter on a hot logging path (varargs array allocation, `toString` cost)?
5. **API review:** a team overloads `update(long)`, `update(String)`, `update(boolean)`. What debugging
   problem does this create, and how would you redesign it so intent is visible at the call site?
<!-- c5-s17-end -->

## 18. What I Should Remember
1. Overloading = same name, different parameter lists. Chosen at **compile time** from **static types**.
2. Return type, `throws`, parameter names and modifiers are **not** part of the signature.
3. Resolution order: strict (widening) → loose (boxing) → varargs.
4. Within a phase, the **most specific** method wins; otherwise `reference to f is ambiguous`.
5. Erasure blocks overloading by type argument (`f(List<String>)` vs `f(List<Integer>)`).
6. A parent-typed reference cannot reach a child-only overload.
7. `equals(MyType)` is an overload, not an override — the #1 production overloading bug.
8. Overloading is *not* polymorphism; overriding uses the runtime type.

## 19. Connection To Other Java Topics
```
Topic 04 Methods (signature = name + parameter types)
        ↓
Topic 05 Method Overloading           <-- you are here
        ↓
Topic 06 Varargs (the phase-3 "last resort" overload)
        ↓
Topic 11 Object contract (equals/hashCode MUST be overridden, never overloaded)
        ↓
Generics & erasure (why type-argument overloading is impossible)
        ↓
Collections/HashMap internals (which call equals(Object) and hashCode())
```
This topic is the reason `HashMap` behaviour is invisible until it breaks: everything depends on
`equals(Object)` being an **override**, i.e. on the signature being exactly right.

## 20. One-Minute Revision
- Same name, different parameters; decided by the compiler, not at runtime.
- Phases: widening → boxing → varargs; fixed arity beats varargs; most specific wins.
- Static type of the receiver/arguments is all that matters.
- Return type/`throws` cannot make an overload; erasure can block one.
- Child-only overloads are invisible through a parent reference.
- Always `@Override` on `equals`, `hashCode`, `toString` — or you will overload by accident.



