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

public class XbrlUtils {
    private static final String INSTANCE_URI = "http://www.xbrl.org/2003/instance";
    private static final String LINKBASE_URI = "http://www.xbrl.org/2003/linkbase";
    private static final String ISO4217_URI = "http://www.xbrl.org/2003/iso4217";
    private static final String DEI_BASE_URI = "http://xbrl.sec.gov/dei/";
    private static final String DEI_2020_URI = "http://xbrl.sec.gov/dei/2020-01-31";
    private static final String DEI_2019_URI = "http://xbrl.sec.gov/dei/2019-01-31";
    private static final String DEI_2014_URI = "http://xbrl.sec.gov/dei/2014-01-31";
    private static final String SRT_BASE_URI = "http://fasb.org/srt/";
    private static final String SRT_2019_URI = "http://fasb.org/srt/2019-01-31";
    private static final String STPR_2018_URI = "http://xbrl.sec.gov/stpr/2018-01-31";
    private static final String US_GAAP_BASE_URI = "http://fasb.org/us-gaap/";
    private static final String US_GAAP_2020_URI = "http://fasb.org/us-gaap/2020-01-31";
    private static final String US_GAAP_2019_URI = "http://fasb.org/us-gaap/2019-01-31";
    private static final String XBRLDI_URI = "http://xbrl.org/2006/xbrldi";
    private static final String XHTML_URI = "http://xbrl.org/2006/xbrldi";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String IX_URI = "http://www.xbrl.org/2013/inlineXBRL";
    private static final String IXT_URI = "http://www.xbrl.org/inlineXBRL/transformation/2015-02-26";
    private static final String IXT_SEC_URI = "http://www.sec.gov/inlineXBRL/transformation/2015-08-31";

    private static final String FACT_FOOTNOTE_URI = "http://www.xbrl.org/2003/arcrole/fact-footnote";
    private static final String LINK_URI = "http://www.xbrl.org/2003/role/link";

    public static boolean isLinkbase(String uri) {
        return LINKBASE_URI.equals(uri);
    }

    public static boolean isDei(String uri) {
        return uri != null && uri.startsWith(DEI_BASE_URI);
    }

    public static boolean isIx(String uri) {
        return uri.equals(IX_URI);
    }

    public static boolean isUSGAAP(String uri) {
        return uri.startsWith(US_GAAP_BASE_URI);
    }

    public static boolean isFactFootnote(String uri) {
        /* arcRole uri is optional, if absent the default is FACT_FOOTNOTE_URI */
        return uri == null || FACT_FOOTNOTE_URI.equals(uri);
    }
}
