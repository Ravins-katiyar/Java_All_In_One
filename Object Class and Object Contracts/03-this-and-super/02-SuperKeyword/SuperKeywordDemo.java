/**
 * Topic 03 - this and super (Intermediate example: 'super').
 *
 * Shows:
 *   - 'super(...)' running the parent constructor first
 *   - 'super.describe()' reusing parent behaviour and extending it
 *   - the fact that 'super' addresses the SAME object (no parent object is created)
 *   - two fields with the same name, and the difference between this.id and super.id
 *
 * Run: javac SuperKeywordDemo.java && java SuperKeywordDemo
 */
public class SuperKeywordDemo {

    public static void main(String[] args) {

        Child child = new Child("C-1", "P-9");
        System.out.println("describe        : " + child.describe());
        System.out.println("parent field    : " + child.parentId());
        System.out.println("same object?    : " + (child == (Object) child));
    }
}

class Parent {

    protected final String id;                 // parent's own id

    Parent(String id) {
        System.out.println("Parent(String) constructor runs first");
        this.id = id;
    }

    String describe() {
        return "Parent[id=" + id + "]";
    }
}

class Child extends Parent {

    private final String id;                   // deliberately hides Parent.id (usually a bad idea)

    Child(String childId, String parentId) {
        super(parentId);                       // MUST be the first statement
        System.out.println("Child(String, String) constructor body runs second");
        this.id = childId;                     // 'this' = current class declaration
    }

    @Override
    String describe() {
        // Reuse the parent's implementation, then add this class's state.
        return super.describe() + " -> Child[id=" + this.id + "]";
    }

    /** super.id reads the parent's field; this.id reads the child's. Same object, two fields. */
    String parentId() {
        return super.id;
    }
}
