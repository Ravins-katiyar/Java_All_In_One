public class TypeCastingDemo {
    public static void main(String[] args) {
        // Widening (Implicit)
        int num = 100;
        long largeNum = num;
        double decimalNum = largeNum;
        System.out.println("Widening: int " + num + " -> double " + decimalNum);

        // Narrowing (Explicit)
        double price = 99.99;
        int roundedPrice = (int) price; // Truncates decimal part
        System.out.println("Narrowing: double " + price + " -> int " + roundedPrice);
    }
}
