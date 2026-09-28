import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Topic 11 - equals() and hashCode() (the real contract).
 *
 * The two contracts, from java.lang.Object's own documentation:
 *
 * equals(Object):
 * 1. reflexive : x.equals(x) is true
 * 2. symmetric : x.equals(y) == y.equals(x)
 * 3. transitive : x=y and y=z implies x=z
 * 4. consistent : repeated calls return the same result (no randomness)
 * 5. non-null : x.equals(null) is false (and never throws)
 *
 * hashCode():
 * 1. consistent within one execution, while equals-relevant state is unchanged
 * 2. equal objects MUST have equal hash codes
 * 3. unequal objects are NOT required to have different hash codes
 * (collisions are legal: performance suffers, correctness does not)
 *
 * Run: javac EqualsHashCodeContractDemo.java && java EqualsHashCodeContractDemo
 */
public class EqualsHashCodeContractDemo {

    public static void main(String[] args) {

        System.out.println("--- 1. equals() without hashCode(): broken in every hash structure ---");
        BrokenEquals b1 = new BrokenEquals("A-1");
        BrokenEquals b2 = new BrokenEquals("A-1");
        System.out.println("b1.equals(b2)          : " + b1.equals(b2));
        System.out.println("hashCodes equal?       : " + (b1.hashCode() == b2.hashCode()));

        Map<BrokenEquals, String> brokenMap = new HashMap<>();
        brokenMap.put(b1, "value-of-A-1");
        System.out.println("map.get(equal key)     : " + brokenMap.get(b2) + "   <- null: different bucket");

        Set<BrokenEquals> brokenSet = new HashSet<>();
        brokenSet.add(b1);
        brokenSet.add(b2);
        System.out.println("set.size() after 2 add : " + brokenSet.size() + "   <- 2: duplicates kept");

        System.out.println();
        System.out.println("--- 2. equals() + hashCode(): behaves correctly ---");
        Good g1 = new Good("A-1");
        Good g2 = new Good("A-1");
        System.out.println("g1.equals(g2)          : " + g1.equals(g2));
        System.out.println("hashCodes equal?       : " + (g1.hashCode() == g2.hashCode()));

        Map<Good, String> goodMap = new HashMap<>();
        goodMap.put(g1, "value-of-A-1");
        System.out.println("map.get(equal key)     : " + goodMap.get(g2));

        Set<Good> goodSet = new HashSet<>();
        goodSet.add(g1);
        goodSet.add(g2);
        System.out.println("set.size() after 2 add : " + goodSet.size() + "   <- 1: deduplicated");

        List<Good> list = new ArrayList<>(List.of(g1));
        System.out.println("list.contains(equal)   : " + list.contains(g2));

        System.out.println();
        System.out.println("--- 3. never mutate a field used by hashCode while it is a map key ---");
        Good key = new Good("A-1");
        Map<Good, String> map = new HashMap<>();
        map.put(key, "value");
        key.setId("A-2"); // changes the hash code
        System.out.println("map.get(key)           : " + map.get(key) + "   <- null: entry is unreachable");
        System.out.println("map.size()             : " + map.size() + "          <- still 1: leaked entry");

        System.out.println();
        System.out.println("--- 4. symmetry: instanceof vs getClass() in equals ---");
        Base base = new Base("1");
        Sub sub = new Sub("1", "extra");
        System.out.println("sub.equals(base)       : " + sub.equals(base));
        System.out.println("base.equals(sub)       : " + base.equals(sub) + "   <- ASYMMETRIC: violates the contract");
        Set<Base> mixed = new HashSet<>();
        mixed.add(base);
        mixed.add(sub);
        System.out.println("mixed set size         : " + mixed.size() + "   <- confusing results follow");

        System.out.println();
        System.out.println("--- 5. the strict fix: getClass() in equals ---");
        StrictBase sb = new StrictBase("1");
        StrictSub ss = new StrictSub("1", "extra");
        System.out.println("ss.equals(sb)          : " + ss.equals(sb));
        System.out.println("sb.equals(ss)          : " + sb.equals(ss) + "   <- symmetric (subtypes are never equal)");

        System.out.println();
        System.out.println("--- 6. == vs equals() for String ---");
        String literal = "account";
        String constructed = new String("account");
        String interned = constructed.intern();
        System.out.println("literal == constructed : " + (literal == constructed) + "   (different objects)");
        System.out.println("literal.equals(constructed) : " + literal.equals(constructed));
        System.out.println("literal == interned    : " + (literal == interned) + "   (same pooled object)");
    }
}

/* -------------------------------------------------------------- 1. broken */

/** Overrides equals but forgets hashCode: the classic HashMap bug. */
class BrokenEquals {

    private final String id;

    BrokenEquals(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BrokenEquals that && id.equals(that.id);
    }
    // hashCode() NOT overridden -> identity hash, so equal objects land in
    // different buckets.
}

/* -------------------------------------------------------------- 2. correct */

/**
 * The canonical shape: identity check, pattern match, field comparison,
 * matching hashCode.
 */
class Good {

    private String id;

    Good(String id) {
        this.id = id;
    }

    void setId(String id) {
        this.id = id; // deliberately mutable: used to show the map-key bug
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { // fast path + reflexivity
            return true;
        }
        if (!(other instanceof Good other2)) { // handles null and wrong type
            return false;
        }
        return id.equals(other2.id); // compare only equals-relevant fields
    }

    @Override
    public int hashCode() {
        return id.hashCode(); // must agree with equals
    }

    @Override
    public String toString() {
        return "Good[" + id + "]";
    }
}

/* ------------------------------------------ 4. asymmetric (broken) equals */

class Base {

    final String id;

    Base(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        // instanceof makes a subclass "equal" in one direction only -> asymmetric.
        return other instanceof Base other2 && id.equals(other2.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

class Sub extends Base {

    final String extra;

    Sub(String id, String extra) {
        super(id);
        this.extra = extra;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Sub other2 && id.equals(other2.id) && extra.equals(other2.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, extra);
    }
}

/* ------------------------------------------ 5. symmetric (strict) equals */

class StrictBase {

    final String id;

    StrictBase(String id) {
        this.id = id;
    }

    /**
     * getClass() makes equality strict: instances of different runtime classes are
     * never equal.
     */
    @Override
    public boolean equals(Object other) {
        return this == other
                || (other != null
                        && getClass() == other.getClass()
                        && id.equals(((StrictBase) other).id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

final class StrictSub extends StrictBase {

    final String extra;

    StrictSub(String id, String extra) {
        super(id);
        this.extra = extra;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other != null
                        && getClass() == other.getClass()
                        && id.equals(((StrictSub) other).id)
                        && extra.equals(((StrictSub) other).extra));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, extra);
    }
}