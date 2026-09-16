public class JitDemo {

    public static long calculate() {

        long result = 0;

        for (int i = 0; i < 100_000_000; i++) {
            result += i;
        }

        return result;
    }

    public static void main(String[] args) {

        long result = calculate();

        System.out.println(result);
    }
}