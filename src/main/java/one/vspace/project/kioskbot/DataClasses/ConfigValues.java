package one.vspace.project.kioskbot.DataClasses;

public class ConfigValues {
    private String telegramBotToken;
    private String webdavUserId;
    private String webdavPassword;
    private String webdavFilePath;
    private String mongoDBHostName;
    private String mongoDBHostPort;
    private String mongoDBDatabaseName;
    private String mongoDBCollectionName;

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public String getWebdavUserId() {
        return webdavUserId;
    }

    public void setWebdavUserId(String webdavUserId) {
        this.webdavUserId = webdavUserId;
    }

    public String getWebdavPassword() {
        return webdavPassword;
    }

    public void setWebdavPassword(String webdavPassword) {
        this.webdavPassword = webdavPassword;
    }

    public String getWebdavFilePath() {
        return webdavFilePath;
    }

    public void setWebdavFilePath(String webdavFilePath) {
        this.webdavFilePath = webdavFilePath;
    }

    public String getMongoDBHostName() {
        return mongoDBHostName;
    }

    public void setMongoDBHostName(String mongoDBHostName) {
        this.mongoDBHostName = mongoDBHostName;
    }

    public String getMongoDBHostPort() {
        return mongoDBHostPort;
    }

    public void setMongoDBHostPort(String mongoDBHostPort) {
        this.mongoDBHostPort = mongoDBHostPort;
    }

    public String getMongoDBDatabaseName() {
        return mongoDBDatabaseName;
    }

    public void setMongoDBDatabaseName(String mongoDBDatabaseName) {
        this.mongoDBDatabaseName = mongoDBDatabaseName;
    }

    public String getMongoDBCollectionName() {
        return mongoDBCollectionName;
    }

    public void setMongoDBCollectionName(String mongoDBCollectionName) {
        this.mongoDBCollectionName = mongoDBCollectionName;
    }
}
