import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class upload_novel_data_to_DB {

    static final String DB_URL = "jdbc:mariadb://localhost:3306/novel";
    static final String USER = "root";
    static final String PASS = "xxxxxxxxxx";
    static final Map<String, Integer> existingTitles = new HashMap<>();

    public static void main(String[] args) {
        String directoryPath = "K:\\VisualStudioCodes\\Java\\Truyen_Scraper\\all_novels\\Books_Links";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            if (conn != null) {
                System.out.println("Connected to the database");

                String fetchAllBooksData = "SELECT Name, Total_Chapters FROM books";
                try (PreparedStatement preparedStatement = conn.prepareStatement(fetchAllBooksData);
                        ResultSet resultSet = preparedStatement.executeQuery()) {
                    // Step 3: Store the retrieved book titles and total chapters in a map
                    while (resultSet.next()) {
                        String existingTitle = resultSet.getString("Name");
                        int totalChapters = resultSet.getInt("Total_Chapters");
                        existingTitles.put(existingTitle, totalChapters);
                    }
                }

                File directory = new File(directoryPath);
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {

                            System.out.println("Working on " + file.getName());
                            List<String> urlsFromFile = readUrlsFromFile(file);

                            book_info current_book = extractBookInfo(urlsFromFile);
                            if (current_book != null) {
                                int novelId = 0;
                                String checkQuery = "SELECT COUNT(*) FROM books WHERE Name = ?";
                                try (PreparedStatement checkStatement = conn.prepareStatement(checkQuery)) {
                                    checkStatement.setString(1, current_book.getName());
                                    try (ResultSet bookList = checkStatement.executeQuery()) {
                                        bookList.next();
                                        int count = bookList.getInt(1);
                                        if (count > 0) {
                                            // update book
                                            String updateQuery = "UPDATE books SET Status = ?, Total_Chapters = Total_Chapters + ? WHERE Name = ?";
                                            try (PreparedStatement updateStatement = conn
                                                    .prepareStatement(updateQuery)) {
                                                updateStatement.setString(1, current_book.getStatus());
                                                updateStatement.setInt(2, current_book.getTotalChapters());
                                                updateStatement.setString(3, current_book.getName());
                                                updateStatement.executeUpdate();
                                                System.out.println("Book updated successfully.");
                                            }
                                        } else {
                                            // add new book
                                            String insertQuery = "INSERT INTO books (Name, Author, Type, Status, Total_Chapters) VALUES (?, ?, ?, ?, ?)";
                                            try (PreparedStatement insertStatement = conn
                                                    .prepareStatement(insertQuery)) {
                                                insertStatement.setString(1, current_book.getName());
                                                insertStatement.setString(2, current_book.getAuthor());
                                                insertStatement.setString(3, current_book.getType());
                                                insertStatement.setString(4, current_book.getStatus());
                                                insertStatement.setInt(5, current_book.getTotalChapters());
                                                insertStatement.executeUpdate();
                                                System.out.println("Data inserted into the books table successfully.");
                                            }
                                        }
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                    }
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }

                                String getIDquery = "SELECT id FROM books WHERE Name = ?";
                                try (PreparedStatement preparedStatement = conn.prepareStatement(getIDquery)) {
                                    preparedStatement.setString(1, current_book.getName());
                                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                        if (resultSet.next()) {
                                            novelId = resultSet.getInt("id");
                                            System.out.println(
                                                    "Book ID for '" + current_book.getName() + "': " + novelId);
                                        } else {
                                            System.out.println("No book found with name: " + current_book.getName());
                                            // Handle the case where no book with the provided name exists
                                        }
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }

                                for (Map.Entry<Integer, String> contents : current_book.getChaptersContent()
                                        .entrySet()) {
                                    int current_number = contents.getKey();
                                    String checkbookdataQuery = "SELECT COUNT(*) FROM bookdata WHERE novel_id = ? AND Chapter = ?";

                                    try (PreparedStatement checkStatement = conn.prepareStatement(checkbookdataQuery)) {
                                        checkStatement.setInt(1, novelId); // Set the book name as the parameter
                                        checkStatement.setInt(2, contents.getKey());
                                        try (ResultSet resultSet = checkStatement.executeQuery()) {
                                            resultSet.next();
                                            int count = resultSet.getInt(1);
                                            if (count > 0) {
                                                String checkchapterexist = "SELECT MAX(chapter) AS max_chapter FROM bookdata WHERE novel_id = ?";
                                                PreparedStatement preparedStatement = conn
                                                        .prepareStatement(checkchapterexist);
                                                preparedStatement.setInt(1, novelId);
                                                try (ResultSet resultSet2 = preparedStatement.executeQuery()) {
                                                    if (resultSet2.next()) {
                                                        current_number = resultSet2.getInt("max_chapter") + 1;
                                                    }
                                                } catch (Exception e) {
                                                    // TODO: handle exception
                                                }
                                            }

                                        }

                                        String query2 = "INSERT INTO bookdata (novel_id, chapter, content) VALUES (?, ?, ?)";
                                        try (PreparedStatement preparedStatement = conn
                                                .prepareStatement(query2)) {
                                            preparedStatement.setInt(1, novelId);
                                            preparedStatement.setInt(2, current_number);
                                            preparedStatement.setString(3, contents.getValue());
                                            preparedStatement.executeUpdate();
                                            System.out.println(contents.getKey()
                                                    + " - Data inserted into the bookdata table successfully.");
                                        }

                                    }
                                }
                            }
                            file.delete();
                            System.out.println("Done with file " + current_book.getName() + " and deleted");
                        }

                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readUrlsFromFile(File file) {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                urls.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }

    private static book_info extractBookInfo(List<String> links) throws SQLException {
        String[] parts = links.get(0).split("/");
        String part1 = parts[0] + "//" + parts[2] + "/" + parts[3] + "/";
        String part2 = parts[4] + "/";
        String fullUrl = part1 + part2;
        Map<Integer, String> chapter_content = new HashMap<>();

        try {
            Document document = Jsoup.connect(fullUrl).get();
            Element bookInfoElement = document.selectFirst("div.book-information.cf");
            String title = bookInfoElement.selectFirst("div.book-info h1").wholeText().trim();
            title = title.replaceAll("[\\u4E00-\\u9FFF]", "");
            String author = bookInfoElement.selectFirst("div.book-info p.tag a").wholeText().trim();
            String type = bookInfoElement.selectFirst("div.book-info p.tag a:last-of-type").wholeText().trim();
            String status = bookInfoElement.selectFirst("div.book-info p.tag span").wholeText().trim();
            int totalChapters = links.size();

            if (existingTitles.containsKey(title)) {
                if (totalChapters <= existingTitles.get(title)) {
                    return null;
                } else {
                    for (int i = 0; i < existingTitles.get(title) - totalChapters; i++) {
                        links.remove(0);
                    }
                }
            }

            for (String url : links) {
                // System.out.println("Processing URL: " + url);
                try {
                    int chapterNumber = Integer.parseInt(url.substring(url.lastIndexOf("-") + 1));
                    if (chapter_content.containsKey(chapterNumber)) {
                        if (!chapter_content.isEmpty()) {
                            int highestKey = Collections.max(chapter_content.keySet());
                            chapterNumber = highestKey + 1;
                        }
                    }
                    chapter_content.put(chapterNumber, scrapeChapterContent(url));
                    System.out.println("Done chapter " + chapterNumber + " - " + title);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing chapter number for URL: " + url);
                    e.printStackTrace();
                }
            }
            return new book_info(title, author, type, status, totalChapters, chapter_content);
        } catch (HttpStatusException e) {
            // Handle the case where the URL returns a 404 error (page not found)
            System.err.println("Error fetching URL: " + e.getUrl() + ", Status code: " + e.getStatusCode());
            e.printStackTrace();
            return null; // Return null to indicate failure in fetching content
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Return null to indicate failure in fetching content
        }
    }

    public static String scrapeChapterContent(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Element h5_tag = document.selectFirst("h5");
            if (h5_tag != null) {
                Element next_div = h5_tag.nextElementSibling();
                if (next_div != null) {
                    String text_content = next_div.wholeText();
                    text_content = text_content.replace("Chương trình ủng hộ Thương hiệu Việt của Tàng Thư Viện", "");
                    if (text_content == " Không tìm được nội dung ") {
                        return null;
                    }
                    return text_content;
                } else {
                    System.out.println("Next div element not found after h5 tag.");
                }
            } else {
                System.out.println("h5 tag not found on the page.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
