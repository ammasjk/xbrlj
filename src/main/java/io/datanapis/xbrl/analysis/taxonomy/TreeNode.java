package io.datanapis.xbrl.analysis.taxonomy;

import com.google.gson.annotations.Expose;
import lombok.Getter;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Getter
public class TreeNode {

    public enum Balance {
        NONE,
        DEBIT,
        CREDIT;

        public String toString() {
            return name();
        }
    }
    private static Balance as(String value) {
        return switch(value) {
            case "none", "NONE" -> Balance.NONE;
            case "credit", "CREDIT" -> Balance.CREDIT;
            case "debit", "DEBIT" -> Balance.DEBIT;
            default -> throw new RuntimeException("Invalid balance [" + value + "]");
        };
    }

    @Expose
    private final String name;            // qualified concept name
    @Expose
    private final int pathHash;           // path hash, used to distinguish between the same node appearing in different locations in the tree
    @Expose
    private final Balance balance;
    @Expose
    private final String labelType;
    @Expose
    private Balance subTreeBalance;
    @Expose
    private List<TreeNode> children;
    private TreeNode parent;    /* Not persisted */

    TreeNode(String name, String balance, String labelType, int pathHash) {
        this.name = name;
        this.pathHash = pathHash;
        this.balance = as(balance);
        this.labelType = labelType;
        this.children = null;
    }

    public String name() {
        return this.name;
    }

    public Balance balance() {
        return this.balance;
    }

    public String labelType() {
        return this.labelType;
    }

    public Balance subTreeBalance() {
        return this.subTreeBalance;
    }

    public Collection<TreeNode> children() {
        return this.children;
    }

    void setParent(TreeNode parent) {
        this.parent = parent;
    }

    void addChild(TreeNode node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        this.children.add(node);
        node.setParent(this);
    }

    void connect() {
        if (children == null)
            return;

        for (TreeNode child : children) {
            child.setParent(this);
            child.connect();
        }
    }

    void collectNodes(Collection<String> collection, Predicate<TreeNode> traversalCondition) {
        traverse((level, node) -> collection.add(node.name()), traversalCondition);
    }

    void nodeIndex(Tree tree, Map<String,List<NodeEntry>> map, Predicate<TreeNode> traversalCondition) {
        traverse((level, node) -> {
            List<NodeEntry> nodes = map.computeIfAbsent(node.name(), k -> new ArrayList<>());
            nodes.add(new NodeEntry(tree, node));
        }, traversalCondition);
    }

    void computeSubTreeBalances() {
        Map<Balance,Integer> map = new HashMap<>();
        this.computeSubTreeBalances(map);
    }

    void print(PrintWriter writer, Predicate<TreeNode> filterCondition, Predicate<TreeNode> traversalCondition) {
        this.print(1, writer, filterCondition, traversalCondition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode node = (TreeNode) o;
        return Objects.equals(name, node.name) && Objects.equals(labelType, node.labelType) && pathHash == node.pathHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pathHash, labelType);
    }

    @Override
    public String toString() {
        return "(" + name + ", " + balance + ", " + labelType + ")";
    }

    private void traverse(int level, BiConsumer<Integer,TreeNode> consumer, Predicate<TreeNode> traversalCondition) {
        boolean visit = traversalCondition.test(this);
        if (!visit)
            return;

        consumer.accept(level, this);
        if (children != null) {
            for (TreeNode child : children) {
                child.traverse(level + 1, consumer, traversalCondition);
            }
        }
    }

    private void traverse(BiConsumer<Integer, TreeNode> consumer, Predicate<TreeNode> traversalCondition) {
        traverse(1, consumer, traversalCondition);
    }

    private void computeSubTreeBalances(Map<Balance,Integer> map) {
        if (children != null) {
            Map<Balance,Integer> childMap = new HashMap<>();
            for (TreeNode child : children()) {
                child.computeSubTreeBalances(childMap);
            }
            if (childMap.size() == 1) {
                Balance b = childMap.keySet().iterator().next();
                subTreeBalance = (b != Balance.NONE) ? b : null;
            } else if (childMap.size() == 2) {
                Iterator<Balance> iterator = childMap.keySet().iterator();
                Balance b1 = iterator.next();
                Balance b2 = iterator.next();
                if (b1 == Balance.NONE) {
                    subTreeBalance = b2;
                } else if (b2 == Balance.NONE) {
                    subTreeBalance = b1;
                } else {
                    subTreeBalance = null;
                }
            }
            for (Map.Entry<Balance,Integer> entry : childMap.entrySet()) {
                map.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        } else {
            subTreeBalance = balance;
        }
        map.merge(balance, 1, Integer::sum);
    }

    private void print(int level, PrintWriter writer, Predicate<TreeNode> filterCondition, Predicate<TreeNode> traversalCondition) {
        boolean visit = traversalCondition.test(this);
        boolean print = filterCondition.test(this);
        if (visit && print)
            writer.printf("%s[%d]%s [%s][%d]%s\n", " ".repeat(4 * (level - 1)), level, name, balance.name(), pathHash, (children == null) ? "" : ":");
        if (visit && children != null) {
            for (TreeNode child : children) {
                child.print(level + 1, writer, filterCondition, traversalCondition);
            }
        }
    }
}
