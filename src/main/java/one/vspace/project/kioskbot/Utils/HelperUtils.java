package one.vspace.project.kioskbot.Utils;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import one.vspace.project.kioskbot.Controller.BotController;
import one.vspace.project.kioskbot.DataClasses.Drink;
import one.vspace.project.kioskbot.Exceptions.ArticleNotFoundException;
import one.vspace.project.kioskbot.Exceptions.TooBigNumberException;
import one.vspace.project.kioskbot.Service.WebDavService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class HelperUtils {

    private static final Logger LOG = LoggerFactory.getLogger(BotController.class);

    @NonNull
    private TelegramBot bot;
    @NonNull
    private WebDavService webDavService;

    private Instant lastDrinksUpdate;

    private List<Drink> drinkList;

    public void setDrinkList() {
        this.drinkList = updateDrinkList();
    }

    private List<Drink> updateDrinkList() {
        lastDrinksUpdate = Instant.now();
        String[] drinks = webDavService.downloadWebDav().replace(',', '.').split("\n");
        List<Drink> drinkList = new ArrayList<>();
        for (String drink : drinks) {
            String[] finalDrinks = drink.trim().split(";");
            drinkList.add(new Drink(finalDrinks[0].trim(), (int) Float.parseFloat(finalDrinks[1].trim()) * 100, finalDrinks[2].trim()));
        }
        return drinkList;
    }

    public Drink getDrinkWithCode(String code) throws ArticleNotFoundException {
        if(isDataOlderThanRefreshTime()){
            drinkList = updateDrinkList();
        }
        for (Drink drink : drinkList) {
            if (drink.getProductId().equals(code))
                return drink;
        }
        throw new ArticleNotFoundException("Code " + code + " does not respond to any article!");
    }

    public void sendMessage(long userId, String message) {
        SendResponse sendResponse = bot.execute(new SendMessage(userId, message));
        if (sendResponse.isOk()) {
            LOG.info("Message " + sendResponse.message() + " was send successfully.");
        }
    }

    private boolean isDataOlderThanRefreshTime() {
        return (lastDrinksUpdate.isBefore(Instant.now().minus(1, ChronoUnit.HOURS)));
    }

    public int getAmountAsIntegerInCent(Update update) throws TooBigNumberException {
        Float amount = Float.parseFloat(update.message().text().replace(",", ".").replace("€", ""));
        if(amount == Integer.MAX_VALUE){
            throw new TooBigNumberException("Number was too big!");
        } else {
            return (int) (amount * 100);
        }
    }

    public double getAmountAsIntegerInEuro(Integer integer){
        double amount = ((double) integer / 100);
        return amount;
    }

}
