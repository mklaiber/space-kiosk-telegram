package one.vspace.project.kioskbot.Controller;

import one.vspace.project.kioskbot.Service.DecodeBarCodeService;
import one.vspace.project.kioskbot.Exceptions.UserNotFoundException;
import one.vspace.project.kioskbot.DataClasses.Commands;
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

    private final int REQUESTED_ADD_AMOUNT = 77;
    private final int REQUESTED_NAME = 99;
    private final int REQUEST_DELETE = 66;
    private final int REQUEST_REMOVE_AMOUNT = 55;
    private final int REQUEST_TRANSPONDER = 44;
    private final int REQUEST_NEW_USER = 33;
    private final int REQUEST_NEW_TRANSPONDER_USER = 22;

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    private ConfigValues configValues = getConfigValues();

    private Map<Long, Integer> requestMap = new HashMap<>();

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
        String[] drinks = webDavDrinkAcessor.download().replace(',', '.').split("\n");
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

        if ((dbService.haveUserAccountInDB(userId, db) == 1) || update.message().text().equals("/register") || update.message().text().equals("/start")) {
            if (update.message().text() != null) {
                switch (update.message().text()) {
                    case Commands.START_COMMAND:
                        startCommandExecuted(update, db);
                        break;
                    case Commands.REGISTER_COMMAND:
                        registerCommandExecuted(update, db);
                        break;
                    case Commands.INFO_COMMAND:
                        infoCommandExecuted(update, db);
                        break;
                    case Commands.NAME_COMMAND:
                        nameCommandExecuted(update, db);
                        break;
                    case Commands.TRANSPONDER_COMMAND:
                        transponderCommandExecuted(update, db);
                        break;
                    case Commands.DELETE_COMMAND:
                        deleteCommandExecuted(update, db);
                        break;
                    case Commands.CODE_COMMAND:
                        codeCommandExecuted(update, db);
                        break;
                    case Commands.UPDATE_COMMAND:
                        updateCommandExecuted(update, db);
                        break;
                    case Commands.ADD_COMMAND:
                        addCommandExecuted(update, db);
                        break;
                    case Commands.REMOVE_COMMAND:
                        removeCommandExecuted(update, db);
                        break;
                    case Commands.GET_COMMAND:
                        getCommandExecuted(update, db);
                        break;
                    default:
                        if (requestMap.containsKey(userId)) {
                            float amount;
                            switch (requestMap.get(userId)) {
                                case REQUESTED_ADD_AMOUNT:
                                    amount = Float.parseFloat(update.message().text().replace(",", ".").replace("€", ""));
                                    dbService.setAmount(userId, db, amount);
                                    sendMessage(userId, decimalFormat.format(amount) + "€ has been added to your account.");
                                    requestMap.remove(userId);
                                    break;
                                case REQUESTED_NAME:
                                    String name = update.message().text();
                                    dbService.setName(userId, db, name);
                                    sendMessage(userId, "Your name has been set to " + name + ".");
                                    requestMap.remove(userId);
                                    break;
                                case REQUEST_DELETE:
                                    if (update.message().text().equals("Yes, I want to delete everything.")) {
                                        dbService.deleteUser(userId, db);
                                        sendMessage(userId, "All data stored about you has been deleted!");
                                    } else {
                                        sendMessage(userId, "Operation canceled.");
                                    }
                                    requestMap.remove(userId);
                                    break;
                                case REQUEST_REMOVE_AMOUNT:
                                    amount = Float.parseFloat(update.message().text().replace(",", ".").replace("€", ""));
                                    dbService.setAmount(userId, db, amount * -1);
                                    sendMessage(userId, "Your amount has been adjusted by "
                                            + decimalFormat.format(amount) + "€ to "
                                            + decimalFormat.format(dbService.getAmount(userId, db)) + "€.");
                                    requestMap.remove(userId);
                                    break;
                                case REQUEST_TRANSPONDER:
                                    String transponderId = update.message().text();
                                    dbService.setTransponder(userId, transponderId, db);
                                    sendMessage(userId, "Your account has been connected with Tag " + transponderId + ".");
                                    requestMap.remove(userId);
                                    break;
                                case REQUEST_NEW_USER:
                                    if(update.message().text().equals("/yes")){
                                        sendMessage(userId, "Please tell me your TagID."
                                                + "That can be seen in the top left corner of the physical kiosk after scanning your tag.");
                                        requestMap.put(userId, REQUEST_NEW_TRANSPONDER_USER);
                                    } else if(update.message().text().equals("/no")){
                                        registerUser(update, db);
                                    } else {
                                        sendMessage(userId, "Unknown Responce. Cancelling action!");
                                    }
                                    requestMap.remove(userId);
                                    break;
                                case REQUEST_NEW_TRANSPONDER_USER:
                                    registerTransponderUser(update, db);
                                    requestMap.remove(userId);
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
                    dbService.setAmount(userId, db, currentDrink.getCost() * -1);
                    sendMessage(userId, "Code: " + codeContent + ","
                            + "\nProduct: " + currentDrink.getName() + ","
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
        } else {
            sendMessage(userId, "Please make an account first!");
        }
    }

        private void getCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            sendMessage(userID, "Your current amount ist " + decimalFormat.format(dbService.getAmount(userID, db)) + "€");
        }


        private void removeCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                requestMap.put(userID, REQUEST_REMOVE_AMOUNT);
                sendMessage(userID, "Please enter the amount you want to remove.");
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void addCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                requestMap.put(userID, REQUESTED_ADD_AMOUNT);
                sendMessage(userID, "Please enter the amount to add.");
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void updateCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            updateDrinkList();
            sendMessage(userID, "Drinklist has been successfully updated.");
        }

        private void codeCommandExecuted (Update update, MongoClient db){
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

        private void deleteCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                requestMap.put(userID, REQUEST_DELETE);
                sendMessage(userID, "Please enter \"Yes, I want to delete everything.\".");
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void transponderCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                requestMap.put(userID, REQUEST_TRANSPONDER);
                sendMessage(userID, "Enter your transponder ID.");
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void nameCommandExecuted (Update update, MongoClient db){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                requestMap.put(userID, REQUESTED_NAME);
                sendMessage(userID, "Please enter your new name.");
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void infoCommandExecuted (Update update, MongoClient mongoClient){
            long userID = update.message().from().id();
            try {
                User currentUser = dbService.getUser(userID, mongoClient);
                sendMessage(userID, "Your Name: " + currentUser.getName()
                        + "\nYour Chat-ID: " + currentUser.getUserID()
                        + "\nMember since: " + new Date(currentUser.getRegisterDate())
                        + "\nCurrent amount: " + String.valueOf(decimalFormat.format(currentUser.getCredit())).replace(".", ",") + "€");
            } catch (UserNotFoundException e) {
                sendMessage(userID, "User not found!");
                e.printStackTrace();
            }
        }

        private void registerTransponderUser(Update update, MongoClient mongoClient){
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
            dbService.setUserToExistingTransponder(newUser, mongoClient);
            sendMessage(userID, "Hallo " + firstName + ", your account have been created."
                    + "Your TagID is " + tagId);
        }

        private void registerUser(Update update, MongoClient mongoClient){
            long userID = update.message().from().id();
            User newUser;
            String firstName = update.message().from().firstName();
            String lastName = update.message().from().lastName();
            if (lastName == null) {
                newUser = new User(userID, date.getTime(), (firstName), 0);
            } else if (firstName == null) {
                newUser = new User(userID, date.getTime(), (lastName), 0);
            } else {
                newUser = new User(userID, date.getTime(), (firstName + " " + lastName), 0);
            }
            dbService.addNewUser(newUser, mongoClient);
            sendMessage(userID, "Hallo " + firstName + ", your account have been created.");
        }

        private void registerCommandExecuted (Update update, MongoClient mongoClient){
            long userID = update.message().from().id();
            if (!requestMap.containsKey(userID)) {
                if (dbService.haveUserAccountInDB(userID, mongoClient) == -1) {
                    sendMessage(userID, "Do you have already a transponder from the physical kiosk system?"
                            + "Send /yes if you have and /no if not.");
                    requestMap.put(userID, REQUEST_NEW_USER);
                } else {
                    sendMessage(userID, "Sorry " + update.message().from().firstName() + ", you already have an account.");
                }
            } else {
                sendMessage(userID, "Please finish your existing operation first.");
            }
        }

        private void startCommandExecuted (Update update, MongoClient mongoClient){
            long userID = update.message().from().id();
            sendMessage(userID, "Hi! Please send a picture of the barcode as UNCOMPRESSED file!"
                    + "\nOtherwise a correct detection cannot be guaranteed."
                    + "\nPlease type /register to start.");
        }

}


