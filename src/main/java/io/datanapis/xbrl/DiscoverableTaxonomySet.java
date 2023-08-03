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
package io.datanapis.xbrl;

import com.google.common.collect.ImmutableMap;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.FromToArc;
import io.datanapis.xbrl.model.arc.LabelArc;
import io.datanapis.xbrl.model.link.*;
import org.dom4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscoverableTaxonomySet {
    private static final Set<String> XSD_TAGS = new HashSet<>();
    static {
        XSD_TAGS.add(TagNames.ATTRIBUTE_GROUP_TAG);
        XSD_TAGS.add(TagNames.ATTRIBUTE_TAG);
        XSD_TAGS.add(TagNames.COMPLEX_TYPE_TAG);
        XSD_TAGS.add(TagNames.SIMPLE_TYPE_TAG);
    }
    private static final Logger log = LoggerFactory.getLogger(DiscoverableTaxonomySet.class);

    private final UriRoleTypeMap roleTypes = new UriRoleTypeMap();
    private final UriArcroleTypeMap arcroleTypes = new UriArcroleTypeMap();
    private final NameConceptMap nameConceptMap = new NameConceptMap();
    private final KeyConceptMap keyConceptMap = new KeyConceptMap();

    public void clear() {
        roleTypes.clear();
        arcroleTypes.clear();

        nameConceptMap.forEach((k, v) -> v.clear());
        nameConceptMap.clear();

        keyConceptMap.forEach((k, v) -> v.clear());
        keyConceptMap.clear();
    }

    public RoleType getRoleType(String roleURI) {
        return roleTypes.computeIfAbsent(roleURI, RoleType::createDynamic);
    }

    public Collection<RoleType> getReportableRoleTypes() {
        List<RoleType> roles = new ArrayList<>();
        for (RoleType roleType : roleTypes.values()) {
            if (roleType.isReportable())
                roles.add(roleType);
        }

        return roles;
    }

    public Collection<RoleType> getAllRoleTypes() {
        return roleTypes.values();
    }

    public ArcroleType getArcRoleType(String arcroleURI) {
        return arcroleTypes.computeIfAbsent(arcroleURI, ArcroleType::createDynamic);
    }

    public Collection<ArcroleType> getAllArcRoleTypes() {
        return arcroleTypes.values();
    }

    public Concept getConcept(Namespace namespace, String name) {
        QName qName = new QName(name, namespace);
        return nameConceptMap.getOrDefault(qName, null);
    }

    public Concept getConcept(QName qName) {
        return nameConceptMap.getOrDefault(qName, null);
    }

    public Concept getConcept(String href) {
        return keyConceptMap.getOrDefault(href, null);
    }

    public Collection<Concept> getMatchingConcepts(Pattern pattern) {
        Collection<Concept> matchingConcepts = new ArrayList<>();
        for (Concept concept : nameConceptMap.values()) {
            Matcher matcher = pattern.matcher(concept.getName());
            if (matcher.matches()) {
                matchingConcepts.add(concept);
            }
        }

        return matchingConcepts;
    }

    private void putConcept(Concept concept) {
        QName qName = new QName(concept.getName(), concept.getNamespace());
        nameConceptMap.put(qName, concept);
        keyConceptMap.put(concept.getKey(), concept);
    }

    public Collection<Concept> getAllConcepts() {
        return nameConceptMap.values();
    }

    public static class Statistics {
        public final int nOfRoleTypes;
        public final int nOfReportableRoleTypes;
        public final int nOfArcroleTypes;
        public final int nOfConcepts;
        public final Map<String,Integer> uriConceptCount;

        private Statistics(DiscoverableTaxonomySet taxonomy) {
            this.nOfRoleTypes = taxonomy.roleTypes.size();
            this.nOfArcroleTypes = taxonomy.arcroleTypes.size();
            this.nOfConcepts = taxonomy.keyConceptMap.size();

            Collection<RoleType> reportableRoleTypes = taxonomy.getReportableRoleTypes();
            this.nOfReportableRoleTypes = reportableRoleTypes.size();

            Counter<String> namespaceCount = new Counter<>();
            for (Map.Entry<QName,Concept> entry : taxonomy.nameConceptMap.entrySet()) {
                namespaceCount.add(entry.getKey().getNamespaceURI());
            }
            this.uriConceptCount = new ImmutableMap.Builder<String,Integer>().putAll(namespaceCount.map()).build();
        }
    }

    public Statistics getStatistics() {
        return new Statistics(this);
    }

    public void logStats() {
        log.info("DiscoverableTaxonomySet Statistics");
        Statistics statistics = getStatistics();

        log.info("Found [{}] roleTypes", statistics.nOfRoleTypes);
        log.info("Found [{}] arcroleTypes", statistics.nOfArcroleTypes);
        log.info("Found [{}] reportable roleTypes", statistics.nOfReportableRoleTypes);
        log.info("Found [{}] concepts", statistics.nOfConcepts);

        log.info("Detailed concept statistics:");
        for (Map.Entry<String,Integer> entry : statistics.uriConceptCount.entrySet()) {
            log.info("Namespace [{}] = [{}]", entry.getKey(), entry.getValue());
        }
    }

    static DiscoverableTaxonomySet fromElement(XbrlReader.Resolver resolver, Element element) {
        DiscoverableTaxonomySet dts = new DiscoverableTaxonomySet();
        dts.read(resolver, element);
        return dts;
    }

    static DiscoverableTaxonomySet fromPath(XbrlReader.Resolver resolver, String path) {
        DiscoverableTaxonomySet dts = new DiscoverableTaxonomySet();
        dts.read(resolver, path);
        return dts;
    }

    private static class SchemaLocation {
        String absolutePath;

        private SchemaLocation(String absolutePath) {
            this.absolutePath = absolutePath;
        }

        private String getFile() {
            int index = absolutePath.lastIndexOf('/');
            if (index > 0) {
                return absolutePath.substring(index + 1);
            } else {
                return absolutePath;
            }
        }
    }

    private static final String ECD_SUB_2023_XSD = "ecd-sub-2023.xsd";
    private static final String ECD_2023_XSD_URL = "https://xbrl.sec.gov/ecd/2023/ecd-2023.xsd";

    /**
     * Collect all the urls that will need to be traversed in the order in which they need to be traversed.
     *
     * @param resolver A mechanism to resolve relative urls to the right absolute urls
     * @param fromPath The path of the document that needs to be walked
     * @return A collection of urls in the order in which the urls need to be traversed
     */
    private Collection<SchemaLocation> collect(XbrlReader.Resolver resolver, String fromPath) {
        Deque<SchemaLocation> todo = new ArrayDeque<>();
        Collection<SchemaLocation> collection = new ArrayList<>();

        todo.push(new SchemaLocation(resolver.getAbsolutePath(fromPath)));
        final Set<String> visited = new HashSet<>();

        while (!todo.isEmpty()) {
            SchemaLocation url = todo.pop();
            if (visited.contains(url.getFile()))
                continue;

            // Order of URLs is important in visited. Hence, visited is a Collection
            visited.add(url.getFile());
            collection.add(url);

            try {
                Element linkedElement = resolver.getRootElement(url.absolutePath);
                for (Element child : linkedElement.elements()) {
                    String childName = child.getName();
                    if (childName.equals(TagNames.IMPORT_TAG)) {
                        //
                        // Handle any schema imports
                        //
                        String schemaLocation = schemaLocationToUrl(child.attributeValue(TagNames.SCHEMA_LOCATION_TAG));
                        if (schemaLocation.contains(ECD_SUB_2023_XSD)) {
                            log.debug("Adding [{}] to queue", ECD_2023_XSD_URL);
                            todo.add(new SchemaLocation(resolver.getAbsolutePath(url.absolutePath, ECD_2023_XSD_URL)));
                        }
                        log.debug("Adding [{}] to queue", schemaLocation);
                        todo.add(new SchemaLocation(resolver.getAbsolutePath(url.absolutePath, schemaLocation)));
                    } else if (childName.equals(TagNames.ANNOTATION_TAG)) {
                        //
                        // Handle LinkBase references inside the appinfo elements
                        //
                        Element appinfo = child.element(TagNames.APPINFO_TAG);
                        if (appinfo != null) {
                            for (Element aiElement : appinfo.elements()) {
                                String aiElementName = aiElement.getName();
                                if (TagNames.LINKBASE_REF_TAG.equals(aiElementName)) {
                                    String arcrole = aiElement.attributeValue(TagNames.ARCROLE_TAG);
                                    String linkUrl = aiElement.attributeValue(TagNames.HREF_TAG);
                                    log.debug("Adding [{}, {}] to queue", linkUrl, arcrole);
                                    todo.add(new SchemaLocation(resolver.getAbsolutePath(url.absolutePath, linkUrl)));
                                }
                            }
                        }
                    } else {
                        //
                        // Handle any location tags inside link elements
                        //
                        switch (childName) {
                            case TagNames.DEFINITION_LINK_TAG:
                            case TagNames.PRESENTATION_LINK_TAG:
                            case TagNames.CALCULATION_LINK_TAG:
                            case TagNames.LABEL_LINK_TAG:
                                for (Element linkChild : child.elements()) {
                                    if (!linkChild.getName().equals(TagNames.LOC_TAG)) {
                                        continue;
                                    }

                                    String linkUrl = linkChild.attributeValue(TagNames.HREF_TAG);
                                    int index = linkUrl.indexOf('#');
                                    if (index < 0) {
                                        continue;
                                    }

                                    linkUrl = linkUrl.substring(0, index);
                                    todo.add(new SchemaLocation(resolver.getAbsolutePath(url.absolutePath, linkUrl)));
                                }
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return collection;
    }

    /**
     * Returns true if url matches the rootHref defined in the primary xsd. Used to identify all roleTypes
     * that should be reported.
     *
     * @param url The URL that is being processed
     * @param rootHref The URL of the root XSD
     * @return True if the last component of URL matches the last component of the primary XSD. False otherwise.
     */
    private boolean isReportable(String url, String rootHref) {
        String nameInUrl, schemaName;

        int index = url.lastIndexOf('/');
        if (index > 0) {
            nameInUrl = url.substring(index + 1);
        } else {
            nameInUrl = url;
        }

        index = rootHref.lastIndexOf('/');
        if (index > 0) {
            schemaName = rootHref.substring(index);
        } else {
            schemaName = rootHref;
        }

        return nameInUrl.equals(schemaName);
    }

    private class LinkedTaxonomyProcessor {
        private final List<LabelLink> labelLinks;
        private final SchemaLocation url;
        private final Element linkedElement;
        private final String targetNamespace;
        private final boolean reportable;
        private final List<Element> linkBaseRoots = new ArrayList<>();

        private LinkedTaxonomyProcessor(String rootSchema, List<LabelLink> labelLinks, SchemaLocation url, Element linkedElement) {
            this.labelLinks = labelLinks;
            this.url = url;
            this.linkedElement = linkedElement;
            this.targetNamespace = linkedElement.attributeValue(TagNames.TARGET_NAMESPACE_TAG);
            this.reportable = isReportable(url.absolutePath, rootSchema);
        }

        private void processAnnotationTag(Element child) {
            Element appinfo = child.element(TagNames.APPINFO_TAG);
            if (appinfo != null) {
                for (Element aiElement : appinfo.elements()) {
                    String aiElementName = aiElement.getName();
                    switch (aiElementName) {
                        case TagNames.DOCUMENTATION_TAG:
                            break;
                        case TagNames.LINKBASE_REF_TAG:
                            /* Ignore! This was already handled in collect() */
                            break;
                        case TagNames.LINKBASE_TAG:
                            /* Just remember the link base elements, we'll process them at the end */
                            linkBaseRoots.add(aiElement);
                            break;
                        case TagNames.ROLE_TYPE_TAG:
                            /* Role definition */
                            RoleType roleType = RoleType.fromElement(url.absolutePath, aiElement, reportable);
                            roleTypes.putIfAbsent(roleType.getRoleURI(), roleType);
                            log.debug("Adding role [{}]", roleType.getRoleURI());
                            break;
                        case TagNames.ARCROLE_TYPE_TAG:
                            /* Arcrole definition */
                            ArcroleType arcroleType = ArcroleType.fromElement(url.absolutePath, aiElement);
                            arcroleTypes.putIfAbsent(arcroleType.getArcroleURI(), arcroleType);
                            log.debug("Adding arcrole [{}]", arcroleType.getArcroleURI());
                            break;
                        default:
                            log.info("Ignoring child [{}] of [{}]", aiElementName, appinfo.getQualifiedName());
                            break;
                    }
                }
            }
        }

        private void processElement(Element child) {
            String childName = child.getName();
            if (childName.equals(TagNames.ELEMENT_TAG)) {
                Concept concept = Concept.fromElement(url.absolutePath, targetNamespace, child);
                if (concept != null) {
                    putConcept(concept);
                }
            } else if (XSD_TAGS.contains(childName)) {
                // Skip the XSD tags. We are not interested in parsing XML schema definitions. Just the XBRL elements.
            } else if (childName.equals(TagNames.ROLE_REF_TAG)) {
                /* Role reference */
                String roleURI = child.attributeValue(TagNames.ROLE_URI_TAG);
                RoleType roleType = getRoleType(roleURI);
                if (roleType == null) {
                    log.info("Missing roleType [{}]", roleURI);
                }
            } else if (childName.equals(TagNames.ARCROLE_REF_TAG)) {
                /* Arcrole reference */
                String arcroleURI = child.attributeValue(TagNames.ARCROLE_URI_TAG);
                ArcroleType arcroleType = getArcRoleType(arcroleURI);
                if (arcroleType == null) {
                    log.info("Missing arcroleType [{}]", arcroleURI);
                }
            } else if (childName.equals(TagNames.DEFINITION_LINK_TAG)) {
                DefinitionLink.fromElement(url.absolutePath, DiscoverableTaxonomySet.this, child);
            } else if (childName.equals(TagNames.PRESENTATION_LINK_TAG)) {
                PresentationLink.fromElement(url.absolutePath, DiscoverableTaxonomySet.this, child);
            } else if (childName.equals(TagNames.CALCULATION_LINK_TAG)) {
                CalculationLink.fromElement(url.absolutePath, DiscoverableTaxonomySet.this, child);
            } else if (childName.equals(TagNames.LABEL_LINK_TAG)) {
                LabelLink link = LabelLink.fromElement(url.absolutePath, DiscoverableTaxonomySet.this, child);
                labelLinks.add(link);
            } else {
                log.info("Ignoring child [{}] of [{}]", childName, linkedElement.getQualifiedName());
            }
        }

        private void processLinkBases() {
            /* Process all the link bases in this document */
            for (Element linkBaseRoot : linkBaseRoots) {
                /* TODO - Do we need to check attributes of the linkbase element? */
                for (Element linkBaseElement : linkBaseRoot.elements()) {
                    processElement(linkBaseElement);
                }
            }
        }

        private void ingest() {
            for (Element child : linkedElement.elements()) {
                String childName = child.getName();
                if (childName.equals(TagNames.ANNOTATION_TAG)) {
                    this.processAnnotationTag(child);
                } else if (!childName.equals(TagNames.IMPORT_TAG)) {
                    /* import tags would've been handled in DiscoverableTaxonomySet::collect */
                    this.processElement(child);
                }
            }

            /* Process any link bases in this document */
            this.processLinkBases();
        }
    }

    /**
     * Recursively read the elements rooted at element
     *
     * @param resolver A mechanism to resolve relative urls to the right absolute urls
     * @param element The root element of the document that needs to be walked
     */
    private void read(XbrlReader.Resolver resolver, Element element) {
        String elementName = element.getName();
        assert elementName.equals(TagNames.SCHEMA_REF_TAG);
        String rootSchema = element.attributeValue(TagNames.HREF_TAG);
        read(resolver, rootSchema);
    }

    /**
     * Recursively read the elements starting from rootSchema
     *
     * @param resolver A mechanism to resolve relative urls to the right absolute urls
     * @param rootSchema path to the root document from where the walk begins
     */
    private void read(XbrlReader.Resolver resolver, String rootSchema) {
        final List<LabelLink> labelLinks = new ArrayList<>();
        final Collection<SchemaLocation> todo = collect(resolver, rootSchema);

        for (SchemaLocation url : todo) {
            log.debug("Working on [{}]", url.absolutePath);

            try {
                Element linkedElement = resolver.getRootElement(url.absolutePath);
                LinkedTaxonomyProcessor linkedTaxonomyProcessor =
                        new LinkedTaxonomyProcessor(rootSchema, labelLinks, url, linkedElement);
                linkedTaxonomyProcessor.ingest();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        this.connectConceptsToLabels(labelLinks);
        this.connectArcs();
    }

    private void connectConceptsToLabels(List<LabelLink> labelLinks) {
        for (LabelLink link : labelLinks) {
            for (LabelArc arc : link.getAllArcs()) {
                Location location = arc.getFrom();
                Concept concept = keyConceptMap.get(location.getHref());
                RoleLabelMap label = arc.getTo();
                concept.addLabels(label);
            }
        }
    }

    private static <ArcType extends FromToArc<ArcType>>
    void connect(DirectedAcyclicLink<ArcType> link) {
        Map<Location,Collection<ArcType>> arcMap = new HashMap<>();
        for (ArcType arc: link.getAllArcs()) {
            arcMap.computeIfAbsent(arc.getTo(), k -> new ArrayList<>());
            arcMap.get(arc.getTo()).add(arc);
        }
        for (ArcType arc : link.getAllArcs()) {
            Location from = arc.getFrom();
            Collection<ArcType> arcs = arcMap.get(from);
            if (arcs != null) {
                for (ArcType parent : arcs) {
                    parent.addChild(arc);
                }
            }
        }
    }

    private void connectArcs() {
        Collection<RoleType> roles = this.getReportableRoleTypes();

        for (RoleType roleType : roles) {
            PresentationLink presentationLink = roleType.getPresentationLink();
            if (presentationLink != null) {
                connect(presentationLink);
            }

            DefinitionLink definitionLink = roleType.getDefinitionLink();
            if (definitionLink != null) {
                connect(definitionLink);
            }

            CalculationLink calculationLink = roleType.getCalculationLink();
            if (calculationLink != null) {
                connect(calculationLink);
            }
        }
    }

    private String schemaLocationToUrl(String schemaLocation) {
        int index = toString().indexOf(' ');
        if (index == -1) {
            return schemaLocation;
        } else {
            return schemaLocation.substring(index + 1);
        }
    }

    private DiscoverableTaxonomySet() {
        arcroleTypes.put(ArcroleType.ALL.getArcroleURI(), ArcroleType.ALL);
        arcroleTypes.put(ArcroleType.NOT_ALL.getArcroleURI(), ArcroleType.NOT_ALL);
        arcroleTypes.put(ArcroleType.CONCEPT_LABEL.getArcroleURI(), ArcroleType.CONCEPT_LABEL);
        arcroleTypes.put(ArcroleType.DIMENSION_DEFAULT.getArcroleURI(), ArcroleType.DIMENSION_DEFAULT);
        arcroleTypes.put(ArcroleType.DIMENSION_DOMAIN.getArcroleURI(), ArcroleType.DIMENSION_DOMAIN);
        arcroleTypes.put(ArcroleType.DOMAIN_MEMBER.getArcroleURI(), ArcroleType.DOMAIN_MEMBER);
        arcroleTypes.put(ArcroleType.FACT_FOOTNOTE.getArcroleURI(), ArcroleType.FACT_FOOTNOTE);
        arcroleTypes.put(ArcroleType.HYPERCUBE_DIMENSION.getArcroleURI(), ArcroleType.HYPERCUBE_DIMENSION);
        arcroleTypes.put(ArcroleType.PARENT_CHILD.getArcroleURI(), ArcroleType.PARENT_CHILD);
        arcroleTypes.put(ArcroleType.SUMMATION_ITEM.getArcroleURI(), ArcroleType.SUMMATION_ITEM);
        arcroleTypes.put(ArcroleType.GENERAL_SPECIAL.getArcroleURI(), ArcroleType.GENERAL_SPECIAL);
        arcroleTypes.put(ArcroleType.ESSENCE_ALIAS.getArcroleURI(), ArcroleType.ESSENCE_ALIAS);
        arcroleTypes.put(ArcroleType.SIMILAR_TUPLES.getArcroleURI(), ArcroleType.SIMILAR_TUPLES);
        arcroleTypes.put(ArcroleType.REQUIRES_ELEMENT.getArcroleURI(), ArcroleType.REQUIRES_ELEMENT);
        arcroleTypes.put(ArcroleType.DEPRECATED_PART_CONCEPT.getArcroleURI(), ArcroleType.DEPRECATED_PART_CONCEPT);
        arcroleTypes.put(ArcroleType.DEPRECATED_CONCEPT.getArcroleURI(), ArcroleType.DEPRECATED_CONCEPT);
        arcroleTypes.put(ArcroleType.DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT.getArcroleURI(), ArcroleType.DIMENSIONALLY_QUALIFIED_DEPRECATED_CONCEPT);
        arcroleTypes.put(ArcroleType.MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT.getArcroleURI(), ArcroleType.MUTUALLY_EXCLUSIVE_DEPRECATED_CONCEPT);
        arcroleTypes.put(ArcroleType.DEPRECATED_AGGREGATE_CONCEPT.getArcroleURI(), ArcroleType.DEPRECATED_AGGREGATE_CONCEPT);
        arcroleTypes.put(ArcroleType.EXPLANATORY_FACT.getArcroleURI(), ArcroleType.EXPLANATORY_FACT);

        roleTypes.put(RoleType.DEPRECATED.getRoleURI(), RoleType.DEPRECATED);
        roleTypes.put(RoleType.COMMON_PRACTICE_REF.getRoleURI(), RoleType.COMMON_PRACTICE_REF);
        roleTypes.put(RoleType.NON_AUTHORITATIVE_LITERATURE_REF.getRoleURI(), RoleType.NON_AUTHORITATIVE_LITERATURE_REF);
        roleTypes.put(RoleType.RECOGNITION_REF.getRoleURI(), RoleType.RECOGNITION_REF);
    }

    public static final String US_GAAP_2012 = "us-gaap-2012-01-31.zip";
    public static final String US_GAAP_2013 = "us-gaap-2013-01-31.zip";
    public static final String US_GAAP_2014 = "us-gaap-2014-01-31.zip";
    public static final String US_GAAP_2015 = "us-gaap-2015-01-31.zip";
    public static final String US_GAAP_2016 = "us-gaap-2016-01-31.zip";
    public static final String US_GAAP_2017 = "us-gaap-2017-01-31.zip";
    public static final String US_GAAP_2018 = "us-gaap-2018-01-31.zip";
    public static final String US_GAAP_2019 = "us-gaap-2019-01-31.zip";
    public static final String US_GAAP_2020 = "us-gaap-2020-01-31.zip";
    public static final String US_GAAP_2021 = "us-gaap-2021-01-31.zip";
    public static final String US_GAAP_2022 = "us-gaap-2022.zip";

    public static final String[] TAXONOMIES = {
            US_GAAP_2012,
            US_GAAP_2013,
            US_GAAP_2014,
            US_GAAP_2015,
            US_GAAP_2016,
            US_GAAP_2017,
            US_GAAP_2018,
            US_GAAP_2019,
            US_GAAP_2020,
            US_GAAP_2021,
            US_GAAP_2022,
    };
}
