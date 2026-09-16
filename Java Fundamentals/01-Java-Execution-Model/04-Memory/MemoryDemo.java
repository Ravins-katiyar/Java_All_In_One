public class MemoryDemo {

    static void calculate() {

        int x = 10;
        int y = 20;

        Person person = new Person("Ravins");

        System.out.println(x + y);
        System.out.println(person.name);
    }

    public static void main(String[] args) {

        calculate();
    }
}

class Person {

    String name;

    Person(String name) {
        this.name = name;
    }
}