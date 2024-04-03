import java.util.List;
import java.util.Map;

public class book_info {
    private String name;
    private String author;
    private String type;
    private String status;
    private int totalChapters;
    private Map<Integer, String> chaptersContent;

    // Constructor
    public book_info(String name, String author, String type, String status, int totalChapters, Map<Integer, String> chaptersContent) {
        this.name = name;
        this.author = author;
        this.type = type;
        this.status = status;
        this.totalChapters = totalChapters;
        this.chaptersContent = chaptersContent;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalChapters() {
        return totalChapters;
    }
    

    public Map<Integer, String> getChaptersContent() {
        return chaptersContent;
    }

    public void setChaptersContent(Map<Integer, String> chaptersContent) {
        this.chaptersContent = chaptersContent;
    }

    // Setters (if needed)
}
