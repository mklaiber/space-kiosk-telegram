package one.vspace.project.kioskbot.Service;

import one.vspace.project.kioskbot.DataClasses.User;
import one.vspace.project.kioskbot.Exceptions.UserNotFoundException;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

public class DBService {

    final int SUCCESS = 1;
    final int FAILURE = 0;
    private String collectionName;
    private String databaseName;

    public DBService(ConfigValues configValues){
        this.collectionName = configValues.getMongoDBCollectionName();
        this.databaseName = configValues.getMongoDBDatabaseName();
    }

    public int addNewUser(User user, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        Document dbObject = new Document();
        dbObject.put("userID", user.getUserID());
        dbObject.put("registerDate", user.getRegisterDate());
        dbObject.put("name", user.getName());
        dbObject.put("credit", user.getCredit());
        dbObject.put("tagID", "NO_TAG_AVAILABLE");
        userData.insertOne(dbObject);
        return SUCCESS;
    }

    public int setUserToExistingTransponder(User user, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        Document dbObject = new Document();
        dbObject.put("userID", user.getUserID());
        dbObject.put("registerDate", user.getRegisterDate());
        dbObject.put("name", user.getName());
        userData.updateOne(new Document("userID", user.getTagId()), new Document("$set", dbObject));
        return SUCCESS;
    }

    public int haveUserAccountInDB(long userId, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        if(dataBaseUser.size()>0){
            return 1;
        } else {
            return -1;
        }
    }

    public User getUser(long userId, MongoClient mongoClient) throws UserNotFoundException {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        if(dataBaseUser.size()>0){
            return new User((long) dataBaseUser.get(0).get("userID"), (long) dataBaseUser.get(0).get("registerDate"), (String) dataBaseUser.get(0).get("name"), (double) dataBaseUser.get(0).get("credit"));
        } else {
            throw new UserNotFoundException(userId + " not found!");
        }
    }

    public void setAmount(long userId, MongoClient mongoClient, float credit) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        double newAmount = getAmount(userId, mongoClient) + credit;
        userData.updateOne(new Document("userID", userId), new Document("$set", new Document("credit", newAmount)));

    }

    public double getAmount(long userId, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        if(dataBaseUser.size() > 0){
            return (double) dataBaseUser.get(0).get("credit");
        } else {
            return -1;
        }
    }

    public void setName(Long userId, MongoClient mongoClient, String name) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        userData.updateOne(new Document("userID", userId), new Document("$set", new Document("name", name)));
    }

    public void deleteUser(Long userId, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        userData.deleteOne(new Document("userID", userId));
    }

    public void setTransponder(Long userId, String tagId, MongoClient mongoClient){
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        userData.updateOne(new Document("userID", userId), new Document("$set", new Document("tagID", tagId)));
    }

    public boolean haveUserTransponder(String transponderId, MongoClient mongoClient){
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("tagID", transponderId)).into(new ArrayList<>());
        if(dataBaseUser.size() > 0){
            return true;
        } else {
            return false;
        }
    }

}
