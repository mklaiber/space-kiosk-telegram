package one.vspace.project.kioskbot.Service;


import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class WebDavService {

    private final String webDavUserID = System.getenv("WEBDAV_USER_ID_ENV");
    private final String webDavPassword = System.getenv("WEBDAV_PASSWORD_ENV");
    private final String webDavUri = System.getenv("WEBDAV_URI_ENV");
    private final String webDavFileName = System.getenv("WEBDAV_FILE_NAME");

    private static final Logger LOG = LoggerFactory.getLogger(WebDavService.class);

    public String downloadWebDav(){
        Sardine sardine = SardineFactory.begin(webDavUserID, webDavPassword);
        try {
            InputStream inputStream = sardine.get(webDavUri + webDavFileName);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int numbersRead;
            long numbersWritten = 0;
            while ((numbersRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, numbersRead);
                numbersWritten += numbersRead;
            }
            LOG.info("Drinks was read successfully!");
            return byteArrayOutputStream.toString();
        } catch (IOException e){
            e.printStackTrace();
            return e.toString();
        }
    }
}
