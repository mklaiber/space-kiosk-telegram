package one.vspace.project.kioskbot.Controller;

import one.vspace.project.kioskbot.Utils.BarcodeUtils;
import one.vspace.project.kioskbot.DataClasses.Constants;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.pengrad.telegrambot.model.Update;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

import one.vspace.project.kioskbot.Utils.CommandUtils;
import one.vspace.project.kioskbot.Utils.HelperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotController {

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    private ConfigValues configValues = getConfigValues();

    private Map<Long, String> requestMap = new HashMap<>();

    private DBService dbService = new DBService(configValues);

    private TelegramBot bot = new TelegramBot(configValues.getTelegramBotToken());

    private Date date = new Date();

    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private WebDavService webDavDrinkAccessor = new WebDavService(getConfigValues());

    private HelperUtils helperUtils = new HelperUtils(bot, webDavDrinkAccessor);

    private CommandUtils commandUtils = new CommandUtils(dbService, helperUtils, requestMap);

    public static void main(String args[]) {

        new BotController().botControl();

    }

    public void botControl() {
        helperUtils.updateDrinkList();
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



    public void process(Update update) {

        MongoClient db = DBConnector.getConnection(configValues);
        long userId = update.message().from().id();

        if (dbService.haveUserAccountInDB(userId, db) || isStartOrRegisterOrMultilevelCommand(update)) {
            if (isMultiLevelCommand(update)) {
                multiLevelCommandWasReceived(update, db);
            } else if (update.message().text() != null) {
                singleLevelCommandWasReceived(update, db);
            } else if (update.message().photo() != null || update.message().document() != null) {
                photoOrDocumentWasReceived(update, db);
            } else {
                helperUtils.sendMessage(userId, "Filetype not supported!");
            }
        } else {
            helperUtils.sendMessage(userId, "Please make an account first!");
        }
    }

    private boolean isMultiLevelCommand(Update update) {
        return requestMap.containsKey(update.message().from().id());
    }

    private void photoOrDocumentWasReceived(Update update, MongoClient db) {
        long userId = update.message().from().id();
        String fullFilePath = "";
        if (update.message().photo() != null) { //if the photo was send as photo (bad)
            fullFilePath = bot.getFullFilePath(bot.execute(new GetFile(update.message().photo()[update.message().photo().length - 1].fileId())).file());
        } else if (update.message().document() != null) { //if the photo was send as document (good)
            fullFilePath = bot.getFullFilePath(bot.execute(new GetFile(update.message().document().fileId())).file());
        }
        String codeContent = "";
        Drink currentDrink;
        try {
            BufferedImage bufferedImage = ImageIO.read(new URL(fullFilePath));
            codeContent = BarcodeUtils.decode(bufferedImage);
            currentDrink = helperUtils.getDrinkWithCode(codeContent);
            dbService.setAmount(userId, -currentDrink.getCost(), db);
            helperUtils.sendMessage(userId, "Code: " + codeContent + ""
                    + "\nProduct: " + currentDrink.getName() + ""
                    + "\nCost: " + decimalFormat.format(helperUtils.getAmountAsIntegerInEuro(currentDrink.getCost())) + "€"
                    + "\nNew amount: " + decimalFormat.format(dbService.getAmount(userId, db) / 100) + "€");
            LOG.info(update.message().from().lastName() + ", "
                    + update.message().from().firstName()
                    + " Code: "
                    + codeContent);
        } catch (IOException e) {
            LOG.error("IOException", e);
            helperUtils.sendMessage(userId, "An error occurred!");
        } catch (ReaderException e) {
            LOG.error("No code in picture", e);
            helperUtils.sendMessage(userId, "No code detected.\nPlease send picture as file!");
        } catch (NumberFormatException e) {
            LOG.info(update.message().from().lastName() + ", " + update.message().from().firstName() + " Code: " + codeContent, e);
            helperUtils.sendMessage(userId, "Code: " + codeContent);
        } catch (NullPointerException e) {
            LOG.info("Filetype not supported!", e);
            helperUtils.sendMessage(userId, "Filetype not supported!");
        } catch (ArticleNotFoundException e) {
            LOG.error("Article not found! ", e);
            helperUtils.sendMessage(userId, "No article Found!");
        }
    }

    private boolean isStartOrRegisterOrMultilevelCommand(Update update) {
        return update.message().text().equals(Constants.REGISTER_COMMAND)
                || update.message().text().equals(Constants.START_COMMAND) ||
                isMultiLevelCommand(update);
    }

    private void singleLevelCommandWasReceived(Update update, MongoClient db) {
        switch (update.message().text()) {
            case Constants.START_COMMAND:
                commandUtils.startCommandExecuted(update, db);
                break;
            case Constants.REGISTER_COMMAND:
                commandUtils.registerCommandExecuted(update, db);
                break;
            case Constants.INFO_COMMAND:
                commandUtils.infoCommandExecuted(update, db);
                break;
            case Constants.NAME_COMMAND:
                commandUtils.nameCommandExecuted(update, db);
                break;
            case Constants.TRANSPONDER_COMMAND:
                commandUtils.transponderCommandExecuted(update, db);
                break;
            case Constants.DELETE_COMMAND:
                commandUtils.deleteCommandExecuted(update, db);
                break;
            case Constants.CODE_COMMAND:
                commandUtils.codeCommandExecuted(update, db);
                break;
            case Constants.UPDATE_COMMAND:
                commandUtils.updateCommandExecuted(update, db);
                break;
            case Constants.ADD_COMMAND:
                commandUtils.addCommandExecuted(update, db);
                break;
            case Constants.REMOVE_COMMAND:
                commandUtils.removeCommandExecuted(update, db);
                break;
            case Constants.GET_COMMAND:
                commandUtils.getCommandExecuted(update, db);
                break;
            default:
                commandUtils.noPossibleAction(update, db);
                break;
        }
    }

    private void multiLevelCommandWasReceived(Update update, MongoClient db) {
        long userId = update.message().from().id();
        if (!requestMap.containsKey(userId)) {
            helperUtils.sendMessage(userId, "Unknown command!");
            return;
        }
        switch (requestMap.get(userId)) {
            case Constants.REQUESTED_ADD_AMOUNT:
                commandUtils.requestedAddAmountCommandWasExecuted(update, db);
                break;
            case Constants.REQUESTED_NAME:
                commandUtils.requestedNameChangeCommandWasExecuted(update, db);
                break;
            case Constants.REQUEST_DELETE:
                commandUtils.requestedDeleteCommandWasExecuted(update, db);
                break;
            case Constants.REQUEST_REMOVE_AMOUNT:
                commandUtils.requestedRemoveAmountCommandWasExecuted(update, db);
                break;
            case Constants.REQUEST_TRANSPONDER:
                commandUtils.requestedSetTransponderCommandWasExecuted(update, db);
                break;
            case Constants.REQUEST_NEW_USER:
                commandUtils.requestedNewUserCommandWasExecuted(update, db);
                break;
            case Constants.REQUEST_NEW_TRANSPONDER_USER:
                commandUtils.requestedNewTransponderUserCommandWasExecuted(update, db);
                break;
        }
    }


}


