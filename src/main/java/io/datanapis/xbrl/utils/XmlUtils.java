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
package io.datanapis.xbrl.utils;

import io.datanapis.xbrl.model.ElementNotFoundException;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.io.PrintWriter;
import java.util.List;

public class XmlUtils {
    public static Element getChild(Element root, String ... names) {
        StringBuilder builder = new StringBuilder();
        Element element = root;
        outer: for (String name : names) {
            for (Element child : element.elements()) {
                if (child.getName().equals(name)) {
                    element = child;
                    if (builder.length() > 0)
                        builder.append("/");
                    builder.append(name);
                    continue outer;
                }
            }

            if (builder.length() > 0)
                builder.append('/');
            builder.append('*').append(name).append('*');

            throw new ElementNotFoundException(element.getName(), builder.toString());
        }

        return element;
    }

    public static void report(PrintWriter writer, Element root) {
        report(0, writer, root);
    }

    private static void report(int level, PrintWriter writer, Element root) {
        String prefix = "";
        if (level > 0) {
            prefix = " ".repeat(4 * level);
        }

        writer.printf("%s%s(%s)\n", prefix, root.getQualifiedName(), attributesToString(root.attributes()));
        for (Element child : root.elements()) {
            report(level + 1, writer, child);
        }
    }

    public static String attributesToString(List<Attribute> attributes) {
        StringBuilder builder = new StringBuilder();
        for (Attribute attribute : attributes) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(attribute.getName());
        }
        return builder.toString();
    }

    public static String elementsToString(List<Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (Element element : elements) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(element.getName());
        }
        return builder.toString();
    }

    public static boolean isTextBlock(Element element) {
        String name = element.getName();
        if (name.endsWith("Text") || name.endsWith("TextBlock") || name.endsWith("Policy"))
            return true;

        for (Element child : element.elements()) {
            if (child.getName().equals("div") || child.getName().equals("span"))
                return true;
        }

        String value = element.getStringValue();
        if (value.contains("</div>") || value.contains("</span>"))
            return true;

        return false;
    }

    public static String getText(Element element) {
        return element.getStringValue();
    }

    public static String asXML(Element element) {
        return element.asXML();
    }

    public static String asString(Element element) {
        return element.getStringValue();
    }

    public static String asXML(List<Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (Element element : elements) {
            builder.append(asXML(element));
        }
        return builder.toString();
    }

    public static String asString(List<Element> elements) {
        StringBuilder builder = new StringBuilder();
        for (Element element : elements) {
            builder.append(element.getStringValue());
        }
        return builder.toString();
    }
}
