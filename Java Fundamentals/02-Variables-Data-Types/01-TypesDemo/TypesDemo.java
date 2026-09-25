public class TypesDemo {
    public static void main(String[] args) {
        int age = 30;                  // Primitive
        Integer boxedAge = age;        // Autoboxing (int -> Integer)
        int unboxedAge = boxedAge;     // Unboxing (Integer -> int)

        double salary = 75000.50;
        int roundedSalary = (int) salary; // Narrowing cast (double -> int) -> 75000

        System.out.println("Age: " + unboxedAge);
        System.out.println("Salary: " + roundedSalary);
    }
}
