package io.datanapis.test;

import io.datanapis.xbrl.analysis.data.XbrlTaxonomyPath;
import io.datanapis.xbrl.analysis.taxonomy.NodeEntry;
import io.datanapis.xbrl.analysis.taxonomy.Taxonomy;
import io.datanapis.xbrl.analysis.taxonomy.TreeNode;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;

public class TaxonomyTest {

    @Test
    public void writeSingle() throws Exception {
        writeSingle(XbrlTaxonomyPath.T2012);
    }

    @Test
    public void writeAll() throws Exception {
        for (XbrlTaxonomyPath path : XbrlTaxonomyPath.values()) {
            writeSingle(path);
            Thread.sleep(5000);
        }
    }

    @Test
    public void readSingle() {
        Taxonomy taxonomy = readSingle(XbrlTaxonomyPath.T2021);
        if (taxonomy != null) {
            System.out.println("Read taxonomy");
            Map<String,List<NodeEntry>> nodeIndex = taxonomy.getIncome().get(0).nodeIndex(treeNode -> true);
            Set<String> concepts = new HashSet<>();
            taxonomy.getIncome().get(0).collectNodes(concepts, node -> !node.getName().equals("us-gaap:PartnershipIncomeAbstract"));
            try (PrintWriter writer = new PrintWriter("/tmp/sample.txt")) {
                taxonomy.getIncome().get(0).print(writer, node -> node.children() != null, node -> !node.getName().equals("us-gaap:PartnershipIncomeAbstract"));
            } catch (IOException ignored) {
            }
            int y = 5;
        }
    }

    @Test
    public void readAll() {
        for (XbrlTaxonomyPath path : XbrlTaxonomyPath.values()) {
            Taxonomy taxonomy = readSingle(path);
            if (taxonomy != null)
                System.out.println("Read taxonomy [" + path.name() + "]");
        }
    }

    private void writeSingle(XbrlTaxonomyPath path) throws Exception {
        Taxonomy.writeTaxonomy(path, Path.of(path.getJsonGzPath()));
    }

    private Taxonomy readSingle(XbrlTaxonomyPath path) {
        return Taxonomy.readTaxonomy(Path.of(path.getJsonGzPath()));
    }
}
