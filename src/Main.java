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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;


public class Main {

    ArrayList<String> branchURLList = new ArrayList<>();
    ArrayList<String> repoNameList = new ArrayList<>();
    ArrayList<String> repoURLList = new ArrayList<>();
    ArrayList<String> savePathList = new ArrayList<>();


    public static void main(String[] args) {
        new Main();
    }

    public Main(){
        Timer timer = new Timer();
        Properties properties = new Properties();

        String folderName = "data";

        Path directoryPath = Paths.get(folderName);

        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            writeToProperties("Test");
        } else {
            try {
                Files.createDirectories(directoryPath);
                System.out.println("Directory created: " + directoryPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            writeToProperties("Test");
        }

        getproperties("Test");

        String accessToken = "AcessToken"; // GitHub personal access token
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Starting Task");
                for (String repoNameFromList : repoNameList){
                    int index = repoNameList.indexOf(repoNameFromList);
                    try {
                        if (checkBranch(branchURLList.get(index), accessToken,repoNameFromList, properties)) {
                            System.out.println("Checked Branch");
                            if (downloadRepository(repoURLList.get(index), accessToken, savePathList.get(index))) {
                                Date date = new Date();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                String dateFormatted = dateFormat.format(date);
                                properties.put(repoNameFromList + " Last Downloaded", dateFormatted.replace(" ", "").replace(":", ""));
                            }
                        }

                        FileOutputStream fileOutputStream = new FileOutputStream("config.properties");
                        properties.store(fileOutputStream, "Config Params");
                        fileOutputStream.close();
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }

                long nextexec = System.currentTimeMillis() + 30000;
                // Format the next execution time
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String nextExecutionTimeString = sdf.format(new Date(nextexec));

                // Display the message
                System.out.println("Timer will run again at: " + nextExecutionTimeString);
            }
        };

        //Schedule the TimerTask to run every 5 minutes (300000 milliseconds)
        timer.schedule(task, 0, 30000);
    }
    public void getproperties(String reponmae) {

        try {
            List<String> lines = Files.readAllLines(Paths.get("data\\" + reponmae + ".txt"));
            for (String line : lines) {
                if (line.startsWith("branchURLList=")) {
                    String branchURL = line.substring("branchURLList=".length());
                    branchURLList.add(branchURL);
                } else if (line.startsWith("repoNameList=")) {
                    String repoName = line.substring("repoNameList=".length());
                    repoNameList.add(repoName);
                } else if (line.startsWith("repoURLList=")) {
                    String repoURL = line.substring("repoURLList=".length());
                    repoURLList.add(repoURL);
                } else if (line.startsWith("savePathList=")) {
                    String savePath = line.substring("savePathList=".length());
                    savePathList.add(savePath);
                }
            }

            // Print the retrieved data
            System.out.println("branchURLList: " + branchURLList);
            System.out.println("repoNameList: " + repoNameList);
            System.out.println("repoURLList: " + repoURLList);
            System.out.println("savePathList: " + savePathList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void writeToProperties(String reponame){

        branchURLList.add("https://api.github.com/repos/LSuter22/Hardcoded_Auth_Admin/branches/master");

        repoNameList.add("Hardcoded_Auth_AdminTest");

        repoURLList.add("https://github.com/LSuter22/Hardcoded_Auth_Admin/archive/master.zip");

        savePathList.add("C:\\Users\\losut\\OneDrive\\backups\\Hardcoded_Auth_Admin.zip");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data\\" + reponame +".txt"))) {
            // Write branchURLList
            for (String branchURL : branchURLList) {
                writer.write("branchURLList=" + branchURL);
                writer.newLine();
            }

            // Write repoNameList
            for (String repoName : repoNameList) {
                writer.write("repoNameList=" + repoName);
                writer.newLine();
            }

            // Write repoURLList
            for (String repoURL : repoURLList) {
                writer.write("repoURLList=" + repoURL);
                writer.newLine();
            }

            // Write savePathList
            for (String savePath : savePathList) {
                writer.write("savePathList=" + savePath);
                writer.newLine();
            }

            System.out.println("Configuration data written to file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            return true;
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