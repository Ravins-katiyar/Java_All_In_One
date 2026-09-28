/**
 * Topic 08 - Recursion (Intermediate example: tree traversal).
 *
 * A tree is a self-similar structure, which is why recursion expresses traversals
 * more cleanly than loops. This file shows:
 *   - the three depth-first orders (pre-order, in-order, post-order)
 *   - node count and height with a single recursive walk
 *   - a small org-chart style hierarchy traversal that carries "depth"
 *
 * Run: javac TreeTraversalDemo.java && java TreeTraversalDemo
 */
import java.util.ArrayList;
import java.util.List;

public class TreeTraversalDemo {

    public static void main(String[] args) {

        //          8
        //        /   \
        //       3     10
        //      / \      \
        //     1   6      14
        //        / \    /
        //       4   7  13
        Node root = new Node(8,
                new Node(3,
                        new Node(1, null, null),
                        new Node(6,
                                new Node(4, null, null),
                                new Node(7, null, null))),
                new Node(10,
                        null,
                        new Node(14,
                                new Node(13, null, null),
                                null)));

        System.out.println("pre-order  : " + preOrder(root));   // root, left, right
        System.out.println("in-order   : " + inOrder(root));    // sorted for a binary SEARCH tree
        System.out.println("post-order : " + postOrder(root));  // left, right, root
        System.out.println("node count : " + count(root));
        System.out.println("height     : " + height(root) + "  (edges on the longest root-to-leaf path)");
        System.out.println("contains 7 : " + contains(root, 7));
        System.out.println("contains 5 : " + contains(root, 5));

        System.out.println();
        System.out.println("--- org hierarchy with carried depth ---");
        Employee ceo = new Employee("CEO", null);
        Employee vp = new Employee("VP Engineering", ceo);
        Employee lead = new Employee("Team Lead", vp);
        Employee dev = new Employee("Developer", lead);

        System.out.println("levels above Developer: " + levelsAbove(dev));
        List<String> chain = new ArrayList<>();
        collectChain(dev, chain);
        System.out.println("chain (self first)    : " + chain);
    }

    /* ------------------------------------------------------ depth-first orders */

    static List<Integer> preOrder(Node node) {
        List<Integer> out = new ArrayList<>();
        preOrder(node, out);
        return out;
    }

    private static void preOrder(Node node, List<Integer> out) {
        if (node == null) return;              // base case
        out.add(node.value);
        preOrder(node.left, out);
        preOrder(node.right, out);
    }

    static List<Integer> inOrder(Node node) {
        List<Integer> out = new ArrayList<>();
        inOrder(node, out);
        return out;
    }

    private static void inOrder(Node node, List<Integer> out) {
        if (node == null) return;
        inOrder(node.left, out);
        out.add(node.value);                   // visit BETWEEN the two recursive calls
        inOrder(node.right, out);
    }

    static List<Integer> postOrder(Node node) {
        List<Integer> out = new ArrayList<>();
        postOrder(node, out);
        return out;
    }

    private static void postOrder(Node node, List<Integer> out) {
        if (node == null) return;
        postOrder(node.left, out);
        postOrder(node.right, out);
        out.add(node.value);                   // visit AFTER both recursive calls
    }

    /* ----------------------------------------------------------- tree metrics */

    static int count(Node node) {
        if (node == null) return 0;
        return 1 + count(node.left) + count(node.right);
    }

    static int height(Node node) {
        if (node == null) return -1;           // empty tree = -1 so a leaf is 0
        return 1 + Math.max(height(node.left), height(node.right));
    }

    static boolean contains(Node node, int target) {
        if (node == null)       return false;  // base case 1
        if (node.value == target) return true; // base case 2
        return contains(node.left, target) || contains(node.right, target);
    }

    /* ---------------------------------------------------------------- hierarchy */

    /** Walks UP a manager chain: recursion with a single branch. */
    static int levelsAbove(Employee employee) {
        Employee manager = employee.manager;
        if (manager == null) {
            return 0;                          // base case: reached the CEO
        }
        return 1 + levelsAbove(manager);
    }

    /** Collects the chain from the employee up to the top. */
    static void collectChain(Employee employee, List<String> chain) {
        if (employee == null) return;          // base case
        chain.add(employee.title);
        collectChain(employee.manager, chain);
    }
}

class Node {
    final int value;
    final Node left;
    final Node right;

    Node(int value, Node left, Node right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }
}

class Employee {
    final String title;
    final Employee manager;

    Employee(String title, Employee manager) {
        this.title = title;
        this.manager = manager;
    }
}
