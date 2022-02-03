package one.vspace.project.kioskbot.DataClasses;

public class Drink {

    private String name;
    private float cost;
    private String productId;

    public Drink(String name, float cost, String productId) {
        this.name = name;
        this.cost = cost;
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
