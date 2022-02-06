package one.vspace.project.kioskbot.DataClasses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Drink {

    private String name;
    private float cost;
    private String productId;

    public Drink(String name, float cost, String productId) {
        this.name = name;
        this.cost = cost;
        this.productId = productId;
    }
}
