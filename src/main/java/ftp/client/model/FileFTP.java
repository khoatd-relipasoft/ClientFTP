package ftp.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileFTP {
    String name;
    // String path;
    String type;
    String path;
    String size;

    public FileFTP(String name, String type, String path) {
        this.name = name;
        this.type = type;
        this.path = path;
    }

}