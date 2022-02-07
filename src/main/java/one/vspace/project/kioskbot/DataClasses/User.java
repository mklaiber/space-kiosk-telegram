package one.vspace.project.kioskbot.DataClasses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private Long userID;
    private long registerDate;
    private String name;
    private int credit;
    private String tagId;

    public User(Long userID, long registerDate, String name, int credit) {
        this.userID = userID;
        this.registerDate = registerDate;
        this.name = name;
        this.credit = credit;
    }

    public User(Long userID, long registerDate, String name, int credit, String tagId) {
        this.userID = userID;
        this.registerDate = registerDate;
        this.name = name;
        this.credit = credit;
        this.tagId = tagId;
    }

}
