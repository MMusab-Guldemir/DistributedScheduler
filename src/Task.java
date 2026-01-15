import java.time.LocalDateTime;
import java.util.UUID;

public class Task {
    public String id;
    public String name;
    public String command;
    public int priority;
    public String status;
    public String assignedWorker;
    public LocalDateTime createdAt;
    
    public Task(String name, String command, int priority) {
        this.id = "TASK-" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.command = command;
        this.priority = priority;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (Priority: %d) - Status: %s", 
            id, name, priority, status);
    }
    
    public String toNetworkString() {
        return String.format("%s|%s|%s|%d|%s", id, name, command, priority, status);
    }
    
    public static Task fromNetworkString(String networkString) {
        String[] parts = networkString.split("\\|");
        Task task = new Task(parts[1], parts[2], Integer.parseInt(parts[3]));
        task.id = parts[0];
        task.status = parts.length > 4 ? parts[4] : "PENDING";
        return task;
    }
}