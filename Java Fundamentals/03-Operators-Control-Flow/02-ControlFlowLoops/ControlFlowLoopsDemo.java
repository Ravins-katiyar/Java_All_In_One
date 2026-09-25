public class ControlFlowLoopsDemo {
    public static void main(String[] args) {
        // For Loop
        System.out.print("For loop: ");
        for (int i = 1; i <= 5; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        // While Loop
        System.out.print("While loop: ");
        int j = 1;
        while (j <= 5) {
            System.out.print(j + " ");
            j++;
        }
        System.out.println();
        
        // Enhanced For Loop
        System.out.print("Enhanced for: ");
        int[] numbers = {10, 20, 30};
        for (int num : numbers) {
            System.out.print(num + " ");
        }
        System.out.println();
    }
}
