public class ModernSwitchDemo {
    public static void main(String[] args) {
        String day = "SATURDAY";
        
        // Modern Switch Expression (Java 14+)
        String typeOfDay = switch (day) {
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
            case "SATURDAY", "SUNDAY" -> "Weekend";
            default -> {
                System.out.println("Unknown day specified");
                yield "Invalid Day"; // yield is used inside a block
            }
        };
        
        System.out.println(day + " is a " + typeOfDay);
    }
}
