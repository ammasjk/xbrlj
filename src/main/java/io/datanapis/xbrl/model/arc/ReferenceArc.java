package io.datanapis.xbrl.model.arc;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Reference;
import io.datanapis.xbrl.model.link.ReferenceLink;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.HashSet;
import java.util.Set;

public final class ReferenceArc extends FromArc {
    private static final Set<String> REFERENCE_ARC_ATTRIBUTES = new HashSet<>();
    static {
        REFERENCE_ARC_ATTRIBUTES.addAll(ABSTRACT_ARC_ATTRIBUTES);
        REFERENCE_ARC_ATTRIBUTES.add(TagNames.TO_TAG);
    }

    private Reference to;

    public Reference getTo() {
        return this.to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReferenceArc r = (ReferenceArc)o;

        if (!getFrom().equals(r.getFrom())) return false;
        if (!getTo().equals(r.getTo())) return false;
        if (!getArcrole().equals(r.getArcrole())) return false;
        if (!getType().equals(r.getType())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getFrom().hashCode();
        result = 31 * result + getTo().hashCode();
        result = 31 * result + getArcrole().hashCode();
        result = 31 * result + getType().hashCode();
        return result;
    }

    public static ReferenceArc fromElement(DiscoverableTaxonomySet dts, String sourceUrl, ReferenceLink link, Element element) {
        ReferenceArc arc = new ReferenceArc(sourceUrl);

        arc.readElement(dts, link, element);

        String to = element.attributeValue(TagNames.TO_TAG);
        arc.to = link.getReference(to);
        if (arc.to == null) {
            return null;
        }

        for (Attribute attribute : element.attributes()) {
            if (!REFERENCE_ARC_ATTRIBUTES.contains(attribute.getName())) {
                log.debug("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return arc;
    }

    private ReferenceArc(String sourceUrl) {
        super(sourceUrl);
    }
}
