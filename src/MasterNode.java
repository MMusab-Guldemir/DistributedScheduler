import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MasterNode extends Node {
    private int port;
    private ServerSocket serverSocket;
    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private ExecutorService threadPool;
    
    public MasterNode(String name, int port) {
        super(name);
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
    }
    
    @Override
    public void start() throws Exception {
        isRunning = true;
        serverSocket = new ServerSocket(port);
        log("Master started on port: " + port);
        
        new Thread(this::acceptWorkerConnections).start();
        new Thread(this::checkWorkerHealth).start();
        addSampleTasks();
    }
    
    @Override
    public void stop() throws Exception {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        threadPool.shutdown();
        log("Master stopped.");
    }
    
    private void acceptWorkerConnections() {
        while (isRunning) {
            try {
                Socket workerSocket = serverSocket.accept();
                threadPool.submit(() -> handleWorkerConnection(workerSocket));
            } catch (IOException e) {
                if (isRunning) log("Connection error: " + e.getMessage());
            }
        }
    }
    
    private void handleWorkerConnection(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String registration = in.readLine();
            if (registration != null && registration.startsWith("REGISTER:")) {
                String[] parts = registration.substring(9).split(":");
                String workerName = parts[0];
                int workerPort = Integer.parseInt(parts[1]);
                
                WorkerInfo worker = new WorkerInfo(workerName, 
                    socket.getInetAddress().getHostAddress(), workerPort);
                workers.put(worker.id, worker);
                
                out.println("REGISTERED:" + worker.id);
                log("New worker registered: " + worker.name + " (" + worker.id + ")");
                
                String message;
                while ((message = in.readLine()) != null) {
                    handleWorkerMessage(worker.id, message);
                }
            }
        } catch (IOException e) {
            log("Worker connection error: " + e.getMessage());
        }
    }
    
    private void handleWorkerMessage(String workerId, String message) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.lastHeartbeat = LocalDateTime.now();
            
            if (message.startsWith("HEARTBEAT")) {
                worker.isAlive = true;
                log("Heartbeat from " + worker.name);
            } else if (message.startsWith("TASK_COMPLETED:")) {
                String taskId = message.substring(14);
                Task task = findTask(taskId);
                if (task != null) {
                    task.status = "COMPLETED";
                    worker.taskCount--;
                    log("Task completed: " + task.id + " by " + worker.name);
                }
            } else if (message.startsWith("TASK_FAILED:")) {
                String taskId = message.substring(12);
                Task task = findTask(taskId);
                if (task != null) {
                    task.status = "FAILED";
                    worker.taskCount--;
                    log("Task failed: " + task.id + " by " + worker.name);
                }
            }
        }
    }
    
    private void checkWorkerHealth() {
        while (isRunning) {
            try {
                Thread.sleep(10000); // Check every 10 seconds
                
                LocalDateTime now = LocalDateTime.now();
                for (WorkerInfo worker : workers.values()) {
                    long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(
                        worker.lastHeartbeat, now);
                    
                    if (secondsSinceLastHeartbeat > 30) { // 30 seconds timeout
                        worker.isAlive = false;
                        log("Worker " + worker.name + " is not responding!");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    // Public methods for console interface
    public void addTask(Task task) {
        tasks.add(task);
        log("Task added: " + task.name);
    }
    
    public void listTasks() {
        System.out.println("\n=== TASKS ===");
        if (tasks.isEmpty()) {
            System.out.println("No tasks available.");
        } else {
            for (Task task : tasks) {
                System.out.println(task);
            }
        }
    }
    
    public void listWorkers() {
        System.out.println("\n=== WORKERS ===");
        if (workers.isEmpty()) {
            System.out.println("No workers registered.");
        } else {
            for (WorkerInfo worker : workers.values()) {
                System.out.println(worker);
            }
        }
    }
    
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }
    
    public boolean hasWorkers() {
        return !workers.isEmpty();
    }
    
    public void sendTaskToWorker(String taskId) {
        Task task = findTask(taskId);
        if (task == null) {
            System.out.println("Task not found!");
            return;
        }
        
        if (task.status.equals("COMPLETED") || task.status.equals("RUNNING")) {
            System.out.println("Task is already " + task.status.toLowerCase());
            return;
        }
        
        // Find available worker using round-robin
        List<WorkerInfo> availableWorkers = workers.values().stream()
            .filter(w -> w.isAlive)
            .toList();
        
        if (availableWorkers.isEmpty()) {
            System.out.println("No available workers!");
            return;
        }
        
        int index = roundRobinIndex.getAndIncrement() % availableWorkers.size();
        WorkerInfo selectedWorker = availableWorkers.get(index);
        
        // Send task to worker
        try (Socket socket = new Socket(selectedWorker.host, selectedWorker.port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println("EXECUTE_TASK:" + task.toNetworkString());
            task.status = "RUNNING";
            task.assignedWorker = selectedWorker.id;
            selectedWorker.taskCount++;
            
            System.out.println("Task " + taskId + " sent to " + selectedWorker.name);
            log("Task " + taskId + " assigned to " + selectedWorker.name);
            
        } catch (IOException e) {
            System.out.println("Failed to send task: " + e.getMessage());
            task.status = "FAILED";
        }
    }
    
    private Task findTask(String taskId) {
        return tasks.stream()
            .filter(t -> t.id.equals(taskId))
            .findFirst()
            .orElse(null);
    }
    
    private void addSampleTasks() {
        addTask(new Task("System Info", "systeminfo", 5));
        addTask(new Task("Directory Listing", "dir", 3));
        addTask(new Task("Network Check", "ping localhost", 7));
        addTask(new Task("Disk Space", "df -h", 6));
        log("Sample tasks added");
    }
}