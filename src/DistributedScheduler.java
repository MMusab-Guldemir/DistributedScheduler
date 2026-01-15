import java.util.Scanner;

public class DistributedScheduler {
    public static void main(String[] args) throws Exception {
        System.out.println("=== DISTRIBUTED TASK SCHEDULER SYSTEM ===");
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Select mode to run:");
        System.out.println("1. Master Node (Controller)");
        System.out.println("2. Worker Node (Worker)");
        System.out.println("3. Test Mode (Master + 2 Workers)");
        System.out.print("Your choice (1-3): ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch(choice) {
            case 1:
                startMasterNode();
                break;
            case 2:
                startWorkerNode();
                break;
            case 3:
                startTestMode();
                break;
            default:
                System.out.println("Invalid choice!");
        }
        
        scanner.close();
    }
    
    private static void startTestMode() {
        System.out.println("\n⚙️ STARTING TEST MODE...");
        
        Thread masterThread = new Thread(() -> {
            try {
                MasterNode master = new MasterNode("Test-Master", 9090);
                master.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        masterThread.start();
        
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        
        for (int i = 1; i <= 2; i++) {
            final int workerId = i;
            Thread workerThread = new Thread(() -> {
                try {
                    WorkerNode worker = new WorkerNode("Worker-" + workerId, "localhost", 9090, 9090 + workerId);
                    worker.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            workerThread.start();
        }
    }
    
    private static void startMasterNode() throws Exception {
        System.out.println("\n👑 STARTING MASTER NODE");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Master name: ");
        String name = scanner.nextLine();
        System.out.print("Port number (9090): ");
        int port = scanner.nextInt();
        scanner.nextLine();
        
        MasterNode master = new MasterNode(name, port);
        master.start();
        
        while (true) {
            System.out.println("\n=== MASTER CONSOLE ===");
            System.out.println("1. Add new task");
            System.out.println("2. List all tasks");
            System.out.println("3. List workers");
            System.out.println("4. Send task to worker");
            System.out.println("5. Exit");
            System.out.print("Choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch(choice) {
                case 1:
                    System.out.print("Task name: ");
                    String taskName = scanner.nextLine();
                    System.out.print("Command: ");
                    String command = scanner.nextLine();
                    System.out.print("Priority (1-10): ");
                    int priority = scanner.nextInt();
                    scanner.nextLine();
                    
                    Task task = new Task(taskName, command, priority);
                    master.addTask(task);
                    System.out.println("✅ Task added: " + task.id);
                    break;
                    
                case 2:
                    master.listTasks();
                    break;
                case 3:
                    master.listWorkers();
                    break;
                case 4:
                    if (master.hasTasks() && master.hasWorkers()) {
                        System.out.print("Task ID to send: ");
                        String taskId = scanner.nextLine();
                        master.sendTaskToWorker(taskId);
                    } else {
                        System.out.println("⚠️ No tasks or workers available!");
                    }
                    break;
                case 5:
                    master.stop();
                    System.out.println("Master stopped.");
                    scanner.close();
                    return;
            }
        }
    }
    
    private static void startWorkerNode() throws Exception {
        System.out.println("\n🔧 STARTING WORKER NODE");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Worker name: ");
        String name = scanner.nextLine();
        System.out.print("Master address (localhost): ");
        String masterHost = scanner.nextLine();
        if (masterHost.isEmpty()) masterHost = "localhost";
        
        System.out.print("Master port (9090): ");
        int masterPort = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Worker port (9091): ");
        int workerPort = scanner.nextInt();
        scanner.nextLine();
        
        WorkerNode worker = new WorkerNode(name, masterHost, masterPort, workerPort);
        worker.start();
        
        System.out.println("\n✅ Worker started! Waiting for tasks from master...");
        System.out.println("Type 'exit' to quit.");
        
        while(true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                worker.stop();
                System.out.println("Worker stopped.");
                scanner.close();
                return;
            }
        }
    }
}