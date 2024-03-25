package io.datanapis.xbrl.analysis.text;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

public class FormattingVisitor implements NodeVisitor {
    private final StringBuilder accum; // holds the accumulated text

    public FormattingVisitor(StringBuilder accum) {
        this.accum = accum;
    }

    // hit when the node is first seen
    public void head(Node node, int depth) {
        String name = node.nodeName();
        if (node instanceof TextNode)
            append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
        else if (name.equals("li"))
            append("\n * ");
        else if (name.equals("dt"))
            append("  ");
        else if (TextUtils.HEAD_TAGS.contains(name))
            append("\n");
    }

    // hit when all the node's children (if any) have been visited
    public void tail(Node node, int depth) {
        String name = node.nodeName();
        if (TextUtils.TAIL_TAGS.contains(name))
            append("\n");
    }

    private void addSeparator(int i, int newLines) {
        /* if we have multiple new lines after accounting for the last char which could also be a new line, append to create a paragraph separation  */
        char c = accum.charAt(accum.length() - 1);
        if (c == '\n')
            --newLines;

        if (newLines > 0) {
            accum.append('\n');
        } else if (!Character.isWhitespace(c) && i > 0) {
            /* make sure there is at least one white space if text has leading white spaces */
            accum.append(' ');
        }
    }

    // appends text to the string builder with a simple word wrap method
    private void append(String text) {
        int newLines = 0;

        /* Skip leading whitespaces in text while counting number of newLines */
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c))
                break;

            if (c == '\n') {
                ++newLines;
            }
            ++i;
        }

        if (i == text.length()) {
            if (!accum.isEmpty() && i > 0)
                addSeparator(i, newLines);
            return;
        } else if (accum.isEmpty()) {
            /* Accum is empty and text already starts at a non-white space, just append it */
            accum.append(text.substring(i));
            return;
        }

        addSeparator(i, newLines);
        accum.append(text.substring(i));
    }

    @Override
    public String toString() {
        return accum.toString();
    }
}
