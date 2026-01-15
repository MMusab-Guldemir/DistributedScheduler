import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerNode extends Node {
    private String masterHost;
    private int masterPort;
    private int workerPort;
    private ServerSocket workerSocket;
    private Socket masterConnection;
    private PrintWriter masterOut;
    private BufferedReader masterIn;
    private ScheduledExecutorService scheduler;
    
    public WorkerNode(String name, String masterHost, int masterPort, int workerPort) {
        super(name);
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.workerPort = workerPort;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    @Override
    public void start() throws Exception {
        isRunning = true;
        
        // Connect to master
        connectToMaster();
        
        // Start listening for tasks
        startTaskServer();
        
        // Start heartbeat
        startHeartbeat();
        
        log("Worker started and connected to master at " + masterHost + ":" + masterPort);
    }
    
    @Override
    public void stop() throws Exception {
        isRunning = false;
        
        if (masterConnection != null && !masterConnection.isClosed()) {
            masterConnection.close();
        }
        
        if (workerSocket != null && !workerSocket.isClosed()) {
            workerSocket.close();
        }
        
        scheduler.shutdown();
        log("Worker stopped.");
    }
    
    private void connectToMaster() throws IOException {
        masterConnection = new Socket(masterHost, masterPort);
        masterOut = new PrintWriter(masterConnection.getOutputStream(), true);
        masterIn = new BufferedReader(new InputStreamReader(masterConnection.getInputStream()));
        
        // Register with master
        masterOut.println("REGISTER:" + name + ":" + workerPort);
        
        String response = masterIn.readLine();
        if (response != null && response.startsWith("REGISTERED:")) {
            log("Registered with master: " + response);
        } else {
            throw new IOException("Failed to register with master");
        }
    }
    
    private void startTaskServer() {
        new Thread(() -> {
            try {
                workerSocket = new ServerSocket(workerPort);
                log("Task server listening on port " + workerPort);
                
                while (isRunning) {
                    Socket taskSocket = workerSocket.accept();
                    handleTaskConnection(taskSocket);
                }
            } catch (IOException e) {
                if (isRunning) log("Task server error: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleTaskConnection(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String command = in.readLine();
            if (command != null && command.startsWith("EXECUTE_TASK:")) {
                String taskString = command.substring(13);
                Task task = Task.fromNetworkString(taskString);
                
                log("Received task: " + task.name);
                out.println("TASK_ACCEPTED:" + task.id);
                
                // Execute task in separate thread
                new Thread(() -> executeTask(task)).start();
            }
        } catch (IOException e) {
            log("Task connection error: " + e.getMessage());
        }
    }
    
    private void executeTask(Task task) {
        try {
            log("Executing task: " + task.command);
            
            // Simulate task execution
            Thread.sleep(3000); // Simulate work
            
            // For real command execution, you would use:
            // Process process = Runtime.getRuntime().exec(task.command);
            
            String result = "Task '" + task.name + "' executed successfully at " + LocalDateTime.now();
            log(result);
            
            // Notify master
            masterOut.println("TASK_COMPLETED:" + task.id);
            
        } catch (Exception e) {
            log("Task execution failed: " + e.getMessage());
            masterOut.println("TASK_FAILED:" + task.id);
        }
    }
    
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isRunning && masterOut != null) {
                try {
                    masterOut.println("HEARTBEAT");
                } catch (Exception e) {
                    log("Heartbeat failed: " + e.getMessage());
                }
            }
        }, 0, 5, TimeUnit.SECONDS); // Send heartbeat every 5 seconds
    }
}