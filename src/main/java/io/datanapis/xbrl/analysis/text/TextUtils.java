package io.datanapis.xbrl.analysis.text;

import org.jsoup.nodes.*;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TextUtils {
    private static final Logger log = LoggerFactory.getLogger(TextUtils.class);

    static final Set<String> HEAD_TAGS = Set.of("p", "h1", "h2", "h3", "h4", "h5", "tr", "div");
    static final Set<String> TAIL_TAGS = Set.of("br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5");

    public static final String ATTRIBUTE_COLSPAN = "colspan";
    public static final String ATTRIBUTE_STYLE = "style";
    public static final String CSS_BACKGROUND_COLOR = "background-color";
    public static final String CSS_FONT_WEIGHT = "font-weight";
    public static final String CSS_PADDING = "padding";
    public static final String CSS_PADDING_TOP = "padding-top";
    public static final String CSS_PADDING_RIGHT = "padding-right";
    public static final String CSS_PADDING_BOTTOM = "padding-bottom";
    public static final String CSS_PADDING_LEFT = "padding-left";
    public static final String CSS_TEXT_ALIGN = "text-align";
    public static final String CSS_TEXT_INDENT = "text-indent";

    public static String style(Element element) {
        for (Attribute attribute : element.attributes()) {
            if (attribute.getKey().equals(ATTRIBUTE_STYLE))
                return attribute.getValue();
        }
        return null;
    }

    public static List<NameValue> splitStyle(String style) {
        List<NameValue> nameValues = new ArrayList<>();
        int i = 0;
        while (i < style.length() && style.charAt(i) == '"')
            ++i;

        int j = style.length() - 1;
        while (j >= 0 && style.charAt(j) == '"')
            --j;

        if (i >= j)
            return null;

        style = style.substring(i, j+1);
        String[] parts = style.split(";");
        for (String part : parts) {
            String[] a = part.split(":");
            if (a.length == 2) {
                nameValues.add(new NameValue(a[0], a[1]));
            }
        }

        return nameValues;
    }

    public static int colspan(Element col) {
        int colspan = 1;
        Attributes attributes = col.attributes();
        for (Attribute attribute : attributes) {
            String key = attribute.getKey();
            if (ATTRIBUTE_COLSPAN.equalsIgnoreCase(key)) {
                try {
                    colspan = Integer.parseInt(attribute.getValue());
                } catch (NumberFormatException e) {
                    log.info("Invalid value for colspan: [{}]", attribute.getValue());
                }
                break;
            }
        }

        return colspan;
    }

    public static Padding getPadding(String style) {
        Float top = null, right = null, bottom = null, left = null;

        List<NameValue> nameValues = splitStyle(style);
        if (Objects.isNull(nameValues))
            return null;

        String paddingTop = getStyleAttribute(nameValues, CSS_PADDING_TOP);
        String paddingRight = getStyleAttribute(nameValues, CSS_PADDING_RIGHT);
        String paddingBottom = getStyleAttribute(nameValues, CSS_PADDING_BOTTOM);
        String paddingLeft = getStyleAttribute(nameValues, CSS_PADDING_LEFT);
        String padding = getStyleAttribute(nameValues, CSS_PADDING);

        if (padding != null) {
            String[] values = padding.split("\\s+");
            switch (values.length) {
                case 1 -> top = right = bottom = left = parseFloat(values[0]);
                case 2 -> {
                    top = bottom = parseFloat(values[0]);
                    right = left = parseFloat(values[1]);
                }
                case 3 -> {
                    top = parseFloat(values[0]);
                    right = left = parseFloat(values[1]);
                    bottom = parseFloat(values[2]);
                }
                case 4 -> {
                    top = parseFloat(values[0]);
                    right = parseFloat(values[1]);
                    bottom = parseFloat(values[2]);
                    left = parseFloat(values[3]);
                }
            }
        }

        if (paddingTop != null) {
            top = parseFloat(paddingTop);
        }

        if (paddingRight != null) {
            right = parseFloat(paddingRight);
        }

        if (paddingBottom != null) {
            bottom = parseFloat(paddingBottom);
        }

        if (paddingLeft != null) {
            left = parseFloat(paddingLeft);
        }

        return new Padding(top, right, bottom, left);
    }

    private static final Pattern FONT_WEIGHT_KEYWORDS = Pattern.compile("^(normal|bold|lighter|bolder|inherit|initial|revert|revert-layer|unset)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FONT_WEIGHT_NUMERIC = Pattern.compile("^\\d+$");

    public static Integer getFontWeight(String style) {
        List<NameValue> nameValues = splitStyle(style);
        if (Objects.isNull(nameValues))
            return null;

        String fontWeight = getStyleAttribute(nameValues, CSS_FONT_WEIGHT);
        if (Objects.isNull(fontWeight))
            return null;

        Matcher matcher = FONT_WEIGHT_NUMERIC.matcher(fontWeight);
        if (matcher.matches()) {
            return Integer.parseInt(fontWeight);
        }

        matcher = FONT_WEIGHT_KEYWORDS.matcher(fontWeight);
        if (matcher.matches()) {
            if (fontWeight.equalsIgnoreCase("bold")) {
                return 700;
            }
        }

        return 400;
    }

    public static Float getTextIndent(String style) {
        List<NameValue> nameValues = splitStyle(style);
        if (Objects.isNull(nameValues))
            return null;

        String textIndent = getStyleAttribute(nameValues, CSS_TEXT_INDENT);
        if (Objects.isNull(textIndent))
            return null;

        return parseFloat(textIndent);
    }

    public static String getTextAlign(String style) {
        List<NameValue> nameValues = splitStyle(style);
        if (Objects.isNull(nameValues))
            return null;

        return getStyleAttribute(nameValues, CSS_TEXT_ALIGN);
    }

    public static String getBackgroundColor(String style) {
        List<NameValue> nameValues = splitStyle(style);
        if (Objects.isNull(nameValues))
            return null;

        return getStyleAttribute(nameValues, CSS_BACKGROUND_COLOR);
    }

    private static final float DEFAULT_FONT_HEIGHT = 11.0f;
    private static final Pattern HTML_UNITS = Pattern.compile("(em|ex|%|px|cm|mm|in|pt|pc|ch|rem|vh|vw|vmin|vmax)");

    private static float parseFloat(String input) {
        String unit = null, value;
        Matcher matcher = HTML_UNITS.matcher(input);
        if (matcher.find()) {
            unit = matcher.group(1);
            value = matcher.replaceAll("");
        } else {
            value = input;
        }
        float val = 0;
        try {
            val = Float.parseFloat(value);
            if (Objects.nonNull(unit)) {
                switch (unit) {
                    case "px" -> {
                        return val;
                    }
                    case "pt" -> {
                        return (val * 4.0f) / 3.0f;
                    }
                    case "in" -> {
                        return (val * 96.0f);
                    }
                    case "em" -> {
                        return (val * DEFAULT_FONT_HEIGHT);
                    }
                    default -> {
                        log.info("parseFloat - unhandled unit [{}]. Value: [{}]", unit, input);
                        return 11.0f;
                    }
                }
            }
            return val;
        } catch (NumberFormatException e) {
            log.info("parseFloat - Exception parsing input: [{}]", input);
            return 11.0f;
        }
    }

    private static String getStyleAttribute(List<NameValue> nameValues, String attribute) {
        for (NameValue nameValue : nameValues) {
            if (nameValue.name().equals(attribute))
                return nameValue.value();
        }
        return null;
    }
}
