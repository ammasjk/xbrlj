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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.model.arc.FootnoteArc;
import io.datanapis.xbrl.model.link.FootnoteLink;
import io.datanapis.xbrl.utils.Utils;
import io.datanapis.xbrl.utils.XbrlUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XbrlInstance {
    private static final Logger log = LoggerFactory.getLogger(XbrlInstance.class);

    static final String XBRL_TAG = "xbrl";
    static final String HTML_TAG = "html";
    static final String BODY_TAG = "body";

    private final String xbrlUrl;
    private DiscoverableTaxonomySet dts;
    private final ContextIdMap contextMap = new ContextIdMap();
    private final UnitIdMap unitMap = new UnitIdMap();
    private final Counter<Namespace> factNamespaces = new Counter<>();
    private final Dei dei;
    private final FactIdMap facts = new FactIdMap();
    private final ConceptFactMap conceptFacts = new ConceptFactMap();
    private Unit defaultCurrency = Unit.USD;

    private XbrlInstance(LocalDate dateFiled, String xbrlUrl) {
        dei = new Dei(dateFiled);
        this.xbrlUrl = xbrlUrl;
    }

    public void clear() {
        dts.clear();

        contextMap.forEach((k, v) -> v.clear());
        contextMap.clear();

        unitMap.clear();
        factNamespaces.clear();
        dei.clear();
        facts.clear();
        conceptFacts.clear();
    }

    static XbrlInstance fromXbrlElement(LocalDate dateFiled, XbrlReader.Resolver resolver, Element root) {
        XbrlInstance instance = new XbrlInstance(dateFiled, resolver.getRootPath());
        instance.parseXbrl(resolver, root);
        return instance;
    }

    static XbrlInstance fromiXBRLElement(LocalDate dateFiled, XbrlReader.Resolver resolver, List<Element> roots) {
        XbrlInstance instance = new XbrlInstance(dateFiled, resolver.getRootPath());
        instance.parseInlineXBRL(resolver, roots);
        return instance;
    }

    public String getXbrlUrl() {
        return this.xbrlUrl;
    }

    public DiscoverableTaxonomySet getTaxonomy() {
        return dts;
    }

    public Concept getConcept(QName qName) {
        return dts.getConcept(qName);
    }

    public Concept getConcept(Namespace namespace, String name) {
        return dts.getConcept(namespace, name);
    }

    public Context getContext(String id) {
        return contextMap.getOrDefault(id, null);
    }

    public Collection<Context> getAllContexts() {
        return contextMap.values();
    }

    public Unit getUnit(String id) {
        return unitMap.getOrDefault(id, null);
    }

    public Collection<Unit> getAllUnits() {
        return unitMap.values();
    }

    public Dei getDei() {
        return dei;
    }

    public LocalDate getPeriodEndDate() {
        return dei.getEstimatedPeriodEndDate();
    }

    public String getFiscalPeriod() {
        return dei.getFiscalPeriod();
    }

    public Collection<Fact> getAllFacts() {
        return facts.values();
    }

    public int nOfFacts() {
        return facts.size();
    }

    public Unit getDefaultCurrency() {
        return defaultCurrency;
    }
    public void setDefaultCurrency(Unit currency) {
        defaultCurrency = currency;
    }

    public String getInstancePrefix() {
        Collection<Namespace> namespaces = factNamespaces.map().keySet();
        String prefix = null;
        for (Namespace namespace : namespaces) {
            String uri = namespace.getURI();
            if (uri.contains("sec.gov") || uri.contains("fasb.org") || uri.contains("xbrl.org"))
                continue;

            if (Objects.isNull(prefix)) {
                prefix = namespace.getPrefix();
            } else {
                log.info("Prefix has already been set to [{}]. Ignoring alternate prefix [{}]", prefix, namespace.getPrefix());
            }
        }

        return prefix;
    }

    public Collection<Concept> getScenarioConcepts() {
        Set<Concept> concepts = new HashSet<>();
        for (Context context : contextMap.values()) {
            Set<ExplicitMember> dimensions = context.getDimensions();
            for (ExplicitMember member : dimensions) {
                concepts.add(member.getDimension());
                concepts.add(member.getMember());
            }

            Collection<TypedMember> typedMembers = context.getTypedMembers();
            for (TypedMember member : typedMembers) {
                Concept concept = member.getDimension();
                assert concept != null;
                concepts.add(concept);
            }
        }

        return concepts;
    }

    public Collection<Fact> getMatchingFacts(Pattern pattern, Predicate<Fact> filter) {
        Collection<Fact> matchingFacts = new ArrayList<>();
        for (Fact fact : facts.values()) {
            Matcher matcher = pattern.matcher(fact.getConcept().getName());
            if (matcher.matches() && filter.test(fact)) {
                matchingFacts.add(fact);
            }
        }

        return matchingFacts;
    }

    /**
     * Get all facts for a given concept across all periods. Only facts that don't have any dimensions
     * (i.e. no explicit members) are returned.
     *
     * @param matchingFacts the TimeOrdered<> collector into which matching facts are added
     * @param concept the concept to be matched
     */
    private void getMatchingFacts(Concept concept, TimeOrdered<DimensionedFact> matchingFacts) {
        Collection<Fact> factCollection = conceptFacts.getFactsFor(concept);
        if (factCollection == null)
            return;

        for (Fact fact : factCollection) {
            assert fact.getConcept().equals(concept);

            Context context = fact.getContext();
            if (!context.hasDimensions()) {
                matchingFacts.add(context.getPeriod(), new DimensionedFact(fact));
            }
        }
    }

    /**
     * Need to do two things - check if each member of dimensions occurs in at least one Axis and
     * reorder dimensions in the order of the matching axis.
     * Note: This is not complete. The list returned is missing presentation attributes such as
     * labels that are important to display things correctly. To achieve this, we have to create
     * a new structure (aka PresentationMember that has the dimension and members but also the labels
     * for each). This isn't a big change but needs some work.
     *
     * @param axes the axes to compare with
     * @param context the context to be matched
     * @return null if there is no match. This means one of the elements in dimension does not appear
     * in any of the axes. If there is a match, a list of ExplicitMembers is returned. The list is
     * ordered by presentation.
     */
    private static List<ExplicitMember> isMatchingContext(Collection<Axis> axes, Context context) {
        /*
         * Rules for filtering out contexts from Edgar Volume 2, Chapter 6 - Section 6.24.2 Context Selection
         *
         * 1. If a context segment has an axis with a member for which the axis does not have that member as a descendant in the
         *    presentation group does not appear in the base set, then the context will not be selected (it will be filtered out).
         * 2. If the presentation contains an axis that does not have its default member as a descendant, and the context segment
         *    has no member for that axis, then the context will not be selected.
         */
        Set<ExplicitMember> dimensions = context.getDimensions();

        /* Check if context matches the axes definition i.e., context dimensions are a subset of axes dimensions */
        Set<Concept> contextAxes = dimensions.stream().map(ExplicitMember::getDimension).collect(Collectors.toSet());
        Set<Concept> networkAxes = axes.stream().map(Axis::getDimension).collect(Collectors.toSet());
        contextAxes.removeAll(networkAxes);

        /* Check if Context has more qualifiers than required by the network. This partly addresses rule 1 */
        if (!contextAxes.isEmpty())
            return null;

        List<ExplicitMember> result = new ArrayList<>();
        for (Axis axis : axes) {
            ExplicitMember contextMember = null;
            for (ExplicitMember member : dimensions) {
                if (member.getDimension().equals(axis.getDimension())) {
                    contextMember = member;
                    break;
                }
            }

            if (contextMember != null) {
                /* Validate rule 2. */
                if (!axis.hasDefaultDomain()) {
                    result.add(contextMember);
                    continue;
                } else if (axis.hasMember(contextMember.getMember())) {
                    result.add(contextMember);
                    continue;
                } else if (axis.hasMemberAsDomain(contextMember.getMember())) {
                    /* Some instances, incorrectly classify a member as a domain, e.g. American Electric Power's subsidiary, AEP Texas Inc, 2020 Q3 */
                    result.add(contextMember);
                    continue;
                }
                /* Rule 2 does not hold */
                return null;
            }
        }

        return result;
    }

    /**
     * Collect facts belonging to concept and return them in matchingFacts. If axes is not null and has a size more
     * than 1, ensure that any fact that is qualified matches a subset of axes. Axes is typically provided by a
     * network such as definition network or a presentation network. Facts get qualified by virtue of their
     * association with a context.
     *
     * @param concept only return facts belonging to concept
     * @param axes the hypercube axes to match
     * @param matchingFacts data structure where the matching facts are collected
     */
    public void getFactsFor(Concept concept, Collection<Axis> axes, TimeOrdered<DimensionedFact> matchingFacts) {
        if (axes == null || axes.isEmpty()) {
            /* We are looking for simple facts */
            getMatchingFacts(concept, matchingFacts);
            return;
        }

        Collection<Fact> factCollection = conceptFacts.getFactsFor(concept);
        if (factCollection == null)
            return;

        for (Fact fact : factCollection) {
            assert fact.getConcept().equals(concept);

            Context context = fact.getContext();

            // If this fact has no dimensions, this is a top-level fact
            if (!context.hasDimensions()) {
                matchingFacts.add(context.getPeriod(), new DimensionedFact(fact));
            } else {
                /* context has dimensions, but make sure the dimensions match axes */
                List<ExplicitMember> dimensions = isMatchingContext(axes, context);
                if (dimensions != null) {
                    matchingFacts.add(context.getPeriod(), new DimensionedFact(fact, dimensions));
                }
            }
        }
    }

    public Collection<Period> getDistinctPeriods() {
        // Separate instant and duration periods
        Set<Instant> instants = new HashSet<>();
        Set<Duration> durations = new HashSet<>();
        for (Context context : contextMap.values()) {
            Period period = context.getPeriod();
            if (period instanceof Instant) {
                instants.add((Instant)period);
            } else if (period instanceof Duration) {
                durations.add((Duration)period);
            }
        }

        // Sort the instant and duration periods by date
        List<Duration> durationList = new ArrayList<>(durations);
        List<Instant> instantList = new ArrayList<>(instants);
        durationList.sort(Duration::compareTo);
        instantList.sort(Comparator.comparing(Instant::getDate));

        // Merge the instant and duration periods by
        Iterator<Instant> first = instantList.iterator();
        Iterator<Duration> second = durationList.iterator();
        Instant i = first.hasNext() ? first.next() : null;
        Duration d = second.hasNext() ? second.next() : null;

        List<Period> result = new ArrayList<>();
        while (i != null && d != null) {
            if (i.getDate().isBefore(d.getStartDate()) || i.getDate().isEqual(d.getStartDate())) {
                result.add(i);
                i = first.hasNext() ? first.next() : null;
            } else if (i.getDate().isAfter(d.getEndDate())) {
                result.add(d);
                d = second.hasNext() ? second.next() : null;
            } else {
                result.add(i);
                i = first.hasNext() ? first.next() : null;
            }
        }
        while (i != null) {
            result.add(i);
            i = first.hasNext() ? first.next() : null;
        }
        while (d != null) {
            result.add(d);
            d = second.hasNext() ? second.next() : null;
        }

        return result;
    }

    private final int DAYS_ALLOWANCE = 10;

    /**
     * Get all contexts that belong to the Most Recent Quarter (MRQ). An MRQ is defined as the
     * roughly 3-month period that ends at the period end date. The period end date is defined
     * by the Document and Entity Information (DEI) section of an XBRL instance document. Any
     * date that falls outside the period end date by up to 3 days is considered part of the
     * MRQ. I believe this is defined by SEC Edgar, but I am not sure.
     *
     * @return Collection of contexts that belong to the Most Recent Quarter.
     */
    public Collection<Context> getMRQContexts() {
        LocalDate periodEndDate = dei.getEstimatedPeriodEndDate();
        List<Context> contextList = new ArrayList<>();
        for (Context context : contextMap.values()) {
            Period period = context.getPeriod();
            if (period instanceof Instant instant) {
                /* An instant can end upto 3 days after the period end date */
                long days = Math.abs(ChronoUnit.DAYS.between(instant.getDate(), periodEndDate));
                if (days > 3)
                    continue;

                contextList.add(context);
            } else if (period instanceof Duration duration) {
                long days = Math.abs(ChronoUnit.DAYS.between(duration.getEndDate(), periodEndDate));
                if (days > 3)
                    continue;

                /* The logic below can potentially be simplified, but it requires extensive testing */
                days = duration.durationInDays();
                if (dei.getDocumentInformation().isQuarterlyReport()) {
                    if (Math.abs(days - 90) >= DAYS_ALLOWANCE)
                        continue;
                } else if (dei.getDocumentInformation().isAnnualReport()) {
                    if (Math.abs(days - 365) >= DAYS_ALLOWANCE) {
                        continue;
                    } else if (Math.abs(days - 90) >= DAYS_ALLOWANCE) {
                        /* Annual reports could have quarterly information i.e., Q4 which needs to be considered */
                        continue;
                    }
                }

                contextList.add(context);
            }
        }
        return contextList;
    }

    /**
     * Get all contexts that belong to the current fiscal year that start at the beginning of the current
     * fiscal year. YTD will change depending on whether the filing is for Q1, Q2, Q3 or FY. As in the case
     * of MRQ, contents with end dates up to 3 days after the period end date are included.
     *
     * @return Collection of contexts that belong to the current fiscal year starting from the beginning.
     */
    public Collection<Context> getYTDContexts() {
        LocalDate periodEndDate = dei.getEstimatedPeriodEndDate();
        String fiscalPeriod = Dei.guessFiscalPeriod(dei, periodEndDate);
        if (Objects.isNull(fiscalPeriod)) {
            /* Default to fiscalPeriod in DEI, if the guess is null */
            fiscalPeriod = dei.getFiscalPeriod();
        } else if (!fiscalPeriod.equals(dei.getFiscalPeriod())) {
            /* Guessed value does not match value in DEI. We are going to trust the guessed value, since the value in the DEI could be incorrect */
            log.info("Guessed fiscal period [{}] does not match DEI fiscal period [{}]", fiscalPeriod, dei.getFiscalPeriod());
        }

        List<Context> contextList = new ArrayList<>();
        for (Context context : contextMap.values()) {
            Period period = context.getPeriod();
            if (period instanceof Instant instant) {
                long days = Math.abs(ChronoUnit.DAYS.between(instant.getDate(), periodEndDate));
                if (days > 3)
                    continue;

                contextList.add(context);
            } else if (period instanceof Duration duration) {
                long days = Math.abs(ChronoUnit.DAYS.between(duration.getEndDate(), periodEndDate));
                if (days > 3)
                    continue;

                days = duration.durationInDays();
                switch (fiscalPeriod) {
                    case "Q1" -> {
                        /* For Q1, YTD is approximately 90 days */
                        if (Math.abs(days - 90) >= DAYS_ALLOWANCE)
                            continue;
                    }
                    case "Q2" -> {
                        /* For Q2, YTD is approximately 180 days */
                        if (Math.abs(days - 180) >= DAYS_ALLOWANCE)
                            continue;
                    }
                    case "Q3" -> {
                        /* For Q3, YTD is approximately 270 days */
                        if (Math.abs(days - 270) >= DAYS_ALLOWANCE)
                            continue;
                    }
                    default -> {
                        if (Math.abs(days - 365) >= DAYS_ALLOWANCE)
                            continue;
                    }
                }

                contextList.add(context);
            }
        }
        return contextList;
    }

    public TimeOrdered<Fact> getFactsFor() {
        TimeOrdered<Fact> facts = new TimeOrdered<>();
        for (Fact fact : this.facts.values()) {
            facts.add(fact.getContext().getPeriod(), fact);
        }
        return facts;
    }

    private void add(Fact fact) {
        Namespace ns = fact.getConcept().getNamespace();
        factNamespaces.add(ns);
        facts.add(fact);
        conceptFacts.add(fact);
    }

    private void parseXbrl(XbrlReader.Resolver resolver, Element root) {
        if (!root.getName().equals(XBRL_TAG))
            throw new MismatchTagException(XBRL_TAG, root.getName());

        List<Element> elements;

        //
        // Process schema_ref, context and unit tags first
        //
        String targetNamespace = root.attributeValue(TagNames.TARGET_NAMESPACE_TAG);
        elements = root.elements();
        for (Element element : elements) {
            String name = element.getName();
            switch (name) {
                case TagNames.SCHEMA_REF_TAG:
                    // In older XBRLs around 2012, an instance may have multiple schemaRef statements corresponding to
                    // the standard taxonomies such as us-gaap and dei. This is in addition to the import statements
                    // in the primary XSD. We are making an assumption that the first schemaRef is the one that will
                    // contain the XBRL instance.
                    if (dts == null)
                        dts = DiscoverableTaxonomySet.fromElement(resolver, element);
                    break;
                case Context.CONTEXT_TAG:
                    Context context = Context.fromElement(dts, element);
                    contextMap.add(context);
                    break;
                case TagNames.UNIT_TAG:
                    Unit unit = Unit.fromElement(element);
                    unitMap.add(unit);
                    break;
            }
        }

        List<FootnoteLink> footnoteLinks = new ArrayList<>();

        //
        // Process facts after
        //
        elements = root.elements();
        for (Element element : elements) {
            String name = element.getName();
            switch (name) {
                case TagNames.SCHEMA_REF_TAG:
                case Context.CONTEXT_TAG:
                case TagNames.UNIT_TAG:
                    break;
                case TagNames.FOOTNOTE_LINK_TAG:
                    FootnoteLink link = FootnoteLink.fromElement(resolver.getRootPath(), dts, element);
                    footnoteLinks.add(link);
                    break;
                default:
                    // We are adding DEI facts as well
                    Fact fact = Fact.fromElement(this, element);
                    this.add(fact);
                    break;
            }
        }

        /* Link footnotes to facts */
        for (FootnoteLink link : footnoteLinks) {
            for (FootnoteArc arc : link.getAllArcs()) {
                Footnote footnote = arc.getTo();
                String factId = arc.getFrom().getHashTag();
                Fact fact = facts.get(factId);
                if (fact != null) {
                    fact.setFootnote(footnote);
                }
            }
        }

        setDeiValues();

        // Sort facts in each context so higher precision facts appear earlier in the list. The same fact can
        // appear with different decimal precision values e.g. millions (-6) and 100's of millions (-8)
        // Prefer the higher precision fact i.e. the -6 in this example.
        for (Context context : contextMap.values()) {
            context.sortFacts();
        }

        // Sort concept facts so higher precision facts appear earlier in the list
        conceptFacts.sort();
    }

    /* Set dei values from facts */
    private void setDeiValues() {
        /*
         * Count occurrences of dates across the different contexts. We are going to use this information to "guess"
         * the end date of an XbrlInstance. Although the DEI is supposed to have the periodEndDate, it is sometimes
         * incorrect. By using the value in the instance, we hope to reduce errors
         */
        Counter<LocalDate> dateCounter = new Counter<>();
        for (Context context : contextMap.values()) {
            Period period = context.getPeriod();
            if (period instanceof Instant instant) {
                dateCounter.add(instant.getDate());
            } else if (period instanceof Duration duration) {
                dateCounter.add(duration.getEndDate());
            }
        }

        /* Identify the most frequently occurring end dates in this instance */
        List<Map.Entry<LocalDate,Integer>> entries = dateCounter.getEntries();
        entries.sort((l, r) -> {
            /* Sort by count descending and by date descending */
            int result = Integer.compare(r.getValue(), l.getValue());
            if (result != 0)
                return result;

            return r.getKey().compareTo(l.getKey());
        });
        if (!entries.isEmpty()) {
            log.info("Context date counts: [{}]", Joiner.on("; ").join(entries));
        }

        /* Set DEI values from XBRL facts. This will set the different values including the periodEndDate */
        for (Fact fact : facts.values()) {
            Concept concept = fact.getConcept();
            if (XbrlUtils.isDei(concept.getNamespace().getURI())) {
                if (log.isDebugEnabled())
                    log.debug("Setting DEI [{}] -> [{}]", fact.getConcept().getQualifiedName(), fact.getValue());
                dei.setPropertiesFrom(this, fact);
            }
        }

        /*
         * Validate periodEndDate in the DEI
         */
        LocalDate periodEndDate = dei.getPeriodEndDate();
        int i = -1;
        for (int j = 0; j < entries.size(); j++) {
            LocalDate key = entries.get(j).getKey();
            if (key.equals(periodEndDate)) {
                i = j;
            }
        }
        if (i >= 3) {
            /*
             * periodEndDate in the DEI is not within the top 3 most frequently occurring end dates. We are going to set
             * the most frequently occurring date as the periodEndDate. Would it be better to just ignore this XBRL altogether?
             */
            LocalDate estimatedEndDate = entries.get(0).getKey();
            dei.setEstimatedPeriodEndDate(estimatedEndDate);
            log.info("Mismatch: Estimated end date [{}] is different from period end date in DEI [{}]", estimatedEndDate, periodEndDate);
            /* Check if the dateFiled in the DEI is consistent with the periodEndDate we estimated */
            if (Objects.nonNull(dei.getDateFiled()) && estimatedEndDate.isAfter(dei.getDateFiled())) {
                log.info("Inconsistent: Estimated end date [{}] is after date filed [{}]", estimatedEndDate, dei.getDateFiled());
            }
        } else {
            if (i == -1) {
                /* This means, the periodEndDate in the DEI does not even occur as the end date in any of the contexts */
                log.info("periodEndDate [{}] in DEI does not occur in any context!", periodEndDate);
            }
            dei.setEstimatedPeriodEndDate(periodEndDate);
        }
    }

    private static final String HTML_NS = "http://www.w3.org/1999/xhtml";
    private static final String INLINE_XBRL_NS = "http://www.xbrl.org/2013/inlineXBRL";

    static boolean isXBRL(Element root) {
        return XBRL_TAG.equals(root.getName());
    }

    static boolean isInlineXBRL(Element root) {
        if (!HTML_TAG.equals(root.getName()))
            return false;

        if (!HTML_NS.equals(root.getNamespaceURI()))
            return false;

        List<Namespace> namespaces = root.declaredNamespaces();
        for (Namespace namespace : namespaces) {
            if (INLINE_XBRL_NS.equals(namespace.getURI()))
                return true;
        }

        return false;
    }

    private void parseInlineXBRL(XbrlReader.Resolver resolver, List<Element> roots) {
        for (Element root : roots) {
            if (!isInlineXBRL(root))
                throw new MismatchTagException(HTML_TAG, root.getName());
        }

        InlineXBRLParser inlineXBRLParser = new InlineXBRLParser(resolver);
        inlineXBRLParser.parse(roots);

        inlineXBRLParser.addSkippedFacts();
        inlineXBRLParser.linkRelationships();
        setDeiValues();

        inlineXBRLParser.clear();
    }

    private static class Relationship {
        private final String from;
        private final String to;
        private final String arcrole;
        private final String linkRole;

        private Relationship(String from, String to, String arcrole, String linkRole) {
            this.from = from;
            this.to = to;
            this.arcrole = arcrole;
            this.linkRole = linkRole;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getArcrole() {
            return arcrole;
        }

        public String getLinkRole() {
            return linkRole;
        }

        public static List<Relationship> relationships(Element element) {
            List<Relationship> relationships = new ArrayList<>();

            String from = element.attributeValue(TagNames.FROM_REFS_TAG);
            String to = element.attributeValue(TagNames.TO_REFS_TAG);
            String arcrole = element.attributeValue(TagNames.ARCROLE_TAG);
            String linkRole = element.attributeValue(TagNames.LINK_ROLE_TAG);

            List<String> fromRefs = Utils.splitString(from);
            List<String> toRefs = Utils.splitString(to);
            for (String fromRef : fromRefs) {
                for (String toRef : toRefs) {
                    relationships.add(new Relationship(fromRef, toRef, arcrole, linkRole));
                }
            }

            return relationships;
        }
    }

    private static class NodeChain {
        private final Element startElement;
        private final List<Element> chain;

        private NodeChain(Element startElement) {
            this.startElement = startElement;
            this.chain = new ArrayList<>();
            this.chain.add(startElement);
        }

        private Element getStartElement() {
            return startElement;
        }

        private List<Element> getChain() {
            return chain;
        }

        private void add(Element element) {
            chain.add(element);
        }
    }

    class InlineXBRLParser {
        private final XbrlReader.Resolver resolver;
        private final List<Element> postProcessList;
        private final Map<String,Element> continuations;
        private final List<Relationship> relationships;
        private final Map<String,Element> footnotes;

        InlineXBRLParser(XbrlReader.Resolver resolver) {
            this.resolver = resolver;
            this.postProcessList = new ArrayList<>();
            this.continuations = new HashMap<>();
            this.relationships = new ArrayList<>();
            this.footnotes = new HashMap<>();
        }

        void clear() {
            postProcessList.clear();
            continuations.clear();
            relationships.clear();
            footnotes.clear();
        }

        private PrintStream getPrintStream(String fileName) {
            try {
                return new PrintStream(fileName);
            } catch (IOException e) {
                return System.out;
            }
        }

        private String getPrefix(List<Namespace> namespaces, Predicate<String> predicate, String defaultPrefix) {
            for (Namespace n : namespaces) {
                if (predicate.test(n.getURI())) {
                    return n.getPrefix();
                }
            }

            return defaultPrefix;
        }

        private Element getBodyElement(Element root) {
            for (Element element : root.elements()) {
                if (element.getName().equals(BODY_TAG))
                    return element;
            }

            return null;
        }

        private void getIxElements(int depth, Element root, List<Element> elements) {
            if (root == null)
                return;

            QName name = root.getQName();
            if (XbrlUtils.isIx(name.getNamespace().getURI())) {
                elements.add(root);
                if (name.getName().equals(TagNames.HEADER_TAG) || name.getName().equals(TagNames.NON_FRACTION_TAG)) {
                    /* Don't recurse the header or nonFraction elements */
                    return;
                }
                depth += 1;
            }

            /* Recurse */
            for (Element element : root.elements()) {
                getIxElements(depth, element, elements);
            }
        }

        private void processIxHeader(XbrlReader.Resolver resolver, Element ixHeader) {
            List<Element> elements = ixHeader.elements();
            for (Element element : elements) {
                String name = element.getName();
                switch (name) {
                    case TagNames.HIDDEN_TAG:
                        /* Need to process this after processing ix:references and ix:resources */
                        break;

                    case TagNames.REFERENCES_TAG:
                        if (dts == null) {
                            /* When an instance has multiple files, the dts may be created multiple times - guard against that */
                            for (Element e : element.elements()) {
                                if (e.getName().equals(TagNames.SCHEMA_REF_TAG)) {
                                    dts = DiscoverableTaxonomySet.fromElement(resolver, e);
                                }
                            }
                        }
                        break;

                    case TagNames.RESOURCES_TAG:
                        for (Element e : element.elements()) {
                            if (e.getName().equals(Context.CONTEXT_TAG)) {
                                Context context = Context.fromElement(dts, e);
                                contextMap.add(context);
                            } else if (e.getName().equals(TagNames.UNIT_TAG)) {
                                Unit unit = Unit.fromElement(e);
                                unitMap.add(unit);
                            } else if (e.getName().equals(TagNames.RELATIONSHIP_TAG)) {
                                relationships.addAll(Relationship.relationships(e));
                            } else {
                                throw new RuntimeException("Unhandled Ix element [" + e.getName() + "] in processIxHeader");
                            }
                        }
                        break;

                    default:
                        throw new RuntimeException("Unhandled Ix element [" + name + "] in processIxHeader");
                }
            }
        }

        private void processIxHidden(Element ixHidden) {
            for (Element element : ixHidden.elements()) {
                String name = element.getName();
                switch (name) {
                    case TagNames.NON_FRACTION_TAG:
                    case TagNames.NON_NUMERIC_TAG: {
                        String attrName = element.attributeValue(TagNames.NAME_TAG);
                        if (attrName != null) {
                            Fact fact = Fact.fromElement(XbrlInstance.this, element, true);
                            XbrlInstance.this.add(fact);
                        } else {
                            throw new RuntimeException("processIxHidden name is null for [" + ixHidden.getName() + "]");
                        }
                        break;
                    }

                    case TagNames.FOOTNOTE_TAG: {
                        Attribute footnoteId = element.attribute(TagNames.ID_TAG);
                        if (footnoteId != null) {
                            footnotes.put(footnoteId.getValue(), element);
                        } else {
                            throw new RuntimeException("Missing id attribute in Footnote");
                        }
                        break;
                    }

                    case TagNames.TUPLE_TAG:
                    case TagNames.FRACTION_TAG:
                        break;
                }
            }
        }

        private void parse(List<Element> roots) {
            List<Element> elements = new ArrayList<>();

            for (Element root : roots) {
                Element body = getBodyElement(root);
                getIxElements(0, body, elements);
            }
            log.info("Found {} ix elements", elements.size());

            /* Process Header elements first */
            for (Element element : elements) {
                String name = element.getName();
                if (TagNames.HEADER_TAG.equals(name)) {
                    processIxHeader(resolver, element);
                }
            }

            /* Process hidden elements next */
            for (Element element : elements) {
                String name = element.getName();
                if (TagNames.HEADER_TAG.equals(name)) {
                    List<Element> childElements = element.elements();
                    if (childElements == null)
                        continue;

                    childElements.forEach(childElement -> {
                        String childName = childElement.getName();
                        if (TagNames.HIDDEN_TAG.equals(childName)) {
                            processIxHidden(childElement);
                        }
                    });
                }
            }

            /* Process all other elements last */
            for (Element element : elements) {
                String attrName = element.attributeValue(TagNames.NAME_TAG);

                String name = element.getName();
                switch (name) {
                    case TagNames.CONTINUATION_TAG:
                        Attribute continuationId = element.attribute(TagNames.ID_TAG);
                        if (continuationId != null) {
                            continuations.put(continuationId.getValue(), element);
                        } else {
                            throw new RuntimeException("Continuation element without an ID");
                        }
                        break;

                    case TagNames.FRACTION_TAG:
                        /* TODO - Need to complete this */
                        throw new RuntimeException("Fraction not implemented!");

                    case TagNames.FOOTNOTE_TAG:
                        Attribute footnoteId = element.attribute(TagNames.ID_TAG);
                        if (footnoteId != null) {
                            footnotes.put(footnoteId.getValue(), element);
                        } else {
                            throw new RuntimeException("Missing id attribute in Footnote");
                        }
                        break;

                    case TagNames.HEADER_TAG:
                        /* Already processed - see above */
                        break;

                    case TagNames.NON_FRACTION_TAG: {
                        // a nonFraction element may sometimes contain another nonFraction element. In this case
                        // the value of the top level nonFraction element is the same as the child nonFraction element
                        List<Element> childElements = element.elements().stream()
                                .filter(e -> TagNames.NON_FRACTION_TAG.equals(e.getName())).toList();
                        if (childElements.size() > 0) {
                            if (childElements.size() > 1) {
                                log.info("More than one nonFraction child!");
                            }
                            /* Child element */
                            Element childElement = childElements.get(0);
                            Fact childFact = Fact.fromElement(XbrlInstance.this, childElement, true);
                            XbrlInstance.this.add(childFact);

                            /* Top level element - value of top level elements is the same as the child element */
                            Fact fact = Fact.fromElement(XbrlInstance.this, element, childFact,true);
                            XbrlInstance.this.add(fact);
                        } else {
                            Fact fact = Fact.fromElement(XbrlInstance.this, element, true);
                            XbrlInstance.this.add(fact);
                        }
                        break;
                    }

                    case TagNames.NON_NUMERIC_TAG: {
                        Attribute continuedAt = element.attribute(TagNames.CONTINUED_AT_TAG);
                        if (continuedAt != null) {
                            postProcessList.add(element);
                        } else {
                            Fact fact = Fact.fromElement(XbrlInstance.this, element, true);
                            XbrlInstance.this.add(fact);
                        }
                        break;
                    }

                    case TagNames.TUPLE_TAG:
                        /* The tuple construct seems to be used to reconstruct XBRL, but we are not doing that - ignore? */
                        log.info("Ignoring {} tag", name);
                        break;

                    case TagNames.EXCLUDE_TAG:
                        /* exclude tags are meant to be ignored and serve no other purpose */
                        break;

                    default:
                        throw new RuntimeException("Unhandled Ix element [" + name+ "] in iXBRL");
                }
            }
        }

        private NodeChain getChain(Element startElement) {
            NodeChain nc = new NodeChain(startElement);
            Attribute continuedAt = startElement.attribute(TagNames.CONTINUED_AT_TAG);
            while (continuedAt != null) {
                Element continuation = continuations.get(continuedAt.getValue());
                nc.add(continuation);
                continuedAt = continuation.attribute(TagNames.CONTINUED_AT_TAG);
            }

            return nc;
        }

        private void addSkippedFacts() {
            for (Element element : postProcessList) {
                NodeChain nc = getChain(element);
                Fact fact = Fact.fromElement(XbrlInstance.this, nc.getChain(), true);
                XbrlInstance.this.add(fact);
            }
        }

        private void linkRelationships() {
            log.info("Found {} relationships", relationships.size());
            // Some XBRLs have too many footnotes, practically one for each fact and almost all of them are the same.
            // Not sure, how useful this really is. Ideally, it will be good to preserve this. However, the current
            // implementation of json serialization is memory intensive. Until, we can make that more efficient we
            // are going to ignore footnotes, when there are too many of them. TODO - Revisit this
            boolean tooManyRelationships = relationships.size() > 999;
            Counter<String> roleCounter = new Counter<>();
            for (Relationship relationship : relationships) {
                Fact fact = facts.get(relationship.getFrom());
                if (fact == null) {
                    log.info("Found null fact in linkRelationships");
                    continue;
                }

                if (!tooManyRelationships && XbrlUtils.isFactFootnote(relationship.getArcrole())) {
                    Element element = footnotes.get(relationship.getTo());
                    NodeChain nc = getChain(element);
                    Footnote footnote = Footnote.fromElements(resolver.getRootPath(), nc.getChain());
                    fact.setFootnote(footnote);
                } else if (!tooManyRelationships) {
                    log.info("Ignoring relationship [{}]", relationship.getArcrole());
                } else {
                    roleCounter.add(relationship.getArcrole());
                }
            }
            if (tooManyRelationships) {
                for (var entry : roleCounter.getEntriesSorted()) {
                    log.info("Ignored [{}] relationships of [{}]", entry.getValue(), entry.getKey());
                }
            }
        }
    }

    public static class Statistics {
        public final int nOfContexts;
        public final int nOfUnits;
        public final int nOfFacts;
        public final int nOfUniqueFacts;
        public final int nOfFactConcepts;
        public final Map<Namespace,Integer> namespaceFactCount;

        private Statistics(XbrlInstance instance) {
            this.nOfContexts = instance.contextMap.size();
            this.nOfUnits = instance.unitMap.size();
            this.nOfFacts = instance.facts.size();

            Map<Fact,Set<Fact>> factSetMap = new HashMap<>();
            Set<Concept> concepts = new HashSet<>();
            for (Fact fact : instance.facts.values()) {
                concepts.add(fact.getConcept());
                Set<Fact> facts = factSetMap.computeIfAbsent(fact, k -> new TreeSet<>(Fact::compareStringValue));
                facts.add(fact);
            }

            int uniqueCount = 0;
            for (Map.Entry<Fact,Set<Fact>> entry : factSetMap.entrySet()) {
                uniqueCount += entry.getValue().size();
            }

            this.nOfFactConcepts = concepts.size();
            this.nOfUniqueFacts = uniqueCount;

            this.namespaceFactCount = new ImmutableMap.Builder<Namespace,Integer>().putAll(instance.factNamespaces.map()).build();
        }
    }

    public static class UnusedStatistics {
        public final int nOfContexts;
        public final int nOfFacts;
        public final int nOfFactConcepts;
        public final List<Fact> unusedFacts;
        public final Map<Period,Integer> periodFactCount;
        public final Map<Namespace,Integer> namespaceFactCount;
        public final Map<String,Integer> conceptFactCount;
        public final Map<String,Collection<String>> conceptFacts = new TreeMap<>();

        private UnusedStatistics(XbrlInstance instance, Set<String> usedFactIds) {
            Set<Fact> usedFacts = new HashSet<>();
            for (String factId : usedFactIds) {
                Fact fact = instance.facts.get(factId);
                assert  fact != null : String.format("Unexpected! Fact is null for id: [%s]!", factId);
                usedFacts.add(fact);
            }

            this.unusedFacts = new ArrayList<>();
            for (Fact fact : instance.facts.values()) {
                if (!usedFacts.contains(fact)) {
                    unusedFacts.add(fact);
                }
            }
            this.nOfFacts = unusedFacts.size();

            Counter<Period> contextCounter = new Counter<>();
            Counter<Namespace> factCounter = new Counter<>();
            Counter<String> conceptFactCounter = new Counter<>();
            for (Fact fact : unusedFacts) {
                contextCounter.add(fact.getContext().getPeriod());

                Concept concept = fact.getConcept();
                factCounter.add(concept.getNamespace());

                String conceptName = concept.getQualifiedName();
                conceptFactCounter.add(conceptName);
                Collection<String> facts = conceptFacts.computeIfAbsent(conceptName, k -> new ArrayList<>());
                facts.add(fact.toString());
            }
            this.nOfContexts = contextCounter.size();
            this.periodFactCount = new ImmutableMap.Builder<Period,Integer>().putAll(contextCounter.map()).build();

            Set<Concept> concepts = new HashSet<>();
            for (Fact fact : unusedFacts) {
                concepts.add(fact.getConcept());
            }
            this.nOfFactConcepts = concepts.size();

            this.namespaceFactCount = new ImmutableMap.Builder<Namespace,Integer>().putAll(factCounter.map()).build();
            this.conceptFactCount = new ImmutableMap.Builder<String,Integer>().putAll(conceptFactCounter.map()).build();
        }
    }

    public Statistics getStatistics() {
        return new Statistics(this);
    }

    public UnusedStatistics getUnusedStatistics(Set<String> factIds) {
        return new UnusedStatistics(this, factIds);
    }

    public void displayStats(Set<String> factIds) {
        dts.logStats();

        Statistics statistics = getStatistics();
        UnusedStatistics unusedStatistics = getUnusedStatistics(factIds);

        log.info("Instance stats:");
        log.info("Found {} contexts", statistics.nOfContexts);
        log.info("Found {} units", statistics.nOfUnits);
        log.info("Found {} facts", statistics.nOfFacts);
        log.info("Unique concepts {}", statistics.nOfFactConcepts);

        log.info("Unused stats:");
        log.info("{} contexts unused", unusedStatistics.nOfContexts);
        log.info("{} facts unused", unusedStatistics.nOfFacts);
        log.info("{} fact concepts unused", unusedStatistics.nOfFactConcepts);

        for (Map.Entry<Namespace,Integer> entry : statistics.namespaceFactCount.entrySet()) {
            Namespace ns = entry.getKey();
            log.info("[{}, {}] = [{}]", ns.getPrefix(), ns.getURI(), entry.getValue());
        }
    }
}
