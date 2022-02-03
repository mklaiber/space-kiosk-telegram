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

    private DBConnector(ConfigValues configValues) {
        MongoClientSettings settings =
                MongoClientSettings.builder()
                        .applyToConnectionPoolSettings(builder ->
                                builder.maxSize(40).minSize(10))
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(configValues.getMongoDBHostName(), Integer.parseInt(configValues.getMongoDBHostPort())))))
                        .build();

        mongoClient = MongoClients.create(settings);
    }

    public static MongoClient getConnection(ConfigValues configValues) {
        if(dbConnector == null){
            dbConnector = new DBConnector(configValues);
        }
        return mongoClient;
    }

    public static void closeDatabase() {
        mongoClient.close();
    }
}
