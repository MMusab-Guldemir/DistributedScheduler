import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.foreign.AddressLayout;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.Port;

class MasterNode extends Node {
    private int port;
    private ServerSocket ServerSocket;
    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private ExecutorService threadPool;

    public MasterNode(String name, int port) {
        super(name);
        this.port = port;
        this.threadPool = Executor.newFixedThreadPool(10);

    }

    public void start() throws Exception {
        isRunning = true;
        ServerSocket = new ServerSocket(port);
        log("Master başlatildi. Port: " + port);

        // Worker başlantılarını dinle
        new Thread(this::acceptWorkerConnections).start();

        // Heartbeat kontrolü
        new Thread(this::checkWorkerHealth).start();

        // Örnek görevler ekle
        addSampleTasks();
    }

    public void stop() throws Exception {
        isRunning = false;
        if (ServerSocket != null && !ServerSocket.isClosed()){
            ServerSocket.close();
        }
        threadPool.shutdown();
        log("Master durdurldu.");
    }
    private void acceptWorkerConnections() {
        while(isRunning){
            try {
                Socket workerSocket = ServerSocket.accept();
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
                    handleWorkerMessage(workerName, isRunning);
                }
            }
        } catch (IOException e) {
            log("Worker bağlanti hatasi: " + e.getMessage());
        }
    }

    private void handleWorkerMessage(String workerId, boolean message) {
        WorkerInfo worker = workers.get(workerId);
        if(worker != null) {
            worker.lastHeartBeat = LocalDateTime.now();
            if(message.startsWith("HEARTBEAT")){
                // Heartbeat mesajı
                worker.isAlive = true;
            } else if (message);
        }
    }
}



