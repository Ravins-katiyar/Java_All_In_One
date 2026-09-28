# Recursion

## 1. Main Idea
**Recursion** is a method calling itself on a **simpler version of the same problem**. Every correct
recursive method needs three things: a **base case** that stops the recursion, a **recursive case**
that delegates a smaller subproblem, and **progress** toward the base case on every call. Each call
pushes a new **stack frame** (topic 04), and each frame holds its **own copies** of the parameters and
locals (topic 07) — so the "state" of every level of the recursion is preserved automatically. Because
frames live on a fixed-size per-thread stack, recursion depth is limited; exceeding it throws
`StackOverflowError`. Java performs **no tail-call optimisation**, so a recursive method that merely
returns its own call still consumes a frame per level and can be rewritten as a loop.

## 2. Why Does This Exist?
Some problems are defined in terms of themselves, and recursion is the shortest correct expression of
them:
- **Mathematical definitions**: `n! = n * (n-1)!`, `fib(n) = fib(n-1) + fib(n-2)`, `gcd(a,b) = gcd(b, a%b)`.
- **Self-similar data**: trees, nested JSON, file systems, org charts, nested comments, XML/HTML DOM.
- **Divide and conquer**: merge sort, quick sort, binary search, fast exponentiation.
- **Backtracking**: permutations, combinations, sudoku, N-Queens, path finding.
- **Grammar/parsers**: a JSON or expression parser is naturally recursive.
Writing these iteratively requires an explicit stack anyway — recursion *is* the explicit stack, hidden
in the call stack.

## 3. Prerequisites
- Topic 04 Methods — a call pushes a frame; bytecode, operand stack, `areturn`.
- Topic 07 Pass-by-Value — each frame holds its own copy of the parameters/locals.
- Control flow (`if`, loops) from `Java Fundamentals/03-Operators-Control-Flow`.
- Basic complexity awareness: O(n), O(2^n), O(log n).

## 4. Core Concepts

### The Three Requirements
```java
static int factorial(int n) {
    if (n <= 1) {              // 1. BASE CASE (stops the recursion)
        return 1;
    }
    return n * factorial(n - 1);   // 2. RECURSIVE CASE, 3. PROGRESS (n -> n-1)
}
```
| Requirement | Meaning | Symptom if missing |
|---|---|---|
| Base case | A condition that returns without recursing | `StackOverflowError` |
| Recursive case | Calls itself with a smaller task | Infinite recursion or wrong result |
| Progress | The argument moves toward the base case | `StackOverflowError` |

### The Call Stack For `factorial(4)`
```
factorial(4) -> 4 * factorial(3)
                  factorial(3) -> 3 * factorial(2)
                                    factorial(2) -> 2 * factorial(1)
                                                      factorial(1) -> 1   <-- base case
                  unwinding:      2*1=2  ->  3*2=6  ->  4*6=24
Stack growth (top = current)
+-------------------+
| factorial(1)      |  <- base case reached, begins to unwind
+-------------------+
| factorial(2)      |
+-------------------+
| factorial(3)      |
+-------------------+
| factorial(4)      |
+-------------------+
| main              |
+-------------------+
```

### Two Shapes Of Recursion
| Shape | Example | Branching | Typical cost |
|---|---|---|---|
| **Linear** | `factorial`, `gcd`, countdown, list length | 1 recursive call | O(n) frames |
| **Tree / branching** | `fib(n)`, tree traversal, permutations | 2+ recursive calls | O(2^n) time, O(n) frames |

Tree recursion without memoisation recomputes subproblems exponentially. That is the whole motivation
for **memoisation**.

### Memoisation (Top-Down Dynamic Programming)
Cache the result of each subproblem so it is computed once:
```java
static long fib(int n, long[] memo) {
    if (n <= 1) return n;                      // base case
    if (memo[n] != 0) return memo[n];          // already computed
    memo[n] = fib(n - 1, memo) + fib(n - 2, memo);
    return memo[n];
}
```
- Complexity drops from **O(2^n) to O(n)** time (O(n) extra space).
- The bottom-up equivalent is a loop filling the array: same complexity, no deep stack.

### No Tail-Call Optimisation
```java
static int sum(int n) { return n == 0 ? 0 : n + sum(n - 1); }   // grows the stack: O(n) frames
static int countDown(int n) { return n == 0 ? 0 : countDown(n - 1); }  // TAIL call, still O(n) frames
```
Java (HotSpot) does **not** convert a tail call into a loop, so deep tail-recursive code still throws
`StackOverflowError`. Rewrite tail recursion as iteration when depth can be large.

### Depth Limits And `-Xss`
- Each thread has its own JVM stack; default size is typically **512 KB–1 MB** depending on platform
  and JVM (HotSpot x64 commonly 1 MB for the main thread). Tune with `-Xss1m` / `-Xss8m`.
- Frames for methods with many locals are larger, so the maximum depth differs per method.
- On JDK 21 with the default settings this machine reaches a depth in the tens of thousands for a
  trivial method (see `03-BacktrackingAndStack/StackDepthDemo.java`), but you should never design code
  to rely on a specific number.

### Recursion vs Iteration
Any recursion can be rewritten with an **explicit stack** (`ArrayDeque`) — which is exactly what a
recursive method does implicitly, but in heap memory instead of the thread stack, so it can grow as
large as the heap allows. That is the standard fix for deep recursion in production.

### Backtracking Skeleton
```java
static void explore(State state) {
    if (isSolution(state)) { record(state); return; }   // base case
    for (Choice c : choices(state)) {
        apply(c, state);          // choose
        explore(state);           // explore
        undo(c, state);           // un-choose  <-- the "backtrack"
    }
}
```
<!-- c8-s4-end -->

## 5. Syntax
```java
public class Recursion {

    // 1. Base case first, always. Guard clauses keep the recursion readable.
    public static long factorial(int n) {
        if (n < 0)  throw new IllegalArgumentException("n must be >= 0");
        if (n <= 1) return 1;                     // base case
        return n * factorial(n - 1);              // recursive case + progress
    }

    // 2. Accumulator parameter: a common way to write readable linear recursion.
    public static int sum(int[] values, int index, int accumulator) {
        if (index == values.length) return accumulator;      // base case
        return sum(values, index + 1, accumulator + values[index]);
    }

    // 3. Mutual recursion: two methods calling each other.
    public static boolean isEven(int n) { return n == 0 || isOdd(n - 1); }
    public static boolean isOdd(int n)  { return n != 0 && isEven(n - 1); }

    // 4. A defensive depth guard for untrusted input.
    private static final int MAX_DEPTH = 1_000;

    public static Node find(Node node, String id, int depth) {
        if (node == null || depth > MAX_DEPTH) return null;    // base cases
        if (node.id().equals(id))              return node;
        Node hit = find(node.left(), id, depth + 1);
        return hit != null ? hit : find(node.right(), id, depth + 1);
    }
}
```

## 6. Simple Example
See `01-RecursionBasics/RecursionBasicsDemo.java` (linear recursion, unwinding order, digit sum, GCD,
string reversal, naive vs memoised Fibonacci with timings, tail recursion vs a loop),
`02-TreeTraversal/TreeTraversalDemo.java` (pre/in/post-order traversal, height, node count, a manager
chain) and `03-BacktrackingAndStack/BacktrackingAndStackDemo.java` (permutations by backtracking, a
measured `StackOverflowError`, and the same traversal rewritten with an explicit heap stack).

## 7. Real-Life Analogy
Recursion is **opening Russian nesting dolls**. Each doll contains a smaller doll of the same kind,
until you reach the solid one — that is the base case. The dolls you have already opened stay on the
table while you work inward: that is the **call stack**, holding every level's state. When you finish,
you close the dolls in reverse order: that is **unwinding**. The table has finite space, so if the
dolls never end you run out of room — `StackOverflowError`. For unlimited nesting you need a warehouse
(heap) plus a written list of what to open next (an explicit stack).
<!-- c8-s7-end -->

## 8. Real Backend Example
```java
// 1. Nested comment thread: recursion mirrors the data shape exactly.
public List<CommentDto> flatten(Comment root) {
    List<CommentDto> out = new ArrayList<>();
    walk(root, 0, out);
    return out;
}

private void walk(Comment node, int depth, List<CommentDto> out) {
    out.add(new CommentDto(node.id(), depth));
    for (Comment reply : node.replies()) {
        walk(reply, depth + 1, out);            // depth is carried per frame
    }
}

// 2. Organisation hierarchy: permission is inherited down/up the tree.
public boolean canApprove(Employee employee, long amountMinorUnits) {
    if (employee.limitMinorUnits() >= amountMinorUnits) return true;   // base case
    Employee manager = employee.manager();
    return manager != null && canApprove(manager, amountMinorUnits);   // walk up the chain
}

// 3. Expression parsing (a tiny recursive-descent parser shape):
//    expression := term  (('+' | '-') term)*
//    term       := factor (('*' | '/') factor)*
//    factor     := NUMBER | '(' expression ')'

// 4. File-system / storage walk WITH a depth guard for untrusted input.
public void scan(Path dir, int depth) {
    if (depth > MAX_DEPTH) {
        throw new IllegalStateException("directory nesting too deep: " + dir);
    }
    File[] children = dir.toFile().listFiles();
    if (children == null) return;                     // base case: not a directory
    for (File child : children) {
        if (child.isDirectory()) {
            scan(child.toPath(), depth + 1);
        } else {
            process(child);
        }
    }
}
```

Production lessons:
- **Deep or untrusted input needs a depth guard.** A user-supplied JSON with 100 000 nested arrays can
  throw `StackOverflowError` on a request thread — and an `Error` should not be part of your normal
  exception handling.
- **Wide trees are better traversed iteratively (BFS with a queue).** Recursive depth is bounded by the
  tree's **height**: a degenerate ("linked-list") tree of 1 million nodes overflows a recursive walk,
  while an iterative one does not.
- **At scale, recursion becomes an explicit stack**: `ArrayDeque` on the heap can grow far beyond the
  thread stack, and it lets you pause/resume work (essential for resumable batch jobs).

## 9. Internal Working
- Every recursive call is an ordinary invocation: a **new frame** is pushed onto the calling thread's
  JVM stack, holding that level's own copies of parameters and locals (topic 07).
- The **return address** inside each frame is what makes unwinding work: when the base case returns,
  every frame resumes right after its `invoke` instruction and aggregates its own `n`.
- Recursion therefore consumes **thread-stack memory**, not heap memory (unless the method allocates).
```
Thread stack (fixed maximum, grows downward)        Heap
+--------------------------+                 +---------------------+
| main frame               |                 | memo[] array, DTOs, |
+--------------------------+                 | explicit Deque      |
| fib(4) frame    n=4      |                 +---------------------+
+--------------------------+
| fib(3) frame    n=3      |
+--------------------------+
| fib(2) frame    n=2      |
+--------------------------+
| fib(1) frame    n=1      |  <- base case: returns, then frames pop one by one
+--------------------------+
```
- **Size limit**: the stack size is fixed per thread; when a frame cannot be pushed the JVM throws
  `StackOverflowError` (`java.lang.Error`). Tune with `-Xss` (e.g. `-Xss2m`); the default is typically
  512 KB–1 MB. Measured on this machine's JDK 21 with default settings, a trivial one-parameter method
  reached a depth in the **tens of thousands** before overflowing — never rely on a specific number.
- **No tail-call elimination**: HotSpot does not convert `return f(x);` into a jump, so tail recursion
  is **not** constant-space in Java. Rewrite as a loop, or use an explicit stack.
- **Each thread owns its stack**, so deep recursion in a thread-pool worker consumes that worker's
  stack; the resulting `StackOverflowError` kills that thread but not the JVM.
- **Cost model**: time ∝ number of calls (O(2^n) for naive `fib`), space ∝ **maximum depth** (O(n)),
  plus any memoisation/cache you add.
<!-- c8-s9-end -->

## 10. Important Rules
1. Every recursive method needs a **base case**, a **recursive case** and **progress** toward the base case.
2. Test the base case explicitly (empty input, `null`, `0`, `1`) — most recursion bugs live there.
3. Recursion uses the **thread stack**; depth is bounded by `-Xss`, so unbounded/untrusted depth must be
   guarded or converted to iteration.
4. Java has **no tail-call optimisation** — tail recursion costs a frame per call.
5. Memoise tree/branching recursion or you will pay exponential time (naive `fib(30)` makes roughly
   2.7 million calls to produce a single number).
6. Adding an accumulator parameter often turns an elegant-but-unclear recursion into a readable one.
7. Anything recursive can be rewritten with an **explicit stack** (`ArrayDeque`) — use heap memory when
   depth can be large.
8. Recursion depth is bounded by the structure's **height**, not its size: know which one can blow up.
9. `StackOverflowError` is a `java.lang.Error`; catching it is diagnostic only, not error handling.
10. Prefer recursion for self-similar data (trees, nesting) and iteration for plain repetition.

## 11. Common Mistakes
- **Missing base case** → instant `StackOverflowError`:
  ```java
  static int bad(int n) { return bad(n - 1); }
  ```
- **Base case that never runs** (no progress):
  ```java
  static int bad(int n) { return n == 0 ? 0 : n + bad(n); }   // n never shrinks
  ```
- **Off-by-one base case** producing a wrong result:
  ```java
  static int factorial(int n) { return n == 0 ? 1 : n * factorial(n - 2); }  // n-2 skips values
  ```
- **Forgetting to copy the accumulated result in backtracking**:
  ```java
  results.add(current);              // WRONG: the same mutable list is added every time
  results.add(new ArrayList<>(current));   // correct
  ```
- **Recomputing overlapping subproblems** (naive `fib`) → exponential time instead of O(n).
- **Assuming tail recursion is optimised** → `StackOverflowError` on deep inputs.
- **Recursing on a `null` child without a guard** → `NullPointerException` instead of a base case.
- **Deep recursion on untrusted input** (user-supplied JSON/XML nesting) → request-thread
  `StackOverflowError`, which cannot be handled as a normal exception.
- **Using recursion for simple linear iteration** (`list.size()` by recursing) → slower and stack-limited.
- **Trying to `catch (StackOverflowError)` and continue** — the stack may not be usable and the state is
  inconsistent. Treat it as a fatal condition and fix the depth.

## 12. Comparison

| Aspect | Recursion | Iteration |
|---|---|---|
| State storage | Call stack (implicit frames) | Local variables / explicit stack |
| Clarity for trees/nesting | Excellent | Verbose |
| Memory | O(depth) stack, limited by `-Xss` | O(1) usually |
| Deep input | `StackOverflowError` | Fine |
| Tail calls | Not optimised in Java | Naturally constant-space |
| Risk | Base case / progress bugs | Loop condition bugs |

| Aspect | Naive recursion | Memoised recursion | Bottom-up loop |
|---|---|---|---|
| `fib(30)` time | ~O(2^30) calls | O(n) | O(n) |
| Extra memory | O(n) stack | O(n) array + O(n) stack | O(1) or O(n) |
| Risk | Timeout | Cache-key mistakes | Index bugs |

| Aspect | Implicit stack (recursion) | Explicit stack (`ArrayDeque`) |
|---|---|---|
| Memory location | Thread stack | Heap |
| Max depth | Thousands | Limited by heap (millions) |
| Pause/resume | Hard | Natural |
| Code size | Smaller | Larger, but debuggable |

## 13. Code Examples
- **Beginner/Intermediate** — `01-RecursionBasics/RecursionBasicsDemo.java`: factorial (recursive vs
  iterative), unwinding order, digit sum, GCD, string reversal, naive vs memoised Fibonacci with real
  timings, and tail recursion vs a loop.
- **Intermediate** — `02-TreeTraversal/TreeTraversalDemo.java`: pre/in/post-order traversal, node count,
  height, search, a manager chain, and an org-hierarchy walk carrying `depth`.
- **Advanced** — `03-BacktrackingAndStack/BacktrackingAndStackDemo.java`: permutations via
  choose/explore/un-choose, a measured `StackOverflowError` depth, and a 200 001-node degenerate tree
  counted **iteratively** where a recursive walk would overflow.
<!-- c8-s13-end -->

## 14. Practice Questions
1. What three things must every correct recursive method have?
2. What error do you get from `static int f(int n) { return f(n - 1); }` and why?
3. Trace `factorial(4)` and list the state of every frame at the moment the base case is hit.
4. In `countdown(int n)` above, why does "unwinding" print in reverse order?
5. Why does `fibNaive(30)` take milliseconds while `fibMemo(30)` takes ~0 ms? What is the complexity of each?
6. Convert the naive `fib` to a bottom-up loop. What memory does it use?
7. Why does Java throw `StackOverflowError` for a tail-recursive method that other languages would optimise?
8. What is the recursion depth of a balanced binary tree with 1 000 000 nodes? And of a degenerate one?
9. Why must backtracking copy the current candidate (`new ArrayList<>(current)`) rather than add it directly?
10. When would you rewrite a recursive traversal using `ArrayDeque` instead?

### Answers
1. A base case, a recursive case, and progress toward the base case.
2. `StackOverflowError` — there is no base case, so frames accumulate until the thread stack is full.
3. Frames `factorial(4)`, `factorial(3)`, `factorial(2)`, `factorial(1)` with `n = 4, 3, 2, 1`; the base
   case returns `1`, then each frame multiplies on the way out (`2, 6, 24`).
4. Because the second print statement runs only **after** the recursive call returns — so it executes as
   the stack unwinds, from the deepest frame outward.
5. Naive recursion recomputes the same subproblems (exponential, ~O(2^n)); memoisation computes each
   subproblem once and reuses it (O(n)).
6. `long a = 0, b = 1; for (int i = 2; i <= n; i++) { long next = a + b; a = b; b = next; }` — O(1) extra
   memory (plus the O(n) or O(1)-if-streaming time).
7. HotSpot does not implement tail-call elimination; `return f(x);` still performs an `invoke` which
   pushes a frame, so the stack grows with depth.
8. Balanced: about 20 levels (log2 of 1 000 000). Degenerate: 1 000 000 levels — far beyond the stack
   limit, so an iterative traversal is mandatory.
9. Because the candidate list is mutated in place by the choose/un-choose steps; adding it directly would
   store a reference to a list that keeps changing, so every recorded permutation would look identical.
10. When depth can be large (deep or untrusted input), when you need pause/resume, or when you want to
    avoid a thread-stack limit entirely — the explicit stack lives on the heap.

## 15. Coding Practice
**Easy** — Write `static int sum(int[] values, int index)` and `static String reverse(String s)`
recursively. Verify against a loop for 0, 1 and 5 elements.
**Medium** — Write `static void printTree(Node node, int indent)` that prints a hierarchy with indentation
carried as a parameter, plus `static int maxDepth(Node node)`. Then make the traversal defensive with a
`MAX_DEPTH` guard that throws a clear exception.
**Hard** — Implement `fibNaive`, `fibMemo` and an iterative `fibBottomUp`. Time all three for `n = 40`
using `System.nanoTime()`, print the results, and explain the measured difference. Then implement
`static <T> List<List<T>> subsets(List<T> items)` with backtracking and reason about its time complexity
(2^n subsets) before running it.
<!-- c8-s15-end -->

## 16. Interview Questions

### Beginner
**Q1. What is recursion?**
- *Testing*: definition.
- *Expected*: a method calling itself on a smaller version of the same problem, with a base case that stops it.
- *Wrong*: "A loop that calls a function."

**Q2. What is a base case and why is it required?**
- *Testing*: the #1 recursion requirement.
- *Expected*: a condition that returns without recursing; without it recursion never terminates and the stack fills.
- *Wrong*: "It is the first line of the method."

**Q3. What error occurs if the base case is missing?**
- *Testing*: runtime knowledge.
- *Expected*: `StackOverflowError`, a `java.lang.Error` thrown from the thread's JVM stack.
- *Wrong*: "Infinite loop" or "OutOfMemoryError".

**Q4. What is the difference between head recursion and tail recursion?**
- *Testing*: recursion shapes.
- *Expected*: tail recursion performs the recursive call as the last operation (nothing to do after it returns); head recursion does work after the call returns (like multiplying by `n`).
- *Wrong*: "Tail recursion is optimised by the JVM."

**Q5. Why is recursion natural for trees?**
- *Testing*: connecting data shape to algorithm.
- *Expected*: a tree is defined recursively (a node plus sub-trees), so a recursive traversal maps directly onto the structure.
- *Wrong*: "Because trees are big."

### Intermediate
**Q6. What is the time complexity of naive recursive Fibonacci, and how do you fix it?**
- *Testing*: complexity + memoisation.
- *Expected*: ~O(2^n) due to recomputed subproblems; memoise with an array/`HashMap` (top-down DP) or use a bottom-up loop → O(n).
- *Wrong*: "O(n), because it makes n calls."

**Q7. How much stack memory does a recursive traversal of a 1 000 000-node degenerate tree need?**
- *Testing*: stack vs heap reasoning.
- *Expected*: one frame per level → 1 000 000 frames, far beyond `-Xss`; it throws `StackOverflowError`. Use an explicit heap stack.
- *Wrong*: "It depends only on the node count, so it is fine."

**Q8. Why is `results.add(current)` wrong in backtracking?**
- *Testing*: aliasing (topic 07).
- *Expected*: `current` is mutated by the choose/un-choose steps, so all stored references point to the same evolving list; you must add a copy.
- *Wrong*: "It throws a `ConcurrentModificationException`."

**Q9. How do you turn a recursive method into an iterative one?**
- *Testing*: the explicit-stack technique.
- *Expected*: replace the call stack with an explicit `Deque` (or queue for BFS), pushing the work that would have been recursive calls and popping in the right order.
- *Wrong*: "It is impossible for tree structures."

**Q10. What does `-Xss` control, and when would you change it?**
- *Testing*: JVM tuning awareness.
- *Expected*: the per-thread stack size. Increase it for legitimately deep recursion, but prefer redesigning to iteration; larger `-Xss` multiplies memory across thousands of threads.
- *Wrong*: "It sets the heap size."

### Advanced
**Q11. Why does Java not perform tail-call optimisation, and what are the consequences?**
- *Testing*: JVM design awareness.
- *Expected*: TCO would break stack-trace fidelity (crucial for debugging/security) and requires changes to the observable stack; consequences are O(depth) stack usage, `StackOverflowError` on deep tail recursion, and the need to rewrite as loops. `invokedynamic`/lambdas do not provide TCO either.
- *Wrong*: "It is optimised by the JIT when the method is small."

**Q12. Is `StackOverflowError` catchable, and should you catch it?**
- *Testing*: Error hierarchy knowledge.
- *Expected*: It is an `Error` and can be caught, but the stack may be unusable, invariants may be broken, and it indicates a design problem. Catch only for diagnostics; do not use as control flow.
- *Wrong*: "It cannot be caught at all."

**Q13. How would you detect a potential `StackOverflowError` before production?**
- *Testing*: engineering practice.
- *Expected*: bound/validate nesting depth at the boundary (JSON, XML, uploads), add a depth-limit test with adversarial input, add a maximum-depth constant with a clear exception, and prefer iterative traversal for untrusted structures.
- *Wrong*: "Set `-Xss` very high."

**Q14. Convert a recursive descent parser to an iterative one. What is hard about it?**
- *Testing*: deep understanding of the call stack.
- *Expected*: you must model the call stack explicitly (state machine per grammar rule with an explicit stack of "what to do next"), which is why recursive descent is usually kept — but grammars validated for bounded depth can remain recursive.
- *Wrong*: "It cannot be done."

**Q15. Mutual recursion — any extra risks?**
- *Testing*: reasoning about indirect cycles.
- *Expected*: the termination argument must cover *both* methods (a shared decreasing measure), the depth grows twice as fast per logical step, and stack traces alternate frames, making diagnosis harder.
- *Wrong*: "There are no risks; it is the same as self-recursion."
<!-- c8-s16a-end -->

## 17. Production-Level Questions
1. **Hostile input:** an API accepts nested JSON and a client sends 200 000 levels of nesting. What happens on the request thread, why can your `@ExceptionHandler` fail to help, and what are two defensive designs?
2. **Stack vs heap:** an org-chart export works in the test environment but fails in production for one large customer. What is the likely shape of the failing hierarchy, and which rewrite fixes it?
3. **Thread pools:** deep recursion runs inside a fixed thread pool and one worker dies with `StackOverflowError`. What is the blast radius, and what would you change first?
4. **Performance:** a pricing service uses recursive memoised calculation and a `HashMap` keyed by a `String` built with concatenation. Where is the hidden cost, and how would you redesign the key and the cache?
5. **Correctness:** a backtracking scheduler produces thousands of identical results. Which line of code is almost certainly wrong, and why does the fix require a copy?
<!-- c8-s17-end -->

## 18. What I Should Remember
1. Recursion = base case + recursive case + progress. Miss any one and it breaks.
2. Every call pushes a frame; depth is limited by the per-thread stack (`-Xss`) → `StackOverflowError`.
3. Java has **no tail-call optimisation**; rewrite tail recursion as a loop.
4. Linear recursion is O(depth) frames; branching recursion is exponential in time without memoisation.
5. Memoisation turns O(2^n) into O(n) for overlapping subproblems.
6. Backtracking = choose / explore / un-choose, and always store a **copy** of the candidate.
7. Recursion depth depends on the structure's **height**; use an explicit `ArrayDeque` for deep/wild input.
8. Guard untrusted nesting with a depth limit; treat `StackOverflowError` as a design defect, not an error to catch.

## 19. Connection To Other Java Topics
```
Topic 04 Methods (a call pushes a frame)
Topic 07 Pass-by-Value (each frame keeps its own copies)
        ↓
Topic 08 Recursion                      <-- you are here
        ↓
Divide and conquer algorithms → Merge/Quick sort, binary search
Tree structures → JSON/XML parsing, file trees, expression parsers
Dynamic programming → memoisation → caching strategies
        ↓
Collections (Deque/ArrayDeque as an explicit stack) → Streams (recursive pipelines)
        ↓
Concurrency: each thread has its own stack (why deep recursion behaves differently in pools)
```
The explicit-stack idea here is also how you make recursive work resumable — which becomes essential
for batch jobs and for avoiding long-lived thread stacks in a thread pool.

## 20. One-Minute Revision
- Base case + recursive case + progress, every time.
- Each call = one frame; too many frames = `StackOverflowError` (an `Error`).
- No TCO in Java: tail recursion is not constant-space → use a loop.
- Naive branching recursion is exponential; memoise → O(n).
- Backtracking needs choose/explore/un-choose and a **copy** of the result.
- Deep or untrusted input → explicit stack on the heap, plus a depth guard.




