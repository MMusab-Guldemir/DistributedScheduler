import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.loading.PrivateClassLoader;
import javax.swing.plaf.basic.BasicScrollPaneUI.ViewportChangeHandler;
import javax.swing.plaf.basic.BasicTreeUI.NodeDimensionsHandler;
import javax.swing.text.MaskFormatter;

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
}