import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class WikipediaSearcher {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Введите поисковый запрос (или 'exit' для выхода): ");
            String searchQuery = scanner.nextLine();

            if (searchQuery.equalsIgnoreCase("exit")) {
                break;
            }

            SearchEngine searchEngine = new SearchEngine();
            List<SearchResult> searchResults = searchEngine.performSearch(searchQuery);

            if (searchResults.isEmpty()) {
                System.out.println("Результатов поиска не найдено.");
            } else {
                System.out.println("Результаты поиска:");
                for (int i = 0; i < searchResults.size(); i++) {
                    System.out.println((i + 1) + ") " + searchResults.get(i).getTitle());
                }

                System.out.print("Введите номер статьи для открытия (или 0 для выхода): ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Очистка буфера

                if (choice == 0) {
                    break;
                } else if (choice > 0 && choice <= searchResults.size()) {
                    String pageId = searchResults.get(choice - 1).getPageId();
                    String articleUrl = "https://ru.wikipedia.org/w/index.php?curid=" + pageId;
                    BrowserOpener browserOpener = new BrowserOpener();
                    browserOpener.openArticle(articleUrl);
                } else {
                    System.out.println("Некорректный ввод.");
                }
            }
        }

        scanner.close();
    }
}

class SearchResult {
    private String title;
    private String pageId;

    public SearchResult(String title, String pageId) {
        this.title = title;
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public String getPageId() {
        return pageId;
    }
}

class SearchEngine {

    public List<SearchResult> performSearch(String searchQuery) {
        String response = sendRequest(searchQuery);
        return parseSearchResults(response);
    }

    private String sendRequest(String searchQuery) {
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            URL url = new URL("https://ru.wikipedia.org/w/api.php?action=query&list=search&utf8=&format=json&srsearch=" + encodedQuery);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                System.err.println("Ошибка при отправке запроса: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.err.println("Ошибка при отправке запроса: " + e.getMessage());
        }
        return "";
    }

    private List<SearchResult> parseSearchResults(String response) {
        List<SearchResult> searchResults = new ArrayList<>();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
        JsonArray searchResultsArray = jsonObject.getAsJsonObject("query").getAsJsonArray("search");

        for (JsonElement element : searchResultsArray) {
            JsonObject searchResult = element.getAsJsonObject();
            String title = searchResult.get("title").getAsString();
            String pageId = searchResult.get("pageid").getAsString();
            searchResults.add(new SearchResult(title, pageId));
        }

        return searchResults;
    }
}

class BrowserOpener {

    public void openArticle(String articleUrl) {
        try {
            Desktop.getDesktop().browse(new URL(articleUrl).toURI());
        } catch (Exception e) {
            System.err.println("Ошибка при открытии статьи: " + e.getMessage());
        }
    }
}