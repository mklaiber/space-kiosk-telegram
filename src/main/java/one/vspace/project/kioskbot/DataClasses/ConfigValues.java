package one.vspace.project.kioskbot.DataClasses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigValues {
    private String telegramBotToken;
    private String webdavUserId;
    private String webdavPassword;
    private String webdavURI;
    private String webdavFileName;
    private String mongoDBHostName;
    private String mongoDBHostPort;
    private String mongoDBDatabaseName;
    private String mongoDBCollectionName;

}
