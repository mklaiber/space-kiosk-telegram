package one.vspace.project.kioskbot.Utils;

import com.mongodb.client.MongoClient;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import one.vspace.project.kioskbot.Controller.BotController;
import one.vspace.project.kioskbot.DataClasses.Constants;
import one.vspace.project.kioskbot.DataClasses.Drink;
import one.vspace.project.kioskbot.DataClasses.User;
import one.vspace.project.kioskbot.Exceptions.ArticleNotFoundException;
import one.vspace.project.kioskbot.Exceptions.TooBigNumberException;
import one.vspace.project.kioskbot.Exceptions.UserNotFoundException;
import one.vspace.project.kioskbot.Service.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

@RequiredArgsConstructor
public class CommandUtils {

    @NonNull
    private DBService dbService;
    @NonNull
    private HelperUtils helperUtils;
    @NonNull
    private Map<Long, String> requestMap;

    private Date date = new Date();

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public void requestedAddAmountCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        int amount = 0;
        try {
            amount = helperUtils.getAmountAsIntegerInCent(update);
            dbService.setAmount(userId, amount, db);
            helperUtils.sendMessage(userId, decimalFormat.format(helperUtils.getAmountAsIntegerInEuro(amount))
                    + "€ has been added to your account.");
        } catch (TooBigNumberException e) {
            helperUtils.sendMessage(userId, "Number was too big.");
        } finally {
            requestMap.remove(userId);
        }

    }

    public void requestedNameChangeCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        String name = update.message().text();
        dbService.setName(userId, db, name);
        helperUtils.sendMessage(userId, "Your name has been set to " + name + ".");
        requestMap.remove(userId);
    }

    public void requestedDeleteCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        if (update.message().text().equals("Yes, I want to delete everything.")) {
            dbService.deleteUser(userId, db);
            helperUtils.sendMessage(userId, "All data stored about you has been deleted!");
        } else {
            helperUtils.sendMessage(userId, "Operation canceled.");
        }
        requestMap.remove(userId);
    }

    public void requestedRemoveAmountCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        try {
            int amount = helperUtils.getAmountAsIntegerInCent(update);
            dbService.setAmount(userId, amount * -1, db);
            helperUtils.sendMessage(userId, "Your amount has been adjusted by "
                    + decimalFormat.format(helperUtils.getAmountAsIntegerInEuro(amount)) + "€ to "
                    + decimalFormat.format(
                    helperUtils.getAmountAsIntegerInEuro(dbService.getAmount(userId, db))) + "€.");
        } catch (NumberFormatException e) {
            helperUtils.sendMessage(userId, "The message was not a number or just too big.");
        } catch (TooBigNumberException e){
            helperUtils.sendMessage(userId, "The number was too big.");
        } finally {
            requestMap.remove(userId);
        }
    }

    public void requestedSetTransponderCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        String transponderId = update.message().text();
        int amount = dbService.setTransponder(userId, transponderId, db);
        if(amount == -1){
            helperUtils.sendMessage(userId, "Your account has been connected with Tag " + transponderId + ".");
        } else {
            helperUtils.sendMessage(userId, "Your account has been connected with Tag " + transponderId + "."
            + "\nYour credit " + decimalFormat.format(helperUtils.getAmountAsIntegerInEuro(amount)) + "€ has been added to your account.");
        }
        requestMap.remove(userId);
    }

    public void requestedNewUserCommandWasExecuted(Update update, MongoClient db) {
        long userId = update.message().from().id();
        if (update.message().text().equals("/yes")) {
            helperUtils.sendMessage(userId, "Please tell me your TagID."
                    + "That can be seen in the top left corner of the physical kiosk after scanning your tag.");
            requestMap.remove(userId);
            requestMap.put(userId, Constants.REQUEST_NEW_TRANSPONDER_USER);
        } else if (update.message().text().equals("/no")) {
            registerUser(update, db);
            requestMap.remove(userId);
        } else {
            helperUtils.sendMessage(userId, "Unknown Responce. Cancelling action!");
            requestMap.remove(userId);
        }
    }

    public void requestedNewTransponderUserCommandWasExecuted(Update update, MongoClient db) {
        registerTransponderUser(update, db);
        requestMap.remove(update.message().from().id());
    }

    public void getCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        helperUtils.sendMessage(userID, "Your current amount ist "
                + decimalFormat.format(
                        helperUtils.getAmountAsIntegerInEuro(dbService.getAmount(userID, mongoClient))) + "€");
    }


    public void removeCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUEST_REMOVE_AMOUNT);
        helperUtils.sendMessage(userID, "Please enter the amount you want to remove.");
    }

    public void addCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUESTED_ADD_AMOUNT);
        helperUtils.sendMessage(userID, "Please enter the amount to add.");
    }

    public void updateCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        helperUtils.setDrinkList();
        helperUtils.sendMessage(userID, "Drinklist has been successfully updated.");
    }

    public void codeCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUEST_MANUAL_EAN_CODE);
        helperUtils.sendMessage(userID, "Enter the EAN-Code.");
    }

    public void deleteCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUEST_DELETE);
        helperUtils.sendMessage(userID, "Please enter \"Yes, I want to delete everything.\".");

    }

    public void transponderCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUEST_TRANSPONDER);
        helperUtils.sendMessage(userID, "Enter your transponder ID.");

    }

    public void nameCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        requestMap.put(userID, Constants.REQUESTED_NAME);
        helperUtils.sendMessage(userID, "Please enter your new name.");

    }

    public void infoCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            try {
                User currentUser = dbService.getUser(userID, mongoClient);
                helperUtils.sendMessage(userID, "Your Name: " + currentUser.getName()
                        + "\nYour Chat-ID: " + currentUser.getUserID()
                        + "\nMember since: " + new Date(currentUser.getRegisterDate())
                        + "\nCurrent amount: " + decimalFormat.format(
                                helperUtils.getAmountAsIntegerInEuro(currentUser.getCredit()))
                        .replace(".", ",") + "€"
                        + (dbService.haveUserTransponder(userID, mongoClient) ? "\nTag-ID: "
                        + currentUser.getTagId() : ""));
            } catch (UserNotFoundException e) {
                helperUtils.sendMessage(userID, "User not found!");
                e.printStackTrace();
            }
        } else {
            helperUtils.sendMessage(userID, "TagID not found. Please send /register again.");
            requestMap.remove(userID);
        }
    }

    private void registerTransponderUser(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        User newUser;
        String firstName = update.message().from().firstName();
        String lastName = update.message().from().lastName();
        String tagId = update.message().text();
        if (lastName == null) {
            newUser = new User(userID, date.getTime(), firstName, 0, tagId);
        } else if (firstName == null) {
            newUser = new User(userID, date.getTime(), lastName, 0, tagId);
        } else {
            newUser = new User(userID, date.getTime(), (firstName + " " + lastName), 0, tagId);
        }
        if (dbService.doesTagIdExist(update.message().text(), mongoClient)) {
            dbService.setUserToExistingTransponder(newUser, mongoClient);
            helperUtils.sendMessage(userID, "Hallo " + firstName + ", your account have been created."
                    + "\nYour TagID is " + tagId);
        } else {
            helperUtils.sendMessage(userID, "TagID not found. Please send /register again.");
        }
    }


    private void registerUser(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        User newUser;
        String firstName = update.message().from().firstName();
        String lastName = update.message().from().lastName();
        if (requestMap.containsKey(userID)) {
            if (lastName == null) {
                newUser = new User(userID, date.getTime(), (firstName), 0);
            } else if (firstName == null) {
                newUser = new User(userID, date.getTime(), (lastName), 0);
            } else {
                newUser = new User(userID, date.getTime(), (firstName + " " + lastName), 0);
            }
            dbService.addNewUser(newUser, mongoClient);
            helperUtils.sendMessage(userID, "Hello " + newUser.getName() + ", your account have been created.");
        }
    }

    public void registerCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        if (requestMap.containsKey(userID)) {
            helperUtils.sendMessage(userID, "Please finish your existing operation first.");
            return;
        }
        if (dbService.haveUserAccountInDB(userID, mongoClient)) {
            helperUtils.sendMessage(userID, "Sorry "
                    + update.message().from().firstName() + ", you already have an account.");
            return;
        }
        helperUtils.sendMessage(userID, "Do you have already a transponder from the physical kiosk system?"
                + "\nSend /yes if you have and /no if not. You also can connect your tag later.");
        requestMap.put(userID, Constants.REQUEST_NEW_USER);
    }

    public void startCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        helperUtils.sendMessage(userID, "Hi! Please send a picture of the barcode as picture or file!"
                + "\nPlease type /register to start.");
    }

    public void noPossibleAction(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        helperUtils.sendMessage(userID, "No possible action.");
    }

    public void requestedManualEANCodeCommandWasExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        String code = update.message().text();
        Drink currentDrink = null;
        try {
            currentDrink = helperUtils.getDrinkWithCode(code);
        } catch (ArticleNotFoundException e) {
            helperUtils.sendMessage(userID, "Article not found!");
            e.printStackTrace();
        } finally {
            requestMap.remove(userID);
        }
        helperUtils.sendMessage(userID, "Code: " + code
                + ", Product: " + currentDrink.getName() + ", Kosten: "
                + helperUtils.getAmountAsIntegerInEuro(currentDrink.getCost()) + "€"
                + "\nTo remove this amount, execute /remove and enter the Amount.");
    }
}
