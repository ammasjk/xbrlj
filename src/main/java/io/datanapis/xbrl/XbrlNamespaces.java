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
package io.datanapis.xbrl;

import org.dom4j.Namespace;

public class XbrlNamespaces {
    public static final String SRT_PREFIX = "srt";
    public static final String US_GAAP_PREFIX = "us-gaap";
    public static final String US_GAAP_NAMESPACE_2012_URI = "http://fasb.org/us-gaap/2012-01-31";
    public static final String US_GAAP_NAMESPACE_2013_URI = "http://fasb.org/us-gaap/2013-01-31";
    public static final String US_GAAP_NAMESPACE_2014_URI = "http://fasb.org/us-gaap/2014-01-31";
    public static final String US_GAAP_NAMESPACE_2015_URI = "http://fasb.org/us-gaap/2015-01-31";
    public static final String US_GAAP_NAMESPACE_2016_URI = "http://fasb.org/us-gaap/2016-01-31";
    public static final String US_GAAP_NAMESPACE_2017_URI = "http://fasb.org/us-gaap/2017-01-31";
    public static final String US_GAAP_NAMESPACE_2018_URI = "http://fasb.org/us-gaap/2018-01-31";
    public static final String US_GAAP_NAMESPACE_2019_URI = "http://fasb.org/us-gaap/2019-01-31";
    public static final String US_GAAP_NAMESPACE_2020_URI = "http://fasb.org/us-gaap/2020-01-31";
    public static final String US_GAAP_NAMESPACE_2021_URI = "http://fasb.org/us-gaap/2021-01-31";
    public static final String US_GAAP_NAMESPACE_2022_URI = "http://fasb.org/us-gaap/2022";
    public static final String US_GAAP_NAMESPACE_2023_URI = "http://fasb.org/us-gaap/2023";
    public static final String US_GAAP_NAMESPACE_2024_URI = "http://fasb.org/us-gaap/2024";

    public static final String XBRLI_PREFIX = "xbrli";
    public static final String XBRLI_NAMESPACE_URI = "http://www.xbrl.org/2003/instance";
    public static final Namespace XBRLI_NAMESPACE = new Namespace(XBRLI_PREFIX, XBRLI_NAMESPACE_URI);
    public static final String XBRLDT_PREFIX = "xbrldt";
    public static final String XBRLDT_NAMESPACE_URI = "http://xbrl.org/2005/xbrldt";
    public static final Namespace XBRLDT_NAMESPACE = new Namespace(XBRLDT_PREFIX, XBRLDT_NAMESPACE_URI);
    public static final String XBRLDI_PREFIX = "xbrldi";
    public static final String XBRLDI_NAMESPACE_URI = "http://xbrl.org/2006/xbrldi";
    public static final Namespace XBRLDI_NAMESPACE = new Namespace(XBRLDI_PREFIX, XBRLDI_NAMESPACE_URI);
    public static final String XBRL_LINK_NAMESPACE_URI = "http://www.xbrl.org/2003/linkbase";

    /**
     * Parse the year from a US-GAAP URI
     *
     * @param uri a US-GAAP URI
     * @return 4 digit year if uri is a valid US-GAAP URI. Null otherwise.
     */
    public static Integer getYearFromGaapURI(String uri) {
        int index = uri.lastIndexOf('/');
        if (index < 0)
            return null;

        try {
            String year = uri.substring(index + 1, index + 5);
            return Integer.parseInt(year);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }
}
