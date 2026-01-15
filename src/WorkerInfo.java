import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerInfo {
    public String id;
    public String name;
    public String host;
    public int port;
    public boolean isAlive;
    public LocalDateTime lastHeartbeat;
    public int taskCount;
    
    public WorkerInfo(String name, String host, int port) {
        this.id = "WORKER-" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.host = host;
        this.port = port;
        this.isAlive = true;
        this.lastHeartbeat = LocalDateTime.now();
        this.taskCount = 0;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s @ %s:%d - Tasks: %d - Alive: %s", 
            id, name, host, port, taskCount, isAlive ? "✅" : "❌");
    }
} 