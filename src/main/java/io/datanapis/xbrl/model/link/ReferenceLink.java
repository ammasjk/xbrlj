package io.datanapis.xbrl.model.link;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Location;
import io.datanapis.xbrl.model.Reference;
import io.datanapis.xbrl.model.arc.ReferenceArc;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.*;

public final class ReferenceLink extends BipartiteLink<ReferenceArc> {
    private static final Set<String> REFERENCE_LINK_ATTRIBUTES = new HashSet<>();
    static {
        REFERENCE_LINK_ATTRIBUTES.addAll(ABSTRACT_LINK_ATTRIBUTES);
    }

    private final Map<String, Reference> references = new HashMap<>();

    public Reference getReference(String label) {
        return references.getOrDefault(label, null);
    }

    public static ReferenceLink fromElement(String sourceUrl, DiscoverableTaxonomySet dts, Element element) {
        ReferenceLink link = new ReferenceLink();

        link.readElement(dts, element);

        /* Process all locations first */
        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                    Location location = Location.fromElement(sourceUrl, child);
                    link.locations.put(location.getLabel(), location);
                    break;
                case TagNames.REFERENCE_TAG:
                    Reference reference = Reference.fromElement(sourceUrl, child);
                    if (reference.isDeprecated() || Objects.nonNull(reference.getDeprecatedDate()) || Objects.nonNull(reference.getReplacements()) && !reference.getReplacements().isEmpty())
                        link.references.put(reference.getLabel(), reference);
                    break;
            }
        }

        for (Element child : element.elements()) {
            String childName = child.getName();
            switch (childName) {
                case TagNames.LOC_TAG:
                case TagNames.REFERENCE_TAG:
                    break;
                case TagNames.REFERENCE_ARC_TAG:
                    ReferenceArc arc = ReferenceArc.fromElement(dts, sourceUrl, link, child);
                    if (arc != null)
                        link.addArc(arc.getFrom(), arc);
                    break;
                default:
                    log.info("Ignoring element [{}] in [{}}]", childName, element.getQualifiedName());
                    break;
            }
        }

        for (Attribute attribute : element.attributes()) {
            if (!REFERENCE_LINK_ATTRIBUTES.contains(attribute.getName())) {
                log.info("Ignoring attribute [{}] in [{}]", attribute.getName(), element.getQualifiedName());
            }
        }

        return link;
    }

    private ReferenceLink() {
    }
}
