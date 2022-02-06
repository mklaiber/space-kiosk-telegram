package one.vspace.project.kioskbot.Controller;

import one.vspace.project.kioskbot.Service.DecodeBarCodeService;
import one.vspace.project.kioskbot.Exceptions.UserNotFoundException;
import one.vspace.project.kioskbot.DataClasses.Constants;
import one.vspace.project.kioskbot.DataClasses.User;
import one.vspace.project.kioskbot.Service.DBService;
import one.vspace.project.kioskbot.Connector.DBConnector;
import one.vspace.project.kioskbot.Service.WebDavService;
import one.vspace.project.kioskbot.DataClasses.ConfigValues;
import one.vspace.project.kioskbot.DataClasses.Drink;
import one.vspace.project.kioskbot.Exceptions.ArticleNotFoundException;
import com.google.gson.Gson;
import com.google.zxing.*;
import com.mongodb.client.MongoClient;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.pengrad.telegrambot.model.Update;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotController {

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    private ConfigValues configValues = getConfigValues();

    private Map<Long, String> requestMap = new HashMap<>();

    private DBService dbService = new DBService(configValues);

    private TelegramBot bot = new TelegramBot(configValues.getTelegramBotToken());

    private Date date = new Date();

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("DD-MM-YYYY HH-MM-SS");

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private long lastDrinkUpdate = 0;

    private String token = "";

    private WebDavService webDavDrinkAcessor = new WebDavService(getConfigValues());

    private List<Drink> drinkList = new ArrayList<>();

    public static void main(String args[]) {

        new BotController().botControl();

    }

    public void botControl() {
        updateDrinkList();
        bot.setUpdatesListener(updates -> {
            updates.forEach(this::process);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public ConfigValues getConfigValues() {
        try {
            Gson gson = new Gson();
            ConfigValues credentials = gson.fromJson(new FileReader("credentials/config.json"), ConfigValues.class);
            return credentials;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateDrinkList() {
        lastDrinkUpdate = date.getTime();
        String[] drinks = webDavDrinkAcessor.downloadWebDav().replace(',', '.').split("\n");
        for (String drink : drinks) {
            String[] finalDrinks = drink.trim().split(";");
            drinkList.add(new Drink(finalDrinks[0], Float.parseFloat(finalDrinks[1]), finalDrinks[2]));
        }
    }

    public Drink getDrinkWithCode(String code) throws ArticleNotFoundException {
        for (Drink drink : drinkList) {
            if (drink.getProductId().equals(code))
                return drink;
        }
        throw new ArticleNotFoundException("Code " + code + " does not respond to any article!");
    }

    private void sendMessage(long userId, String message) {
        SendResponse sendResponse = bot.execute(new SendMessage(userId, message));
    }

    public void process(Update update) {

        MongoClient db = DBConnector.getConnection(configValues);
        long userId = update.message().from().id();

        if ((lastDrinkUpdate - date.getTime() - 3600) > 0) {
            lastDrinkUpdate = date.getTime();
            updateDrinkList();
        }

        if (dbService.haveUserAccountInDB(userId, db) || update.message().text().equals(Constants.REGISTER_COMMAND)
                || update.message().text().equals(Constants.START_COMMAND) || requestMap.containsKey(userId)) {
            if (update.message().text() != null) {
                switch (update.message().text()) {
                    case Constants.START_COMMAND:
                        startCommandExecuted(update, db);
                        commandLog(update, Constants.START_COMMAND);
                        break;
                    case Constants.REGISTER_COMMAND:
                        registerCommandExecuted(update, db);
                        commandLog(update, Constants.REGISTER_COMMAND);
                        break;
                    case Constants.INFO_COMMAND:
                        infoCommandExecuted(update, db);
                        commandLog(update, Constants.INFO_COMMAND);
                        break;
                    case Constants.NAME_COMMAND:
                        nameCommandExecuted(update, db);
                        commandLog(update, Constants.NAME_COMMAND);
                        break;
                    case Constants.TRANSPONDER_COMMAND:
                        transponderCommandExecuted(update, db);
                        commandLog(update, Constants.TRANSPONDER_COMMAND);
                        break;
                    case Constants.DELETE_COMMAND:
                        deleteCommandExecuted(update, db);
                        commandLog(update, Constants.DELETE_COMMAND);
                        break;
                    case Constants.CODE_COMMAND:
                        codeCommandExecuted(update, db);
                        commandLog(update, Constants.CODE_COMMAND);
                        break;
                    case Constants.UPDATE_COMMAND:
                        updateCommandExecuted(update, db);
                        commandLog(update, Constants.UPDATE_COMMAND);
                        break;
                    case Constants.ADD_COMMAND:
                        addCommandExecuted(update, db);
                        commandLog(update, Constants.ADD_COMMAND);
                        break;
                    case Constants.REMOVE_COMMAND:
                        removeCommandExecuted(update, db);
                        commandLog(update, Constants.REMOVE_COMMAND);
                        break;
                    case Constants.GET_COMMAND:
                        getCommandExecuted(update, db);
                        commandLog(update, Constants.GET_COMMAND);
                        break;
                    default:
                        if (requestMap.containsKey(userId)) {
                            float amount;
                            switch (requestMap.get(userId)) {
                                case Constants.REQUESTED_ADD_AMOUNT:
                                    amount = Float.parseFloat(update.message().text().replace(",", ".").replace("€", ""));
                                    dbService.setAmount(userId, amount, db);
                                    sendMessage(userId, decimalFormat.format(amount) + "€ has been added to your account.");
                                    commandLog(update, Constants.REQUESTED_ADD_AMOUNT);
                                    requestMap.remove(userId);
                                    break;
                                case Constants.REQUESTED_NAME:
                                    String name = update.message().text();
                                    dbService.setName(userId, db, name);
                                    sendMessage(userId, "Your name has been set to " + name + ".");
                                    commandLog(update, Constants.REQUESTED_NAME);
                                    requestMap.remove(userId);
                                    break;
                                case Constants.REQUEST_DELETE:
                                    if (update.message().text().equals("Yes, I want to delete everything.")) {
                                        dbService.deleteUser(userId, db);
                                        sendMessage(userId, "All data stored about you has been deleted!");
                                    } else {
                                        sendMessage(userId, "Operation canceled.");
                                    }
                                    commandLog(update, Constants.REQUEST_DELETE);
                                    requestMap.remove(userId);
                                    break;
                                case Constants.REQUEST_REMOVE_AMOUNT:
                                    amount = Float.parseFloat(update.message().text().replace(",", ".").replace("€", ""));
                                    dbService.setAmount(userId,amount * -1, db);
                                    sendMessage(userId, "Your amount has been adjusted by "
                                            + decimalFormat.format(amount) + "€ to "
                                            + decimalFormat.format(dbService.getAmount(userId, db)) + "€.");
                                    commandLog(update, Constants.REQUEST_REMOVE_AMOUNT);
                                    requestMap.remove(userId);
                                    break;
                                case Constants.REQUEST_TRANSPONDER:
                                    String transponderId = update.message().text();
                                    dbService.setTransponder(userId, transponderId, db);
                                    sendMessage(userId, "Your account has been connected with Tag " + transponderId + ".");
                                    commandLog(update, Constants.REQUEST_TRANSPONDER);
                                    requestMap.remove(userId);
                                    break;
                                case Constants.REQUEST_NEW_USER:
                                    if (update.message().text().equals("/yes")) {
                                        sendMessage(userId, "Please tell me your TagID."
                                                + "That can be seen in the top left corner of the physical kiosk after scanning your tag.");
                                        requestMap.remove(userId);
                                        requestMap.put(userId, Constants.REQUEST_NEW_TRANSPONDER_USER);
                                    } else if (update.message().text().equals("/no")) {
                                        registerUser(update, db);
                                        requestMap.remove(userId);
                                    } else {
                                        sendMessage(userId, "Unknown Responce. Cancelling action!");
                                        requestMap.remove(userId);
                                    }
                                    commandLog(update, Constants.REQUEST_NEW_USER);
                                    break;
                                case Constants.REQUEST_NEW_TRANSPONDER_USER:
                                    registerTransponderUser(update, db);
                                    requestMap.remove(userId);
                                    commandLog(update, Constants.REQUEST_NEW_TRANSPONDER_USER);
                                    break;
                            }
                        } else {
                            sendMessage(userId, "Unknown command!");
                        }

                }
            } else if (update.message().photo() != null || update.message().document() != null) {
                String fullPath = "";
                if (update.message().photo() != null) { //if the photo was send as photo (bad)
                    fullPath = bot.getFullFilePath(bot.execute(new GetFile(update.message().photo()[1].fileId())).file());
                } else if (update.message().document() != null) { //if the photo was send as document (good)
                    fullPath = bot.getFullFilePath(bot.execute(new GetFile(update.message().document().fileId())).file());
                }
                String codeContent = "";
                Drink currentDrink;
                try {
                    BufferedImage bufferedImage = ImageIO.read(new URL(fullPath));
                    codeContent = new DecodeBarCodeService().decodeCode(bufferedImage);
                    currentDrink = getDrinkWithCode(codeContent);
                    dbService.setAmount(userId, currentDrink.getCost() * -1, db);
                    sendMessage(userId, "Code: " + codeContent + ""
                            + "\nProduct: " + currentDrink.getName() + ""
                            + "\nCost: " + decimalFormat.format(currentDrink.getCost()) + "€"
                            + "\nNew amount: " + decimalFormat.format(dbService.getAmount(userId, db)) + "€");
                    LOG.info(update.message().from().lastName() + ", "
                            + update.message().from().firstName()
                            + " Code: "
                            + codeContent);
                } catch (IOException e) {
                    sendMessage(userId, "An error occurred!");
                    LOG.error("IOException");
                } catch (ReaderException e) {
                    sendMessage(userId, "Send as uncompressed file!");
                    LOG.error("No code in the picture");
                } catch (NumberFormatException e) {
                    sendMessage(userId, "Code: " + codeContent);
                    LOG.info(update.message().from().lastName() + ", " + update.message().from().firstName() + " Code: " + codeContent);
                } catch (NullPointerException e) {
                    sendMessage(userId, "No supported file!");
                } catch (ArticleNotFoundException e) {
                    sendMessage(userId, "No article Found!");
                    LOG.error("Article not found!");
                }
            } else {
                sendMessage(userId, "Filetype not supported!");
            }
        } else if (update.message().text().equals("/cancel")) {
            if (requestMap.containsKey(userId)) {
                requestMap.remove(userId);
                sendMessage(userId, "Your action have been canceled!");
            } else {
                sendMessage(userId, "You don't have an active action!");
            }
        } else {
            sendMessage(userId, "Please make an account first!");
        }
    }

    private void getCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        sendMessage(userID, "Your current amount ist " + decimalFormat.format(dbService.getAmount(userID, db)) + "€");
    }


    private void removeCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            requestMap.put(userID, Constants.REQUEST_REMOVE_AMOUNT);
            sendMessage(userID, "Please enter the amount you want to remove.");
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void addCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            requestMap.put(userID, Constants.REQUESTED_ADD_AMOUNT);
            sendMessage(userID, "Please enter the amount to add.");
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void updateCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        updateDrinkList();
        sendMessage(userID, "Drinklist has been successfully updated.");
    }

    private void codeCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        String code = update.message().text().split(" ")[1];
        Drink currentDrink = null;
        try {
            currentDrink = getDrinkWithCode(code);
        } catch (ArticleNotFoundException e) {
            sendMessage(userID, "Article not found!");
            e.printStackTrace();
        }
        sendMessage(userID, "Code: " + code
                + ", Product: " + currentDrink.getName() + ", Kosten: " + currentDrink.getCost() + "€");
    }

    private void deleteCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            requestMap.put(userID, Constants.REQUEST_DELETE);
            sendMessage(userID, "Please enter \"Yes, I want to delete everything.\".");
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void transponderCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            requestMap.put(userID, Constants.REQUEST_TRANSPONDER);
            sendMessage(userID, "Enter your transponder ID.");
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void nameCommandExecuted(Update update, MongoClient db) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            requestMap.put(userID, Constants.REQUESTED_NAME);
            sendMessage(userID, "Please enter your new name.");
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void infoCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            try {
                User currentUser = dbService.getUser(userID, mongoClient);
                sendMessage(userID, "Your Name: " + currentUser.getName()
                        + "\nYour Chat-ID: " + currentUser.getUserID()
                        + "\nMember since: " + new Date(currentUser.getRegisterDate())
                        + "\nCurrent amount: " + String.valueOf(decimalFormat.format(currentUser.getCredit())).replace(".", ",") + "€"
                        + (dbService.haveUserTransponder(userID, mongoClient) ? "\nTag-ID: " + currentUser.getTagId() : ""));
            } catch (UserNotFoundException e) {
                sendMessage(userID, "User not found!");
                e.printStackTrace();
            }
        } else {
            sendMessage(userID, "TagID not found. Please send /register again.");
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
                sendMessage(userID, "Hallo " + firstName + ", your account have been created."
                        + "\nYour TagID is " + tagId);
            } else {
                sendMessage(userID, "TagID not found. Please send /register again.");
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
            sendMessage(userID, "Hello " + newUser.getName() + ", your account have been created.");
        }
    }

    private void registerCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        if (!requestMap.containsKey(userID)) {
            if (!dbService.haveUserAccountInDB(userID, mongoClient)) {
                sendMessage(userID, "Do you have already a transponder from the physical kiosk system?"
                        + "\nSend /yes if you have and /no if not. You also can connect your tag later.");
                requestMap.put(userID, Constants.REQUEST_NEW_USER);
            } else {
                sendMessage(userID, "Sorry " + update.message().from().firstName() + ", you already have an account.");
            }
        } else {
            sendMessage(userID, "Please finish your existing operation first.");
        }
    }

    private void startCommandExecuted(Update update, MongoClient mongoClient) {
        long userID = update.message().from().id();
        sendMessage(userID, "Hi! Please send a picture of the barcode as UNCOMPRESSED file!"
                + "\nOtherwise a correct detection cannot be guaranteed."
                + "\nPlease type /register to start.");
    }

    private void commandLog(Update update, String executedComment){
        LOG.info(update.message().from().firstName() + " " + update.message().from().lastName() + ": "
                + " Chat ID: " + update.message().from().id() + ", Executed Comment: " + executedComment);
    }

}


