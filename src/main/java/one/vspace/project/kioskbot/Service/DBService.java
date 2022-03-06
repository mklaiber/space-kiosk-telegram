package one.vspace.project.kioskbot.Service;

import one.vspace.project.kioskbot.DataClasses.Constants;
import one.vspace.project.kioskbot.DataClasses.User;
import one.vspace.project.kioskbot.Exceptions.UserNotFoundException;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;

public class DBService {

    private String collectionName;
    private String databaseName;

    public DBService(ConfigValues configValues){
        this.collectionName = configValues.getMongoDBCollectionName();
        this.databaseName = configValues.getMongoDBDatabaseName();
    }

    public void addNewUser(User user, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        Document dbObject = new Document();
        dbObject.put("userID", user.getUserID());
        dbObject.put("registerDate", user.getRegisterDate());
        dbObject.put("name", user.getName());
        dbObject.put("credit", user.getCredit());
        userData.insertOne(dbObject);
    }

    public void setUserToExistingTransponder(User user, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        Document dbObject = new Document();
        dbObject.put("userID", user.getUserID());
        dbObject.put("registerDate", user.getRegisterDate());
        dbObject.put("name", user.getName());
        userData.updateOne(new Document("tagID", user.getTagId()), new Document("$set", dbObject));
    }

    public boolean haveUserAccountInDB(long userId, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        if(dataBaseUser.isEmpty()){
            return false;
        } else {
            return true;
        }
    }

    public User getUser(long userId, MongoClient mongoClient) throws UserNotFoundException {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        if(dataBaseUser.size()>0){
            return new User((long) dataBaseUser.get(0).get("userID"), (long) dataBaseUser.get(0).get("registerDate"),
                    (String) dataBaseUser.get(0).get("name"), (int) dataBaseUser.get(0).get("credit"), (String) dataBaseUser.get(0).get("tagID"));
        } else {
            throw new UserNotFoundException(userId + " not found!");
        }
    }

    public void setAmount(long userId, int credit, MongoClient mongoClient) {
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        userData.updateOne(new Document("userID", userId), new Document("$inc", new Document("credit", credit)));

    }

    public int getAmount(long userId, MongoClient mongoClient) throws IllegalArgumentException{
        try {
            MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
            ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
            if (dataBaseUser.size() > 0) {
                return (int) dataBaseUser.get(0).get("credit");
            } else {
                return -1;
            }
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Message was too big or no number.");
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

    public int setTransponder(Long userId, String tagId, MongoClient mongoClient){
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        if(!doesTagIdExist(tagId, mongoClient)) {
            userData.updateOne(new Document("userID", userId), new Document("$set", new Document("tagID", tagId)));
            return -1;
        } else {
            ArrayList<Document> dataBaseUser = userData.find(eq("tagID", tagId)).into(new ArrayList<>());
            userData.deleteOne(new Document("tagID", tagId));
            setTransponder(userId, tagId, mongoClient);
            setAmount(userId, (int) dataBaseUser.get(0).get("credit"), mongoClient);
            return (int) dataBaseUser.get(0).get("credit");
        }
    }

    public boolean haveUserTransponder(long userId, MongoClient mongoClient){
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("userID", userId)).into(new ArrayList<>());
        return dataBaseUser.get(0).containsKey("tagID");
    }

    public boolean doesTagIdExist(String tagId, MongoClient mongoClient){
        MongoCollection<Document> userData = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        ArrayList<Document> dataBaseUser = userData.find(eq("tagID", tagId)).into(new ArrayList<>());
        return !dataBaseUser.isEmpty();
    }

}
