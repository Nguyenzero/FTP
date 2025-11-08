package server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Enumeration;

public class FTPServer {
    private static final int PORT = 2121;
    private static final String SERVER_DIR = "server_files/";

    public static void main(String[] args) {
        try {
            InetAddress wifiAddress = getWiFiIPv4Address();
            if (wifiAddress == null) {
                System.err.println("⚠️ Không tìm thấy IPv4 Wi-Fi, dùng localhost thay thế.");
                wifiAddress = InetAddress.getLocalHost();
            }

            // Tạo thư mục chứa file nếu chưa có
            Files.createDirectories(Paths.get(SERVER_DIR));

            // Tạo ServerSocket trên địa chỉ Wi-Fi
            ServerSocket serverSocket = new ServerSocket(PORT, 50, wifiAddress);
            System.out.println("✅ FTP Server đang chạy tại: " + wifiAddress.getHostAddress() + ":" + PORT);
            System.out.println("📂 Thư mục server: " + new File(SERVER_DIR).getAbsolutePath());

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ✅ Hàm chọn đúng IPv4 của card Wi-Fi (thường tên là "Wi-Fi" hoặc "Wireless")
     */
    private static InetAddress getWiFiIPv4Address() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();

            String name = ni.getDisplayName().toLowerCase();
            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

            // Ưu tiên card có tên "wi-fi", "wireless", "wlan"
            if (name.contains("wi-fi") || name.contains("wireless") || name.contains("wlan")) {
                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        return addr;
                    }
                }
            }
        }

        // Nếu không tìm được card Wi-Fi, thử lấy bất kỳ IPv4 nào có thể
        for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        return addr;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 📦 Xử lý kết nối client (UPLOAD / DOWNLOAD / LIST)
     */
    private static void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String command = dis.readUTF();

            if (command.equals("UPLOAD")) {
                String filename = dis.readUTF();
                long fileSize = dis.readLong();

                FileOutputStream fos = new FileOutputStream(SERVER_DIR + filename);
                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;

                while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
                fos.close();
                dos.writeUTF("UPLOAD_OK");
                System.out.println("⬆️ Nhận file: " + filename);
            }

            else if (command.equals("DOWNLOAD")) {
                String filename = dis.readUTF();
                File file = new File(SERVER_DIR + filename);
                if (!file.exists()) {
                    dos.writeUTF("FILE_NOT_FOUND");
                    return;
                }

                dos.writeUTF("FILE_FOUND");
                dos.writeLong(file.length());
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
                fis.close();
                System.out.println("⬇️ Gửi file: " + filename);
            }

            else if (command.equals("LIST")) {
                File folder = new File(SERVER_DIR);
                File[] files = folder.listFiles();
                if (files == null) {
                    dos.writeInt(0);
                    return;
                }
                dos.writeInt(files.length);
                for (File f : files) {
                    dos.writeUTF(f.getName());
                }
                System.out.println("📂 Gửi danh sách file cho client.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
