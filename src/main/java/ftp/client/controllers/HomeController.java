package ftp.client.controllers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

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
import ftp.client.model.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private FTPClient ftpClient;

    @RequestMapping("/")
    public String root() {
        return "index";

    }

    @RequestMapping("/upload")
    public String upload(@PathParam("pathToFile") String pathToFile, @PathParam("pathToFolder") String pathToFolder) {
        System.out.println(pathToFile);
        System.out.println(pathToFolder);
        return "upload-success";
    }

    @RequestMapping("/download")
    public void download(@PathParam("path") String path, HttpServletResponse response, @PathParam("name") String name)
            throws IOException {
        System.out.println(path);
        InputStream inputStream = ftpClient.retrieveFileStream(path);
        // inputStream.read(b);
        // InputStream inputStream2 = new BufferedInputStream(new
        // ByteArrayInputStream(b));
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment; filename=" + name);
        response.setContentLength(inputStream.available());
        FileCopyUtils.copy(inputStream, response.getOutputStream());

        // return "home";
    }

    @RequestMapping("/home")
    public String login(@ModelAttribute("user") User user, Model model) throws IOException {
        if (connectFTPServer(6000, user.getAddress(), Integer.parseInt(user.getPort()), user.getUsername(),
                user.getPassword())) {
            model.addAttribute("path", "/");
            model.addAttribute("files", listDirectory(ftpClient, "", "/", 0));
            return "home";
        }

        // List<FileFTP> list2 = new ArrayList<>();
        // list2.add(new FileFTP("testtest", "folder"));
        // List<FileFTP> list = new ArrayList<>();
        // list.add(new FileFTP("test 1", list2, "folder"));
        // list.add(new FileFTP("test2.mp3", "file"));
        // list.add(new FileFTP("test3.mp3", "file"));
        // list.add(new FileFTP("test4.mp3", "folder"));

        // model.addAttribute("files", list);

        return "index";

        // return "redirect:/";
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