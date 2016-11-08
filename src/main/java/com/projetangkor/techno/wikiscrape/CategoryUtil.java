package com.projetangkor.techno.wikiscrape;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryUtil {

    private static final List<String> dump = new ArrayList<>();

    public static Map<String, String> getArticles(String category, String host, int level) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        if (!category.startsWith("Category:")) {
            category = "Category:" + category;
        }
        category = category.replace(" ", "_");
        String apiLink = host + "w/api.php?action=query&list=categorymembers&cmtitle=" + category + "&cmlimit=500&format=json";
        String result = IOUtils.toString(new URL(apiLink), Charset.forName("UTF-8"));
        JSONObject query = (JSONObject) ((JSONObject) new JSONParser().parse(result)).get("query");
        JSONArray categoryMembers = (JSONArray) query.get("categorymembers");
        for (Object categoryMember : categoryMembers) {
            JSONObject member = (JSONObject) categoryMember;
            String title = (String) member.get("title");
            if (dump.contains(title)) {
                continue;
            }
            dump.add(title);
            if (!title.startsWith("Category:") && !title.startsWith("Template:")) {
                map.put(title, host + "wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8"));
            } else if (title.startsWith("Category:")) {
                if (level < 5) {
                    System.out.println("Going into subcategory " + title + "... (" + level + ")");
                    map.putAll(getArticles(title, host, level + 1));
                }
            }
        }
        if (level == 0) {
            dump.clear();
        }
        return map;
    }
}
