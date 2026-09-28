# Pass-by-Value

## 1. Main Idea
Java is **always pass-by-value** — there is no pass-by-reference in Java, at any version. For a
primitive parameter, the method receives a **copy of the bits** (the number itself). For a
reference parameter, the method receives a **copy of the reference** — a second arrow pointing at
the *same* object, not the object itself and not the caller's variable. This single rule explains
all four classic behaviours: you *can* change the object's fields through a copied reference, you
*cannot* reassign the caller's variable, you *cannot* write a `swap` method for references, and you
*cannot* make an argument `null` outside the method. The confusion is not about the rule; it is
about the fact that mutating a shared object is visible to the caller, which *feels* like
pass-by-reference. The accurate term for Java's behaviour is **"pass-by-value of the reference"**
(sometimes called *call-by-sharing*).

## 2. Why Does This Exist?
- **Simplicity and safety**: no pointer arithmetic, no dangling pointers, no "out" parameters, and no
  hidden aliasing of local variables between caller and callee.
- **Consistent semantics**: one rule for primitives, references, arrays, collections and generics.
- **Predictable garbage collection**: because the caller's variable cannot be rebound by the callee,
  reachability is easier to reason about.
- **Cheap calls**: only a word-sized value (a primitive or a reference) is copied — never a whole
  object graph — which is why passing a `List` with a million elements is as cheap as passing a
  `String` reference.

The trade-off: a method cannot return two values the C++ way, and it cannot replace the caller's
object. Java's answer is to **return a value** or to pass a **holder/mutable object deliberately**.

## 3. Prerequisites
- Topic 01 Classes and Objects (reference vs object, `==`).
- Topic 04 Methods (parameters are copied into local variables).
- `Java Fundamentals/01-Java-Execution-Model`: stack frames, heap, references.
- `Java Fundamentals/02-Variables-Data-Types`: primitives vs references, wrapper classes,
  `String` immutability basics.

## 4. Core Concepts

### The Four Canonical Experiments
| Experiment | Visible to caller? | Why |
|---|---|---|
| Reassign a **primitive** parameter (`n++`) | No | The copy changed, not the argument |
| Reassign a **reference** parameter (`w = new Wallet()`) | No | The copied arrow was re-pointed; the caller's arrow is untouched |
| **Mutate** the object through the reference (`w.amount = 999`) | Yes | Both arrows point at the same object |
| Reassign an **element** of an array/collection (`a[0] = 9`) | Yes | The array object is shared; you mutated it, you did not rebind it |

### What Exactly Is Copied
| Argument type | What is copied | Can the callee change the caller's variable? | Can the callee change shared state? |
|---|---|---|---|
| `int`, `long`, `double`, `boolean`, `char`, ... | The value itself | No | n/a |
| Object reference | The reference (address) | No | Yes — if the object is mutable |
| Array reference | The reference | No | Yes — elements and length-fixed contents |
| `String` reference | The reference | No | No — `String` is immutable, so every "change" returns a new object |
| Wrapper reference (`Integer`, `Long`, ...) | The reference | No | No — wrappers are immutable |
| `record` reference | The reference | No | No — record components are `final` |

### Why `swap` Cannot Work
```java
static void swap(Wallet a, Wallet b) {
    Wallet temp = a;
    a = b;
    b = temp;          // only the two LOCAL copies are swapped
}
// caller:
Wallet x = new Wallet("X"), y = new Wallet("Y");
swap(x, y);
// x is still "X" and y is still "Y"
```
To actually swap, you must return something (`Swap result = swap(x, y);`) or swap inside a mutable
container (`list.set(0, ...)`), or use a holder object with mutable fields.

### The `String` Trap (Looks Like Pass-by-Reference Failure)
```java
static void appendSuffix(String s) { s = s + "!"; }        // no effect on the caller
static void appendSuffix(StringBuilder s) { s.append("!"); } // visible: StringBuilder is mutable
```
The first case fails for two reasons combined: the reference copy is rebound *and* `String` is
immutable. The second succeeds because the object itself is mutable. Understanding which of the two
is responsible is exactly what interviewers probe.

### `final` Parameters
`void m(final Wallet w)` prevents rebinding `w` inside the method but **does not** make the wallet
immutable — `w.amount = 5;` still compiles. `final` on a parameter is documentation, not protection.

### Consequences For API Design
- Prefer **pure functions**: compute and **return** the result instead of mutating an argument.
- If you must mutate, make it explicit in the method name (`sortInPlace`, `applyTo`, `fill`).
- If you must store an argument, **defensively copy** it (`List.copyOf`, `clone()`), otherwise the
  caller keeps a handle to your internal state.
- If you must return internal state, return an **unmodifiable view** or a copy.

## 5. Syntax
```java
public class PassByValueDemo {

    // Primitive: the value is copied. No effect outside.
    static void bumpInt(int n) { n++; }

    // Reference: the ARROW is copied. Rebinding it has no effect outside.
    static void replace(Wallet w) { w = new Wallet("NEW", 0); }

    // Reference: mutating the SHARED object is visible outside.
    static void deposit(Wallet w, long amount) { w.amount += amount; }

    // Array: the reference is copied, elements are shared.
    static void zeroFirst(long[] values) { values[0] = 0; }

    // Returning a value is how you change the caller's view.
    static Wallet replaced(Wallet w) { return new Wallet(w.id, 0); }

    // Defensive copy: the caller cannot mutate our field afterwards.
    static java.util.List<String> safeCopy(java.util.List<String> in) {
        return java.util.List.copyOf(in);
    }
}
```
<!-- c7-s5-end -->

## 6. Simple Example
See `01-PassByValue/PassByValueDemo.java`, which runs all four canonical experiments and prints the
verdict for each, and `02-DefensiveCopy/DefensiveCopyDemo.java`, which shows why a method that
*stores* an argument must copy it, and how to emulate an out-parameter with a holder object.

## 7. Real-Life Analogy
Pass-by-value is **photocopying an address** and handing the photocopy to a courier. The courier
cannot change which house you live in (rebinding your variable) — they hold a different piece of
paper. But if the courier travels to that address and repaints the front door, you will see the
change when you get home, because it is the same house. And if you hand over a photocopy of a
**locked, sealed box's** address (`String`), the courier can only look at it: any "change" they make
creates a brand-new box at a new address, leaving yours untouched.

## 8. Real Backend Example
```java
// 1. Pure function: compute and return. No aliasing surprises, trivially testable.
BigDecimal withDiscount(BigDecimal price, BigDecimal discount) {
    return price.subtract(discount);
}

// 2. Deliberate in-place mutation: the name states it, the caller must know.
void applyDiscountInPlace(Order order, BigDecimal discount) {
    order.setTotal(order.getTotal().subtract(discount));   // visible to the caller
}

// 3. Hibernate dirty checking depends on EXACTLY this reference semantics:
//    load() returns a managed instance; mutating its fields marks it dirty,
//    and the ORM flushes an UPDATE without any explicit save() call.
@Transactional
public void renameAccount(String id, String newName) {
    Account account = accounts.find(id).orElseThrow();     // managed entity
    account.setName(newName);                              // mutation is the update
}

// 4. The classic production bug: storing a caller-owned collection.
final class Basket {
    private final List<String> items;
    Basket(List<String> items) {
        this.items = items;              // WRONG: the caller keeps the same list
    }
}

// 5. The fix: defensive copy on the way in and an unmodifiable view on the way out.
final class SafeBasket {
    private final List<String> items;
    SafeBasket(List<String> items) {
        this.items = List.copyOf(items); // copy: caller cannot mutate our state
    }
    List<String> items() {
        return items;                    // already immutable (List.copyOf)
    }
}
```

Production lessons:
- **Do not return mutable internal collections** — return `List.copyOf(...)` or
  `Collections.unmodifiableList(...)`. Remember that `unmodifiableList` is a *view*: the caller
  cannot change the list, but your own changes are visible through it. `List.copyOf` is a **snapshot**.
- **Interfaces are not immunity**: passing an `Order` "as an interface" does not stop the method from
  mutating it. Encapsulation depends on immutability and visibility, not on parameter types.
- **String concatenation inside a loop** looks like it mutates a `String`; it actually creates a new
  object each iteration. Use `StringBuilder` when the result must accumulate.

## 9. Internal Working
- A method call copies each argument into the callee's **local variable slot**: primitives via
  `iload/lstore`-style copies, references by copying the reference value. There is no
  "reference to a variable" in the JVM instruction set.
```java
static void deposit(Wallet w, long amount) { w.amount += amount; }
```
```
Caller frame                          Callee frame
+--------------------------+          +--------------------------+
| wallet  -> 0x7f3a1000    |          | w       -> 0x7f3a1000    |  <-- copy of the reference
| amount  = 100            |  call    | amount  = 100            |  <-- copy of the primitive
+--------------------------+          +--------------------------+
                                     Heap: Wallet @0x7f3a1000 { amount = 100 }
                                       both references point here
```
- **Rebinding** (`w = new Wallet(...)`) changes only the callee's slot, so the caller is unaffected.
- **Mutating** (`w.amount = ...`) dereferences the shared reference and writes into the shared heap
  object, so the change is visible to every holder of that reference.
- **The callee's reference copy keeps the object reachable**: during the call, the object cannot be
  garbage collected even if the caller rebinds or nulls its own variable. (Reachability is computed
  from all live frames, not just the caller's.)
- **No `swap`**: swapping two parameters swaps two stack slots. Nothing on the heap is affected.
- **Cost**: passing a reference is one word (4 bytes with compressed oops, otherwise 8) regardless of
  the object's size — which is why passing a large `List` is free.
<!-- c7-s9-end -->

## 10. Important Rules
1. Java has **only** pass-by-value. What is copied for an object is the **reference**, never the object.
2. A callee can **mutate** the shared object but can never **rebind** the caller's variable.
3. `swap(a, b)` for references cannot work. Return a holder/array, or swap inside a mutable container.
4. Arrays behave like objects: elements are shared, the array variable is not.
5. `String`, wrappers, `BigDecimal`, `LocalDate` and `record` instances are **immutable** — nothing a
   callee does can change the caller's value.
6. `final` on a parameter prevents rebinding only; it does **not** prevent mutation of the object.
7. If a method **stores** an argument, copy it (`List.copyOf`, `Map.copyOf`, `Set.copyOf`, `clone()`).
8. If a method **returns** internal state, return a copy or an unmodifiable view — and know the
   difference between a view (`unmodifiableList`) and a snapshot (`copyOf`).
9. Name mutating methods so the caller knows: `sortInPlace`, `applyTo`, `fill`, `update`.
10. Prefer returning new values (pure functions) over mutating arguments — easier to test, thread-safe.

## 11. Common Mistakes
- **Believing Java is pass-by-reference for objects** — the model is "pass-by-value of the reference".
- **Expecting a `swap` helper to work**:
  ```java
  static void swap(Box a, Box b) { Box t = a; a = b; b = t; }   // caller unaffected
  ```
- **Thinking `String` reassignment failing proves pass-by-value** — it is *both* rebinding *and*
  immutability; `StringBuilder` isolates the second factor.
- **Storing the caller's collection**:
  ```java
  Basket(List<String> items) { this.items = items; }      // caller keeps a handle
  ```
- **Confusing `unmodifiableList` with a copy**:
  ```java
  List<String> view = Collections.unmodifiableList(mine);  // view: later changes to 'mine' show up
  List<String> copy = List.copyOf(mine);                   // snapshot: frozen at this moment
  ```
- **Returning an internal array directly** — the caller can modify your state:
  `String[] tags() { return tags; }` → return `tags.clone()`.
- **`final` parameter mistaken for immutability**: `void m(final Wallet w) { w.amount = 9; }` compiles.
- **Mutating an argument "just to be helpful"** without documenting it — the classic source of bugs in
  layered code where the caller reuses the object afterwards.
- **Assuming `Integer`/`Long` parameters can be used as out-parameters** — wrappers are immutable;
  a `long[]` or an `AtomicLong` is needed for mutation.

## 12. Comparison

| Aspect | Java | C++ |
|---|---|---|
| Default for primitives | Pass-by-value | Pass-by-value |
| Default for objects | Pass-by-value **of the reference** | Pass-by-value copies the whole object |
| Pass-by-reference option | **None** | `T&` / `T*` |
| Can callee rebind the caller's variable? | No | Yes, with a reference/pointer |
| Can callee mutate shared state? | Yes, if the object is mutable | Yes |
| Dangling pointers / pointer arithmetic | Impossible | Possible |

| Aspect | Primitive argument | Reference argument |
|---|---|---|
| What is copied | The value | The reference value (one word) |
| Callee rebinds → caller sees? | No | No |
| Callee mutates → caller sees? | Not applicable | Yes, if mutable |
| Cost | Fixed | Fixed (independent of object size) |

| Aspect | `List.copyOf(...)` | `Collections.unmodifiableList(...)` |
|---|---|---|
| Type | Immutable snapshot | Unmodifiable **view** of the original |
| Reflects later changes to the source | No | Yes |
| Null elements | Rejected | Allowed |
| Good for | Storing/returning internal state safely | Read-only exposure when you must share live data |

## 13. Code Examples
- **Beginner/Intermediate** — `01-PassByValue/PassByValueDemo.java`: the six experiments above, with
  the printed verdicts (primitive, reference rebinding, object mutation, array element, swap, and
  `String` vs `StringBuilder`).
- **Advanced** — `02-DefensiveCopy/DefensiveCopyDemo.java`: a mutable object leaking through a
  constructor and an accessor, the `List.copyOf` fix, the difference between a view and a snapshot,
  and a holder pattern for emulating an out-parameter.
<!-- c7-s13-end -->

## 14. Practice Questions
1. Is Java pass-by-value or pass-by-reference? What exactly is copied for an object argument?
2. Why does this print `5`?
   ```java
   static void bump(int n) { n++; }
   int x = 5; bump(x); System.out.println(x);
   ```
3. Why does this print `[1]` and not `[2]`?
   ```java
   static void replace(int[] a) { a = new int[]{2}; }
   int[] arr = {1}; replace(arr); System.out.println(Arrays.toString(arr));
   ```
4. Why does this print `[9]`?
   ```java
   static void change(int[] a) { a[0] = 9; }
   int[] arr = {1}; change(arr); System.out.println(Arrays.toString(arr));
   ```
5. Why can `swap(a, b)` never swap two reference arguments? Give two working alternatives.
6. `String s = "a"; mutate(s);` where `mutate` does `s = s + "b";` — is `s` changed? How many reasons?
7. Is `StringBuilder` passed by reference? Explain the output of the same experiment.
8. Why does `void m(final Wallet w) { w.amount = 5; }` compile?
9. What is the difference between `Collections.unmodifiableList(list)` and `List.copyOf(list)` if the
   original list is modified afterwards?
10. A method stores the `List` argument in a field without copying. Describe the exact risk in a
    multi-threaded service.

### Answers
1. Always pass-by-value. For objects, the copied value is the **reference**, not the object.
2. The callee increments its own copy; the caller's variable is untouched.
3. Reassigning the parameter rebinds only the callee's local reference; the caller's array is untouched.
4. The reference is copied, so both point to the *same* array; writing `a[0]` mutates that shared array.
5. Swapping changes only the callee's two stack slots. Alternatives: return the swapped values (array,
   record, or a `Swap` result object), or swap positions inside a mutable container
   (`list.set(0, ...)`), or use mutable holders.
6. No. Two independent reasons: the reference copy is rebound, **and** `String` is immutable so the
   concatenation creates a new object.
7. No — it is the same pass-by-value rule. The experiment differs only because `StringBuilder` is
   **mutable**, so `append` mutates the shared object and the caller sees the change.
8. Because `final` on a parameter only prevents rebinding `w`; mutating the object `w` points to is
   still allowed (the reference is `final`, the object is not immutable).
9. `unmodifiableList` is a **view**: it reflects the later modification (and throws on the caller's own
   attempts to modify). `List.copyOf` is a **snapshot**: it is unaffected by later changes.
10. The caller keeps a reference to the same list, so it (or another thread) can mutate the object's
    internal state at any time — bypassing all validation and breaking invariants and thread-safety.

## 15. Coding Practice
**Easy** — Write `void increment(int x)` and `void increment(int[] x)` and call both with a local
variable. Print the result and explain in a comment why they differ.
**Medium** — Write a `Cart` class that takes a `List<Item>` in its constructor and exposes
`items()`. First write the leaky version, prove the leak from `main`, then fix it with `List.copyOf`,
and prove the leak is gone.
**Hard** — Implement `static <T> void swap(List<T> list, int i, int j)` (which *does* work) and explain
why it works while `swap(T a, T b)` cannot. Then implement an out-parameter style method
`static boolean tryParse(String text, long[] out)` and rewrite it to return a `OptionalLong` — compare
readability and explain which is more idiomatic in modern Java.
<!-- c7-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. Is Java pass-by-value or pass-by-reference?**
- *Testing*: the classic question, and whether you can explain the nuance.
- *Expected*: Always pass-by-value. For objects the copied value is the reference; the object itself is never copied and the caller's variable can never be rebound.
- *Wrong*: "Primitives are by value, objects are by reference."

**Q2. If Java is pass-by-value, why can a method change my object's fields?**
- *Testing*: reference vs object.
- *Expected*: The reference is copied, but both references point to the same object, so mutating through it is visible to everyone holding that reference.
- *Wrong*: "Because objects are passed by reference."

**Q3. Can a method change the value of my `int` variable?**
- *Testing*: basic copy semantics.
- *Expected*: No; it only changes its own copy. Return the new value and assign it.
- *Wrong*: "Yes, if it assigns to the parameter."

**Q4. Why does a `swap` method not work in Java?**
- *Testing*: understanding of variable rebinding.
- *Expected*: It swaps the callee's local copies of the references; nothing on the heap changes.
- *Wrong*: "Because `swap` is not a keyword."

**Q5. What is the difference between mutating an object and reassigning a parameter?**
- *Testing*: the core distinction.
- *Expected*: Mutating dereferences the copied reference and writes shared heap state (visible). Reassigning only changes the local slot (invisible).
- *Wrong*: "Both are visible to the caller."

### Intermediate
**Q6. What is the output, and why?**
```java
String s = "a"; append(s); System.out.println(s);
static void append(String v) { v = v + "b"; }
```
- *Testing*: immutability plus pass-by-value.
- *Expected*: `a` — the parameter is rebound and `String` is immutable, so a new object was created only inside the method.
- *Wrong*: "`ab`, because `String` is passed by reference."

**Q7. Same experiment with `StringBuilder`: what is printed?**
- *Testing*: isolating the two reasons.
- *Expected*: `ab` — the reference is still copied, but `append` mutates the same mutable object.
- *Wrong*: "Nothing, because Java is pass-by-value."

**Q8. A constructor stores the `List` argument directly. What is the bug called and how do you fix it?**
- *Testing*: defensive copying.
- *Expected*: Aliasing / escaping reference. Fix with `List.copyOf(...)` (snapshot) or `new ArrayList<>(...)`, and copy/clone on the way out too.
- *Wrong*: "It is fine because `List` is an interface."

**Q9. What is the difference between `Collections.unmodifiableList` and `List.copyOf`?**
- *Testing*: view vs snapshot.
- *Expected*: the former is an unmodifiable **view** that tracks later changes to the backing list; the latter is an immutable **snapshot** (and rejects nulls).
- *Wrong*: "They are identical."

**Q10. Does `final` on a parameter make the argument immutable?**
- *Testing*: modifier precision.
- *Expected*: No — it only prevents rebinding the parameter inside the method; the object's fields can still be changed if mutable.
- *Wrong*: "Yes, `final` makes it constant."
<!-- c7-s16a-end -->

### Advanced
**Q11. Why is "pass-by-value of the reference" the accurate description, and why do some call it *call-by-sharing*?**
- *Testing*: precise mental model.
- *Expected*: the caller's variable is copied (value semantics for the variable) but the copy points to the same object (sharing of mutable state) — so the callee can share state but cannot rebind the caller's variable.
- *Wrong*: "It is a mixture of both, depending on the type."

**Q12. Does the callee's reference copy affect reachability and garbage collection?**
- *Testing*: GC interplay.
- *Expected*: Yes — the live stack frame holds a reference, so the object stays reachable for the duration of the call even if the caller rebinds its own variable mid-call. Reachability is computed from all GC roots, including other frames.
- *Wrong*: "The object can be collected as soon as the caller rebinds."

**Q13. Why does Java avoid pass-by-reference, and what does that cost?**
- *Testing*: language design trade-offs.
- *Expected*: It removes pointer aliasing of variables, out-parameters and dangling references, giving simpler memory safety and reasoning; the cost is no multi-value return and no in-place variable replacement (you return objects/records instead).
- *Wrong*: "It is just a historical accident."

**Q14. How does pass-by-value interact with thread safety when objects are shared between threads?**
- *Testing*: concurrency reasoning (leads to the JMM module).
- *Expected*: Copying the reference shares mutable state without any memory-safety guarantee: another thread may see stale or partially published fields unless `final`, `volatile` or proper synchronisation (safe publication) is used. Immutability is the clean answer.
- *Wrong*: "Copying the reference makes it thread-safe."

**Q15. Would passing a 10-million-element `List` be expensive? What about a 10-million-element `long[]`?**
- *Testing*: understanding that only the reference is copied.
- *Expected*: Both are one word to pass — no copying of contents. Cost appears only if you copy (`List.copyOf`, `clone`), serialise, or stream the contents. The catch: copying for safety is the expensive part, not the call.
- *Wrong*: "Both copy the whole collection."

## 17. Production-Level Questions
1. **Aliasing bug:** a `ReportBuilder` stores the `List<String> columns` passed by the controller. Later the
   controller reuses the same list for another report. What breaks, and what is the minimal fix?
2. **ORM surprise:** a service method takes an entity and calls `entity.setStatus(...)`; a colleague
   claims "we never called `save()`". Why is the database updated anyway, and what does that imply for
   transaction boundaries?
3. **Concurrency:** a `Cache` stores a `Map` argument in a field. Under load, `ConcurrentModificationException`
   or corrupt reads appear. Which pass-by-value property is responsible, and how do you fix it without
   changing the call sites?
4. **API design:** you need a method that returns both a value and a flag. What are the modern options, and
   why is `long[] out` a poor one (readability, nullability, thread safety)?
5. **Immutability audit:** a codebase uses `record` for DTOs but passes `List` fields directly in the
   canonical constructor. Is the record fully immutable, and what change makes it so?
<!-- c7-s17-end -->

## 18. What I Should Remember
1. Java is **always** pass-by-value; for objects the value copied is the **reference**.
2. Mutating through a copied reference is visible; rebinding it is not.
3. Therefore `swap` cannot work — return values or swap inside a mutable container.
4. `String`, wrappers, `BigDecimal`, `LocalDate` and `record` are immutable → no in-place changes visible.
5. `StringBuilder` proves the difference: same rule, mutable object.
6. `final` on a parameter stops rebinding, not mutation.
7. Copy arguments you store (`List.copyOf`, `clone`) and copy/clone internal state you return.
8. `List.copyOf` = snapshot; `Collections.unmodifiableList` = live view.

## 19. Connection To Other Java Topics
```
Topic 04-06 (methods, overloading, varargs: arguments are copied into parameters)
        ↓
Topic 07 Pass-by-Value                 <-- you are here
        ↓
Topic 08 Recursion (each frame holds its own copies of parameters/locals)
        ↓
Topic 11 Object contract (equals/hashCode + immutability for safe keys)
        ↓
Immutability & defensive copying  →  Thread safety & safe publication (JMM)
        ↓
Collections (mutable views vs snapshots) → Streams (immutable pipelines)
```
This topic is the foundation of every later correctness discussion: aliasing, immutability and
thread-safety all start with "what exactly was copied?".

## 20. One-Minute Revision
- Only the value is copied: bits for primitives, the reference for objects.
- Mutate → visible; rebind → invisible.
- No `swap`; return the result instead.
- `String` "doesn't change" because of rebinding **and** immutability; `StringBuilder` changes because
  it is mutable.
- `final` parameter ≠ immutable object.
- Copy in (`List.copyOf`) and copy out (`clone`); know snapshot vs view.



