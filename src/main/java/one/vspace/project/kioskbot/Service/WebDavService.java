package one.vspace.project.kioskbot.Service;


import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

public class WebDavService {

    private static ConfigValues configValues;

    public WebDavService(ConfigValues configValues){
        this.configValues = configValues;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDavService.class);

    public String download() {
        ByteArrayOutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {


            URL url = new URL(configValues.getWebdavFilePath());
            out = new ByteArrayOutputStream();
            conn = url.openConnection();
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(configValues.getWebdavUserId(), configValues.getWebdavPassword().toCharArray());
                }
            });
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            return out.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public String downloadWebDav(){
        //Sardine sardine = new SardineFactory.begin(configValues.getWebdavUserId(), configValues.getWebdavPassword());

        return "";
    }

}
