public class WrapperClassesDemo {
    public static void main(String[] args) {
        // Autoboxing
        Integer count = 100;
        
        // Unboxing
        int rawCount = count;
        
        // Integer Caching (-128 to 127)
        Integer a = 100;
        Integer b = 100;
        System.out.println("a == b for 100: " + (a == b)); // true

        Integer x = 200;
        Integer y = 200;
        System.out.println("x == y for 200: " + (x == y)); // false
        System.out.println("x.equals(y) for 200: " + x.equals(y)); // true
    }
}
