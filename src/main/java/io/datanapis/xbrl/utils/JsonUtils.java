package io.datanapis.xbrl.utils;

import org.jsoup.safety.Safelist;

import java.util.List;

public class JsonUtils {

    private static final String ATTRIBUTE_STYLE = "style";
    private static final String ATTRIBUTE_CLASS = "class";

    public static Safelist relaxed() {
        Safelist safelist = Safelist.relaxed();
        for (var tag : List.of("div", "span", "table", "th", "tr", "td")) {
            safelist.addAttributes(tag, ATTRIBUTE_STYLE, ATTRIBUTE_CLASS);
        }
        return safelist;
    }
}
