package org.tine2k.accessLogParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class DownloadMain {

    private static final Pattern PATTERN = Pattern.compile("\\<a href=\"(localhost_access.*)\"\\>");

    public static void main(String[] args) throws ClientProtocolException, IOException {

        String url = args[0];
        String targetPath = args[1];

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        StringWriter writer = new StringWriter();
        IOUtils.copy(response.getEntity().getContent(), writer);
        response.close();
        writer.close();

        Matcher matcher = PATTERN.matcher(writer.toString());

        while (matcher.find()) {
            System.err.println(matcher.group(1));
            String filename = matcher.group(1);
            HttpGet getFile = new HttpGet(url + filename);
            CloseableHttpResponse responseFile = httpclient.execute(getFile);
            File dir = new File(targetPath);
            dir.mkdir();
            FileWriter fileWriter = new FileWriter(new File(dir, filename));
            IOUtils.copy(responseFile.getEntity().getContent(), fileWriter);
            responseFile.close();
            fileWriter.close();
        }

    }
}
