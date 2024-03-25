package io.datanapis.xbrl.model;

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.XbrlNamespaces;
import io.datanapis.xbrl.utils.Utils;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Reference {
    private static final Logger log = LoggerFactory.getLogger(Reference.class);

    public static final String TIN_IMPLEMENTATION_NOTE = "http://fasb.org/us-gaap/role/tin/taxonomyImplementationNote";
    public static final String TIN_RESOURCE_URI = "http://fasb.org/us-gaap/role/tin/resource";
    public static final String TIN_TRANSITION_URI = "http://fasb.org/us-gaap/role/tin/transition";
    public static final String TIN_USAGE_URI = "http://fasb.org/us-gaap/role/tin/usage";
    public static final String TIN_VALUE_URI = "http://fasb.org/us-gaap/role/tin/value";
    public static final String CHANGE_NOTE_URI = "http://fasb.org/srt/role/changeNote/changeNote";
    public static final String DEPRECATION_NOTE_URI = "http://fasb.org/srt/role/deprecationNote/deprecationNote";

    private final String sourceUrl;
    private String label;
    private String role;
    private String type;
    private boolean deprecated;
    private LocalDate deprecatedDate;
    private List<String> replacements;

    public String getSourceUrl() {
        return this.sourceUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getRole() {
        return role;
    }
    public boolean isTaxonomyImplementationNote() {
        if (Objects.isNull(role))
            return false;

        switch (role) {
            case TIN_IMPLEMENTATION_NOTE, TIN_RESOURCE_URI, TIN_TRANSITION_URI, TIN_USAGE_URI, TIN_VALUE_URI -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean isChangeNote() {
        return Objects.nonNull(role) && role.equals(CHANGE_NOTE_URI);
    }

    public boolean isDeprecationNote() {
        return Objects.nonNull(role) && role.equals(DEPRECATION_NOTE_URI);
    }

    public String getType() {
        return type;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public LocalDate getDeprecatedDate() {
        return deprecatedDate;
    }

    public List<String> getReplacements() {
        return replacements;
    }

    public static Reference fromElement(String sourceUrl, Element element) {
        Reference reference = new Reference(sourceUrl);

        reference.label = element.attributeValue(TagNames.LABEL_TAG);
        reference.role = element.attributeValue(TagNames.ROLE_TAG);
        reference.type = element.attributeValue(TagNames.TYPE_TAG);
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.DEPRECATED_DATE_2_TAG -> {
                    try {
                        reference.deprecatedDate = LocalDate.parse(child.getTextTrim());
                    } catch (DateTimeParseException e) {
                        log.info("Exception parsing deprecated data [{}]", child.getText());
                    }
                }
                case TagNames.ELEMENT_DEPRECATED_TAG -> reference.deprecated = Boolean.parseBoolean(child.getTextTrim());
                case TagNames.DEPRECATION_REPLACEMENT_TAG -> reference.replacements = getReplacements(child.getTextTrim());
            }
        }

        return reference;
    }

    @NotNull
    private static List<String> getReplacements(String text) {
        List<String> parts = Utils.splitString(text);
        List<String> replacements = new ArrayList<>();
        for (String part : parts) {
            int i = part.indexOf(':');
            if (i > 0) {
                /* part is already qualified, technically : cannot/should not appear at index 0 */
                replacements.add(part);
            } else {
                /* Explicitly qualify with us-gaap */
                replacements.add(XbrlNamespaces.US_GAAP_PREFIX + ":" + part);
            }
        }
        return replacements;
    }

    private Reference(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
