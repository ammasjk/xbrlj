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

import io.datanapis.xbrl.analysis.text.FormattingVisitor;
import io.datanapis.xbrl.analysis.text.NameValue;
import io.datanapis.xbrl.analysis.text.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TextBlockProcessor {
    private static final Logger log = LoggerFactory.getLogger(TextBlockProcessor.class);

    private final Document document;
    private final Elements tables;

    public interface TableProcessor {
        default boolean process(String outerHtml) {
            return true;
        }

        default void tableStart(int current, int nTables) {
        }
        default void tableEnd(int current, int nTables) {
        }

        default void rowStart(int current, int nRows) {
        }
        default void rowEnd(int current, int nRows) {
        }

        default void column(int start, int colspan, int nCols, List<NameValue> styles, String value) {
            column(start, colspan, nCols, value);
        }

        default void column(int start, int colspan, int nCols, String value) {
        }
    }

    public TextBlockProcessor(String html) {
        this(html, true);
    }

    public TextBlockProcessor(String html, boolean skipTables) {
        document = Jsoup.parse(html);
        if (skipTables) {
            tables = document.select("table").clone();
            document.select("table").remove();
        } else {
            tables = document.select("table");
        }
    }

    public String getParagraphs() {
        Elements paragraphs;
        paragraphs = document.select("body");
        return toText(paragraphs);
    }

    private static String compactHtml(String html) {
        return html.replaceAll("\n", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getHtml() {
        return compactHtml(document.outerHtml());
    }

    public String getTablesAsHTML() {
        return compactHtml(tables.outerHtml());
    }

    public void getTables(Consumer<String> htmlConsumer) {
        for (Element table : this.tables) {
            htmlConsumer.accept(compactHtml(table.outerHtml()));
        }
    }

    public String getTables() {
        TablePrinter tablePrinter = new TablePrinter();
        return getTables(tablePrinter);
    }

    public String getTables(TableProcessor tableProcessor) {
        Elements tables = this.tables;
        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i);
            String html = compactHtml(table.outerHtml());
            boolean ok = tableProcessor.process(html);
            if (!ok)
                continue;

            tableProcessor.tableStart(i, tables.size());

            Elements rows = table.select("tr");
            for (int r = 0; r < rows.size(); r++) {
                Element row = rows.get(r);
                tableProcessor.rowStart(r, rows.size());

                Elements cols = row.select("th,td");
                for (int c = 0; c < cols.size(); c++) {
                    Element col = cols.get(c);
                    List<NameValue> styles = new ArrayList<>();
                    col.traverse((node, depth) -> {
                        if (node instanceof Element element) {
                            String style = TextUtils.style(element);
                            if (Objects.nonNull(style)) {
                                styles.add(new NameValue(node.nodeName(), style));
                            }
                        }
                    });
                    tableProcessor.column(c, TextUtils.colspan(col), cols.size(), styles, col.text());
                }

                tableProcessor.rowEnd(r, rows.size());
            }

            tableProcessor.tableEnd(i, tables.size());
        }

        return tableProcessor.toString();
    }

    private static class TablePrinter implements TableProcessor {
        private final StringBuilder builder = new StringBuilder();

        private TablePrinter() {
        }

        @Override
        public void tableStart(int current, int nTables) {
        }

        @Override
        public void tableEnd(int current, int nTables) {
            builder.append("\n");
        }

        @Override
        public void rowStart(int current, int nRows) {
        }

        @Override
        public void rowEnd(int current, int nRows) {
            builder.append("\n");
        }

        @Override
        public void column(int start, int colspan, int nCols, String value) {
            int width;
            String format;
            if (start == 0) {
                width = 20 + 15 * colspan;
                if (start + colspan < nCols)
                    --width;
                format = String.format("%%-%d.%ds%%s", width, width);
                builder.append(String.format(format, value, (start + colspan < nCols) ? "|" : ""));
            } else {
                width = 15 * colspan;
                if (start + colspan < nCols)
                    --width;
                format = String.format("%%%d.%ds%%s", width, width);
                builder.append(String.format(format, value, (start + colspan < nCols) ? "|" : ""));
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    private static String toText(Elements elements) {
        StringBuilder builder = new StringBuilder();
        FormattingVisitor formatter = new FormattingVisitor(builder);
        for (Element element : elements) {
            NodeTraversor.traverse(formatter, element);
        }
        return builder.toString();
    }
}
