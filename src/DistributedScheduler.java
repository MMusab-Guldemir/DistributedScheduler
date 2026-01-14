[15:09, 14.01.2026] Musab Guldemir: import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.loading.PrivateClassLoader;
import javax.swing.plaf.basic.BasicScrollPaneUI.ViewportChangeHandler;
import javax.swing.plaf.basic.BasicTreeUI.NodeDimensionsHandler;

import java.beans.beancontext.BeanContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.StringConcatFactory;
import java.net.FileNameMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.SocketSecurityException;
import java.security.PrivateKey;
import java.text.StringCharacterIterator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;

// ====================== MAIN CLASS ======================

public class DistributedScheduler {

    public static void main(String[] args) throws Exception {
        System.out.println("=== DAGTTIK GÖREV ZAMANLICI SISTEMI ===");

        Scanner scanner = new Scanner(System.in);
        System.out.println("Çaliştirmak istediğiniz modu seçiniz: ");
        System.out.println("1. Master Node (Yönetici)");
        System.out.println("2. Worker Node (İşçi)");
        System.out.println("3. Test Modu (Master + 2 Worker)");
        System.out.print("Seçiminiz (1-3): ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // NewLine'ı temizle

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
                System.out.println("Geçersiz Seçim!");
        }

        scanner.close();
    }

    private static void startTestMode() {
        System.out.println("\n⚙️ TEST MODU BAŞLATILIYOR...");

        // Master'ı ayrı thread'de başlat
        Thread masterThgThread = new Thread(() -> {
            try {
                MasterNode master = new MasterNode("Test-Master", 9090);

                master.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        masterThread.start();

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Worker'ları ayrı thread'lerde başlat
        
        for (int i = 1; i <= 2; i++){
            final int workerId = i;
            Thread workerThread = new Thread(() ->{
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

    private static void startMasterNode() throws Excepiton {
        System.out.println("\n👑 MASTER NODE BAŞLATILIYOR");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Master adi: ");
        String name = scanner.nextLine();
        System.out.print("Port numarasi (9090): ");
        int port = scanner.nextInt();
        scanner.nextLine();

        MasterNode master = new MasterNode(name, port);
        master.start();

        // Konsol arayüz

        while (true){
            System.out.println("\n=== MASTER KONSOLU ===");
            System.out.println("1. Yeni görev ekle");
            System.out.println("2. Tüm görevleri listele");
            System.out.println("3. Worker'lari listele");
            System.out.println("4. Görev gönder");
            System.out.println("5. Çikiş");
            System.out.print("Seçimi: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch(choice) {
                case 1:
                    System.out.print("Görev adi: ");
                    String taskName = scanner.nextLine();
                    System.out.print("Komut: ");
                    String command = scanner.nextLine();
                    System.out.print("Öncelik (1-10)");
                    int priority = scanner.nextInt();
                    scanner.nextLine();

                    Task task = new Task(taskName, command, priority);
                    master.addTask(task);
                    System.out.println("✅ Görev eklendi: " + task.id);

                    break;

                case 2:
                    master.lisTasks();
                    break;
                case 3:
                    master.listWorkers();
                    break;
                case 4:
                    if (master.hasTasks() && master.hasWorkers()) {
                        System.out.print("Gönderilecek görev ID: ");
                        String taskId = scanner.nextLine();
                        master.sendTaskToWorker(taskId);
                    } else {
                        System.out.println("⚠️ Görev veya Worker yok!");
                    }
                    break;
                
                case 5:
                    master.stop();
                    System.out.println("Master durduruldu.");
                    scanner.close();
                    return;
            }
        }
    }


    private static void startWorkerNode() throws Exception {
        System.out.println("\n🔧 WORKER NODE BAŞLATILIYOR");
        Scanner scanner = new Scanner(System.in);

        System.out.print("Worker adi: ");
        String name = scanner.nextLine();
        System.out.print("Master adresi (localhost): ");
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

        System.out.println("\n✅ Worker başlatildi! Master'dan görev bekleniyor...");
        System.out.println("Çikmak için 'exit' yazin. ");

        while(true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")){
                worker.stop();
                System.out.println("Worker durduruldu.");
                scanner.close();
                return;

            }

        }
    }

}

// ====================== MODELLER ======================

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

    public static Task fromNetworkString(String networkString) {

        String[] parts = networkString.split("\\|");
        Task task = new Task(parts[1], parts[2], Integer.parseInt(parts[3]));
        task.id = parts[0];
        return task;
    }
}

class WorkerInfo {
    String id;
    String name;
    String host;
    int port;
    boolean isAlive;
    LocalDateTime lastHeartbeat;
    int taskCount;

    public WorkerInfo(String name, String host, int port) {
        this.id = "Worker-" + UUID.randomUUID().toString().substring(0,8);

        this.name = name;
        this.host = host;
        this.port = port;
        this.isAlive = true;
        this.lastHeartbeat = LocalDateTime.now();
        this.taskCount = 0;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s @ %s.%d - Görev: %d - Canli: %s", id, name, host, port, taskCount, isAlive ? "✅": "❌");
    }
}

// ====================== ABSTRACT NODE ====================== 

abstract class Node {
    protected String id;
    protected String name;
    protected boolean isRunning;
    protected final List<String> logs = new ArrayList<>();

    public  Node(String name) {
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
// ====================== MASTER NODE IMPLEMENTATION ======================

class MasterNode extends Node {
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
        log("Master başlatildi. Port: " + port);

        // Worker başlantılarını dinle
        new Thread(this::acceptWorkerConnections).start();

        // Heartbeat kontrolü
        new Thread(this::checkWorkerHealth).start();

        // Örnek görevler ekle
        addSampleTasks();
    }

    @Override 
    public void stop() throws Exception {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()){
            serverSocket.close();
        }
        threadPool.shutdown();
        log("Master durdurldu.");
    }
    private void acceptWorkerConnections() {
        while(isRunning){
            try {
                Socket workerSocket = serverSocket.accept();
                threadPool.submit(() -> handleWorkerConnection(workerSocket));
            } catch (IOException e) {
                if (isRunning) log("Bağlanti hatasi: " + e.getMessage());
            }
        }
    }

    private void handleWorkerConnection(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Worker'ın kayıt mesajını al
            String registration = in.readLine();
            if (registration != null && registration.startsWith("REGİSTER:")) {
                String[] parts = registration.substring(9).split(":");
                String workerName = parts[0];
                int workerPort = Integer.parseInt(parts[1]);

                WorkerInfo worker = new WorkerInfo(workerName, socket.getInetAddress().getHostAddress(), workerPort);
                workers.put(worker.id, worker);

                out.println("REGİSTERED:" + worker.id);
                log("Yeni worker kaydedildi: " + worker.name + " (" + worker.id + ") ");

                // Worker'dan gelen mesajlari dinle
                String message;
                while((message = in.readLine()) != null) {
                    handleWorkerMessage(worker.id, message);
                }
            }
        } catch (IOException e) {
            log("Worker bağlanti hatasi: " + e.getMessage());
        }
    }

private void handleWorkerMessage(String workerId, String message) {
    WorkerInfo worker = workers.get(workerId);
    if(worker != null) {
        worker.lastHeartbeat = LocalDateTime.now();
        if(message.startsWith("HEARTBEAT")){
            // Heartbeat mesajı
            worker.isAlive = true;
        } else if (message)
    }
}

}
[15:10, 14.01.2026] Musab Guldemir: 