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
package io.datanapis.test;

import io.datanapis.xbrl.analysis.TextBlockProcessor;
import io.datanapis.xbrl.analysis.text.Padding;
import io.datanapis.xbrl.analysis.text.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public class JsoupPlayground {
    private static final String FOLDER_PREFIX;
    static {
        FOLDER_PREFIX = System.getProperty("folder.prefix", System.getProperty("user.home") + File.separator + "Downloads");
    }

    public static class UriDefinitionPair {
        private final String uri;
        private final String definition;

        private UriDefinitionPair(String uri, String definition) {
            this.uri = uri;
            this.definition = definition;
        }

        public String getUri() {
            return uri;
        }

        public String getDefinition() {
            return definition;
        }

        public static class Builder {
            private String uri = null;
            private String definition = null;

            void reset() {
                uri = null;
                definition = null;
            }

            Builder uri(String uri) {
                this.uri = uri;
                return this;
            }

            Builder definition(String definition) {
                this.definition = definition;
                return this;
            }

            UriDefinitionPair build() {
                return new UriDefinitionPair(this.uri, this.definition);
            }
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void extractRoles() throws Exception {
        SortedMap<String,UriDefinitionPair> roles = new TreeMap<>();
        SortedMap<String,UriDefinitionPair> arcroles = new TreeMap<>();

        // Download Link Role Registry from https://specifications.xbrl.org/registries/lrr-2.0/ into FOLDER_PREFIX/Link Role Registry.html
        String linkRoleRegistry = FOLDER_PREFIX + File.separator + "Link Role Registry.html";
        String html = Files.readString(Paths.get(linkRoleRegistry));
        Document linkRoles = Jsoup.parse(html);
        String name = null;
        SortedMap<String,UriDefinitionPair> map = null;
        final UriDefinitionPair.Builder builder = new UriDefinitionPair.Builder();
        for (Element element : linkRoles.select("h2,dl")) {
            if (element.tagName().equals("h2")) {
                String text = element.text();
                if (text.contains("(role)")) {
                    if (name != null) {
                        map.put(name, builder.build());
                    }
                    name = text.replace("(role)", "").trim();
                    map = roles;
                    builder.reset();
                } else if (text.contains("(arcrole)")) {
                    if (name != null) {
                        map.put(name, builder.build());
                    }
                    name = text.replace("(arcrole)", "").trim();
                    map = arcroles;
                    builder.reset();
                }
            }
            boolean capture = false;
            Consumer<String> consumer = null;
            for (Element dtOrDd: element.select("dt,dd")) {
                if (dtOrDd.is("dt")) {
                    String text = dtOrDd.text().trim();
                    if (text.equalsIgnoreCase("uri")) {
                        consumer = builder::uri;
                    } else if (text.equalsIgnoreCase("definition")) {
                        consumer = builder::definition;
                    }
                } else if (dtOrDd.is("dd")) {
                    if (consumer != null) {
                        consumer.accept(dtOrDd.text().trim());
                        consumer = null;
                    }
                }
            }
        }
        if (name != null) {
            map.put(name, builder.build());
        }

        System.out.printf("%d, %d\n", roles.size(), arcroles.size());
        System.out.println("Roles:");
        for (Map.Entry<String,UriDefinitionPair> entry : roles.entrySet()) {
            System.out.printf("[%s] = {%s, %s}\n", entry.getKey(), entry.getValue().uri, entry.getValue().definition);
        }
        System.out.println("Arcroles:");
        for (Map.Entry<String,UriDefinitionPair> entry : arcroles.entrySet()) {
            System.out.printf("[%s] = {%s, %s}\n", entry.getKey(), entry.getValue().uri, entry.getValue().definition);
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void removeTables() throws Exception {
        try (InputStream inputStream = JsoupPlayground.class.getResourceAsStream("/test.html");
             InputStreamReader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            StringBuilder builder = new StringBuilder();
            bufferedReader.lines().forEach(builder::append);
            String html = builder.toString();
            TextBlockProcessor processor = new TextBlockProcessor(html);
            String value = processor.getTables();
            System.out.println(processor.getParagraphs());
            System.out.println("\nTable:");
            System.out.println(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void extractText() throws Exception {
        try (InputStream inputStream = JsoupPlayground.class.getResourceAsStream("/paragraphs.html");
             InputStreamReader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            StringBuilder builder = new StringBuilder();
            bufferedReader.lines().forEach(builder::append);
            String html = builder.toString();
            TextBlockProcessor textBlockProcessor = new TextBlockProcessor(html);
            System.out.println(textBlockProcessor.getParagraphs());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] STYLES = new String[] {
            "\"padding:2px 1pt 2px 25.75pt;text-align:left;vertical-align:bottom\"",
            "\"padding:2px 1pt;text-align:left;vertical-align:bottom\"",
            "\"padding:2px 0 2px 1pt;text-align:right;vertical-align:bottom\"",
            "\"padding-left:11.25pt\"",
            "background-color:#d0e5f8;border-bottom:1pt solid #000000;border-left:1pt solid #000000;border-top:1pt solid #000000;padding:2px 1pt;text-align:center;vertical-align:bottom",
            "\"color:#000000;font-family:'Amplitude',sans-serif;font-size:6pt;font-weight:400;line-height:100%\"",
            "\"color:#000000;font-family:'Amplitude',sans-serif;font-size:6pt;font-weight:700;line-height:100%\"",
            "\"padding:2px 0 2px 1pt;text-align:right;vertical-align:bottom\"",
            "background-color:#d0e5f8;border-bottom:1pt solid #000000;border-left:1pt solid #000000;border-top:1pt solid #000000;padding:2px 1pt;text-align:center;vertical-align:bottom",
            "padding-left:29.25pt;text-indent:-4.5pt"
    };

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testPadding() throws Exception {
        for (String style : STYLES) {
            Padding padding = TextUtils.getPadding(style);
            System.out.print(style);
            System.out.print(" ==> ");
            System.out.println(padding);
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testFontWeight() throws Exception {
        for (String style : STYLES) {
            Integer fontWeight = TextUtils.getFontWeight(style);
            System.out.print(style);
            System.out.print(" ==> ");
            System.out.println(fontWeight);
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testBackgroundColor() throws Exception {
        for (String style : STYLES) {
            String bgColor = TextUtils.getBackgroundColor(style);
            System.out.print(style);
            System.out.print(" ==> ");
            System.out.println(bgColor);
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testTextAlign() throws Exception {
        for (String style : STYLES) {
            String bgColor = TextUtils.getTextAlign(style);
            System.out.print(style);
            System.out.print(" ==> ");
            System.out.println(bgColor);
        }
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testTextIndent() throws Exception {
        for (String style : STYLES) {
            Float indent = TextUtils.getTextIndent(style);
            System.out.print(style);
            System.out.print(" ==> ");
            System.out.println(indent);
        }
    }
}
