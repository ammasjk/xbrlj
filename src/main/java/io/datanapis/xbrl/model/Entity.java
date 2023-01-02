/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datanapis.xbrl.model;

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.TagNames;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class Entity {
    private static final String CIK_STRING = "http://www.sec.gov/CIK";
    private static final Logger log = LoggerFactory.getLogger(Entity.class);

    /* For a sample, see Context */
    private final String scheme;
    private final String id;

    /**
     * The dimensions for this context. Dimensions qualify a fact to specific (axis, member) combinations.
     * When facts are displayed in a table, the concepts becomes the rows of the table and the dimensions
     * become the columns of the table. It is important to note, that a table may have columns with sub-columns
     * etc. The best way to think of tables is as pivot tables where information can be summarized along multiple axes.
     * A LinkedHashSet guarantees iteration order in the same order the elements were inserted into the set.
     */
    private final Set<ExplicitMember> explicitMembers = new LinkedHashSet<>();

    /**
     * A typed member is another way to qualify facts. In this case, members may not be QNames and therefore may not
     * be concepts. In most examples in Edgar, member is usually a date. Don't know if this is always the case.
     */
    private final Set<TypedMember> typedMembers = new LinkedHashSet<>();

    private static Concept getConcept(DiscoverableTaxonomySet dts, QName name) {
        Concept concept = dts.getConcept(name);
        return concept;
    }

    public static Entity fromElement(DiscoverableTaxonomySet dts, Element element) {
        Entity entity = null;
        for (Element child : element.elements()) {
            Attribute attribute;
            switch (child.getName()) {
                case TagNames.IDENTIFIER_TAG:
                    attribute = child.attribute(TagNames.SCHEME_TAG);
                    String id = child.getTextTrim();
                    entity = new Entity(attribute.getValue(), id);
                    break;
                case TagNames.SEGMENT_TAG:
                    for (Element segmentChild : child.elements()) {
                        QName dimension;
                        switch (segmentChild.getName()) {
                            case TagNames.EXPLICIT_MEMBER_TAG:
                                attribute = segmentChild.attribute(TagNames.DIMENSION_TAG);
                                dimension = segmentChild.getQName(attribute.getValue());
                                QName member = segmentChild.getQName(segmentChild.getTextTrim());
                                ExplicitMember explicitMember =
                                        new ExplicitMember(getConcept(dts, dimension),
                                                getConcept(dts, member));
                                assert entity != null;
                                entity.addDimension(explicitMember);
                                break;
                            case TagNames.TYPED_MEMBER_TAG:
                                attribute = segmentChild.attribute(TagNames.DIMENSION_TAG);
                                dimension = segmentChild.getQName(attribute.getValue());
                                List<Element> members = segmentChild.elements();
                                if (members.size() != 1) {
                                    throw new RuntimeException("TypedMember has more than 1 child");
                                }
                                Element memberElement = members.get(0);
                                QName domain = segmentChild.getQName(memberElement.getQualifiedName());
                                String value = memberElement.getTextTrim();
                                TypedMember typedMember = new TypedMember(getConcept(dts, dimension), value);
                                assert entity != null;
                                entity.addTypedMember(typedMember);
                                log.debug("TypedMember [{}] / [{}] / [{}]",
                                        dimension.getQualifiedName(), domain.getQualifiedName(), value);
                                break;
                            default:
                                throw new UnsupportedElementException(child.getName(), segmentChild.getName());
                        }
                    }
                    break;
                default:
                    throw new UnsupportedElementException(element.getName(), child.getName());
            }
        }

        return entity;
    }

    private Entity(String scheme, String id) {
        this.scheme = scheme;
        this.id = id;
    }

    public String getScheme() {
        return scheme;
    }

    public String getId() {
        return id;
    }

    public boolean hasDimensions() {
        return explicitMembers.size() > 0;
    }

    public boolean hasTypedMembers() {
        return typedMembers.size() > 0;
    }

    private static final String SRT_PREFIX = "srt";
    private static final String US_GAAP_PREFIX = "us-gaap";

    private static boolean isProductOrService(Concept dimension) {
        return dimension.getNamespace().getPrefix().equals(SRT_PREFIX) &&
                dimension.getName().equals("ProductOrServiceAxis");
    }

    private static boolean isSegment(Concept dimension) {
        return dimension.getNamespace().getPrefix().equals(US_GAAP_PREFIX) &&
                dimension.getName().equals("StatementBusinessSegmentsAxis");
    }

    public Set<ExplicitMember> getDimensions() {
        return explicitMembers;
    }

    public void addDimension(ExplicitMember member) {
        explicitMembers.add(member);
    }

    public Set<TypedMember> getTypedMembers() {
        return typedMembers;
    }

    public void addTypedMember(TypedMember member) {
        typedMembers.add(member);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;

        if (!scheme.equals(entity.scheme)) return false;
        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        int result = scheme.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (scheme != null && !scheme.equals(CIK_STRING)) {
            return "Entity(scheme='" + scheme + '\'' + ", id='" + id + '\'' + ')';
        } else {
            return "Entity(" + id + ')';
        }
    }
}
