import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReadBookInfoFromFiles_testing {

    public static void main(String[] args) {
        String directoryPath = "K:\\VisualStudioCodes\\Java\\Truyen_Scraper\\all_novels\\Books_Links";
        List<String> allBookInfo = new ArrayList<>();

        // Get list of files in the directory
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            // Iterate over each file in the directory
            int index = 1;
            for (File file : files) {
                if (file.isFile()) {
                    // Read the URL from the current file
                    String urlFromFile = readUrlFromFile(file);
                    if (urlFromFile != null) {
                        // Extract book information for the URL
                        String bookInfo = extractBookInfo(urlFromFile);
                        allBookInfo.add(bookInfo);
                    }
                }
                System.out.println("Processing file " + index + ": " + file.getName());
                index++;
            }
        }

        // Save all book information to a single text file
        saveBookInfoToFile(allBookInfo);
    }

    private static String readUrlFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            return reader.readLine(); // Read the first line containing the URL
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Return null if there's an error
        }
    }

    private static String extractBookInfo(String url) {
        // Extract parts 1 and 2 from the URL
        String[] parts = url.split("/");
        String part1 = parts[0] + "//" + parts[2] + "/" + parts[3] + "/";
        String part2 = parts[4] + "/";
        String fullUrl = part1 + part2;

        try {
            Document document = Jsoup.connect(fullUrl).get();
            Element bookInfoElement = document.selectFirst("div.book-information.cf");

            String title = bookInfoElement.selectFirst("div.book-info h1").wholeText().trim();
            String author = bookInfoElement.selectFirst("div.book-info p.tag a").wholeText().trim();
            String type = bookInfoElement.selectFirst("div.book-info p.tag a:last-of-type").wholeText().trim();
            String status = bookInfoElement.selectFirst("div.book-info p.tag span").wholeText().trim();
            

            return String.format("%s, %s, %s, %s, %s", title, author, type, status, fullUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ""; // Return empty string if there's an error
        }
    }

    private static void saveBookInfoToFile(List<String> bookInfoList) {
        String outputFilePath = "K:\\VisualStudioCodes\\Java\\Truyen_Scraper\\all_novels\\all_book_info.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, StandardCharsets.UTF_8))) {
            for (String bookInfo : bookInfoList) {
                writer.write(bookInfo);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle IO exception if needed
        }
    }
}
