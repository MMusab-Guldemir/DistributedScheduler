import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Node {
    protected String id;
    protected String name;
    protected boolean isRunning;
    protected final List<String> logs = new ArrayList<>();
    
    public Node(String name) {
        this.id = "NODE-" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.isRunning = false;
    }
    
    protected void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = String.format("[%s] %s: %s", timestamp, name, message);
        logs.add(logEntry);
        System.out.println(logEntry);
    }
    
    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;
} 