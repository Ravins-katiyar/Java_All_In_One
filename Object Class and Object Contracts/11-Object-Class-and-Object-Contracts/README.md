# Object Class and Object Contracts

## 1. Main Idea
`java.lang.Object` is the **root of every class hierarchy**. Every type you write extends it, implicitly
(`class X { }` is `class X extends Object { }`) or indirectly through a superclass. Because of that, every
object in a Java program shares the same base API and — more importantly — inherits the same **contracts**:
`equals`, `hashCode`, `toString`, `getClass`, `clone`, `finalize`, and the monitor methods
`wait`/`notify`/`notifyAll`. Those contracts are not suggestions; they are documented behavioural
requirements (the JLS and the `Object` Javadoc) that library code depends on — above all `HashMap`,
`HashSet`, `record`, JPA, Spring and every caching layer. Two matter more than all the others:
**`equals` must define logical equality**, and **`hashCode` must agree with `equals`**. Everything in this
module (objects, constructors, methods, overloading, pass-by-value, `static`/`final`, access modifiers)
exists so you can implement those two methods correctly.

## 2. Why Does This Exist?
- **A universal base type** so any object can be stored, printed, compared, cloned and locked generically
  (`Object[] args`, `List<Object>`, `synchronized (lock)`).
- **Hash-based collections** (`HashMap`, `HashSet`, `ConcurrentHashMap`) *require* a working
  `equals`/`hashCode` pair. Without it, lookups silently fail — the most common Java correctness bug.
- **Deduplication and identity semantics** (`Set`, `distinct()`, cache keys, `Map` keys).
- **Diagnosability** — `toString()` is what you read in logs and debuggers; the default
  `ClassName@1b6d3586` is nearly useless.
- **Value semantics** for DTOs, money, IDs and coordinates — "two objects with the same data are the same
  thing" — which Java 16+ `record` generates for free.
- **Framework integration**: Hibernate dirty checking, Spring caches, JSON serialisation, test assertions
  (`assertEquals`) and `ConcurrentHashMap` keys all rely on these contracts.

## 3. Prerequisites
This is the **culmination** topic of the module; every earlier topic feeds it.
- Topic 01 Classes and Objects — instance fields; **what "equal" should compare**.
- Topic 02 Constructors — invariants established at construction (immutability makes the contract easy).
- Topic 03 `this` and `super` — `super.equals(...)` in a subclass override; `invokevirtual`.
- Topic 04 Methods — `equals`, `hashCode`, `toString` are ordinary overridable methods; use `@Override`.
- Topic 05 Method Overloading — `equals(MyType)` **overloads** instead of overriding `equals(Object)`.
- Topic 07 Pass-by-Value — `==` compares references; mutation vs rebinding.
- Topic 09 `static` and `final` — `final` fields give immutability and safe publication (JMM).
- Topic 10 Access Modifiers — the overrides must stay `public`.
- `Java Fundamentals/02-Variables-Data-Types` — `==` vs `.equals()` for `String`/wrappers.
<!-- c11-s3-end -->

## 4. Core Concepts

### The Members Of `Object` (verified from the JDK 21 runtime with `javap -p java.lang.Object`)
| Method | Signature | Default behaviour | Override? |
|---|---|---|---|
| `getClass` | `public final native Class<?> getClass()` | Runtime class from the object header | **No** — `final` |
| `hashCode` | `public native int hashCode()` | Identity-derived hash value | **Yes** — whenever you override `equals` |
| `equals` | `public boolean equals(Object obj)` | Reference (identity) comparison | **Yes** for value types |
| `toString` | `public String toString()` | `getClass().getName() + "@" + Integer.toHexString(hashCode())` | Yes — for logs/debugging |
| `clone` | `protected native Object clone() throws CloneNotSupportedException` | Shallow field-by-field copy; requires `Cloneable` | Rarely; prefer a copy constructor |
| `notify` / `notifyAll` | `public final native void notify()` | Wakes one/all waiter(s) on this monitor | No — `final` |
| `wait` | `public final void wait()` / `wait(long)` / `wait(long,int)` | Waits on this monitor; throws `InterruptedException` | No — `final` |
| `finalize` | `protected void finalize() throws Throwable` | Ran before GC; **deprecated for removal since Java 18** | No — use `Cleaner`/try-with-resources |

`Object` has **no** `compareTo` (that is `Comparable`), no `size`, no `id`, no `builder`. Everything else in
Java's object protocol is added by interfaces or subclasses.

### The `equals` Contract (five rules)
| Rule | Statement | How it breaks in practice |
|---|---|---|
| Reflexive | `x.equals(x)` is `true` | Comparing a field that changes between calls |
| **Symmetric** | `x.equals(y) == y.equals(x)` | Parent uses `instanceof`, child uses `getClass()` |
| **Transitive** | `x=y` and `y=z` ⇒ `x=z` | Mixing `instanceof` in a hierarchy that adds fields |
| Consistent | Repeated calls return the same result while state is unchanged | Comparing `System.currentTimeMillis()` or a mutable counter |
| Non-null | `x.equals(null)` is `false` and never throws | Calling a method on the parameter with no null/type guard |

Verified asymmetry (parent uses `instanceof`, child uses `instanceof`):
```
sub.equals(base)  : false
base.equals(sub)  : true    <- ASYMMETRIC: violates the contract
```

### The `hashCode` Contract (three rules)
| Rule | Statement | Consequence if broken |
|---|---|---|
| Consistent | Same result within one execution while equals-relevant state is unchanged | The bucket changes underneath the entry |
| **Agreement** | `x.equals(y)` ⇒ `x.hashCode() == y.hashCode()` | `HashMap`/`HashSet` lookups silently fail |
| Collisions allowed | Unequal objects *may* share a hash code | Only performance suffers, not correctness |

The agreement is **one-directional**: equal objects must share a hash code, but equal hash codes do **not**
imply equality — so `HashMap` always confirms a candidate with `equals` after locating the bucket.

### How Hash-Based Collections Use The Contract
```
map.put(key, value):
  1. h = key.hashCode(), spread with (h ^ (h >>> 16)), index = (n - 1) & hash
  2. walk that bucket: for each entry, check (k == key) || key.equals(k)
  3. replace the value on an equals match, otherwise append a node

map.get(key):
  1. recompute the bucket from the CURRENT hashCode()
  2. repeat the same (k == key) || key.equals(k) check
```
Two consequences you can predict without reading `HashMap` source:
- **Equals without hashCode** → the objects hash to *different* buckets, so `get` never even checks them:
  `map.get(equalKey)` returns `null` although `equals` returns `true`.
- **Mutating a key** → the bucket is computed from the *new* hash code, so the entry becomes unreachable
  from its own key (and `size()` still counts it).
<!-- c11-s4b-end -->

### `getClass()` — Final, Identity, No Override
```java
Object declared = new ArrayList<String>();
declared.getClass();   // class java.util.ArrayList  (the RUNTIME class)
```
- Recovered from the object header, so it always returns the real runtime class.
- `final`, so it cannot be overridden — which makes it reliable inside `equals`:
  `other != null && getClass() == other.getClass()`.
- Use `getClass() == ...` for **strict** equality and `instanceof` for **hierarchy-tolerant** equality; the
  choice decides whether your `equals` is symmetric across subclasses (see section 12).

### `toString()` — The Debugging Contract
- Default output: `getClass().getName() + "@" + Integer.toHexString(hashCode())`
  (verified: `Plain@6e0be858`).
- Rule of thumb: include the type and the fields that identify the object; **exclude** secrets, PII,
  passwords, tokens and huge collections.
- Records generate a useful `toString()` automatically (`Money[minorUnits=1250, currency=EUR]`).
- Logging frameworks call `toString()` lazily; a `toString()` that throws is a production incident, so keep
  it null-safe and side-effect free.

### `clone()` — Shallow, Fragile, Rarely The Right Answer
```java
class Tags implements Cloneable {
    int[] values;
    @Override public Object clone() throws CloneNotSupportedException {
        return super.clone();      // SHALLOW: the array reference is copied, not the array
    }
}
```
- `clone()` is `protected` and `native`. It performs a **field-by-field (shallow) copy**.
- It requires the `Cloneable` marker interface, otherwise it throws `CloneNotSupportedException`
  (verified). `Cloneable` itself declares no methods — a well-known design wart.
- Nested mutable state is **shared** after cloning (verified: `copy.values == original.values` is `true`), so
  a "copy" can corrupt the original.
- Preferred alternatives: a **copy constructor** (topic 02), a `static` factory (`Money.of(...)`), a
  `record` (immutable by construction), or serialisation-based deep copy for graphs.
- Never call `clone()` on a type you did not write the `clone()` for.

### `finalize()` And `wait`/`notify`
- `finalize()` is **deprecated for removal since Java 18** and should never be used for resource cleanup:
  execution is not guaranteed, timing is unpredictable, and it can resurrect objects. Use
  `try-with-resources` (`AutoCloseable`) or `java.lang.ref.Cleaner`.
- `wait()`/`notify()`/`notifyAll()` implement the monitor that underlies `synchronized`. They are `final`,
  require the calling thread to **own the monitor** (verified: calling `wait` outside `synchronized` throws
  `IllegalMonitorStateException`), and `wait` can wake up spuriously — always wait in a loop. Prefer
  `java.util.concurrent` (`BlockingQueue`, `CountDownLatch`, `Condition`) in modern code.

### `record` — The Contract Generated For You (Java 16+)
```java
record Money(long minorUnits, String currency) {
    Money {                                          // compact constructor: validate/normalise
        if (minorUnits < 0) throw new IllegalArgumentException("negative");
        currency = currency.toUpperCase();
    }
}
```
- The compiler generates the canonical constructor, accessors, and `equals`/`hashCode`/`toString` based on
  **all** components (verified: `HashSet` deduplicates two equal records).
- Verified with `javap -v`: the generated methods use `invokedynamic` bound to
  `java.lang.runtime.ObjectMethods.bootstrap`, so the semantics are the documented ones.
- A record's superclass is `java.lang.Record` and it is `final`; components are `private final`.
- **Gotcha**: an **array** component uses reference equality, because records compare components with
  `Object.equals`. Two records holding equal-but-distinct arrays are **not** equal (verified). Use a
  `List` component or write your own `equals`.

### `java.util.Objects` — Null-Safe Helpers (Java 7+)
| Helper | Use |
|---|---|
| `Objects.equals(a, b)` | Null-safe content comparison; `true` for two `null`s |
| `Objects.hashCode(o)` | `0` for `null`, otherwise `o.hashCode()` |
| `Objects.hash(a, b, c)` | Convenient varargs hash (allocates an array — fine outside hot loops) |
| `Objects.requireNonNull(x, "msg")` | Fail fast in constructors with a meaningful message |
| `Objects.toString(o, "default")` | Null-safe `toString` with a fallback |
| `Objects.checkIndex(i, len)` | Bounds check that produces a proper exception (Java 9+) |

### Identity vs Equality
```java
String a = "account";
String b = new String("account");
a == b              // false  -> two different objects
a.equals(b)         // true   -> same characters
a == b.intern()     // true   -> the pooled instance
```
`==` answers "is this the same object?"; `equals` answers "are these the same value?".
Databases use identity (primary keys); domain models usually use value equality; entity classes often need
identity equality (see section 8).
<!-- c11-s4c-end -->

## 5. Syntax
```java
import java.util.Objects;

public final class AccountId {                       // final + private final fields = immutable

    private final String value;

    public AccountId(String value) {
        this.value = Objects.requireNonNull(value, "value is required").trim().toUpperCase();
    }

    public String value() { return value; }

    // 1) equals(Object): the exact signature. @Override proves it is an override (topic 05).
    @Override
    public boolean equals(Object other) {
        if (this == other) {                         // reflexivity + fast path
            return true;
        }
        if (!(other instanceof AccountId that)) {    // handles null and any other type
            return false;
        }
        return value.equals(that.value);             // compare the fields that define identity
    }

    // 2) hashCode(): must agree with equals. 31 is the classic multiplier for hand-written hashes.
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    // 3) toString(): useful, safe to log, no secrets.
    @Override
    public String toString() {
        return "AccountId[" + value + "]";
    }
}
```
Multi-field manual hash (the canonical shape, used by `Objects.hash` and IDE generators):
```java
@Override
public int hashCode() {
    int result = 31 + accountId.hashCode();
    result = 31 * result + Long.hashCode(minorUnits);
    result = 31 * result + currency.hashCode();
    return result;
}
// equivalent, shorter, allocates an array:
@Override
public int hashCode() {
    return Objects.hash(accountId, minorUnits, currency);
}
```
And the Java 16+ shortcut for the whole contract:
```java
public record AccountId(String value) {
    public AccountId {
        value = Objects.requireNonNull(value, "value is required").trim().toUpperCase();
    }
}
```

## 6. Simple Example
Three runnable examples, all verified on JDK 21:
- `01-ObjectClassMethods/ObjectClassMethodsDemo.java` — the defaults of `Object` (`equals` identity,
  identity-based `hashCode`, `ClassName@hash` `toString`, `final getClass`), `java.util.Objects` helpers,
  shallow `clone()` and `CloneNotSupportedException`, and `wait()` without a monitor.
- `02-EqualsHashCodeContract/EqualsHashCodeContractDemo.java` — equals-without-hashCode breaking `HashMap`
  and `HashSet`, the correct pair, the mutable-key bug, the `instanceof` asymmetry, the strict `getClass()`
  fix, and `==` vs `equals` for `String`.
- `03-RecordsAndContracts/RecordsContractDemo.java` — the generated record contract, `record` vs a plain
  class in a `HashSet`, `java.lang.Record` as the superclass, and the array-component gotcha.

## 7. Real-Life Analogy
The `equals`/`hashCode` contract is a **coat check**:
- `hashCode()` is the **hanger number** the clerk writes on your ticket. It is only a *grouping*, not an
  identity — two coats can share hanger number 7 (a collision), which is allowed; it just makes the clerk
  check a few coats.
- `equals()` is the clerk **comparing the coat you described** with the one on the hanger.
- The contract says: *if two coats are the same coat, they must get the same hanger number.* Break that and
  the clerk looks in the wrong row and reports "no such coat" even while your coat hangs in the building.
- And just like a ticket, a key that changes after being filed (mutating a field used in `hashCode`) is
  unreachable — the clerk now looks in a row that no longer contains your item.
<!-- c11-s7-end -->

## 8. Real Backend Example

**1. Value object (DTO / money / ID) — value equality, generated or hand-written**
```java
public record Money(long minorUnits, String currency) {
    public Money {                                   // validate + normalise once, at construction
        if (minorUnits < 0) throw new IllegalArgumentException("negative amount");
        currency = Objects.requireNonNull(currency).toUpperCase();
    }
}
// Works as a Map key, Set element and cache key with no extra code.
Map<Money, BigDecimal> conversions = new HashMap<>();
```

**2. JPA entity — the hard case: identity vs value equality**
```java
@Entity
public class AccountEntity {

    @Id
    @GeneratedValue
    private Long id;                     // null until the entity is persisted

    private String accountNumber;

    protected AccountEntity() { }         // framework constructor (topic 02)

    // Approach A: business key (works for both transient and persistent instances)
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AccountEntity that)) return false;
        return accountNumber != null && accountNumber.equals(that.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber == null ? 0 : accountNumber.hashCode();   // constant for transient entities
    }
}
```
Why this is genuinely hard:
- If `equals` uses the **generated id**, two unsaved entities are never equal, and then `id` changes on
  insert — so an entity placed in a `HashSet` before `flush()` becomes unreachable afterwards.
- If `equals` uses mutable business fields, equality changes over the entity's lifetime — again breaking
  hash-key stability.
- The pragmatic rules: use a **business/natural key**, make it `final` after construction, never put
  entities into `HashSet`/`HashMap` before persisting, and prefer **DTOs/records for value equality**.
- Many teams deliberately keep **identity semantics** for entities (`Object.equals`) and only use
  `equals`/`hashCode` on DTOs. Hibernate's `contains`/`remove` on collections of entities then behaves
  differently — which is itself a decision to make consciously.

**3. Cache keys and deduplication in a service**
```java
@Cacheable("fx-rates")
public Rate rateFor(Money amount, LocalDate on) { ... }   // key derives from equals/hashCode of the args
// A broken equals/hashCode pair here means cache misses forever (no error, just cost).
```

**4. Test assertions depend on the same contract**
```java
assertEquals(new Money(100, "EUR"), new Money(100, "eur"));   // passes only with value equality
Set<Money> distinct = new HashSet<>(List.of(m1, m2));
assertEquals(1, distinct.size());                             // deduplication via equals/hashCode
```

**5. Logging and observability**
```java
log.info("settling transfer {}", transfer);   // calls toString()
```
A missing `toString()` gives `com.acme.Transfer@3f0ee7cb` in production logs; a `toString()` that leaks
tokens or iterates a lazy JPA collection is worse (it can trigger queries or `LazyInitializationException`).

## 9. Internal Working
- **`Object` is the root**: the class file of every type (except `Object` itself) has a `super_class` entry
  pointing at a chain that ends at `java.lang.Object`. `javap -c -p X` shows
  `invokespecial java/lang/Object."<init>":()V` in every constructor.
- **Default `hashCode()`**: `Object.hashCode` is `native` and derived from the object's identity. Verified on
  this JDK: `obj.hashCode() == System.identityHashCode(obj)` is `true` for a class that does not override
  it. It is *stable within one execution* but **not** guaranteed across JVM runs, and not guaranteed unique.
- **Default `toString()`**: `getClass().getName() + "@" + Integer.toHexString(hashCode())` — verified output
  `Plain@6e0be858`.
- **Dispatch**: `equals`, `hashCode` and `toString` are ordinary virtual methods — call sites compile to
  `invokevirtual`, so the override runs (topic 03). `equals(Object)` must keep exactly that parameter type:
  `equals(MyType)` produces a **different descriptor** and is never called by collections (topic 05).
- **`Cloneable` is a marker**: `Object.clone()` checks `this instanceof Cloneable` at runtime and throws
  `CloneNotSupportedException` otherwise. The copy itself is a raw memory copy of the fields (shallow).
- **Records**: the generated `hashCode`, `equals` and `toString` are `invokedynamic` call sites bound to
  `java.lang.runtime.ObjectMethods.bootstrap` (verified in the constant pool with `javap -v`). The bootstrap
  builds a `MethodHandle` over each component's accessor, so components are compared with `Object.equals`
  — which is exactly why an **array** component gives reference equality.
- **`HashMap` lookup cost**: with a good hash, average O(1); all-colliding keys degenerate to O(n) per
  operation (a bucket becomes a linked list, or a red-black tree above 8 nodes within a bucket). A constant
  `hashCode()` is therefore *correct but slow* — a deliberate design trade-off, not a bug.
- **`String.hashCode`**: `s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]`, cached in a field after first
  computation — because `String` is immutable, caching is safe (contrast with a mutable key).
<!-- c11-s9-end -->

## 10. Important Rules
1. Never override `equals` without overriding `hashCode`, and vice versa.
2. `equals` must have the exact signature `public boolean equals(Object other)` — annotate it `@Override`.
3. Follow all five `equals` rules: reflexive, symmetric, transitive, consistent, non-null.
4. Follow all three `hashCode` rules: consistent, **agreement with equals**, collisions permitted.
5. Never include mutable state in `equals`/`hashCode` for objects used as keys; prefer `final` fields.
6. Use the same set of fields in `equals` and `hashCode` — never a subset or a superset.
7. `getClass()` in `equals` gives strict class equality; `instanceof` allows equality across subclasses —
   choose deliberately and document it.
8. `toString` must be informative, null-safe, side-effect free, and free of secrets/PII.
9. Never override `getClass` (`final`), and do not override `wait`/`notify`/`notifyAll` (`final`).
10. Avoid `finalize` (deprecated for removal since Java 18); prefer `try-with-resources`/`Cleaner`.
11. Prefer `record`, immutable value objects or `Objects.equals`/`Objects.hash` over hand-written logic.
12. Never use `==` for object content (`String`, wrappers, `BigDecimal`); for `BigDecimal` use `compareTo`
    because `equals` also compares scale.

## 11. Common Mistakes
- **Overriding `equals` but not `hashCode`** — the compiler can warn you (verified under `-Xlint:overrides`):
  ```
  warning: [overrides] Class BrokenEquals overrides equals, but neither it nor any superclass overrides hashCode method
  ```
  Runtime consequence (verified): `map.get(equalKey)` returns `null` and `HashSet` keeps duplicates.
- **`equals(MyType)` instead of `equals(Object)`** — that is an *overload*, so collections never call it
  (topic 05). `@Override` would have caught it.
- **Mutating a field used by `hashCode` while the object is a key** (verified):
  ```
  map.get(key) : null   <- the entry is unreachable
  map.size()   : 1      <- still counted
  ```
- **Asymmetric equals across a hierarchy** (verified: `base.equals(sub) = true`, `sub.equals(base) = false`).
- **`equals` that throws on `null`**:
  ```java
  public boolean equals(Object o) { return id.equals(o.id); }                    // NPE when o is null
  public boolean equals(Object o) { return o != null && o instanceof X x && id.equals(x.id); }  // correct
  ```
- **Mixing `getClass()` in one class and `instanceof` in another** in the same hierarchy.
- **`hashCode` returning a constant** — correct, but lookups degrade to O(n) scans.
- **Using `hashCode()` as an equality test** — collisions are legal; always confirm with `equals`.
- **`toString` containing a password, token or full card number** — a real compliance incident.
- **`toString` that triggers a lazy ORM load** — extra queries or `LazyInitializationException`.
- **Persisting or logging the default `hashCode`** — it is not stable across JVM runs.
- **Records with array components** (verified: two records holding equal arrays are **not** equal).
- **Comparing `BigDecimal` with `equals`** — `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is
  `false`; use `compareTo`.
- **Calling `clone()` without implementing `Cloneable`** (verified: `CloneNotSupportedException`), or
  assuming `clone()` is deep.
- **Generating `equals`/`hashCode` from only some fields** and then wondering why the cache never hits.
- **Assuming `synchronized` uses `equals`** — monitors use identity, not logical equality.
<!-- c11-s11-end -->

## 12. Comparison

| Aspect | `==` | `.equals()` |
|---|---|---|
| Compares | References (identity) | Logical content (when overridden) |
| Overridable | No | Yes |
| Default behaviour | Identity | `Object.equals` = identity |
| Use for | Primitives, enums, singletons, `null` checks | Value types, strings, wrappers |
| Risk | Comparing equal-but-distinct objects | Forgetting `hashCode`, wrong signature |

| Aspect | `instanceof` in `equals` | `getClass()` in `equals` |
|---|---|---|
| Equality across subclasses | Yes (hierarchy-tolerant) | No (strict) |
| Symmetry | Hard to maintain when subclasses add fields | Given, if every class uses `getClass()` |
| Transitivity | Can break in a hierarchy | Preserved |
| Use when | The hierarchy shares one identity and subclasses add no identity fields | Value classes, `final` classes, strict semantics |
| With Hibernate proxies | Better (a proxy can equal the entity) | Worse (proxy class ≠ entity class) |

| Aspect | `clone()` | Copy constructor / `static` factory |
|---|---|---|
| Requires | `Cloneable` + overriding `clone()` | Nothing |
| Depth | Shallow (manual work for deep copies) | Explicit — whatever you implement |
| Exceptions | `CloneNotSupportedException` (checked) | None |
| Subclass friendly | Poor (`super.clone()` chains) | Good |
| Recommendation | Legacy code | Preferred |

| Aspect | `hashCode` strategy | Notes |
|---|---|---|
| `Objects.hash(a, b, c)` | Concise; allocates an array (varargs) | Fine outside hot loops |
| Manual `31 * result + field` | No allocation; matches IDE generators | Use `Objects.hashCode(f)` for nullable fields |
| Constant (`return 0;`) | Correct but O(n) lookups | Only for objects never used as keys |
| `record` generated | Best default for value types | All components participate |

| Aspect | Identity equality | Value equality |
|---|---|---|
| Definition | Same object (`==`) | Same data (`equals`) |
| Typical types | JPA entities, Spring beans, threads | DTOs, records, money, IDs |
| `hashCode` | Default (identity) | Derived from fields |
| Risk | Duplicates in a `Set` | Equal objects become different keys without `hashCode` |

| Aspect | Plain class | `record` |
|---|---|---|
| `equals`/`hashCode`/`toString` | You write them | Generated from all components |
| Immutability | Optional (discipline required) | Enforced (`final` components) |
| Extensibility | Can extend / be extended | `final`, extends `java.lang.Record` |
| Array components | You control (use `Arrays.equals`) | Reference equality (gotcha) |
| Best for | Entities, mutable objects | Value objects, DTOs, keys |

## 13. Code Examples
- **Beginner/Intermediate** — `01-ObjectClassMethods/ObjectClassMethodsDemo.java`: verified defaults
  (`a.equals(b) = false`, `hashCode() == identityHashCode(a) = true`, `Plain@6e0be858`), the `Objects`
  helpers, shallow `clone()` with a shared array, `CloneNotSupportedException`, and
  `IllegalMonitorStateException` from `wait()` without a monitor.
- **Intermediate/Advanced** — `02-EqualsHashCodeContract/EqualsHashCodeContractDemo.java`: equals without
  hashCode (`map.get` → `null`, `HashSet` size 2), the correct pair (`value-of-A-1`, size 1,
  `list.contains` → true), the mutable-key bug, the asymmetric `instanceof` hierarchy, the symmetric
  `getClass()` fix, and `==` vs `equals` with `intern()`.
- **Advanced** — `03-RecordsAndContracts/RecordsContractDemo.java`: records deduplicating in a `HashSet`,
  `java.lang.Record` as the superclass, `javap -v` proof of the `ObjectMethods` bootstrap, and the
  array-component gotcha.
<!-- c11-s13-end -->

## 14. Practice Questions
1. List `Object`'s methods and say which you may override.
2. State the five rules of the `equals` contract and the three rules of the `hashCode` contract.
3. What is the output, and why?
   ```java
   class X { String v; X(String v) { this.v = v; }
             public boolean equals(Object o) { return o instanceof X x && v.equals(x.v); } }
   Map<X, String> m = new HashMap<>();
   m.put(new X("a"), "1");
   System.out.println(m.get(new X("a")));
   ```
4. Why is `hashCode() == other.hashCode()` not a valid equality test?
5. What happens if you `put` an object into a `HashSet` and then change a field used by `hashCode()`?
6. `Base.equals` uses `instanceof`; `Sub.equals` uses `instanceof` but adds a field. Is the pair symmetric?
   Transitive?
7. Why is `equals(MyType)` a bug even though it compiles?
8. What does `System.out.println(new Object())` print, and how is that string constructed?
9. Why is `Cloneable` called a "broken" interface, and what is the modern alternative?
10. A `record` holds an `int[]`. Are two records with equal array contents equal? Why?
11. Why must you not compare two `BigDecimal` values with `equals`?
12. When is `getClass()`-based `equals` a *bad* choice in a JPA/Hibernate application?

### Answers
1. `getClass` (no — `final`), `hashCode` (yes), `equals` (yes), `toString` (yes), `clone` (rarely),
   `notify`/`notifyAll` (no — `final`), `wait` overloads (no — `final`), `finalize` (no, and deprecated
   for removal since Java 18).
2. `equals`: reflexive, symmetric, transitive, consistent, non-null. `hashCode`: consistent, equal objects
   must have equal hash codes, unequal objects may collide.
3. Prints `null`. `equals` is overridden but `hashCode` is not, so the two `X` instances land in different
   buckets and `get` never calls `equals`.
4. Because unequal objects are allowed to share a hash code (collisions): hash equality is necessary but not
   sufficient. Always confirm with `equals`.
5. The entry becomes unreachable: lookups compute the new hash code and search a different bucket, while the
   stale entry stays — so `size()` is unchanged and the entry leaks.
6. Not symmetric: `base.equals(sub)` can be `true` while `sub.equals(base)` is `false`. Transitivity also
   breaks when subclasses add identity fields.
7. Because it is an **overload** of `Object.equals`, not an override: collections and frameworks call
   `equals(Object)`, so your method is never invoked. `@Override` would not compile on it.
8. Something like `java.lang.Object@1b6d3586`:
   `getClass().getName() + "@" + Integer.toHexString(hashCode())`.
9. It declares no methods and exists only as a runtime marker that `Object.clone()` checks. Modern
   alternatives: copy constructors, `record`, static factories.
10. Not equal. Records compare components with `Object.equals`, and for arrays that is reference equality;
    two distinct arrays with identical contents are not `equals`. Use a `List` component or a custom
    `equals`.
11. `BigDecimal.equals` compares value **and** scale, so `1.0` and `1.00` are unequal; use
    `compareTo(...) == 0` for numeric equality.
12. When the entity can be a lazy proxy: the proxy is a generated subclass, so `getClass()` differs from the
    entity class and equality fails. `instanceof` (or business-key comparison) behaves better.

## 15. Coding Practice
**Easy** — Write a `final class Coordinate` with two `final int` fields, a constructor, `equals`, `hashCode`
and `toString`. Verify with a `HashSet` that two coordinates with the same values collapse to one entry.
**Medium** — Write `final class Money` (minor units + currency, normalised in the constructor) with a correct
contract, then use it as a `HashMap` key. Add a broken variant (equals without hashCode) and show the
difference in `get`, `size` and `contains`.
**Hard** — Starting from `02-EqualsHashCodeContract/EqualsHashCodeContractDemo.java`: (a) reproduce the
asymmetric `Base`/`Sub` pair, (b) fix it with `getClass()`, (c) explain what breaks if a third subclass is
added, then (d) redesign the hierarchy so equality is symmetric *and* subclasses can participate (hint:
compare only common identity fields in a `final` base `equals`, or move equality into a value object).
Finally, re-implement the same type as a `record` and compare code size and guarantees.
<!-- c11-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. Why does every class implicitly extend `Object`?**
- *Testing*: the class hierarchy.
- *Expected*: so every object shares a common base type and API (`equals`, `hashCode`, `toString`, `getClass`, ...), enabling generic storage and uniform contracts.
- *Wrong*: "Because of interfaces."

**Q2. What is the default implementation of `equals`?**
- *Testing*: identity semantics.
- *Expected*: `Object.equals` is reference comparison — equivalent to `==`.
- *Wrong*: "It compares fields."

**Q3. What does `hashCode` return by default?**
- *Testing*: identity hash.
- *Expected*: an identity-derived (native) value; verified as equal to `System.identityHashCode(obj)` on this JDK.
- *Wrong*: "Always the memory address" (not guaranteed and not exactly true).

**Q4. What does the default `toString` print?**
- *Testing*: diagnostic output.
- *Expected*: `getClass().getName() + "@" + Integer.toHexString(hashCode())`, e.g. `Plain@6e0be858`.
- *Wrong*: "The field values."

**Q5. Can you override `getClass`?**
- *Testing*: modifier awareness.
- *Expected*: No, it is `final` (and `native`); it reads the object header.
- *Wrong*: "Yes, but you should not."

### Intermediate
**Q6. Why must `equals` and `hashCode` be overridden together?**
- *Testing*: the central question of this topic.
- *Expected*: `HashMap`/`HashSet` locate a bucket by `hashCode` and only then confirm with `equals`. Equal objects with different hash codes land in different buckets, so lookups silently fail and duplicates appear.
- *Wrong*: "Because the compiler requires it" (it does not — it only warns).

**Q7. What is the output?**
```java
Map<X, String> m = new HashMap<>();
m.put(new X("a"), "1");
System.out.println(m.get(new X("a")));   // X overrides equals(Object) only
```
- *Testing*: practical consequence.
- *Expected*: `null` — different buckets, so `equals` is never reached.
- *Wrong*: `"1"`.

**Q8. Why is `hashCode() != other.hashCode()` enough to conclude the objects are unequal, but equality of hash codes insufficient?**
- *Testing*: one-directional logic of the contract.
- *Expected*: the contract guarantees equal objects share a hash code, so different hash codes imply inequality; collisions mean equal hash codes do not imply equality.
- *Wrong*: "Hash codes are unique."

**Q9. What happens if you mutate a field used in `hashCode` after inserting the object into a `HashMap`?**
- *Testing*: key stability.
- *Expected*: the entry becomes unreachable (lookups use the new hash code), while `size()` still counts it — an effective leak.
- *Wrong*: "The map rehashes automatically."

**Q10. What is the difference between `equals` and `==`?**
- *Testing*: the foundational distinction.
- *Expected*: `==` compares references; `equals` is a method that can implement logical equality. Also note `equals` must be overridden to mean anything beyond identity.
- *Wrong*: "They are the same for objects."
<!-- c11-s16a-end -->

### Advanced
**Q11. `instanceof` vs `getClass()` in `equals` — what exactly breaks, and which do you choose?**
- *Testing*: contract rigour.
- *Expected*: `instanceof` makes a parent "equal" to a subclass that adds identity fields, breaking symmetry and sometimes transitivity (verified: `base.equals(sub) = true` while `sub.equals(base) = false`). `getClass()` is symmetric and transitive but forbids any equality across subclasses (and breaks with Hibernate proxies). Choose per hierarchy, keep it consistent, and document it; often the best answer is a `final` value class.
- *Wrong*: "`instanceof` is always correct."

**Q12. How does `HashMap` use `hashCode` and `equals` internally, and what is the worst-case complexity?**
- *Testing*: real internals knowledge.
- *Expected*: spread the hash (`h ^ (h >>> 16)`), index with `(n - 1) & hash`, walk the bucket comparing `k == key || key.equals(k)`; treeify a bucket above 8 nodes. Average O(1); O(log n) after treeification; O(n) with a constant hash code and no treeification (or with many equal-hash unequal keys).
- *Wrong*: "`HashMap` uses `equals` only."

**Q13. Why does a `record` with an array component violate the spirit of value equality?**
- *Testing*: connection between generated code and semantics.
- *Expected*: the generated `equals` delegates to `ObjectMethods` and compares components with `Objects.equals`-style reference equality, so two equal-looking arrays are unequal; the record is also mutable *through* the array (integration leak). Use a `List` component or a custom `equals`/accessor copy.
- *Wrong*: "Records deep-compare everything."

**Q14. How should `equals`/`hashCode` be implemented on a JPA entity, and why is there no perfect answer?**
- *Testing*: production experience.
- *Expected*: the dilemma — generated ids are null before insert and change afterwards; business keys may mutate. Practical answers: use an immutable natural/business key with a constant `hashCode` for transient entities (accepting collisions), or keep identity semantics and never rely on `equals` for entities; never expose entities as `Set`/`Map` keys across a `flush`; prefer DTOs/records for value equality.
- *Wrong*: "Just use the id."

**Q15. Why does `Object.clone()` require `Cloneable`, why is that considered a design flaw, and what does the JVM actually do?**
- *Testing*: deep API/bytecode understanding.
- *Expected*: `Cloneable` is a marker interface with no methods; `Object.clone` performs a `native` field-wise shallow copy and checks `this instanceof Cloneable`, throwing `CloneNotSupportedException` otherwise. The flaw: a marker that changes the behaviour of another class's method is unintuitive, the method is `protected`, the return type is `Object`, and the copy is shallow so nested mutable state is shared. Modern preference: copy constructors, `record`, or explicit `copy()` methods.
- *Wrong*: "`Cloneable` declares `clone()`."

## 17. Production-Level Questions
1. **Silent cache misses:** a `@Cacheable` method takes a `Money` value object with `equals` but no
   `hashCode`. Everything works locally and the cache never hits in production. Explain the mechanism and
   the fix, and why no exception is ever thrown.
2. **Leaking keys:** a long-lived `ConcurrentHashMap` cache uses a mutable request DTO as its key. After a
   deployment, memory grows without bound. What exactly happened, and what are three ways to prevent it?
3. **Enterprise equality:** a `Set<AccountEntity>` is populated before `flush()` and used after. Duplicates
   appear, and `contains` returns `false` for a row that is clearly in the database. Diagnose it and propose a
   policy for entity equality across the codebase.
4. **Logging incident:** an audit log includes a full `toString()` of a payment object, and the resulting logs
   contain card numbers. What was wrong at the design level, and how would you make it impossible?
5. **Performance:** a service returns `List<Order>` and uses `List.contains` inside a loop with a `getClass()`
   based `equals` on a Hibernate proxy. What happens, and how do you fix the algorithm and the contract?
6. **Migration:** a team replaces 200 hand-written `equals`/`hashCode` pairs with `record`s. What behaviour
   could change, and how would you test the migration?
<!-- c11-s17-end -->

## 18. What I Should Remember
1. Every class extends `Object`, implicitly or indirectly — the contract methods arrive with it.
2. `equals` = logical equality (5 rules); `hashCode` = must agree with `equals` (3 rules).
3. Always override both together, with the exact signature `public boolean equals(Object other)` and `@Override`.
4. `HashMap`/`HashSet` hash first, then compare with `equals` — so a missing or inconsistent `hashCode` breaks
   lookups silently.
5. Never mutate a field used by `hashCode` while the object is a key; prefer `final` fields or use `record`.
6. `instanceof` vs `getClass()` in `equals` is a deliberate, documented trade-off (proxies vs strictness).
7. `toString` must be informative, null-safe, side-effect free and free of secrets.
8. `clone()` is shallow and fragile; prefer copy constructors, factories or `record`.
9. `finalize()` is deprecated for removal — use `try-with-resources`/`Cleaner`.
10. When in doubt: make a `record` (or an immutable `final` class), add `@Override`, and let tests prove the
    contract (`equals`, `hashCode` agreement, `HashSet` size, `HashMap.get`).

## 19. Connection To Other Java Topics
```
All of topics 01-10 (objects, constructors, this/super, methods, overloading,
pass-by-value, recursion, static/final, access modifiers)
        ↓
Topic 11 Object class and object contracts   <-- you are here
        ↓
Interfaces (Comparable/Comparator for ordering) → Generics (type-safe maps/sets)
        ↓
Collections & HashMap internals (buckets, spreading, treeification, resizing)
        ↓
JPA/Hibernate entities (identity vs value equality, proxies, dirty checking)
        ↓
Streams (distinct, groupingBy use the contract) → Concurrency (keys in ConcurrentHashMap)
        ↓
Immutability → safe publication → Java Memory Model
```
This topic is the reason `HashMap` internals will feel like a natural next step: a hash map is buckets of
references manipulated *exactly* through `hashCode()` and `equals()`, and you now know the rules those
two methods must obey.

## 20. One-Minute Revision
- `Object` is the root: defaults are identity `equals`, identity-based `hashCode`, `ClassName@hash` toString.
- Equal objects → equal hash codes; unequal objects may collide (one-directional).
- Override both together, exact signature, `@Override`, same fields, `final` fields, no secrets in `toString`.
- `instanceof` vs `getClass()` = proxy-friendly vs strict; document the choice.
- `clone()` is shallow; `finalize()` is dead; records give you the contract for free (except for arrays).
- **"Why must a class that overrides `equals()` also override `hashCode()`?"** — because hash-based
  collections find the *bucket* by `hashCode` before ever calling `equals`.










