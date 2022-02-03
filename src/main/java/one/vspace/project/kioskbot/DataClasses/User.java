package one.vspace.project.kioskbot.DataClasses;

public class User {

    private Long userID;
    private long registerDate;
    private String name;
    private double amount;

    public User(Long userID, long registerDate, String name, double amount) {
        this.userID = userID;
        this.registerDate = registerDate;
        this.name = name;
        this.amount = amount;
    }

    public User() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public long getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(long registerDate) {
        this.registerDate = registerDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
