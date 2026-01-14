import java.time.LocalDateTime;
import java.util.UUID;

class WorkerInfo {
    String id;
    String name;
    String host;
    int port;
    boolean isAlive;
    LocalDateTime lastHeartBeat;
    int taskCount;


    public WorkerInfo(String name, String host, int port){
        this.id = "Worker-" + UUID.randomUUID().toString().substring(0,8);

        this.name = name;
        this.host = host;
        this.port = port;
        this.isAlive = true;
        this.lastHeartBeat = LocalDateTime.now();
        this.taskCount = 0;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s @ %s.%d - Görev: %d - Canli: %s", id, name, host, port, taskCount, isAlive ? "✅": "❌");
    }
}


