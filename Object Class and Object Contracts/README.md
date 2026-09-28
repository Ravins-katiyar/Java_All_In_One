# Object Class and Object Contracts (Module Index)

## What This Module Is

This module is the bridge between "Java syntax" and "Java object-oriented design".
The previous modules taught you how a program *runs* (Execution Model), how it stores
*values* (Variables and Data Types) and how it *decides* (Operators and Control Flow).
None of those modules created a single object.

Here you learn what an object actually **is** in memory, how it is **created**, how its
**state and behaviour** are expressed, how methods are **chosen** by the compiler, how
arguments travel across a method call, and finally the **contract every Java object
silently signs** with the JVM (`equals`, `hashCode`, `toString`, `getClass`, `clone`).

That last part is the reason this module exists. Without it, `HashMap`, `HashSet`,
`record`, JPA entities, caching and Spring beans will look like magic instead of code.

## Prerequisites (from Java Fundamentals)

| You must already know | From module |
|---|---|
| Heap vs Stack, references, class loading | `Java Fundamentals/01-Java-Execution-Model` |
| Primitives vs references, wrapper classes, autoboxing | `Java Fundamentals/02-Variables-Data-Types` |
| Operators, control flow, modern `switch` | `Java Fundamentals/03-Operators-Control-Flow` |

## Learning Order (Dependency Map)

```
Classes and Objects            (01)  <- you cannot understand anything below without this
      |
      v
Constructors                   (02)  <- how objects are born + initialisation order
      |
      v
this and super                 (03)  <- how the object and its parent refer to themselves
      |
      v
Methods                        (04)  <- behaviour + the idea of a method signature
      |
      +--> Method Overloading  (05)  <- compile-time method selection (needs signatures)
      |
      +--> Varargs             (06)  <- flexible arity + why it is the "last resort" overload
      |
      v
Pass-by-Value                  (07)  <- what really crosses a method boundary
      |
      v
Recursion                      (08)  <- a method calling itself; uses Stack + pass-by-value
      |
      v
static and final               (09)  <- state that is not per-object / state that cannot change
      |
      v
Access Modifiers               (10)  <- who is allowed to touch all of the above
      |
      v
Object Class and Contracts     (11)  <- the contract that makes HashMap/HashSet/record work
```

Note: `11` is the **culmination**, not the first topic, even though the module is named
after it. `equals()`/`hashCode()` need overriding (topic 04/05), inheritance and
`super` (topic 03) and instance fields (topic 01). Studying the contract first would be
memorising two method names without understanding when they are called.

## Folder Layout

```
Object Class and Object Contracts/
├── 01-Classes-and-Objects/            README.md + runnable examples
├── 02-Constructors/                   README.md + runnable examples
├── 03-this-and-super/                 README.md + runnable examples
├── 04-Methods/                        README.md + runnable examples
├── 05-Method-Overloading/             README.md + runnable examples
├── 06-Varargs/                        README.md + runnable examples
├── 07-Pass-by-Value/                  README.md + runnable examples
├── 08-Recursion/                      README.md + runnable examples
├── 09-static-and-final/               README.md + runnable examples
├── 10-Access-Modifiers/               README.md + multi-package examples
└── 11-Object-Class-and-Object-Contracts/ README.md + runnable examples
```

## How To Run The Examples

Every example is a single `public` class with a `main` method (except the
multi-package Access Modifier and inheritance demos).

```bash
# from inside a topic's example folder (each numbered folder holds independent programs)
javac ClassAndObjectDemo.java && java ClassAndObjectDemo

# for the multi-package access-modifier demo
cd 10-Access-Modifiers/01-AccessModifiersDemo
javac library/*.java app/*.java
java app.AccessModifierDemo
```

All code targets **Java 17+/21 syntax** (records, `sealed`, switch expressions,
`instanceof` patterns, text blocks) and was compiled and run on the JDK 21 installed in
this repository.

## The Central Thread: "The Object Contract"

Everything in this module feeds one question that interviewers ask constantly:

> *"Why must a class that overrides `equals()` also override `hashCode()`?"*

The answer is assembled across topics:

```
01 instance fields  ->  what "equal" should even compare
04 overriding       ->  how you change inherited behaviour at runtime
03 super/Object     ->  where the default equals/hashCode come from
11 contract         ->  the 5 rules + HashSet bucket lookup
```

## Connection To Other Java Topics

```
Java Fundamentals (01-03)
        ↓
Object Class and Object Contracts   <-- you are here
        ↓
Inheritance & Polymorphism (deep dive)
        ↓
Interfaces, Abstract Classes, Generics
        ↓
Collections + HashMap Internals
        ↓
Functional Programming, Streams
```

## One-Minute Revision

- An **object** lives on the heap; a **reference** lives on the stack.
- A **constructor** creates and initialises; it is not inherited.
- `this` = current object, `super` = parent part of the current object.
- Overloading = same name, **different parameter lists**, chosen at **compile time**.
- Varargs (`Type...`) = sugar for an array parameter, must be **last**, weakest overload.
- Java is **always pass-by-value**; objects pass the value of a *reference*.
- `static` = belongs to the class, `final` = assign once / cannot override / cannot extend.
- `equals()` without `hashCode()` **breaks `HashMap` and `HashSet`**.
