public class ControlFlowDemo {
    public static void main(String[] args) {
        int score = 85;
        String grade = (score >= 90) ? "A" : (score >= 80) ? "B" : "C";

        String status = "ACTIVE";
        String response = switch (status) {
            case "ACTIVE", "RUNNING" -> "System is operational";
            case "PENDING" -> "System is starting up";
            default -> "Unknown status";
        };

        System.out.println("Grade: " + grade);
        System.out.println("Status Response: " + response);
    }
}
