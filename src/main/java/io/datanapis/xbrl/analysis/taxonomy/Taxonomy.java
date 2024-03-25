package io.datanapis.xbrl.analysis.taxonomy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.datanapis.xbrl.DiscoverableTaxonomySet;
import io.datanapis.xbrl.XbrlReader;
import io.datanapis.xbrl.analysis.PresentationGraphNode;
import io.datanapis.xbrl.analysis.PresentationNetworkConsumer;
import io.datanapis.xbrl.analysis.PresentationTaxonomy;
import io.datanapis.xbrl.analysis.data.XbrlTaxonomyPath;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.RoleType;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Getter @Setter
public class Taxonomy {
    @Expose
    private List<Tree> income;
    @Expose
    private List<Tree> financialPosition;
    @Expose
    private List<Tree> cashFlow;
    @Expose
    private List<Tree> other;

    public Taxonomy() {
        this.income = new ArrayList<>();
        this.financialPosition = new ArrayList<>();
        this.cashFlow = new ArrayList<>();
        this.other = new ArrayList<>();
    }

    public Collection<Tree> income() {
        return this.income;
    }

    public Map<String,List<NodeEntry>> incomeIndex(Predicate<TreeNode> traversalCondition) {
        return getIndex(this.income, traversalCondition);
    }

    public Collection<Tree> financialPosition() {
        return this.financialPosition;
    }

    public Map<String,List<NodeEntry>> financialPositionIndex(Predicate<TreeNode> traversalCondition) {
        return getIndex(this.financialPosition, traversalCondition);
    }

    public Collection<Tree> cashFlow() {
        return this.cashFlow;
    }

    public Map<String,List<NodeEntry>> cashFlowIndex(Predicate<TreeNode> traversalCondition) {
        return getIndex(this.cashFlow, traversalCondition);
    }

    public Collection<Tree> other() {
        return this.other;
    }
    public Map<String,List<NodeEntry>> otherIndex(Predicate<TreeNode> traversalCondition) {
        return getIndex(this.other, traversalCondition);
    }

    private static Map<String,List<NodeEntry>> getIndex(Collection<Tree> trees, Predicate<TreeNode> traversalCondition) {
        Map<String,List<NodeEntry>> map = new HashMap<>();
        for (Tree tree : trees) {
            tree.getRoot().nodeIndex(tree, map, traversalCondition);
        }
        return map;
    }

    private static final String DISCLOSURE = "disclosure";
    private static final String STATEMENT = "statement";

    private static Gson gson() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
//      builder.setPrettyPrinting();
        return builder.create();
    }

    public static void writeTaxonomy(XbrlTaxonomyPath xbrlTaxonomyPath, Path path) throws Exception {
        XbrlReader reader = new XbrlReader();
        DiscoverableTaxonomySet dts = reader.getTaxonomy(xbrlTaxonomyPath.toString(), true);

        Taxonomy taxonomy = new Taxonomy();
        PresentationTaxonomy presentationTaxonomy = new PresentationTaxonomy(dts);
        Collection<RoleType> allRoles = dts.getAllRoleTypes();
        for (RoleType roleType : allRoles) {
            String roleURI = roleType.getRoleURI().toLowerCase();
            boolean isDisclosure = roleURI.contains(DISCLOSURE);
            boolean isStatement = roleURI.contains(STATEMENT);

            String title = roleType.getDefinition();
            boolean income = false, financialPosition = false, cashFlow = false;
            Meta meta = isIncome(title);
            if (meta == null) {
                meta = isFinancialPosition(title);
                if (meta == null) {
                    meta = isCashFlow(title);
                    if (meta == null) {
                        meta = isOther(title);
                    } else {
                        cashFlow = true;
                    }
                } else {
                    financialPosition = true;
                }
            } else {
                income = true;
            }

            if (roleType.getPresentationLink() != null && meta != null) {
                if (isStatement && !isDisclosure) {
                    Meta m = meta;
                    TreeBuilder treeBuilder = new TreeBuilder(title, m.startingConcept(), xbrlTaxonomyPath.allowedDuplicates());
                    presentationTaxonomy.walk(roleType, new PresentationNetworkConsumer() {
                        private boolean hasRoot = false;

                        @Override
                        public void rootStart(PresentationGraphNode n) {
                            if (m.startingConcept == null) {
                                treeBuilder.makeRoot(n);
                                hasRoot = true;
                            }
                        }

                        @Override
                        public void nodeStart(PresentationGraphNode n, Deque<PresentationGraphNode> p) {
                            Concept concept = n.getConcept();
                            if (!hasRoot && concept.getQualifiedName().equals(m.startingConcept())) {
                                treeBuilder.makeRoot(n);
                                hasRoot = true;
                            } else if (hasRoot) {
                                treeBuilder.addNode(n, p);
                            }
                        }
                    });

                    Tree tree = treeBuilder.build();
                    if (tree != null) {
                        if (income) {
                            taxonomy.income().add(tree);
                        } else if (financialPosition) {
                            taxonomy.financialPosition().add(tree);
                        } else if (cashFlow) {
                            taxonomy.cashFlow().add(tree);
                        } else {
                            taxonomy.other().add(tree);
                        }
                    }
                }
            }
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
             Writer writer = new OutputStreamWriter(gzipOutputStream)) {

            Gson gson = gson();
            gson.toJson(taxonomy, writer);
        }
    }

    public static Taxonomy readTaxonomy(InputStream inputStream) {
        if (inputStream == null)
            return null;

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
             InputStreamReader reader = new InputStreamReader(gzipInputStream)) {
            Gson gson = gson();
            Taxonomy taxonomy = gson.fromJson(reader, Taxonomy.class);
            taxonomy.getIncome().forEach(tree -> tree.getRoot().connect());
            taxonomy.getFinancialPosition().forEach(tree -> tree.getRoot().connect());
            taxonomy.getCashFlow().forEach(tree -> tree.getRoot().connect());
            return taxonomy;
        } catch (IOException e) {
            return null;
        }
    }

    public static Taxonomy readTaxonomy(Path path) {
        try (FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
            return readTaxonomy(fileInputStream);
        } catch (IOException e) {
            return null;
        }
    }

    record Meta(String title, String startingConcept) {}

    private static final String STATEMENT_LINE_ITEMS = "us-gaap:StatementLineItems";

    private static final List<Meta> INCOME_STATEMENTS = List.of(
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncome", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeSecuritiesBasedIncome", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeRealEstateInvestmentTrusts", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeRealEstateExcludingREITs", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeInterestBasedRevenue", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeInsuranceBasedRevenue", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfIncomeAlternative", null)
    );
    private static final List<Meta> FINANCIAL_POSITION_STATEMENTS = List.of(
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-SecuritiesBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-RealEstateOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-InvestmentBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionUnclassified-DepositBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionClassified", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfFinancialPositionClassified-RealEstateOperations", null)
    );
    private static final List<Meta> CASH_FLOW_STATEMENTS = List.of(
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirect", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectSecuritiesBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectRealEstate", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectInvestmentBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectDepositBasedOperations", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfCashFlowsIndirectAdditionalElements", null)
    );
    private static final List<Meta> OTHER_STATEMENTS = List.of(
            new Meta("http://fasb.org/us-gaap/role/statement/CommonDomainMembers", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfOtherComprehensiveIncome", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfOtherComprehensiveIncomeAlternative", null),
            new Meta("http://fasb.org/us-gaap/role/statement/StatementOfShareholdersEquityAndOtherComprehensiveIncome", null)
    );

    private static Meta isCategory(String title, List<Meta> list) {
        List<Meta> matching = list.stream().filter(meta -> meta.title().equals(title)).toList();
        if (matching.isEmpty())
            return null;
        assert matching.size() == 1;
        return matching.get(0);
    }
    private static Meta isIncome(String title) {
        return isCategory(title, INCOME_STATEMENTS);
    }
    private static Meta isFinancialPosition(String title) {
        return isCategory(title, FINANCIAL_POSITION_STATEMENTS);
    }
    private static Meta isCashFlow(String title) {
        return isCategory(title, CASH_FLOW_STATEMENTS);
    }
    private static Meta isOther(String title) {
        return isCategory(title, OTHER_STATEMENTS);
    }
}
