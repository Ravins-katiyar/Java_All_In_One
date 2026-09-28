/**
 * Topic 04 - Methods (Advanced example: stack frames and the call stack).
 *
 * Shows what a method call really costs at runtime:
 *   - every call pushes a frame onto the calling thread's JVM stack
 *   - the thread's stack trace exposes those frames
 *   - unbounded recursion exhausts the stack -> StackOverflowError (an Error, not an Exception)
 *
 * Run: javac StackFrameDemo.java && java StackFrameDemo
 */
public class StackFrameDemo {

    static int depth;

    public static void main(String[] args) {

        System.out.println("--- current frames (main -> printFrames -> getStackTrace) ---");
        printFrames();

        System.out.println("--- outbound frames (main -> levelOne -> levelTwo -> levelThree) ---");
        levelOne();

        System.out.println("--- exhausting the stack ---");
        System.out.println("deepest frame reached : " + deepestFrame());
    }

    /** Prints the frames currently on this thread's stack, innermost first. */
    static void printFrames() {
        StackTraceElement[] frames = Thread.currentThread().getStackTrace();
        int limit = Math.min(frames.length, 5);
        for (int i = 0; i < limit; i++) {
            System.out.println("  frame " + i + " -> "
                    + frames[i].getClassName() + "." + frames[i].getMethodName()
                    + ":" + frames[i].getLineNumber());
        }
    }

    static void levelOne()   { levelTwo(); }
    static void levelTwo()   { levelThree(); }
    static void levelThree() { System.out.println("  levelThree reached (3 nested frames)"); }

    /** No base case on purpose: the stack grows until the JVM refuses to push more frames. */
    static int activate(int frameNumber) {
        depth = frameNumber;
        return activate(frameNumber + 1);   // HotSpot performs no tail-call elimination
    }

    static int deepestFrame() {
        try {
            activate(0);
            return -1;                                  // unreachable
        } catch (StackOverflowError error) {            // an Error, not an Exception
            return depth;                               // last frame number that was pushed
        }
    }
}
