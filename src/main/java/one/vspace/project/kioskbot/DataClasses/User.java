package one.vspace.project.kioskbot.DataClasses;

public class User {

    private Long userID;
    private long registerDate;
    private String name;
    private double credit;
    private String tagId;

    public User(Long userID, long registerDate, String name, double credit) {
        this.userID = userID;
        this.registerDate = registerDate;
        this.name = name;
        this.credit = credit;
    }

    public User(Long userID, long registerDate, String name, double credit, String tagId) {
        this.userID = userID;
        this.registerDate = registerDate;
        this.name = name;
        this.credit = credit;
        this.tagId = tagId;
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

    public double getCredit() {
        return credit;
    }

    public void setAmount(double credit) {
        this.credit = credit;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }
}
