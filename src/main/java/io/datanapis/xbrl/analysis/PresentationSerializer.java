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

import com.google.gson.*;
import io.datanapis.xbrl.model.*;
import io.datanapis.xbrl.utils.EdgarUtils;

import java.util.*;

public class PresentationSerializer extends AbstractSerializer implements PresentationProcessor {
    private static final String BALANCE = "balance";
    private static final String DISCLOSURE = "Disclosure";
    private static final String DISCLOSURES = "disclosures";
    private static final String DOCUMENT = "Document";
    private static final String DOCUMENTS = "documents";
    private static final String FOOTNOTE = "footnote";
    private static final String LABEL = "label";
    private static final String LABEL_TYPE = "labelType";
    private static final String LEVEL = "level";
    private static final String SCHEDULE = "Schedule";
    private static final String STATEMENT = "Statement";
    private static final String STATEMENTS = "statements";
    private static final String TABLE_HTML = "tableHtml";
    private static final String VALUE = "value";

    private final boolean separateTables;
    private final JsonArray documents;
    private final JsonArray statements;
    private final JsonArray disclosures;
    private final Deque<Integer> levels = new ArrayDeque<>();

    public PresentationSerializer() {
        this(true);
    }

    public PresentationSerializer(boolean separateTables) {
        this.separateTables = separateTables;
        documents = new JsonArray();
        statements = new JsonArray();
        disclosures = new JsonArray();
    }

    public JsonObject asJson() {
        JsonObject root = new JsonObject();
        root.add(DOCUMENTS, documents);
        root.add(STATEMENTS, statements);
        root.add(DISCLOSURES, disclosures);
        return root;
    }

    private int topLevel() {
        return levels.peek();
    }

    private static String getLabel(PresentationGraphNode graphNode) {
        String label = graphNode.getLabel();
        return label;
    }

    private static String getLabelType(PresentationGraphNode node) {
        if (node.getArc() != null) {
            return node.getArc().getPreferredLabelType();
        }
        return null;
    }

    @Override
    public boolean groupDimensionedFacts() {
        return false;
    }

    @Override
    public void start(RoleType roleType, TimeOrdered<DimensionedFact> facts) {
        super.start();
    }

    @Override
    public void end(RoleType roleType) {
        String[] groups = EdgarUtils.splitDefinition(roleType);
        if (groups != null) {
            String sortCode = groups[0];
            String type = groups[1];
            String title = groups[2];

            currentRoleType.addProperty(SORT_CODE, sortCode);
            currentRoleType.addProperty(TITLE, title);

            switch (type) {
                case DISCLOSURE:
                    disclosures.add(currentRoleType);
                    break;

                case STATEMENT:
                    statements.add(currentRoleType);
                    break;

                case DOCUMENT:
                    documents.add(currentRoleType);
                    break;

                case SCHEDULE:
                default:
                    break;
            }
        }

        super.end();
    }

    @Override
    public void rootStart(PresentationGraphNode root) {
    }

    @Override
    public void rootEnd(PresentationGraphNode root) {
    }

    @Override
    public void periodStart(PresentationGraphNode root, Period period, ReportingPeriodType rpType) {
        super.periodStart(period, rpType);
    }

    @Override
    public void periodEnd(PresentationGraphNode root, Period period) {
        super.periodEnd(period);
    }

    private static void addConceptProperties(JsonObject object, PresentationGraphNode node) {
        Concept concept = node.getConcept();
        object.addProperty(NAME, concept.getQualifiedName());
        object.addProperty(LABEL, getLabel(node));
        object.addProperty(LABEL_TYPE, getLabelType(node));
        object.addProperty(BALANCE, concept.getBalance().toString());
    }

    private static void addConceptProperties(JsonObject object, PresentationGraphNode node, int level) {
        addConceptProperties(object, node);
        object.addProperty(LEVEL, level);
    }

    private void addPrevious(int level) {
        if (!levels.isEmpty()) {
            if (level <= topLevel()) {
                while (!levels.isEmpty() && level <= topLevel()) {
                    levels.pop();
                    super.nodeEnd();
                }
            }
        }
    }

    @Override
    public void internalNodeStart(PresentationGraphNode node, int level) {
        addPrevious(level);
        JsonObject object = super.nodeStart();
        addConceptProperties(object, node, level);
    }

    @Override
    public void internalNodeEnd(PresentationGraphNode node, int level) {
        addPrevious(level);
        super.nodeEnd();
    }

    private JsonObject makeFact(PresentationGraphNode node, Fact fact) {
        JsonObject object = super.nodeStart();

        Concept concept = node.getConcept();
        addConceptProperties(object, node);

        String name = concept.getName();
        String type = concept.getTypeName();
        if (type.equals("textBlockItemType") || name.endsWith("Text") || name.endsWith("TextBlock") || name.endsWith("Policy")) {
            String html = Fact.getValue(fact);
            TextBlockProcessor textProcessor = new TextBlockProcessor(html, separateTables);
            object.addProperty(VALUE, textProcessor.getParagraphs());
            if (separateTables) {
                String tables = textProcessor.getTablesAsHTML();
                object.addProperty(TABLE_HTML, tables);
            }
        } else {
            if (fact.getLongValue() != null) {
                long value = fact.getLongValue();
                if (node.isNegated()) {
                    value = -value;
                }
                object.addProperty(VALUE, value);
            } else if (fact.getDoubleValue() != null) {
                double value = fact.getDoubleValue();
                if (node.isNegated()) {
                    value = -value;
                }
                object.addProperty(VALUE, value);
            } else {
                object.addProperty(VALUE, fact.getValue());
            }

            if (fact.getLongValue() != null || fact.getDoubleValue() != null) {
                object.addProperty(DECIMALS, fact.getDecimals());
                object.addProperty(TYPE, concept.getTypeName());
                Unit unit = fact.getUnit();
                if (unit != null)
                    object.addProperty(UNIT, unit.toString());
            }

            Footnote footnote = fact.getFootnote();
            if (footnote != null) {
                TextBlockProcessor textProcessor = new TextBlockProcessor(footnote.getValue());
                object.addProperty(FOOTNOTE, textProcessor.getParagraphs());
            }
        }

        return object;
    }

    @Override
    public void lineItem(PresentationGraphNode node, int level, Fact fact) {
        addPrevious(level);
        levels.push(level);
        makeFact(node, fact);
    }

    @Override
    public void lineItem(PresentationGraphNode node, int level, DimensionedFact fact, PresentationInfoProvider infoProvider) {
        addPrevious(level);
        levels.push(level);
        JsonObject object = makeFact(node, fact.getFact());

        JsonArray array = new JsonArray();
        if (fact.getDimensions() != null) {
            List<ExplicitMember> dimensions = fact.getDimensions();
            for (ExplicitMember member : dimensions) {
                Pair<String, String> labels = infoProvider.getLabel(member);
                JsonObject qualifier = new JsonObject();
                qualifier.addProperty(AXIS, member.getDimension().getQualifiedName());
                qualifier.addProperty(AXIS_VALUE, labels.getFirst());
                qualifier.addProperty(MEMBER, member.getMember().getQualifiedName());
                qualifier.addProperty(MEMBER_VALUE, labels.getSecond());
                array.add(qualifier);
            }
        } else if (fact.getTypedMembers() != null) {
            for (TypedMember member : fact.getTypedMembers()) {
                String axisLabel = infoProvider.getAxisLabel(member.getDimension());
                JsonObject qualifier = new JsonObject();
                qualifier.addProperty(AXIS, member.getDimension().getQualifiedName());
                qualifier.addProperty(AXIS_VALUE, axisLabel);
                qualifier.addProperty(MEMBER, member.getMember());
                qualifier.addProperty(MEMBER_VALUE, member.getMember());
                array.add(qualifier);
            }
        }
        object.add(DIMENSIONS, array);
    }
}
