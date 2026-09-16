public class BytecodeDemo {

    static int square(int number) {
        return number * number;
    }

    public static void main(String[] args) {

        int number = 10;

        int result = square(number);

        System.out.println("Result = " + result);
    }
}