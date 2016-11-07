package com.projetangkor.techno.wikiscrape;

import java.io.File;
import java.io.IOException;

public enum WikiIndices {
    STYLESHEET_SITE("styles_site.css", "https://simple.wikipedia.org/w/load.php?debug=false&lang=en&modules=site.styles&only=styles&skin=vector"),
    STYLESHEET_CLIENT("styles_client.css", "https://simple.wikipedia.org/w/load.php?debug=false&lang=en&modules=ext.cite.styles%7Cext.gadget.ReferenceTooltips%7Cext.uls.interlanguage%7Cext.visualEditor.desktopArticleTarget.noscript%7Cext.wikimediaBadges%7Cmediawiki.legacy.commonPrint%2Cshared%7Cmediawiki.sectionAnchor%7Cmediawiki.skinning.interface%7Cskins.vector.styles%7Cwikibase.client.init&only=styles&skin=vector"),
    JAVASCRIPT_STARTUP("startup.js", "https://simple.wikipedia.org/w/load.php?debug=false&lang=en&modules=startup&only=scripts&skin=vector"),
    JAVASCRIPT_JQUERY("jquery.js", "https://simple.wikipedia.org/w/load.php?debug=false&lang=en&modules=jquery%2Cmediawiki&only=scripts&skin=vector&version=0803kc5"),
    IMAGE_LOGO("logo.png", "https://simple.wikipedia.org/static/images/project-logos/simplewiki.png");

    private final String fileName, location;

    WikiIndices(String fileName, String location) {
        this.fileName = fileName;
        this.location = location;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLocation() {
        return location;
    }

    public void downloadFile(File destination) {
        try {
            NetIO.downloadTo(location, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
