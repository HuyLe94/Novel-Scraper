import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class all_novels_getlinks {

    public static void main(String[] args) {
        String base_url = "https://truyen.tangthuvien.vn/doc-truyen/page/%d?page=0&limit=99999&web=1";
        List<String> all_href_links = new ArrayList<>();
        //5756,14516,18124,18125,18199,18203,18795,18916,18917,18919,18920,18921,18922,19010,19011,19209,19702 error 500 stoped,
        int page_number = 19210;
        int error_count = 0;
        List<Integer> error_list = new ArrayList<>();
        String folderDirectory = "Books_Links";

        createFolderIfNotExists(folderDirectory);

        while (error_count<10) {
            String url = String.format(base_url, page_number);
            System.out.println(page_number);
            try {
                Document document = Jsoup.connect(url).get();
                Elements ul_elements = document.select("ul.cf");

                if(ul_elements.isEmpty()){
                    break;
                }
                else if (ul_elements.stream().allMatch(ul -> ul.select("li").isEmpty())) {
                    page_number++;
                    continue;
                }

                for (Element ul : ul_elements) {
                    Elements li_elements = ul.select("li");
                    for (Element li : li_elements) {
                        Element a_element = li.selectFirst("a");
                        if (a_element != null) {
                            String href = a_element.attr("href");
                            all_href_links.add(href);
                        }
                    }
                }
                error_count = 0;
                // Save the collected links into a text file before incrementing page number
                /*for (String item : all_href_links) {
                    System.out.println(item);
                }*/
                saveLinksToFile(all_href_links, folderDirectory);
                all_href_links.clear(); // Clear the list after saving to avoid duplicates

            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 500) {
                    error_list.add(page_number);
                    error_count++;
                    page_number++;
                    //System.out.println("HTTP 500 error encountered. Breaking out of the loop.");
                    continue;
                } else {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            page_number++;
        }
        saveIntegersToFile(error_list, folderDirectory);
        System.out.println("All links have been stored in text files");
    }

    private static void saveLinksToFile(List<String> links, String folderDirectory) {
        if (links.isEmpty()) {
            return; // No links to save
        }

        // Extract the filename from the first link to use for all links on the same page
        String firstLink = links.get(0);
        String[] parts = firstLink.split("/");
        String filename = folderDirectory + File.separator + parts[parts.length - 2] + ".txt";
        //System.out.println("Filename for this page: " + filename);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8, false))) {
            for (String link : links) {
                writer.write(link);
                writer.newLine(); // Add a new line after each link
            }
            //System.out.println("Links saved to file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save links to file: " + filename);
        }
    }

    private static void saveIntegersToFile(List<Integer> integers, String folderDirectory) {
        if (integers.isEmpty()) {
            return; // No integers to save
        }
    
        // Create the filename for the text file
        String filename = folderDirectory + File.separator + "integers.txt";
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, StandardCharsets.UTF_8, true))) {
            for (Integer number : integers) {
                writer.write(number.toString()); // Convert integer to string and write to file
                writer.newLine(); // Add a new line after each number
            }
            System.out.println("Integers saved to file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save integers to file: " + filename);
        }
    }

    private static void createFolderIfNotExists(String folderDirectory) {
        Path folderPath = Paths.get(folderDirectory);

        // Check if the folder exists, if not, create it
        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectories(folderPath);
                System.out.println("Folder created successfully: " + folderDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create folder: " + folderDirectory);
            }
        } else {
            System.out.println("Folder already exists: " + folderDirectory);
        }
    }
}