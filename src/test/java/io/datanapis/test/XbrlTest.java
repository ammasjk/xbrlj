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
package io.datanapis.test;

import com.google.common.base.Joiner;
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.XbrlReader;
import io.datanapis.xbrl.analysis.*;
import io.datanapis.xbrl.analysis.data.XbrlInstancePath;
import io.datanapis.xbrl.analysis.data.XbrlTaxonomyPath;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.utils.EdgarUtils;
import io.datanapis.xbrl.utils.Utils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.regex.Pattern;

public class XbrlTest {
    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testListInsert() {
        List<Integer> one = new ArrayList<>();
        one.add(2);

        Utils.insert(one, 1, Integer::compare);
        Utils.insert(one, 5, Integer::compare);
        Utils.insert(one, 6, Integer::compare);
        Utils.insert(one, 3, Integer::compare);
        Utils.insert(one, 4, Integer::compare);

        FileSystem fs = FileSystems.getDefault();
        Path downloads = fs.getPath("");
        System.out.println(downloads.toAbsolutePath());
    }

    private void countDocumentationLabels(Collection<Concept> allConcepts) {
        int count = 0;
        Map<String,Integer> prefixCounts = new TreeMap<>();
        for (Concept concept : allConcepts) {
            if (concept.getLabel(Label.ROLE_TYPE_DOCUMENTATION) != null) {
                ++count;
                prefixCounts.merge(concept.getPrefix(), 1, Integer::sum);
            }
        }
        System.out.printf("Found [%d] concepts with documentation labels. %f\n", count, count / (double)allConcepts.size());
        Joiner.MapJoiner mapJoiner = Joiner.on(',').withKeyValueSeparator('=');
        System.out.println("Documentation statistics: (" + mapJoiner.join(prefixCounts) + ")");
    }

    private static final String T_PREFIX = "http://fasb.org/us-gaap/role/statement/";

    private static final Set<String> INCOME_STATEMENTS = Set.of(
                "http://fasb.org/us-gaap/role/statement/StatementOfIncome",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeSecuritiesBasedIncome",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeRealEstateInvestmentTrusts",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeRealEstateExcludingREITs",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeInterestBasedRevenue",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeInsuranceBasedRevenue",
                "http://fasb.org/us-gaap/role/statement/StatementOfIncomeAlternative"
            );
    private static final Set<String> FINANCIAL_POSITION_STATEMENTS = Set.of(
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-SecuritiesBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-RealEstateOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-InvestmentBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-DepositBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionClassified",
                "http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionClassified-RealEstateOperations"
            );
    private static final Set<String> CASH_FLOW_STATEMENTS = Set.of(
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirect",
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectSecuritiesBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectRealEstate",
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectInvestmentBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectDepositBasedOperations",
                "http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectAdditionalElements"
            );

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testTaxonomy() throws Exception {
        testTaxonomy(XbrlTaxonomyPath.T2024);
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testAllTaxonomy() throws Exception {
        for (XbrlTaxonomyPath xbrlTaxonomyPath : XbrlTaxonomyPath.values()) {
            testTaxonomy(xbrlTaxonomyPath, s -> true, true, true, true);
            Thread.sleep(5000);
        }
    }

    private void testTaxonomy(XbrlTaxonomyPath xbrlTaxonomyPath) throws Exception {
        testTaxonomy(xbrlTaxonomyPath, s -> true, true, true, true);
    }

    private void testTaxonomy(XbrlTaxonomyPath xbrlTaxonomyPath, Predicate<String> filter, boolean definition, boolean calculation, boolean presentation) throws Exception {
        int definitionLinks = 0, presentationLinks = 0, calculationLinks = 0;

        XbrlReader reader = new XbrlReader();
        DiscoverableTaxonomySet dts = reader.getTaxonomy(xbrlTaxonomyPath.toString(), true);
        Collection<Concept> allConcepts = dts.getAllConcepts();
        countDocumentationLabels(allConcepts);

        Collection<Concept> concepts = dts.getMatchingConcepts(Pattern.compile("AdditionsToNoncurrentAssets"));

        RoleType deprecated = RoleType.DEPRECATED;
        System.out.printf("RoleType deprecated has presentation: %b, calculation: %b, definition: %b\n",
                deprecated.getPresentationLink() != null, deprecated.getCalculationLink() != null, deprecated.getDefinitionLink() != null);

        final PrintWriter writer = new PrintWriter(new FileWriter(xbrlTaxonomyPath.getFilePath()));
        DefinitionTaxonomy definitionTaxonomy = new DefinitionTaxonomy(dts);
        PresentationTaxonomy presentationTaxonomy = new PresentationTaxonomy(dts);
        CalculationTaxonomy calculationTaxonomy = new CalculationTaxonomy(dts);

        Collection<RoleType> allRoles = dts.getAllRoleTypes();
        for (RoleType roleType : allRoles) {
            String roleURI = roleType.getRoleURI().toLowerCase();
            boolean isDisclosure = roleURI.contains("disclosure");
            boolean isStatement = roleURI.contains("statement");

            String title = roleType.getDefinition();
            if (definition && filter.test(title) && roleType.getDefinitionLink() != null) {
                ++definitionLinks;
                if (roleType.isDeprecated() || isStatement) {
                    definitionTaxonomy.walk(roleType, new DefinitionTaxonomy.WriterConsumer(writer));
                }
            }
            if (presentation && filter.test(title) && roleType.getPresentationLink() != null) {
                ++presentationLinks;
                if (roleType.isDeprecated() || (isStatement && !isDisclosure)) {
                    presentationTaxonomy.walk(roleType, new PresentationTaxonomy.WriterConsumer(writer));
                }
            }
            if (calculation && filter.test(title) && roleType.getCalculationLink() != null) {
                ++calculationLinks;
                if (isStatement && !isDisclosure) {
                    calculationTaxonomy.walk(roleType, new CalculationTaxonomy.WriterConsumer(writer));
                }
            }
        }

        writer.close();

        dts.logStats();
        System.out.printf("Cache stats: [request: %d, network: %d, hit: %d]\n",
                reader.requestCount(), reader.networkCount(), reader.hitCount());
        System.out.printf("  Definition links: [%d]\n", definitionLinks);
        System.out.printf("Presentation Links: [%d]\n", presentationLinks);
        System.out.printf(" Calculation Links: [%d]\n", calculationLinks);
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void getDeprecatedConcepts() throws Exception {
        Map<String,Set<String>> map = new HashMap<>();
        Map<String,Set<String>> axisMap = new HashMap<>();
        Map<String,Set<String>> memberMap = new HashMap<>();
        getDeprecatedConcepts(XbrlTaxonomyPath.T2023, map, axisMap, memberMap);
        System.out.printf("Deprecated concept data: concepts:[%d], axis:[%d], member:[%d]\n", map.size(), axisMap.size(), memberMap.size());
        int count = 0;
        for (var entry : map.entrySet()) {
            System.out.printf("\t[%s] -> %s\n", entry.getKey(), entry.getValue().toString());
            if (++count > 100)
                break;
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void getAllDeprecatedConcepts() throws Exception {
        Map<String,Set<String>> map = new TreeMap<>();
        Map<String,Set<String>> axisMap = new TreeMap<>();
        Map<String,Set<String>> memberMap = new TreeMap<>();
        List<XbrlTaxonomyPath> list = new ArrayList<>(List.of(XbrlTaxonomyPath.values()));
        list.sort((l, r) -> Integer.compare(r.year(), l.year()));

        for (XbrlTaxonomyPath xbrlTaxonomyPath : list) {
            getDeprecatedConcepts(xbrlTaxonomyPath, map, axisMap, memberMap);
            Thread.sleep(5000);
        }
        System.out.printf("Deprecated concept data: concepts:[%d], axis:[%d], member:[%d]\n", map.size(), axisMap.size(), memberMap.size());
        for (var entry : map.entrySet()) {
            System.out.printf("    [%s] -> [%s]\n", entry.getKey(), entry.getValue());
        }
    }

    private void getDeprecatedConcepts(XbrlTaxonomyPath xbrlTaxonomyPath, Map<String,Set<String>> map, Map<String,Set<String>> axisMap, Map<String,Set<String>> memberMap) throws Exception {
        XbrlReader reader = new XbrlReader();
        DiscoverableTaxonomySet dts = reader.getTaxonomy(xbrlTaxonomyPath.toString(), true);

        DefinitionTaxonomy definitionTaxonomy = new DefinitionTaxonomy(dts);
        Collection<RoleType> allRoles = dts.getAllRoleTypes();
        for (RoleType roleType : allRoles) {
            if (roleType.getDefinitionLink() == null)
                continue;

            if (!roleType.isDeprecated())
                continue;

            definitionTaxonomy.walk(roleType, new DefinitionNetworkConsumer() {
                @Override
                public void nodeStart(DefinitionGraphNode node, Deque<DefinitionGraphNode> path) {
                    String nodeName = node.getQualifiedName();

                    if (path.size() > 1) {
                        System.out.printf("[%s] has path length of [%d]\n", nodeName, path.size());
                    }
                    String replacement = path.getLast().getQualifiedName();

                    Set<String> replacements;
                    if (nodeName.endsWith("Axis") || replacement.endsWith("Axis")) {
                        replacements = axisMap.computeIfAbsent(nodeName, k -> new HashSet<>());
                    } else if (nodeName.endsWith("Member") || replacement.endsWith("Member")) {
                        replacements = memberMap.computeIfAbsent(nodeName, k -> new HashSet<>());
                    } else {
                        replacements = map.computeIfAbsent(nodeName, k -> new HashSet<>());
                        Set<String> s = map.get(replacement);
                        if (Objects.nonNull(s) && s.size() == 1) {
                            System.out.printf("Replacement [%s] for [%s] has been replaced by [%s]\n", replacement, nodeName, s.iterator().next());
                        } else if (Objects.nonNull(s) && s.size() > 1) {
                            System.out.printf("Replacement [%s] for [%s] has been replaced by [%d] concepts\n", replacement, nodeName, s.size());
                        }
                    }
                    replacements.add(replacement);
                }
            });
        }

        Collection<Concept> concepts = dts.getAllConcepts();
        for (Concept concept : concepts) {
            List<String> replacements = concept.getReplacements();
            if (Objects.isNull(replacements))
                continue;

            System.out.printf("Replacements for [%s]:", concept.getQualifiedName());
            Collection<Concept> replacementConcepts = dts.asConcepts(replacements);
            for (Concept c :  replacementConcepts) {
                System.out.printf(" [%s]", c.getQualifiedName());
            }
            System.out.println();
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void writeTaxonomy() throws Exception {
        writeTaxonomy(XbrlTaxonomyPath.T2020, s -> true);
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void writeAllTaxonomy() throws Exception {
        Set<String> candidates = new HashSet<>(INCOME_STATEMENTS);
        candidates.addAll(FINANCIAL_POSITION_STATEMENTS);
        candidates.addAll(CASH_FLOW_STATEMENTS);

        for (XbrlTaxonomyPath taxonomyPath : XbrlTaxonomyPath.values()) {
            writeTaxonomy(taxonomyPath, candidates::contains);
            Thread.sleep(5000);
        }
    }

    private void writeTaxonomy(XbrlTaxonomyPath xbrlTaxonomyPath, Predicate<String> filter) throws Exception {
        XbrlReader reader = new XbrlReader();
        DiscoverableTaxonomySet dts = reader.getTaxonomy(xbrlTaxonomyPath.toString(), true);

        try (PrintWriter writer = new PrintWriter(new FileWriter(xbrlTaxonomyPath.getCSVPath()))) {
            PresentationTaxonomy presentationTaxonomy = new PresentationTaxonomy(dts);
            Collection<RoleType> allRoles = dts.getAllRoleTypes();
            for (RoleType roleType : allRoles) {
                String roleURI = roleType.getRoleURI().toLowerCase();
                boolean isDisclosure = roleURI.contains("disclosure");
                boolean isStatement = roleURI.contains("statement");

                String title = roleType.getDefinition();
                int index = title.lastIndexOf("/");
                String label;
                if (index >= 0) {
                    label = title.substring(index + 1);
                } else {
                    label = title;
                }

                if (roleType.getPresentationLink() != null && filter.test(title)) {
                    if (isStatement && !isDisclosure) {
                        Function<Deque<PresentationGraphNode>,String> mapper = p -> {
                            final StringBuilder builder = new StringBuilder();
                            Iterator<PresentationGraphNode> iterator = p.descendingIterator();
                            boolean print = false;
                            while (iterator.hasNext()) {
                                PresentationGraphNode graphNode = iterator.next();
                                Concept concept = graphNode.getConcept();
                                if (concept.getQualifiedName().equals("us-gaap:StatementLineItems")) {
                                    print = true;
                                    continue;
                                }
                                if (!print)
                                    continue;
                                if (!builder.isEmpty())
                                    builder.append('|');
                                builder.append(graphNode.getConcept().getQualifiedName());
                            }
                            return builder.toString();
                        };

                        presentationTaxonomy.walk(roleType, new PresentationNetworkConsumer() {
                            @Override
                            public void nodeEnd(PresentationGraphNode n, Deque<PresentationGraphNode> p) {
                                if (n.hasChildren())
                                    return;

                                String path = mapper.apply(p);
                                if (!path.isEmpty()) {
                                    writer.printf("%s,%s,%s\n", n.getConcept().getQualifiedName(), label, path);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testSingle() throws Exception {
        testSingle(XbrlInstancePath.T2023_GOPRO_INC_10Q, false, false, false);
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testAll() throws Exception {
        XbrlInstancePath from = XbrlInstancePath.T2012_GLOBAL_POWER_EQUIPMENT_GROUP_INC_10Q;
        boolean skip = false;
        for (XbrlInstancePath instancePath : XbrlInstancePath.values()) {
            if (instancePath.equals(from)) {
                skip = false;
            }
            if (skip)
                continue;
            testSingle(instancePath, false, false, false);
            Thread.sleep(1000);
        }
        System.out.println("Testing completed!");
    }

    private void testSingle(XbrlInstancePath instancePath, boolean asJson, boolean outputContexts, boolean succinct) throws Exception {
        System.out.println("Testing [" + instancePath.name() + "]");
        XbrlReader reader = new XbrlReader();
        XbrlInstance instance;

        if (instancePath.getLocalPath() == null) {
            instance = reader.getInstance(null, instancePath.getPath());
        } else {
            try (InputStream is = new FileInputStream(instancePath.getLocalPath())) {
                instance = reader.getInstanceFromZipStream(null, instancePath.getPath(), is);
            }
        }

        System.out.printf("Cache stats: [request: %d, network: %d, hit: %d]\n",
                XbrlReader.requestCount(), XbrlReader.networkCount(), XbrlReader.hitCount());

        DiscoverableTaxonomySet dts = instance.getTaxonomy();
        Collection<Concept> allConcepts = dts.getAllConcepts();
        countDocumentationLabels(allConcepts);

        Collection<RoleType> taxonomyRoles = dts.getAllRoleTypes();
        Collection<RoleType> roles = dts.getReportableRoleTypes();

        DefinitionNetwork definitionNetwork = new DefinitionNetwork(instance);
        CalculationSerializer calculationSerializer = new CalculationSerializer();
        CalculationNetwork calculationNetwork = new CalculationNetwork(instance, calculationSerializer);

        String path = asJson ? instancePath.getJsonPath() : instancePath.getFilePath();
        PrintWriter writer = new PrintWriter(new FileWriter(path));

        if (outputContexts) {
            writer.println("Contexts:");
            Collection<Context> contexts = instance.getAllContexts();
            for (Context context : contexts) {
                writer.println(context.toString());
            }
            writer.println();
        }

        PrettyPrinter printer = new PrettyPrinter(writer, true, false, false);
        PresentationSerializer presentationSerializer = new PresentationSerializer(true);
        PresentationNetwork fileWriter = new PresentationNetwork(instance, asJson ? presentationSerializer : printer);

        for (RoleType roleType : roles) {
            if (!succinct) {
                if (asJson) {
                    calculationNetwork.validateCalculation(null, roleType);
                } else {
                    calculationNetwork.validateCalculation(writer, roleType);
                    writer.println();
                    definitionNetwork.walk(roleType, new DefinitionTaxonomy.WriterConsumer(writer));
//                    fileWriter.walk(roleType, new PresentationTaxonomy.WriterConsumer(writer));
                }
                fileWriter.process(roleType);
            } else {
                String[] groups = EdgarUtils.splitDefinition(roleType);
                if (Objects.nonNull(groups) && groups[1].equalsIgnoreCase("statement")) {
                    fileWriter.process(roleType);
                }
            }
        }

        fileWriter.complete();
        XbrlInstance.Statistics statistics = instance.getStatistics();
        XbrlInstance.UnusedStatistics unusedStatistics = instance.getUnusedStatistics(fileWriter.getFactsUsed());

        if (asJson) {
            JsonSerializer jsonSerializer = new JsonSerializer()
                    .prettyPrint(false)
                    .serializeNulls(false)
                    .dei(instance.getDei())
                    .statistics(statistics)
                    .unusedStatistics(unusedStatistics)
                    .presentation(presentationSerializer.asJson())
                    .calculations(calculationSerializer.asJson());
            String json = jsonSerializer.serialize();
            writer.println(json);
            writer.close();
            jsonSerializer = null;
        }

        instance.displayStats(fileWriter.getFactsUsed());

        Dei dei = instance.getDei();
        String prefix = instance.getInstancePrefix();
        if (Objects.nonNull(prefix))
            prefix = prefix.toUpperCase();
        System.out.printf("Prefix: [%s], Fiscal Period, guessed vs provided: [%d, %s] / [%d, %s], Tickers: [%s]\n",
                prefix, Dei.guessFiscalYear(dei, dei.getEstimatedPeriodEndDate()),
                Dei.guessFiscalPeriod(dei, dei.getEstimatedPeriodEndDate()), dei.getFiscalYear(), dei.getFiscalPeriod(),
                Joiner.on(", ").join(dei.getTickers()));
        instance.clear();

        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
    }

    private static final String[] NOT_WORKING_URLS = {
            /* With the latest changes, these files are working as well */
            "https://www.sec.gov/Archives/edgar/data/100790/000002991518000005/ucc-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1571804/000164033420001240/gvbt-20181231.xml",
            "https://www.sec.gov/Archives/edgar/data/1528356/000121390020011615/gne-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1393540/000164033420001364/igen-20191231.xml",
            "https://www.sec.gov/Archives/edgar/data/1338065/000133806520000028/dpm-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1451512/000147793219004600/trtcd-20190630.xml",
            "https://www.sec.gov/Archives/edgar/data/1097396/000119312520215960/ck0001097396-20200630.xml",
            "https://www.sec.gov/Archives/edgar/data/1684508/000147793220003843/cann-20200229.xml",
            "https://www.sec.gov/Archives/edgar/data/1141688/000149315220014985/form10-q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1627606/000147793220005516/dtea-20200801.xml",
            "https://www.sec.gov/Archives/edgar/data/1393540/000164033420001684/igen-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/798528/000119312520220504/omex-20200630.xml",
    };

    private static final String[] URLS = {
            "https://www.sec.gov/Archives/edgar/data/1020214/000156459020048950/cers-10q_20200930_htm.xml",

            /* Files working ok. Not 100% but close */
            "https://www.sec.gov/Archives/edgar/data/1228627/000114036120010906/rexn-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1745078/000155335020000525/vynl-20191231.xml",
            "https://www.sec.gov/Archives/edgar/data/1465470/000165495420007019/shmp-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1609065/000156459020027590/pbhc-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1627554/000101738620000165/quest-20200131.xml",
            "https://www.sec.gov/Archives/edgar/data/8192/000162828018001324/exc-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/71180/000108131618000014/bhe-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1437402/000155837018002056/ardx-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1274173/000104746918001112/jhg-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1043121/000165642318000004/bxp-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1620179/000104746918001782/xela-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1327978/000132797818000020/ck0001327978-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1545654/000154565418000014/alex-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/896264/000156276220000164/usna-20200328x10q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1411168/000137647420000097/dug-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1000209/000156459020022990/mfin-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/754811/000118518520000645/grow-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1132509/000147793220002386/ekkh-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/894560/000155116320000039/both-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1269190/000119312520138643/cta-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/886744/000156459020027307/gern-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1513818/000156459020021944/arav-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1245791/000121390020011182/gidyl-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/925660/000154812320000062/flxt-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1681206/000117494720000638/nodk-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/99780/000009978020000046/trn331202010q-q1_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1390844/000139084420000006/wbc10q3312020_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1798562/000121390020015197/soac-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1377149/000138713120004906/crvw-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/926282/000156459020023537/adtn-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/277509/000027750920000030/fss-2020331x10q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1158420/000110465920061987/hgsh-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1638290/000156459020023454/mcft-20200329.xml",
            "https://www.sec.gov/Archives/edgar/data/1599947/000159994720000022/terp-20191231_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1618835/000161883520000105/evfm-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1611647/000156459020021307/frpt-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1540729/000156459020015553/felp-20191231.xml",
            "https://www.sec.gov/Archives/edgar/data/1472787/000156459020019332/faf-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1413754/000107878220000339/marz-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1599617/000156459020021642/dnow-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/108312/000010831220000006/wwd-20200331x10q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1753391/000175339120000008/none-20200430.xml",
            "https://www.sec.gov/Archives/edgar/data/1504167/000110465920072899/tmbr-20200430.xml",
            "https://www.sec.gov/Archives/edgar/data/818479/000081847920000023/xray-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1395942/000139594220000072/karq1202010-q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/896493/000121465920005205/dpw-20191231.xml",
            "https://www.sec.gov/Archives/edgar/data/57725/000155837020005713/lci-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/711772/000156459020023671/catc-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/350894/000035089420000026/seic-331202010q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/20639/000114036120010960/abcp-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/788329/000114036120010697/jout-20200327.xml",
            "https://www.sec.gov/Archives/edgar/data/1506439/000165495420005620/shsp-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1061027/000156459020022937/snss-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/717605/000156459020017486/hxl-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1326321/000089424520000015/aei26-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/12400/000001240020000004/bhp10qq12020_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1794621/000119312520138270/ccac-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/31235/000156459020024666/kodk-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/812128/000081212820000010/safm-20200430_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/797564/000167479620000011/hstc-20191231_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1590750/000159075020000053/mgen-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1067983/000156459020020599/brka-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1324404/000132440420000013/cf-03312020x10q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/74046/000007404620000029/odc10-q4302020_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1646228/000155837020006844/home-20200519x10k_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1681087/000156459020022815/avro-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1376321/000117184320004597/cnet-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1626745/000149315220009135/fvti-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1376793/000168316820001684/cvat-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/215466/000021546620000102/cde-03312010q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1366868/000136686820000045/gsat-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1275014/000156459020021826/uctt-10q_20200327_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/105418/000010541820000016/wmk-20200328.xml",
            "https://www.sec.gov/Archives/edgar/data/1518336/000149315220009677/drem-20191231.xml",
            "https://www.sec.gov/Archives/edgar/data/1750777/000147793220003601/hwke-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/60714/000156459020022839/lxu-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/703604/000070360420000042/laws-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/352915/000156459020023579/uhs-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/794170/000079417020000039/tol-20200430_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1594466/000159446620000076/pe-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/915912/000091591220000018/q1202010-q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1684144/000117184320003586/zom-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1314772/000110465920060206/sumr-20200328.xml",
            "https://www.sec.gov/Archives/edgar/data/1530766/000118518520000595/bsgm-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1046568/000156459020022879/prdo-10q_20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1419275/000118518520000436/grbx-20190930.xml",
            "https://www.sec.gov/Archives/edgar/data/1728117/000156459020024690/goss-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/1069157/000106915720000040/ewbc10q3312020_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1581164/000162828020006700/stay-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1094285/000109428520000068/tdy-20200329_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/20212/000002021220000058/chdn-20200331_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1638287/000155837020006879/nrbo-20200331.xml",
            "https://www.sec.gov/Archives/edgar/data/831489/000110465920053508/scrh-20190331.xml",
            "https://www.sec.gov/Archives/edgar/data/1005286/000100528620000004/lndc-20200531_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/874710/000110465920109282/ahpi-20200630.xml",
            "https://www.sec.gov/Archives/edgar/data/1091883/000109188320000110/cirq20628202010q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/793306/000165495420009186/bdco-20200630.xml",
            "https://www.sec.gov/Archives/edgar/data/1579298/000156459020041539/burl-10q_20200801_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1676725/000156459020039719/idya-20200630.xml",
            "https://www.sec.gov/Archives/edgar/data/1555280/000155528020000261/zts-20200630_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/1766502/000176650219000013/chwyq2201910-q_htm.xml",
            "https://www.sec.gov/Archives/edgar/data/108516/000156459019027053/wor-20190531.xml",
            "https://www.sec.gov/Archives/edgar/data/921582/000119312518061184/imax-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/4457/000000445718000005/uhal-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/75488/000100498018000003/pcg-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1024126/000113626118000075/ptx-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/37637/000132616018000034/duk-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1614184/000156459018006217/cade-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/40211/000004021118000023/gmt-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1065598/000164033418000086/dgtw-20171130.xml",
            "https://www.sec.gov/Archives/edgar/data/1586049/000143774918003397/oxfd-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/40570/000147793218000832/gee-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/700564/000070056418000010/fult-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1473597/000117184318002020/stri-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1666114/000121390018001849/unl-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1594864/000159486418000005/juno-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/702165/000070216518000011/nsc-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1462047/000168316818000252/pgcg-20171031.xml",
            "https://www.sec.gov/Archives/edgar/data/1442626/000114420418014420/brg-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1432939/000154998318000002/crdx-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/9346/000000934618000017/bwinb-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1065332/000106533218000003/egov-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1058623/000105862318000016/cmls-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/814046/000089262618000046/alplt-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1325814/000132581418000047/fhlbdm-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/922358/000092235818000003/fgp-20180131.xml",
            "https://www.sec.gov/Archives/edgar/data/1651577/000147793218000177/cpst-20171130.xml",
            "https://www.sec.gov/Archives/edgar/data/1402453/000149315218003963/hher-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1488039/000161577418001693/atos-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/61339/000116172818000003/mgee-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1416265/000141626518000173/prosper-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/860543/000147793218000863/cgnd-20170930.xml",
            "https://www.sec.gov/Archives/edgar/data/1137883/000114420418013470/bcli-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1556364/000155636418000014/orm-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1025996/000102599618000107/krc-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1596961/000165495418001935/rmbl-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/823277/000082327718000005/chscp-20171130.xml",
            "https://www.sec.gov/Archives/edgar/data/1314052/000161577418000934/avxl-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/68622/000006862218000004/ctq-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1227268/000119312518098940/ck0001227268-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1528356/000121390018003067/gne-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/945989/000095015918000137/atea-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1505367/000107878218000202/apol-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/865752/000110465918014057/mnst-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1512927/000114420418014999/cuii-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1059784/000160706218000082/gnbt-20180131.xml",
            "https://www.sec.gov/Archives/edgar/data/1708176/000121390018003743/gpaq-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1114995/000156459018005846/pi-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1440024/000144002418000028/rrts-20170630.xml",
            "https://www.sec.gov/Archives/edgar/data/46207/000035470718000024/he-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/703604/000070360418000013/laws-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/935703/000093570318000013/dltr-20180203.xml",
            "https://www.sec.gov/Archives/edgar/data/1537435/000153743518000028/tgen-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/87047/000143774918005707/sbp-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1692063/000169206318000056/sndr-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/1218683/000121390018003682/wizp-20171231.xml",
            "https://www.sec.gov/Archives/edgar/data/100790/000002991518000005/ucc-20171231.xml",
    };

    private static String fileName(String url) {
        int startIndex = url.lastIndexOf("/");
        if (startIndex > 0) {
            int endIndex = url.lastIndexOf('.');
            if (endIndex > 0) {
                return url.substring(startIndex + 1, endIndex) + ".output";
            } else {
                return url.substring(startIndex + 1) + ".output";
            }
        }

        return null;
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testURLs() throws Exception {
        XbrlReader reader = new XbrlReader();
        for (String url : URLS) {
            try {
                System.out.println(url);
                XbrlInstance instance = reader.getInstance(null, url);

                DiscoverableTaxonomySet dts = instance.getTaxonomy();
                Collection<RoleType> roles = dts.getReportableRoleTypes();

                String fileName = fileName(url);

                PrintWriter writer;
                if (fileName != null) {
                    writer = new PrintWriter(Path.of("/tmp", fileName).toFile());
                } else {
                    writer = new PrintWriter(PrintWriter.nullWriter());
                }

                DefinitionNetwork definitionNetwork = new DefinitionNetwork(instance);
                CalculationNetwork calculationNetwork = new CalculationNetwork(instance, new CalculationProcessor() {
                    @Override
                    public void calculationRootStart(CalculationGraphNode root) {
                        CalculationProcessor.super.calculationRootStart(root);
                    }
                });

                PrettyPrinter printer = new PrettyPrinter(writer, true,false, false);
                PresentationNetwork fileWriter = new PresentationNetwork(instance, printer);

                for (RoleType roleType : roles) {
                    writer.printf("{%s}\n", roleType.getDefinition());
                    calculationNetwork.validateCalculation(writer, roleType);
                    definitionNetwork.walk(roleType, new DefinitionTaxonomy.WriterConsumer(writer));
                    fileWriter.process(roleType);
                }

                fileWriter.complete();

                XbrlInstance.Statistics statistics = instance.getStatistics();
                XbrlInstance.UnusedStatistics unusedStatistics = instance.getUnusedStatistics(fileWriter.getFactsUsed());
                printInfo(instance, fileWriter, statistics, unusedStatistics);
                Thread.sleep(1000);
            } catch (AssertionError e) {
                e.printStackTrace();
                reader = new XbrlReader();
            }
        }

        System.out.printf("Cache stats: [request: %d, network: %d, hit: %d]\n",
                reader.requestCount(), reader.networkCount(), reader.hitCount());
    }

    private static void printInfo(XbrlInstance instance, PresentationNetwork presentationNetwork,
                                  XbrlInstance.Statistics statistics, XbrlInstance.UnusedStatistics unusedStatistics) {
        if (statistics.nOfUniqueFacts != presentationNetwork.nOfFactsUsed()) {
            System.out.printf("Facts in instance: [%d], Unique Facts: [%d], [%.2f%%], Facts used in presentation: [%d], [%.2f%%]\n",
                    instance.nOfFacts(), statistics.nOfUniqueFacts, (statistics.nOfUniqueFacts * 100.0 / instance.nOfFacts()),
                    presentationNetwork.nOfFactsUsed(), (presentationNetwork.nOfFactsUsed() * 100.0 / instance.nOfFacts()));
        } else {
            System.out.printf("Facts in instance: [%d], Unique Facts: [%d], [%.2f%%]. All unique facts used in presentation\n",
                    instance.nOfFacts(), statistics.nOfUniqueFacts, (statistics.nOfUniqueFacts * 100.0 / instance.nOfFacts()));
        }

//        System.out.println();
//        System.out.println("Unused Fact Statistics:");
//        System.out.printf("Number of Contexts: [%d]\n", unusedStatistics.nOfContexts);
//        System.out.printf("Number of Facts: [%d]\n", unusedStatistics.nOfFacts);
//        System.out.printf("Number of Concepts: [%d]\n", unusedStatistics.nOfFactConcepts);
    }

    /* Results of running the above URLs
https://www.sec.gov/Archives/edgar/data/1020214/000156459020048950/cers-10q_20200930_htm.xml
Facts in instance: [710], Unique Facts: [678], [95.49%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1228627/000114036120010906/rexn-20200331.xml
Facts in instance: [476], Unique Facts: [476], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1745078/000155335020000525/vynl-20191231.xml
Facts in instance: [398], Unique Facts: [398], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1465470/000165495420007019/shmp-20200331.xml
Facts in instance: [433], Unique Facts: [433], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1609065/000156459020027590/pbhc-20200331.xml
Facts in instance: [2289], Unique Facts: [2289], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1627554/000101738620000165/quest-20200131.xml
Facts in instance: [199], Unique Facts: [199], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/8192/000162828018001324/exc-20171231.xml
Facts in instance: [15851], Unique Facts: [15851], [100.00%], Facts used in presentation: [15838], [99.92%]
https://www.sec.gov/Archives/edgar/data/71180/000108131618000014/bhe-20171231.xml
Facts in instance: [8762], Unique Facts: [8762], [100.00%], Facts used in presentation: [8761], [99.99%]
https://www.sec.gov/Archives/edgar/data/1437402/000155837018002056/ardx-20171231.xml
Facts in instance: [793], Unique Facts: [793], [100.00%], Facts used in presentation: [792], [99.87%]
https://www.sec.gov/Archives/edgar/data/1274173/000104746918001112/jhg-20171231.xml
Facts in instance: [2023], Unique Facts: [2023], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1043121/000165642318000004/bxp-20171231.xml
Facts in instance: [6247], Unique Facts: [6247], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1620179/000104746918001782/xela-20171231.xml
Facts in instance: [1340], Unique Facts: [1340], [100.00%], Facts used in presentation: [1330], [99.25%]
https://www.sec.gov/Archives/edgar/data/1327978/000132797818000020/ck0001327978-20171231.xml
Facts in instance: [1819], Unique Facts: [1819], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1545654/000154565418000014/alex-20171231.xml
Facts in instance: [2114], Unique Facts: [2114], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/896264/000156276220000164/usna-20200328x10q_htm.xml
Facts in instance: [358], Unique Facts: [330], [92.18%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1411168/000137647420000097/dug-20200331_htm.xml
Facts in instance: [124], Unique Facts: [110], [88.71%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1000209/000156459020022990/mfin-20200331.xml
Facts in instance: [1681], Unique Facts: [1681], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/754811/000118518520000645/grow-20200331.xml
Facts in instance: [953], Unique Facts: [953], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1132509/000147793220002386/ekkh-20200331.xml
Facts in instance: [290], Unique Facts: [283], [97.59%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/894560/000155116320000039/both-20200331.xml
Facts in instance: [253], Unique Facts: [253], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1269190/000119312520138643/cta-20200331.xml
Facts in instance: [457], Unique Facts: [457], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/886744/000156459020027307/gern-20200331.xml
Facts in instance: [402], Unique Facts: [402], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1513818/000156459020021944/arav-20200331.xml
Facts in instance: [334], Unique Facts: [334], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1245791/000121390020011182/gidyl-20200331.xml
Facts in instance: [375], Unique Facts: [375], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/925660/000154812320000062/flxt-20200331.xml
Facts in instance: [303], Unique Facts: [303], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1681206/000117494720000638/nodk-20200331.xml
Facts in instance: [1566], Unique Facts: [1566], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/99780/000009978020000046/trn331202010q-q1_htm.xml
Facts in instance: [1930], Unique Facts: [1698], [87.98%], Facts used in presentation: [1692], [87.67%]
https://www.sec.gov/Archives/edgar/data/1390844/000139084420000006/wbc10q3312020_htm.xml
Facts in instance: [742], Unique Facts: [714], [96.23%], Facts used in presentation: [711], [95.82%]
https://www.sec.gov/Archives/edgar/data/1798562/000121390020015197/soac-20200331.xml
Facts in instance: [181], Unique Facts: [181], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1377149/000138713120004906/crvw-20200331.xml
Facts in instance: [528], Unique Facts: [528], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/926282/000156459020023537/adtn-10q_20200331_htm.xml
Facts in instance: [1076], Unique Facts: [996], [92.57%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/277509/000027750920000030/fss-2020331x10q_htm.xml
Facts in instance: [663], Unique Facts: [611], [92.16%], Facts used in presentation: [609], [91.86%]
https://www.sec.gov/Archives/edgar/data/1158420/000110465920061987/hgsh-20200331.xml
Facts in instance: [585], Unique Facts: [546], [93.33%], Facts used in presentation: [545], [93.16%]
https://www.sec.gov/Archives/edgar/data/1638290/000156459020023454/mcft-20200329.xml
Facts in instance: [697], Unique Facts: [697], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1599947/000159994720000022/terp-20191231_htm.xml
Facts in instance: [35], Unique Facts: [34], [97.14%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1618835/000161883520000105/evfm-20200331.xml
Facts in instance: [574], Unique Facts: [574], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1611647/000156459020021307/frpt-10q_20200331_htm.xml
Facts in instance: [449], Unique Facts: [434], [96.66%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1540729/000156459020015553/felp-20191231.xml
Facts in instance: [1394], Unique Facts: [1394], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1472787/000156459020019332/faf-10q_20200331_htm.xml
Facts in instance: [1376], Unique Facts: [1251], [90.92%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1413754/000107878220000339/marz-20200331.xml
Facts in instance: [257], Unique Facts: [257], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1599617/000156459020021642/dnow-10q_20200331_htm.xml
Facts in instance: [400], Unique Facts: [371], [92.75%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/108312/000010831220000006/wwd-20200331x10q_htm.xml
Facts in instance: [1816], Unique Facts: [1651], [90.91%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1753391/000175339120000008/none-20200430.xml
Facts in instance: [199], Unique Facts: [199], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1504167/000110465920072899/tmbr-20200430.xml
Facts in instance: [464], Unique Facts: [452], [97.41%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/818479/000081847920000023/xray-20200331_htm.xml
Facts in instance: [994], Unique Facts: [925], [93.06%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1395942/000139594220000072/karq1202010-q_htm.xml
Facts in instance: [637], Unique Facts: [581], [91.21%], Facts used in presentation: [572], [89.80%]
https://www.sec.gov/Archives/edgar/data/896493/000121465920005205/dpw-20191231.xml
Facts in instance: [2121], Unique Facts: [2121], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/57725/000155837020005713/lci-20200331.xml
Facts in instance: [1088], Unique Facts: [996], [91.54%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/711772/000156459020023671/catc-10q_20200331_htm.xml
Facts in instance: [1481], Unique Facts: [1402], [94.67%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/350894/000035089420000026/seic-331202010q_htm.xml
Facts in instance: [912], Unique Facts: [821], [90.02%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/20639/000114036120010960/abcp-20200331.xml
Facts in instance: [178], Unique Facts: [178], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/788329/000114036120010697/jout-20200327.xml
Facts in instance: [771], Unique Facts: [771], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1506439/000165495420005620/shsp-20200331.xml
Facts in instance: [543], Unique Facts: [543], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1061027/000156459020022937/snss-20200331.xml
Facts in instance: [317], Unique Facts: [317], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/717605/000156459020017486/hxl-10q_20200331_htm.xml
Facts in instance: [642], Unique Facts: [591], [92.06%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1326321/000089424520000015/aei26-20200331.xml
Facts in instance: [177], Unique Facts: [177], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/12400/000001240020000004/bhp10qq12020_htm.xml
Facts in instance: [437], Unique Facts: [409], [93.59%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1794621/000119312520138270/ccac-20200331.xml
Facts in instance: [254], Unique Facts: [254], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/31235/000156459020024666/kodk-20200331.xml
Facts in instance: [880], Unique Facts: [880], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/812128/000081212820000010/safm-20200430_htm.xml
Facts in instance: [512], Unique Facts: [480], [93.75%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/797564/000167479620000011/hstc-20191231_htm.xml
Facts in instance: [213], Unique Facts: [206], [96.71%], Facts used in presentation: [205], [96.24%]
https://www.sec.gov/Archives/edgar/data/1590750/000159075020000053/mgen-20200331.xml
Facts in instance: [548], Unique Facts: [548], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1067983/000156459020020599/brka-10q_20200331_htm.xml
Facts in instance: [1331], Unique Facts: [1271], [95.49%], Facts used in presentation: [1268], [95.27%]
https://www.sec.gov/Archives/edgar/data/1324404/000132440420000013/cf-03312020x10q_htm.xml
Facts in instance: [877], Unique Facts: [803], [91.56%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/74046/000007404620000029/odc10-q4302020_htm.xml
Facts in instance: [908], Unique Facts: [826], [90.97%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1646228/000155837020006844/home-20200519x10k_htm.xml
Facts in instance: [987], Unique Facts: [934], [94.63%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1681087/000156459020022815/avro-20200331.xml
Facts in instance: [364], Unique Facts: [364], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1376321/000117184320004597/cnet-20200331.xml
Facts in instance: [908], Unique Facts: [908], [100.00%], Facts used in presentation: [906], [99.78%]
https://www.sec.gov/Archives/edgar/data/1626745/000149315220009135/fvti-20200331.xml
Facts in instance: [351], Unique Facts: [351], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1376793/000168316820001684/cvat-20200331.xml
Facts in instance: [345], Unique Facts: [345], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/215466/000021546620000102/cde-03312010q_htm.xml
Facts in instance: [1783], Unique Facts: [1552], [87.04%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1366868/000136686820000045/gsat-20200331.xml
Facts in instance: [601], Unique Facts: [601], [100.00%], Facts used in presentation: [600], [99.83%]
https://www.sec.gov/Archives/edgar/data/1275014/000156459020021826/uctt-10q_20200327_htm.xml
Facts in instance: [742], Unique Facts: [701], [94.47%], Facts used in presentation: [700], [94.34%]
https://www.sec.gov/Archives/edgar/data/105418/000010541820000016/wmk-20200328.xml
Facts in instance: [336], Unique Facts: [336], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1518336/000149315220009677/drem-20191231.xml
Facts in instance: [455], Unique Facts: [455], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1750777/000147793220003601/hwke-20200331.xml
Facts in instance: [738], Unique Facts: [738], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/60714/000156459020022839/lxu-20200331.xml
Facts in instance: [498], Unique Facts: [498], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/703604/000070360420000042/laws-20200331.xml
Facts in instance: [418], Unique Facts: [418], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/352915/000156459020023579/uhs-10q_20200331_htm.xml
Facts in instance: [929], Unique Facts: [881], [94.83%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/794170/000079417020000039/tol-20200430_htm.xml
Facts in instance: [2532], Unique Facts: [2264], [89.42%], Facts used in presentation: [2260], [89.26%]
https://www.sec.gov/Archives/edgar/data/1594466/000159446620000076/pe-20200331_htm.xml
Facts in instance: [931], Unique Facts: [867], [93.13%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/915912/000091591220000018/q1202010-q_htm.xml
Facts in instance: [927], Unique Facts: [841], [90.72%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1684144/000117184320003586/zom-20200331.xml
Facts in instance: [791], Unique Facts: [791], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1314772/000110465920060206/sumr-20200328.xml
Facts in instance: [353], Unique Facts: [341], [96.60%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1530766/000118518520000595/bsgm-20200331.xml
Facts in instance: [667], Unique Facts: [667], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1046568/000156459020022879/prdo-10q_20200331_htm.xml
Facts in instance: [678], Unique Facts: [638], [94.10%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1419275/000118518520000436/grbx-20190930.xml
Facts in instance: [664], Unique Facts: [664], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1728117/000156459020024690/goss-20200331.xml
Facts in instance: [491], Unique Facts: [491], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1069157/000106915720000040/ewbc10q3312020_htm.xml
Facts in instance: [3025], Unique Facts: [2815], [93.06%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1581164/000162828020006700/stay-20200331_htm.xml
Facts in instance: [1533], Unique Facts: [1377], [89.82%], Facts used in presentation: [1375], [89.69%]
https://www.sec.gov/Archives/edgar/data/1094285/000109428520000068/tdy-20200329_htm.xml
Facts in instance: [770], Unique Facts: [693], [90.00%], Facts used in presentation: [692], [89.87%]
https://www.sec.gov/Archives/edgar/data/20212/000002021220000058/chdn-20200331_htm.xml
Facts in instance: [817], Unique Facts: [738], [90.33%], Facts used in presentation: [734], [89.84%]
https://www.sec.gov/Archives/edgar/data/1638287/000155837020006879/nrbo-20200331.xml
Facts in instance: [398], Unique Facts: [377], [94.72%], Facts used in presentation: [374], [93.97%]
https://www.sec.gov/Archives/edgar/data/831489/000110465920053508/scrh-20190331.xml
Facts in instance: [316], Unique Facts: [287], [90.82%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1005286/000100528620000004/lndc-20200531_htm.xml
Facts in instance: [1592], Unique Facts: [1468], [92.21%], Facts used in presentation: [1467], [92.15%]
https://www.sec.gov/Archives/edgar/data/874710/000110465920109282/ahpi-20200630.xml
Facts in instance: [715], Unique Facts: [661], [92.45%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1091883/000109188320000110/cirq20628202010q_htm.xml
Facts in instance: [1066], Unique Facts: [975], [91.46%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/793306/000165495420009186/bdco-20200630.xml
Facts in instance: [908], Unique Facts: [908], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1579298/000156459020041539/burl-10q_20200801_htm.xml
Facts in instance: [905], Unique Facts: [848], [93.70%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1676725/000156459020039719/idya-20200630.xml
Facts in instance: [573], Unique Facts: [573], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1555280/000155528020000261/zts-20200630_htm.xml
Facts in instance: [1239], Unique Facts: [1082], [87.33%], Facts used in presentation: [1076], [86.84%]
https://www.sec.gov/Archives/edgar/data/1766502/000176650219000013/chwyq2201910-q_htm.xml
Facts in instance: [451], Unique Facts: [420], [93.13%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/108516/000156459019027053/wor-20190531.xml
Facts in instance: [1819], Unique Facts: [1819], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/4457/000000445718000005/uhal-20171231.xml
Facts in instance: [2159], Unique Facts: [2159], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/75488/000100498018000003/pcg-20171231.xml
Facts in instance: [2417], Unique Facts: [2417], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1024126/000113626118000075/ptx-20171231.xml
Facts in instance: [978], Unique Facts: [978], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/37637/000132616018000034/duk-20171231.xml
Facts in instance: [9754], Unique Facts: [9754], [100.00%], Facts used in presentation: [9753], [99.99%]
https://www.sec.gov/Archives/edgar/data/1614184/000156459018006217/cade-20171231.xml
Facts in instance: [2589], Unique Facts: [2589], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/40211/000004021118000023/gmt-20171231.xml
Facts in instance: [1978], Unique Facts: [1978], [100.00%], Facts used in presentation: [1975], [99.85%]
https://www.sec.gov/Archives/edgar/data/1065598/000164033418000086/dgtw-20171130.xml
Facts in instance: [652], Unique Facts: [652], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1586049/000143774918003397/oxfd-20171231.xml
Facts in instance: [1070], Unique Facts: [1070], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/40570/000147793218000832/gee-20171231.xml
Facts in instance: [563], Unique Facts: [563], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/700564/000070056418000010/fult-20171231.xml
Facts in instance: [3155], Unique Facts: [3155], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1473597/000117184318002020/stri-20171231.xml
Facts in instance: [689], Unique Facts: [689], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1666114/000121390018001849/unl-20171231.xml
Facts in instance: [168], Unique Facts: [168], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1594864/000159486418000005/juno-20171231.xml
Facts in instance: [1283], Unique Facts: [1283], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/702165/000070216518000011/nsc-20171231.xml
Facts in instance: [1321], Unique Facts: [1321], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1462047/000168316818000252/pgcg-20171031.xml
Facts in instance: [670], Unique Facts: [670], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1442626/000114420418014420/brg-20171231.xml
Facts in instance: [3150], Unique Facts: [3150], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1432939/000154998318000002/crdx-20171231.xml
Facts in instance: [161], Unique Facts: [161], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/9346/000000934618000017/bwinb-20171231.xml
Facts in instance: [2065], Unique Facts: [2065], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1065332/000106533218000003/egov-20171231.xml
Facts in instance: [939], Unique Facts: [939], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1058623/000105862318000016/cmls-20171231.xml
Facts in instance: [2044], Unique Facts: [2044], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/814046/000089262618000046/alplt-20171231.xml
Facts in instance: [139], Unique Facts: [139], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1325814/000132581418000047/fhlbdm-20171231.xml
Facts in instance: [2199], Unique Facts: [2199], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/922358/000092235818000003/fgp-20180131.xml
Facts in instance: [2476], Unique Facts: [2476], [100.00%], Facts used in presentation: [2472], [99.84%]
https://www.sec.gov/Archives/edgar/data/1651577/000147793218000177/cpst-20171130.xml
Facts in instance: [168], Unique Facts: [168], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1402453/000149315218003963/hher-20171231.xml
Facts in instance: [430], Unique Facts: [430], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1488039/000161577418001693/atos-20171231.xml
Facts in instance: [550], Unique Facts: [550], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/61339/000116172818000003/mgee-20171231.xml
Facts in instance: [2926], Unique Facts: [2926], [100.00%], Facts used in presentation: [2924], [99.93%]
https://www.sec.gov/Archives/edgar/data/1416265/000141626518000173/prosper-20171231.xml
Facts in instance: [2039], Unique Facts: [2039], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/860543/000147793218000863/cgnd-20170930.xml
Facts in instance: [212], Unique Facts: [212], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1137883/000114420418013470/bcli-20171231.xml
Facts in instance: [510], Unique Facts: [510], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1556364/000155636418000014/orm-20171231.xml
Facts in instance: [1736], Unique Facts: [1736], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1025996/000102599618000107/krc-20171231.xml
Facts in instance: [3200], Unique Facts: [3200], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1596961/000165495418001935/rmbl-20171231.xml
Facts in instance: [461], Unique Facts: [461], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/823277/000082327718000005/chscp-20171130.xml
Facts in instance: [819], Unique Facts: [819], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1314052/000161577418000934/avxl-20171231.xml
Facts in instance: [383], Unique Facts: [383], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/68622/000006862218000004/ctq-20171231.xml
Facts in instance: [653], Unique Facts: [653], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1227268/000119312518098940/ck0001227268-20171231.xml
Facts in instance: [343], Unique Facts: [343], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1528356/000121390018003067/gne-20171231.xml
Facts in instance: [1431], Unique Facts: [1431], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/945989/000095015918000137/atea-20171231.xml
Facts in instance: [209], Unique Facts: [209], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1505367/000107878218000202/apol-20171231.xml
Facts in instance: [181], Unique Facts: [181], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/865752/000110465918014057/mnst-20171231.xml
Facts in instance: [1305], Unique Facts: [1305], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1512927/000114420418014999/cuii-20171231.xml
Facts in instance: [1636], Unique Facts: [1636], [100.00%], Facts used in presentation: [1630], [99.63%]
https://www.sec.gov/Archives/edgar/data/1059784/000160706218000082/gnbt-20180131.xml
Facts in instance: [597], Unique Facts: [597], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1708176/000121390018003743/gpaq-20171231.xml
Facts in instance: [169], Unique Facts: [169], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1114995/000156459018005846/pi-20171231.xml
Facts in instance: [897], Unique Facts: [897], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1440024/000144002418000028/rrts-20170630.xml
Facts in instance: [584], Unique Facts: [584], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/46207/000035470718000024/he-20171231.xml
Facts in instance: [6166], Unique Facts: [6166], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/703604/000070360418000013/laws-20171231.xml
Facts in instance: [877], Unique Facts: [877], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/935703/000093570318000013/dltr-20180203.xml
Facts in instance: [1795], Unique Facts: [1795], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1537435/000153743518000028/tgen-20171231.xml
Facts in instance: [773], Unique Facts: [773], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/87047/000143774918005707/sbp-20171231.xml
Facts in instance: [524], Unique Facts: [524], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1692063/000169206318000056/sndr-20171231.xml
Facts in instance: [1034], Unique Facts: [1034], [100.00%]. All unique facts used in presentation
https://www.sec.gov/Archives/edgar/data/1218683/000121390018003682/wizp-20171231.xml
Facts in instance: [631], Unique Facts: [631], [100.00%], Facts used in presentation: [627], [99.37%]
https://www.sec.gov/Archives/edgar/data/100790/000002991518000005/ucc-20171231.xml
Facts in instance: [1266], Unique Facts: [1266], [100.00%], Facts used in presentation: [1263], [99.76%]
Cache stats: [request: 8919, network: 1789, hit: 7130]
     */
}
