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

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;


public class Main {

    ArrayList<String> branchURLList = new ArrayList<>();
    ArrayList<String> repoNameList = new ArrayList<>();
    ArrayList<String> repoURLList = new ArrayList<>();
    ArrayList<String> savePathList = new ArrayList<>();

    JTextArea TextArea;
    JTextField RepoURLField;
    JTextField BranchURLField;
    JTextField RepoNameField;

    public static void main(String[] args) {
        new Main();
    }

    protected TitledBorder createTitledBorder(String text) {
        final TitledBorder border = new TitledBorder(" " + text + " ");
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        return border;
    }

    public Main() {

        JFrame frame = new JFrame();
        JPanel OutputPanel = new JPanel();
        JPanel addPanel = new JPanel();

        OutputPanel.setBorder(createTitledBorder("Output"));
        OutputPanel.setLayout(new FlowLayout());
        OutputPanel.setPreferredSize(new Dimension(1000,750));
        addPanel.setBorder(createTitledBorder("Add"));
        addPanel.setLayout(new FlowLayout());
        addPanel.setPreferredSize(new Dimension(1000,250));
        frame.setPreferredSize(new Dimension(1010,1030));
        frame.setLayout(new BorderLayout());

        TextArea = new JTextArea();
        TextArea.setSize(990,740);
        TextArea.setLineWrap(true);
        TextArea.setOpaque(false);
        TextArea.setBorder(BorderFactory.createEmptyBorder(0,5,0,10));
        TextArea.setEditable(false);

        RepoURLField = new JTextField();
        RepoURLField.setPreferredSize(new Dimension(980,25));
        RepoURLField.setToolTipText("Repo URL");

        BranchURLField = new JTextField();
        BranchURLField.setPreferredSize(new Dimension(980,25));
        BranchURLField.setToolTipText("Branch URL");

        RepoNameField = new JTextField();
        RepoNameField.setPreferredSize(new Dimension(980,25));
        RepoNameField.setToolTipText("Repo Name");

        JButton addtoconfig = new JButton(new AbstractAction("Add to Backup") {
            @Override
            public void actionPerformed(ActionEvent e) {
                writeToProperties(RepoNameField.getText(),BranchURLField.getText(),RepoURLField.getText(),"/Users/lukesuter/Desktop/TLJ Qdesk.zip");
            }
        });

        OutputPanel.add(TextArea);
        addPanel.add(RepoNameField);
        addPanel.add(RepoURLField);
        addPanel.add(BranchURLField);
        addPanel.add(addtoconfig);

        Timer timer = new Timer();
        Properties properties = new Properties();

        File Directory = new File("data");

        if (!Directory.exists()) {
            Directory.mkdir();
        }

        writeToProperties("Q-Desk","https://api.github.com/repos/TLJ-Group/TLJ_Qdesk/branches/master","https://github.com/TLJ-Group/TLJ_Qdesk/archive/master.zip","/Users/lukesuter/Desktop/TLJ Qdesk.zip");

        getRepoNames();

        for (String s: repoNameList){
            getproperties(s);
        }


        String accessToken = "Access token"; // GitHub personal access token

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Starting Task");
                for (String repoNameFromList : repoNameList) {
                    int index = repoNameList.indexOf(repoNameFromList);
                    try {
                        if (checkBranch(branchURLList.get(index), accessToken, repoNameFromList, properties)) {
                            System.out.println("Checked Branch");
                            if (downloadRepository(repoURLList.get(index), accessToken, savePathList.get(index))) {
                                System.out.println("Downlaoded " + repoNameFromList);
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

                long nextexec = System.currentTimeMillis() + 900000;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String nextExecutionTimeString = sdf.format(new Date(nextexec));

                // Display the message
                System.out.println("Timer will run again at: " + nextExecutionTimeString);
            }
        };

        //Schedule the TimerTask to run every 15 minutes (900000 milliseconds)
        timer.schedule(task, 0, 900000);

        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setTitle("Github Backup");
        frame.add(OutputPanel,BorderLayout.NORTH);
        frame.add(addPanel,BorderLayout.SOUTH);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

    }

    public void getproperties(String reponmae) {

        try {
            List<String> lines = Files.readAllLines(Paths.get("data\\" + reponmae + ".txt"));
            for (String line : lines) {
                if (line.startsWith("branchURLList=")) {
                    String branchURL = line.substring("branchURLList=".length());
                    branchURLList.add(branchURL);
                }else if (line.startsWith("repoURLList=")) {
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

    public void getRepoNames(){
        try {
            List<String> lines = Files.readAllLines(Paths.get("data\\reponames.txt"));
            repoNameList.addAll(lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToProperties(String reponame,String BranchURL,String repoURL,String savePath) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data\\" + reponame + ".txt"))) {
            // Write branchURLList
                writer.write("branchURLList=" + BranchURL);
                writer.newLine();

            // Write repoNameList
                writer.write("repoNameList=" + reponame);
                writer.newLine();

            // Write repoURLList
                writer.write("repoURLList=" + repoURL);
                writer.newLine();

            // Write savePathList
                writer.write("savePathList=" + savePath);
                writer.newLine();

            System.out.println("Configuration data written to file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data\\reponames.txt"))) {
            writer.write(reponame);
            writer.newLine();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        System.out.println("Configuration data written to file.");
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