import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class Main {
    public static void main(String[] args) {
        Timer timer = new Timer();
        Properties properties = new Properties();

        ArrayList<String> repoNameList = new ArrayList<>();
        ArrayList<String> repoURLList = new ArrayList<>();
        ArrayList<String> branchURLList = new ArrayList<>();
        ArrayList<String> savePathList = new ArrayList<>();

        String repositoryUrl = "https://github.com/LSuter22/Hardcoded_Auth_Admin/archive/master.zip"; //repo url
        String branchurl = "https://api.github.com/repos/LSuter22/Hardcoded_Auth_Admin/branches/master";
        String repoName = "Hardcoded_Auth_Admin";
        String accessToken = "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"; // GitHub personal access token
        String savePath = "/Users/lukesuter/Desktop/repository.zip"; // Path to save repo

        repoNameList.add(repoName);
        repoURLList.add(repositoryUrl);
        branchURLList.add(branchurl);
        savePathList.add(savePath);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Starting Task");
                for (String repoNameFromList : repoNameList){
                    int index = repoNameList.indexOf(repoName);
                try {
                    if (checkBranch(branchURLList.get(index), accessToken,repoNameFromList, properties)) {
                        if (downloadRepository(repoURLList.get(index), accessToken, savePathList.get(index))) {
                            Date date = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String dateFormatted = dateFormat.format(date);
                            properties.put(repoName + " Last Downloaded", dateFormatted.replace(" ", "").replace(":", ""));
                        }
                    }

                    FileOutputStream fileOutputStream = new FileOutputStream("config.properties");
                    properties.store(fileOutputStream, "Config Params");
                    fileOutputStream.close();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }

                }
            }
        };

        // Schedule the TimerTask to run every 5 minutes (300000 milliseconds)
        //timer.schedule(task, 0, 300000);
    }

    public static Boolean checkBranch(String url,String accessToken,String repoName, Properties properties) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + accessToken);


        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();
        String jsonResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        System.out.println(parseUpdatedAt(jsonResponse).replace(" ","").replace(":",""));
        if(properties.get(repoName + "_Last_Updated") != null){
            String lastUpdatedTimeProperties = properties.getProperty(repoName + "_Last_Updated");
            if (!lastUpdatedTimeProperties.equals(parseUpdatedAt(jsonResponse).replace(" ","").replace(":",""))){
                System.out.println("Not equal to previous download");
                properties.put(repoName + "_Last_Updated",parseUpdatedAt(jsonResponse).replace(" ","").replace(":",""));
                return true;
            }else {
                System.out.println("Equal to Previous Download");
            }
        }else{
            System.out.println("Not Previously Downloaded, Setting Date to 2010");
            properties.put(repoName + "_Last_Updated", "01_01_2010");
        }
        return false;
    }

    public static boolean downloadRepository(String repositoryUrl, String accessToken, String savePath) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(repositoryUrl);
        request.addHeader("Authorization", "Bearer " + accessToken);
        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();

        File file = new File(savePath);
        if (file.delete()) {
            System.out.println("Deleted old version of repo");
        } else {
            System.out.println("Failed to delete");
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            return true;
        } finally {
            EntityUtils.consume(entity);
            response.close();
            httpClient.close();
        }
    }

    public static String parseUpdatedAt(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);
        return jsonObject.getAsJsonObject("commit").getAsJsonObject("commit").getAsJsonObject("author").get("date").getAsString();
    }

}