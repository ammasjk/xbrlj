package io.datanapis.xbrl.analysis.taxonomy;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Getter @Setter
public class Tree {
    @Expose
    private String title;
    @Expose
    private TreeNode root;

    public Tree() {
        this.title = null;
        this.root = null;
    }

    public Tree(String title) {
        this.title = title;
        this.root = null;
    }

    public Tree(String title, TreeNode root) {
        this.title = title;
        this.root = root;
    }

    public Map<String, List<NodeEntry>> nodeIndex(Predicate<TreeNode> traversalCondition) {
        Map<String,List<NodeEntry>> map = new HashMap<>();
        if (root != null) {
            root.nodeIndex(this, map, traversalCondition);
        }
        return map;
    }

    public void collectNodes(Collection<String> collection, Predicate<TreeNode> traversalCondition) {
        if (root != null)
            root.collectNodes(collection, traversalCondition);
    }

    public void print(PrintWriter writer, Predicate<TreeNode> filterCondition, Predicate<TreeNode> traversalCondition) {
        writer.println(title + ":");
        if (root != null)
            root.print(writer, filterCondition, traversalCondition);
    }
}
