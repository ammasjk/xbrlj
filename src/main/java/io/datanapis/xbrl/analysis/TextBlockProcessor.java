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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TextBlockProcessor {
    private final Document document;
    private final boolean skipTables;

    public interface TableProcessor {
        void tableStart(int current, int nTables);
        void tableEnd(int current, int nTables);

        void rowStart(int current, int nRows);
        void rowEnd(int current, int nRows);

        void column(int current, int nCols, String value);
    }

    public TextBlockProcessor(String html) {
        this(html, true);
    }

    public TextBlockProcessor(String html, boolean skipTables) {
        document = Jsoup.parse(html);
        this.skipTables = skipTables;
    }

    public String getParagraphs() {
        Elements paragraphs;
        if (skipTables) {
            /* We should be skipping tables, but this is not working well. Leaving it as is for now */
            paragraphs = document.select("body,p,span,div");
        } else {
            paragraphs = document.select("body,p,span,div");
        }
        return paragraphs.text();
    }

    public String getTablesAsHTML() {
        Elements table = document.select("table");
        return table.outerHtml();
    }

    public String getTables() {
        TablePrinter tablePrinter = new TablePrinter();
        return getTables(tablePrinter);
    }

    public String getTables(TableProcessor tableProcessor) {
        Elements tables = document.select("table");
        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i);
            tableProcessor.tableStart(i, tables.size());

            Elements rows = table.select("tr");
            for (int r = 0; r < rows.size(); r++) {
                Element row = rows.get(r);
                tableProcessor.rowStart(r, rows.size());

                Elements cols = row.select("th,td");
                for (int c = 0; c < cols.size(); c++) {
                    Element col = cols.get(c);
                    tableProcessor.column(c, cols.size(), col.text());
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
        public void column(int current, int nCols, String value) {
            if (current == 0) {
                builder.append(String.format("%-30.30s%s", value, (current < nCols - 1) ? "|" : ""));
            } else {
                builder.append(String.format("%15.15s%s", value, (current < nCols - 1) ? "|" : ""));
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
