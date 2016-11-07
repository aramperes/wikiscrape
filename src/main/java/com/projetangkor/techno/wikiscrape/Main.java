package com.projetangkor.techno.wikiscrape;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static String pageDirDefault =
            "# Format: PageName=URL\r\n" +
                    "# Lines starting with '#' are ignored\r\n" +
                    "Canada=https://simple.wikipedia.org/wiki/Canada\r\n";
    private static final DecimalFormat format = new DecimalFormat("0.00");

    private static final HashMap<String, String> pages = new HashMap<>();
    private static final List<String> skipPatch = new ArrayList<>();

    public static void main(String[] args) {
        init();
    }

    private static void init() {
        downloadResources();
        pageDirectory();
        downloadPages();
        patchPages();
    }

    private static void downloadResources() {
        System.out.println("> Downloading required resources...");
        File resourcesDir = new File("resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdir();
        }
        for (WikiIndices index : WikiIndices.values()) {
            System.out.print("  - " + index.name() + " (" + index.getFileName() + ")... ");
            File file = new File(resourcesDir, index.getFileName());
            if (!file.exists()) {
                index.downloadFile(file);
                System.out.println("DONE");
            } else {
                System.out.println("OK");
            }
        }
        System.out.println("> Done downloading " + WikiIndices.values().length + " resources.");
    }

    private static void pageDirectory() {
        System.out.print("> Validating page directory... ");
        File pageDir = new File("pages.txt");
        if (pageDir.exists()) {
            System.out.println("OK");
        } else {
            System.out.println("FAILED");
            System.out.print("> pages.txt file missing! Creating file with example... ");
            try {
                FileWriter writer = new FileWriter(pageDir);
                IOUtils.write(pageDirDefault, writer);
                writer.close();
                System.out.println("DONE");
            } catch (IOException e) {
                System.out.println("FAILED");
                System.out.println("Failed to create 'pages.txt'!");
                e.printStackTrace();
                System.exit(1);
            }
            System.out.println(">>> Please fill in the 'pages.txt' file with the Wiki pages to download");
            System.exit(0);
        }
        System.out.print("> Reading page directory... ");
        try {
            List<String> lines = Files.readAllLines(pageDir.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (!line.contains("=")) {
                    continue;
                }
                String[] split = line.split("=", 2);
                String key = split[0], url = split[1];
                pages.put(key, url);
            }
            System.out.println("DONE");
        } catch (IOException e) {
            System.out.println("FAILED");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void downloadPages() {
        if (pages.size() == 0) {
            System.out.println("> There are no pages to download! Please fill in 'pages.txt'. Exitting...");
            System.exit(0);
            return;
        }
        File directory = new File("pages");
        if (!directory.exists()) {
            directory.mkdir();
        }
        int length = 0;
        System.out.println("> Downloading the following pages (" + pages.size() + "): " + StringUtils.join(pages.keySet().toArray(), ", ") + ".");
        for (Map.Entry<String, String> entry : pages.entrySet()) {
            File destination = new File(directory, entry.getKey() + ".html");
            String location = entry.getValue();
            System.out.print("  - Downloading " + entry.getKey() + "... ");
            if (destination.exists()) {
                System.out.println("OK");
                length += destination.length();
                skipPatch.add(entry.getKey());
                continue;
            }
            try {
                NetIO.downloadTo(location, destination);
            } catch (IOException e) {
                System.out.println("FAILED");
                e.printStackTrace();
                System.exit(1);
            }
            System.out.println("DONE");
            length += destination.length();
        }
        double size = ((double) length) / (1024 * 1024);
        System.out.println("> Completed download of " + pages.size() + " pages, totalling " + format.format(size) + "MB.");
    }

    private static void patchPages() {
        System.out.println("> Patching pages for offline display.");
        File directory = new File("pages");
        for (Map.Entry<String, String> entry : pages.entrySet()) {
            File destination = new File(directory, entry.getKey() + ".html");
            System.out.print("  - " + entry.getKey() + ": ");
            if (skipPatch.contains(entry.getKey())) {
                System.out.println("SKIPPED");
                continue;
            }
            System.out.print("\n    - Patching head locations... ");
            try {
                Document document = Jsoup.parse(destination, "UTF-8");
                Element head = document.head();
                Elements stylesheets = head.getElementsByAttributeValue("rel", "stylesheet");
                stylesheets.get(0).attr("href", "../resources/" + WikiIndices.STYLESHEET_CLIENT.getFileName());
                stylesheets.get(1).attr("href", "../resources/" + WikiIndices.STYLESHEET_SITE.getFileName());
                //head.getElementsByTag("script").get(2).attr("src", "../resources/" + WikiIndices.JAVASCRIPT_STARTUP.getFileName());
                //head.prependElement("script").attr("src", "../resources/" + WikiIndices.JAVASCRIPT_JQUERY.getFileName());
                head.getElementsByTag("script").get(2).remove();
                document.getElementsByClass("mw-wiki-logo").get(0).attr("style", "background-image: url(../resources/" + WikiIndices.IMAGE_LOGO.getFileName() + ")");
                document.getElementById("p-personal").remove();
                document.getElementById("left-navigation").remove();
                document.getElementById("right-navigation").remove();
                document.getElementById("mw-head-base").remove();
                document.getElementById("mw-page-base").remove();
                document.getElementById("mw-navigation").remove();
                document.getElementById("footer").remove();
                document.getElementById("catlinks").remove();
                if (document.getElementById("coordinates") != null) document.getElementById("coordinates").remove();
                document.getElementsByClass("mw-editsection").remove();
                document.getElementsByClass("navbox").remove();
                document.getElementsByClass("error").remove();
                document.getElementsByClass("mainpage").remove();
                document.getElementsByClass("metadata").remove();
                document.getElementById("content").attr("style", "margin: 10px 0px 0px 10px;");
                for (Element link : document.getElementsByTag("a")) {
                    if (link.attr("href").startsWith("/w")) {
                        link.tagName("text");
                    } else if (link.className().contains("cite_") || link.attr("href").contains("cite_")) {
                        link.parent().remove();
                    } else if (!link.attr("href").startsWith("#")) {
                        link.remove();
                    }
                }

                if (document.getElementById("References") != null) {
                    Element references = document.getElementById("References").parent();
                    references.nextElementSibling().remove();
                    references.remove();
                }

                System.out.println("DONE");

                File imageDir = new File(directory, "images");
                if (!imageDir.exists()) {
                    imageDir.mkdir();
                }
                System.out.print("    - Resolving images... ");
                int imgLength = 0, imgCount = 0;
                HashMap<String, Pair<Element, File>> imageMap = new HashMap<>();
                for (Element img : document.getElementsByTag("img")) {
                    String location = img.attr("src");
                    if (location.startsWith("//")) {
                        location = "https:" + location;
                    } else if (location.startsWith("/wiki/")) {
                        location = "https://simple.wikipedia.org" + location;
                    } else {
                        continue;
                    }
                    if (!location.contains("png") && !location.contains("svg") && !location.contains("jpg") && !location.contains(".jpeg")) {
                        continue;
                    }
                    File imageDest = new File(imageDir, NetIO.fileName(location));
                    if (imageDest.exists()) {
                        continue;
                    }
                    imgLength += NetIO.lengthOf(location);
                    imgCount++;
                    imageMap.put(location, new ImmutablePair<>(img, imageDest));
                }
                System.out.println("DONE");

                System.out.print("    - Downloading " + imgCount + " images (totalling " + format.format(((double) imgLength) / (1024 * 1024)) + "MB)... ");
                for (Map.Entry<String, Pair<Element, File>> e : imageMap.entrySet()) {
                    NetIO.downloadTo(e.getKey(), e.getValue().getValue());
                    e.getValue().getKey().attr("src", "images/" + e.getValue().getValue().getName().replace("%", "%25"));
                }
                System.out.println("DONE");

                System.out.print("    - Saving HTML file... ");
                Files.write(destination.toPath(), document.html().getBytes(Charset.forName("UTF-16")));
                System.out.println("DONE");
            } catch (IOException e) {
                System.out.println("FAILED");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
