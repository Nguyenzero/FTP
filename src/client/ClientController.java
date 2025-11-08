package client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.*;
import java.util.Objects;

public class ClientController {
    @FXML private TextField txtServerIP;
    @FXML private ListView<String> listFiles;
    @FXML private Label lblStatus;

    private String serverIP;
    private final int SERVER_PORT = 2121;

    @FXML
    private void connectServer() {
        serverIP = txtServerIP.getText().trim();
        if (serverIP.isEmpty()) {
            lblStatus.setText("⚠️ Vui lòng nhập địa chỉ IP của Server!");
            return;
        }
        lblStatus.setText("✅ Đã kết nối tới server: " + serverIP);
        refreshFileList();
    }

    @FXML
    private void uploadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp để tải lên");
        File file = chooser.showOpenDialog(null);
        if (file == null) return;

        try (Socket socket = new Socket(serverIP, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF("UPLOAD");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String response = dis.readUTF();
            if (Objects.equals(response, "UPLOAD_OK")) {
                lblStatus.setText("⬆️ Đã tải lên: " + file.getName());
                refreshFileList();
            }

        } catch (IOException e) {
            lblStatus.setText("❌ Lỗi khi tải lên file.");
            e.printStackTrace();
        }
    }

    @FXML
    private void downloadFile() {
        String fileName = listFiles.getSelectionModel().getSelectedItem();
        if (fileName == null) {
            lblStatus.setText("⚠️ Vui lòng chọn file để tải xuống!");
            return;
        }

        try (Socket socket = new Socket(serverIP, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);

            String reply = dis.readUTF();
            if (reply.equals("FILE_FOUND")) {

                long fileSize = dis.readLong();

                // ✅ Lấy thư mục Downloads của Windows
                String userHome = System.getProperty("user.home");
                File downloadDir = new File(userHome, "Downloads");
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                // ✅ File sẽ lưu vào Downloads
                File saveFile = new File(downloadDir, fileName);
                FileOutputStream fos = new FileOutputStream(saveFile);

                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;

                while (totalRead < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
                fos.close();

                lblStatus.setText("✅ File đã tải về thư mục Downloads: " + saveFile.getAbsolutePath());
            } else {
                lblStatus.setText("❌ File không tồn tại trên server.");
            }

        } catch (IOException e) {
            lblStatus.setText("❌ Lỗi khi tải xuống file.");
            e.printStackTrace();
        }
    }


    @FXML
    private void refreshFileList() {
        listFiles.getItems().clear();
        try (Socket socket = new Socket(serverIP, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST");
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                listFiles.getItems().add(dis.readUTF());
            }
            lblStatus.setText("🔄 Đã tải danh sách tệp từ server.");

        } catch (IOException e) {
            lblStatus.setText("⚠️ Không thể tải danh sách tệp (Server chưa sẵn sàng).");
        }
    }
}
