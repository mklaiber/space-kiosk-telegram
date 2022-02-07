package one.vspace.project.kioskbot.DataClasses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Drink {

    private String name;
    private int cost;
    private String productId;

    public Drink(String name, int cost, String productId) {
        this.name = name;
        this.cost = cost;
        this.productId = productId;
    }
}
