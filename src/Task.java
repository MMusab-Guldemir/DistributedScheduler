import java.time.LocalDateTime;
import java.util.UUID;

class Task {
    String id;
    String name;
    String command;
    int priority;
    String status;
    String assignedWorker;
    LocalDateTime createdAt;

    public Task(String name, String command, int priority) {
        this.id = "TASK-" + UUID.randomUUID().toString().substring(0,8);
        this.name = name;
        this.command = command;
        this.priority = priority;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (Öncelik: %d) - Durum %s", id,
            name, priority, status);
   }

   public String toNetworkString() {
    return String.format("%s|%s|%s|%d", id, name, command, priority);

   }

   public static Task fromNerworkString(String newworkString) {

    String[] parts = newworkString.split("\\|");
    Task task = new Task(parts[1], parts[2], Integer.parseInt(parts[3]));
    task.id = parts[0];
    return task;
   }
}
