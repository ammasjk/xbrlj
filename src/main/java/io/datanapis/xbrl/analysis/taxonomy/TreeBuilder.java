package io.datanapis.xbrl.analysis.taxonomy;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.datanapis.xbrl.analysis.PresentationGraphNode;

import java.util.*;

public class TreeBuilder {
    record Key(String name, String labelType, int pathHash) {}

    private final HashFunction hashFunction;
    private final StringBuilder builder;
    private final String startingConcept;
    private final Set<String> allowedDuplicates;
    private final Tree tree;
    private TreeNode root;
    private final Map<Key,TreeNode> nodeMap;

    public TreeBuilder(String title, String startingConcept, Set<String> allowedDuplicates) {
        this.hashFunction = Hashing.murmur3_32_fixed();
        this.builder = new StringBuilder();
        this.startingConcept = startingConcept;
        this.allowedDuplicates = allowedDuplicates;
        this.tree = new Tree(title);
        this.nodeMap = new HashMap<>();
    }

    public void makeRoot(PresentationGraphNode node) {
        /*
         * pathHash is needed to distinguish occurrences of the same qualifiedName in different parts of the tree.
         * We are hashing the path to the node and using it to distinguish between the different occurrences. The
         * pathHash for the root node is the hash value of the "" string.
         */
        int pathHash = hashFunction.hashUnencodedChars("").asInt();
        String labelType = node.getArc() != null ? node.getArc().getPreferredLabelType() : null;
        TreeNode root = new TreeNode(node.getQualifiedName(), node.getConcept().getBalance().toString(), labelType, pathHash);
        this.root = root;

        Key key = new Key(node.getQualifiedName(), labelType, pathHash);
        TreeNode prev = nodeMap.put(key, root);
        assert prev == null;
    }

    public void addNode(PresentationGraphNode node, Deque<PresentationGraphNode> path) {
        /*
         * Convert the deque to an array. We need to calculate the pathHash of all entries from 0 until pathArray.length - 1.
         * This becomes the path hash for the parent node.
         *
         * The pathHash for the child is the hash of the complete path. We are using '>' to separate entries. We are also
         * skipping entries until us-gaap:StatementLineItems. Meaning, we are only considering entries rooted at
         * us-gaap:StatementLineItems.
         */
        PresentationGraphNode[] pathArray = path.toArray(new PresentationGraphNode[0]);
        assert pathArray.length > 0;

        /* Reset builder */
        builder.setLength(0);

        /* Skip nodes before startingConcept which is usually us-gaap:StatementLineItems */
        int i = pathArray.length - 1;
        if (startingConcept != null) {
            while (i >= 0) {
                if (pathArray[i].getQualifiedName().equals(startingConcept))
                    break;
                --i;
            }
        }

        /* Compute the pathHash for the parent and get the parent's TreeNode */
        for (; i > 0; i--) {
            if (!builder.isEmpty())
                builder.append('>');
            builder.append(pathArray[i].getQualifiedName());
        }
        int parentHash = hashFunction.hashUnencodedChars(builder.toString()).asInt();
        String parentLabelType = pathArray[0].getArc() != null ? pathArray[0].getArc().getPreferredLabelType() : null;
        Key parentKey = new Key(pathArray[0].getQualifiedName(), parentLabelType, parentHash);
        TreeNode parentNode = nodeMap.get(parentKey);
        assert parentNode != null;

        /* Compute the pathHash for the node. Append '>' if necessary */
        if (!builder.isEmpty()) {
            builder.append('>');
        }
        builder.append(pathArray[0].getQualifiedName());
        int nodeHash = hashFunction.hashUnencodedChars(builder.toString()).asInt();
        String nodeLabelType = node.getArc() != null ? node.getArc().getPreferredLabelType() : null;
        Key nodeKey = new Key(node.getQualifiedName(), nodeLabelType, nodeHash);

        /* Create and add a TreeNode for node and add it as a child of parentNode */
        TreeNode treeNode = new TreeNode(node.getQualifiedName(), node.getConcept().getBalance().toString(), node.getArc().getPreferredLabelType(), nodeHash);
        TreeNode prev = nodeMap.get(nodeKey);
        if (prev != null) {
            /* The 2012 taxonomy has a few nodes duplicated. They are definitionally the same just duplicated. This is only an issue for the 2012 taxonomy */
            if (allowedDuplicates == null || !allowedDuplicates.contains(nodeKey.name())) {
                throw new RuntimeException("Duplicate node [" + nodeKey.name() + "]");
            }
        } else {
            nodeMap.put(nodeKey, treeNode);
            parentNode.addChild(treeNode);
        }
    }

    public Tree build() {
        if (root != null) {
            root.computeSubTreeBalances();
            tree.setRoot(root);
            return tree;
        } else {
            return null;
        }
    }
}
