package ftp.client.controllers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.util.Charsets;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.omg.CORBA.portable.OutputStream;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {

    private FTPClient ftpClient = new FTPClient();;

    @RequestMapping("/")
    public String root() {
        if (!ftpClient.isConnected())
            return "index";
        else
            return "redirect:/home";

    }

    @RequestMapping("/upload")
    public String upload(@RequestParam("fileName") MultipartFile file, @RequestParam("pathToFile") String fileName,
            @RequestParam("pathToFolder") String folder, Model model) throws IOException {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
        ftpClient.changeWorkingDirectory(folder);
        InputStream a = file.getInputStream();
        boolean check = ftpClient.storeFile(fileName, a);
        if (check)
            model.addAttribute("message", "Upload done: " + fileName + ".");
        else
            model.addAttribute("message", "Can not upload this file");
        return "nf-success";
    }

    @RequestMapping("/delete")
    public String delete(@RequestParam("name") String name, @RequestParam("path") String path, Model model)
            throws IOException {
        boolean isDeleted = ftpClient.deleteFile(path);
        if (isDeleted) {
            model.addAttribute("message", "Deleted " + name + ".");

        } else {
            model.addAttribute("message", "Can not delete " + name + " because you don't have permission.");
        }
        return "nf-success";
    }

    @RequestMapping("/download")
    public void download(@PathParam("path") String path, HttpServletResponse response, @PathParam("name") String name)
            throws Exception {
        File downloadFile1 = new File("D:/Store/" + name);
        BufferedOutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
        boolean success = ftpClient.retrieveFile(path, outputStream1);
        outputStream1.close();
        InputStream myStream = new FileInputStream(downloadFile1);

        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-disposition", "attachment; filename=" + name);
        response.addHeader("Cache-Control", "no-store");
        response.setContentLengthLong(downloadFile1.length());

        IOUtils.copy(myStream, response.getOutputStream());
        response.flushBuffer();
    }

    @RequestMapping("/newfolder")
    public String addFolder(@PathParam("name") String name, @PathParam("path") String path, Model model)
            throws IOException {
        ftpClient.changeWorkingDirectory(path);
        boolean check = ftpClient.makeDirectory(name);
        if (check)
            model.addAttribute("message", "Add new foler: " + name + " to " + path);
        else
            model.addAttribute("message", "Can not create a new folder here.");
        return "nf-success";
    }

    @RequestMapping("/home")
    public String home(Model model) throws IOException {
        if (checkCon() == false)
            return "redirect:/";
        model.addAttribute("path", "/");
        model.addAttribute("files", listDirectory(ftpClient, "", "/", 0));
        return "home";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("user") User user, Model model) throws IOException {
        ftpClient.setControlEncoding("UTF-8");
        if (connectFTPServer(6000, user.getAddress(), Integer.parseInt(user.getPort()), user.getUsername(),
                user.getPassword())) {

            return "redirect:/home";
        }

        return "index";
    }

    @RequestMapping("/folder")
    public String goFolder(@RequestParam("path") String path, Model model) throws IOException {
        if (checkCon() == false)
            return "redirect:/";
        model.addAttribute("path", path);
        model.addAttribute("files", listDirectory(ftpClient, path, "", 0));
        return "home";
    }

    @RequestMapping("/logout")
    public String logout() throws IOException {
        ftpClient.disconnect();
        return "redirect:/";
    }

    private boolean connectFTPServer(int FTP_TIMEOUT, String FTP_SERVER_ADDRESS, int FTP_SERVER_PORT_NUMBER,
            String FTP_USERNAME, String FTP_PASSWORD) {
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
                } else {
                    String extra = "file";
                    String last = currentFileName.substring(currentFileName.length() - 3);
                    if (last.compareTo("rar") == 0) {
                        extra = "rar";
                    }

                    else if (last.compareTo("pdf") == 0)
                        extra = last;
                    else if (last.compareTo("exe") == 0)
                        extra = last;
                    String t = "KB";
                    long size = aFile.getSize() / 1024;
                    if (size > 1024) {
                        size /= 1024;
                        t = "MB";
                    }

                    String value = String.valueOf(size) + t;

                    files.add(new FileFTP(currentFileName, extra, parentDir + "/" + currentFileName, value));
                }
            }
        }
        return files;
    }

    public boolean checkCon() {
        if (ftpClient.isConnected())
            return true;
        return false;
    }
}