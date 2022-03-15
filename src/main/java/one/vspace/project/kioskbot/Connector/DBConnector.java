package one.vspace.project.kioskbot.Connector;

import one.vspace.project.kioskbot.Controller.BotController;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class DBConnector {

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    private static MongoClient mongoClient;
    private static DBConnector dbConnector;
    private final static String mongoDBHostName = System.getenv("MONGODB_HOST_NAME_ENV");
    private final static int mongoDBHostPort = Integer.parseInt(System.getenv("MONGODB_HOST_PORT_ENV"));

    private DBConnector() {
        MongoClientSettings settings =
                MongoClientSettings.builder()
                        .applyToConnectionPoolSettings(builder ->
                                builder.maxSize(40).minSize(10))
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(mongoDBHostName, mongoDBHostPort))))
                        .build();

        mongoClient = MongoClients.create(settings);
    }

    public static MongoClient getConnection() {
        if(dbConnector == null){
            dbConnector = new DBConnector();
        }
        return mongoClient;
    }

    public static void closeDatabase() {
        mongoClient.close();
    }
}
