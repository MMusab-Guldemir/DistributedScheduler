import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.loading.PrivateClassLoader;
import javax.swing.plaf.basic.BasicScrollPaneUI.ViewportChangeHandler;
import javax.swing.plaf.basic.BasicTreeUI.NodeDimensionsHandler;
import javax.swing.text.MaskFormatter;
import javax.swing.text.TabSet;

import java.beans.beancontext.BeanContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.classfile.instruction.StoreInstruction;
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

// ======================== Main Class ========================

public class DistributedScheduler {
    public static void main(String[] args) {
        System.out.println("=== DAILY DUTY SCHEDULING SYSTEM ===");

        Scanner scanner = new Scanner(System.in);
        System.out.println("Select the mode you wish to run: ");
        System.out.println("1. Master Node (Yönetici)");
        System.out.println("2. Worker Node (İşçi)");
        System.out.println("3. Test Modu (Master + 2 Worker)");
        System.out.print("Seçiminiz (1/2/3): ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        switch (choice) {
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

    private static void startMasterNode() {
        System.out.println("\n⚙️ TEST MODU BAŞLATILIYOR...");

        // Master'ı ayrı thread'de başlat
        Thread masterThgThread = new Thread(() -> {
            try {
                MasterNode master = new MasterNode("Test-Master", 9090);

                master.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        masterThread.start();

        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

        // Worker'ları ayrı thread'lerde başlat!

        for (int i = 1; i <= 2; i++) {
            final int workerId = i;
            Thread workerThread = new Thread(() -> {
                try {
                    WorkerNode worker = new WorkerNode("Worker-" + workerId, "localhost", 9090, 9009, workerId);
                    worker.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void startMasterNode() throws Exception {
        System.out.println("\n👑 MASTER NODE BAŞLATILIYOR");
        Scanner scanner = new Scanner(System.in);

        System.out.println("Master adi: ");
        String name = scanner.nextLine();
        System.out.println("Port numarasi(9090): ");
        int port = scanner.nextInt();
        scanner.nextLine();

        MasterNode master = new MasterNode(name, port);
        master.start();

        // Konsol arayüz

        while (true) {
            System.out.println("\n=== MASTER KONSOLU ===");
            System.out.println("1. Yeni görev ekle");
            System.out.println("2. Tüm görevleri listele");
            System.out.println("3. Worker'lari listele");
            System.out.println("4. Görev gönder");
            System.out.println("5. Çikiş");
            System.out.print("Seçimi: ");
            scanner.nextLine();

            int choice = scanner.nextInt();
            scanner.nextLine(); 

            switch(choice) {
                case 1:
                    System.out.print("Görev adi: ");
                    String taskName = scanner.nextLine();
                    System.out.print("Komut: ");
                    String command = scanner.nextLine();
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
                        System.out.println("Gönderilecek görev ID: ");
                        String taskId = scanner.nextLine();
                        master.sendTaskToWorker(taskId);
                    } else {
                        System.out.println("⚠️ Görev veya Worker yok!");
                    }

                    break;

                case 5:
                    master.stop();
                    System.out.println("Master durduruldu");
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

        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                worker.stop();
                System.out.println("Worker durduruldu. ");
                scanner.close();
                return;
            }
        }
    }
}



