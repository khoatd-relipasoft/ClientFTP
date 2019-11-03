package ftp.client.controllers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import ftp.client.model.FileFTP;
import ftp.client.model.FileUpload;
import ftp.client.model.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {

    private FTPClient ftpClient;

    @RequestMapping("/")
    public String root() {
        return "index";

    }

    @RequestMapping("/upload")
    public String upload(@RequestParam("fileName") MultipartFile file, @RequestParam("pathToFile") String fileName,
            @RequestParam("pathToFolder") String folder) throws IOException {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
        System.out.println(fileName);
        ftpClient.changeWorkingDirectory(folder);
        InputStream a = file.getInputStream();
        ftpClient.storeFile(fileName, a);
        return "upload-success";
    }

    @RequestMapping("/download")
    public void download(@PathParam("path") String path, HttpServletResponse response, @PathParam("name") String name)
            throws IOException {
        System.out.println(path);
        // ftpClient.setControlEncoding("UTF-8");
        ftpClient.setAutodetectUTF8(true);
        InputStream inputStream = ftpClient.retrieveFileStream(path);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment; filename=" + name);
        response.setContentLength(inputStream.available());
        FileCopyUtils.copy(inputStream, response.getOutputStream());
    }

    @PostMapping("/home")
    public String login(@ModelAttribute("user") User user, Model model) throws IOException {

        if (connectFTPServer(6000, user.getAddress(), Integer.parseInt(user.getPort()), user.getUsername(),
                user.getPassword())) {
            ftpClient.setControlEncoding("UTF-8");
            model.addAttribute("path", "/");
            model.addAttribute("files", listDirectory(ftpClient, "", "/", 0));
            return "home";
        }

        return "index";
    }

    @RequestMapping("/folder")
    public String goFolder(@RequestParam("path") String path, Model model) throws IOException {
        System.out.println(path);
        model.addAttribute("path", path);
        model.addAttribute("files", listDirectory(ftpClient, path, "", 0));
        return "home";
    }

    private boolean connectFTPServer(int FTP_TIMEOUT, String FTP_SERVER_ADDRESS, int FTP_SERVER_PORT_NUMBER,
            String FTP_USERNAME, String FTP_PASSWORD) {
        ftpClient = new FTPClient();
        try {
            System.out.println("connecting ftp server...");
            // connect to ftp server
            ftpClient.setDefaultTimeout(FTP_TIMEOUT);
            ftpClient.connect(FTP_SERVER_ADDRESS, FTP_SERVER_PORT_NUMBER);
            // run the passive mode command
            ftpClient.enterLocalPassiveMode();
            // check reply code
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                System.out.println("Can't connect");
                throw new IOException("FTP server not respond!");
            } else {
                ftpClient.setSoTimeout(FTP_TIMEOUT);
                // login ftp server
                if (!ftpClient.login(FTP_USERNAME, FTP_PASSWORD)) {
                    throw new IOException("Username or password is incorrect!");
                }
                ftpClient.setDataTimeout(FTP_TIMEOUT);
                System.out.println("connected");
                return true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public List<FileFTP> listDirectory(FTPClient ftpClient, String parentDir, String currentDir, int level)
            throws IOException {
        FTPFile[] subFiles = ftpClient.listFiles(parentDir + currentDir);
        List<FileFTP> files = new ArrayList();
        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and directory itself
                    continue;
                }
                if (aFile.isDirectory()) {
                    files.add(new FileFTP(currentFileName, "folder", parentDir + "/" + currentFileName));
                    // System.out.println("[" + currentFileName + "]");
                } else {
                    // System.out.println(currentFileName);
                    files.add(new FileFTP(currentFileName, "file", parentDir + "/" + currentFileName));
                }
            }
        }
        return files;
    }
}