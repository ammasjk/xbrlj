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
package io.datanapis.xbrl.analysis;

import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class PrettyPrinter implements PresentationProcessor {
    private static final int TAB_WIDTH = 4;
    private static final int WIDTH = 120;

    private final boolean printDefinition;
    private final boolean verbose;
    private final boolean useLabels;
    private final boolean skipTables;
    private final boolean htmlTables;
    private final int width;
    private final int tabWidth;
    private final PrintWriter writer;
    private List<ExplicitMember> previous;

    enum Type {
        NAME,
        VALUE,
        TEXT;
    }

    public PrettyPrinter(PrintWriter writer) {
        this(writer, true, false, false);
    }

    public PrettyPrinter(PrintWriter writer, boolean printDefinition, boolean useLabels, boolean verbose) {
        this(writer, printDefinition, verbose, useLabels, false, true, WIDTH, TAB_WIDTH);
    }

    public PrettyPrinter(PrintWriter writer, boolean printDefinition, boolean useLabels, boolean skipTables, boolean htmlTables, boolean verbose) {
        this(writer, printDefinition, verbose, useLabels, skipTables, htmlTables, WIDTH, TAB_WIDTH);
    }

    public PrettyPrinter(PrintWriter writer, boolean printDefinition, boolean verbose,
                         boolean useLabels, boolean skipTables, boolean htmlTables, int width, int tabWidth) {
        this.writer = writer;
        this.printDefinition = printDefinition;
        this.verbose = verbose;
        this.useLabels = useLabels;
        this.skipTables = skipTables;
        this.htmlTables = htmlTables;
        this.width = width;
        this.tabWidth = tabWidth;
        this.previous = null;
    }

    public void complete() {
        writer.close();
    }

    private int width(int indent) {
        return width - indent * tabWidth;
    }

    private String format(int indent, Type type) {
        int width = width(indent);
        StringBuilder builder = new StringBuilder();
        builder.append(" ".repeat(indent * TAB_WIDTH));
        builder.append("%-");
        builder.append(width);
        builder.append('.');
        builder.append(width);
        builder.append("s");
        if (type == Type.VALUE) {
            builder.append("|        %25.25s").append(" [%6.6s] %9.9s %s");
        } else if (type == Type.TEXT) {
            builder.append("| %s");
        }
        builder.append("\n");
        return builder.toString();
    }

    private String pNameFormat(int indent) {
        return format(indent, Type.NAME);
    }

    private String pValueFormat(int indent) {
        return format(indent, Type.VALUE);
    }

    private String pTextFormat(int indent) {
        return format(indent, Type.TEXT);
    }

    private String getLabel(PresentationGraphNode graphNode) {
        if (useLabels) {
            return graphNode.getLabel();
        } else {
            return graphNode.getConcept().getQualifiedName();
        }
    }

    private LabelPair getMemberLabel(PresentationGraphNode lineItem, ExplicitMember member, PresentationInfoProvider infoProvider) {
        if (useLabels) {
            return infoProvider.getLabel(lineItem, member);
        }

        return new LabelPair(member.getDimension().getQualifiedName(), member.getMember().getQualifiedName());
    }

    private String getAxisLabel(PresentationGraphNode lineItem, Concept axis, PresentationInfoProvider infoProvider) {
        if (useLabels) {
            return infoProvider.getAxisLabel(lineItem, axis);
        }

        return axis.getQualifiedName();
    }

    @Override
    public void start(RoleType roleType, TimeOrdered<DimensionedFact> facts) {
        if (verbose) {
            printFacts(writer, facts);
        }

        writer.println("Presentation: [" + roleType.getDefinition() + "]");
    }

    @Override
    public void end(RoleType roleType) {
    }

    @Override
    public void rootStart(PresentationGraphNode root) {
        if (printDefinition) {
            try (StringWriter sw = new StringWriter()) {
                root.displayNetwork(new PrintWriter(sw));
                sw.close();
                writer.println(sw);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void rootEnd(PresentationGraphNode root) {
    }

    @Override
    public void periodStart(PresentationGraphNode root, Period period, ReportingPeriodType rpType) {
        writer.printf("Period [%s] [%s]\n", period.toString(), rpType.toString());
    }

    @Override
    public void periodEnd(PresentationGraphNode root, Period period) {
        writer.println();
        writer.println();
    }

    @Override
    public void internalNodeStart(PresentationGraphNode node, int level) {
        writer.printf(pNameFormat(level), getLabel(node));
        previous = null;
    }

    @Override
    public void internalNodeEnd(PresentationGraphNode node, int level) {
        previous = null;
    }

    private static String getLabelType(PresentationGraphNode node) {
        StringBuilder builder = new StringBuilder();
        String labelType = node.getArc().getPreferredLabelType();
        if (labelType.contains(Label.PERIOD_START)) {
            builder.append("beg");
        } else if (labelType.contains(Label.PERIOD_END)) {
            builder.append("end");
        }

        if (labelType.contains(Label.TOTAL)) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append("tot");
        }

        if (!builder.isEmpty()) {
            return "[" + builder + "]";
        } else {
            return "";
        }
    }

    @Override
    public void lineItem(PresentationGraphNode node, int level, Fact fact) {
        Concept concept = node.getConcept();
        String name = concept.getName();
        String type = concept.getTypeName();
        if (type.equals("textBlockItemType") || name.endsWith("Text") || name.endsWith("TextBlock") || name.endsWith("Policy")) {
            String html = Fact.getValue(fact);
            TextBlockProcessor textProcessor = new TextBlockProcessor(html);
            if (skipTables) {
                writer.printf(pTextFormat(level), getLabel(node), textProcessor.getHtml());
            } else {
                if (!htmlTables) {
                    writer.printf(pTextFormat(level), getLabel(node),
                            textProcessor.getHtml() + "\n" + textProcessor.getTables());
                } else {
                    textProcessor.getTables(tableHtml -> {
                        writer.printf(pTextFormat(level), "", tableHtml);
                    });
                }
            }
        } else {
            writer.printf(pValueFormat(level), getLabel(node), Fact.getValue(fact, node.isNegated()),
                    concept.getBalance(), getLabelType(node), Fact.getUnit(fact));
        }
        previous = null;
    }

    @Override
    public void lineItem(PresentationGraphNode node, int level, DimensionedFact fact, PresentationInfoProvider infoProvider) {
        Concept concept = node.getConcept();
        StringBuilder builder = new StringBuilder();

        int levelIncrement = 1;
        if (fact.getDimensions() != null) {
            List<ExplicitMember> dimensions = fact.getDimensions();
            int i = 0, j = 0;
            if (Objects.nonNull(previous)) {
                while (i < previous.size() && j < dimensions.size()) {
                    if (previous.get(i).equals(dimensions.get(j))) {
                        ++i;
                        ++j;
                    } else {
                        break;
                    }
                }
            }

            while (j < dimensions.size() - 1) {
                levelIncrement = infoProvider.level(node, dimensions.subList(0, j+1));
                ExplicitMember member = dimensions.get(j);
                LabelPair labels = getMemberLabel(node, member, infoProvider);
                builder.append(labels.getSecond());
                writer.printf(pNameFormat(level + levelIncrement), builder);
                builder.setLength(0);
                ++j;
            }

            levelIncrement = infoProvider.level(node, dimensions);
            ExplicitMember member = dimensions.get(dimensions.size() - 1);
            LabelPair labels = getMemberLabel(node, member, infoProvider);
            builder.append(labels.getSecond());

            previous = List.copyOf(dimensions);
        } else if (fact.getTypedMembers() != null) {
            previous = null;
            for (TypedMember member : fact.getTypedMembers()) {
                if (!builder.isEmpty()) {
                    builder.append(", ");
                }
                builder.append(member.getMember());
            }
        }
        writer.printf(pValueFormat(level + levelIncrement), builder, Fact.getValue(fact.getFact(), node.isNegated()),
                concept.getBalance(), getLabelType(node), Fact.getUnit(fact.getFact()));
    }

    private static void printFacts(PrintWriter writer, TimeOrdered<DimensionedFact> facts) {
        for (Period period : facts.keySet()) {
            Collection<DimensionedFact> dfs = facts.get(period);
            writer.printf("Period [%s], found [%d] facts\n", period.toString(), dfs.size());
            for (DimensionedFact fact : dfs) {
                writer.print("    " + fact.getFact().toString());
                if (fact.getDimensions() == null) {
                    writer.println();
                } else {
                    writer.print(" : ");
                    Utils.print(writer, "", fact.getDimensions());
                }
            }
        }
    }
}
