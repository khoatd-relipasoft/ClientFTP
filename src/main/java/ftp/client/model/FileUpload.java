package ftp.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUpload {
    String pathToFile;
    String pathToFolder;
    String fileName;
}