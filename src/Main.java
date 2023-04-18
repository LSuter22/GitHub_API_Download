import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws IOException {
        String repositoryUrl = "https://github.com/LSuter22/Hardcoded_Auth_Admin/archive/master.zip"; // Replace with your repository URL
        String accessToken = "Access Token Here"; // Replace with your GitHub access token
        String savePath = "/Users/lukesuter/Desktop/repository.zip"; // Replace with the desired save path

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(repositoryUrl);
        request.addHeader("Authorization", "Bearer " + accessToken);
        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();

        try (FileOutputStream fileOutputStream = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Repository downloaded successfully!");
        } finally {
            EntityUtils.consume(entity);
            response.close();
            httpClient.close();
        }
    }
}