package com.projetangkor.techno.wikiscrape;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetIO {

    public static void downloadTo(String location, File destination) throws IOException {
        if (!destination.exists()) {
            destination.createNewFile();
        }
        URL url = new URL(location);
        FileUtils.copyURLToFile(url, destination);
    }

    public static int lengthOf(String location) throws IOException {
        URL url = new URL(location);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        InputStream inputStream = connection.getInputStream();
        int length = connection.getContentLength();
        inputStream.close();
        connection.disconnect();
        return length;
    }

    public static String fileName(String full) {
        return FilenameUtils.getName(full);
    }
}
