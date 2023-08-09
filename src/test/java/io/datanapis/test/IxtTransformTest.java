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

import io.datanapis.xbrl.utils.IxtTransform;
import org.junit.Assert;
import org.junit.Test;

public class IxtTransformTest {

    public static final String NUMBER_WITH_SPACE_AND_DECIMALS = "1 123.23";
    public static final String NUMBER_WITH_COMMA_AND_DECIMALS = "1,123.23";
    public static final String NUMBER_AND_DECIMALS = "1,123.23";
    public static final String DECIMALS_ONLY = ".23";
    public static final String NUM_DOT_DECIMAL_FORMAT = "num-dot-decimal";

    @Test
    public void testTransform() {
        String value = IxtTransform.parseDate("October 2026", "datemonthyearen");
        System.out.println(value);
    }

    @Test
    public void testTransformNumberWithDecimals() {
        final String expectedNumberAndDecimal = "1123.23";
        Assert.assertEquals(expectedNumberAndDecimal,
          IxtTransform.transformWithFormat(NUM_DOT_DECIMAL_FORMAT, NUMBER_WITH_SPACE_AND_DECIMALS));

        Assert.assertEquals(expectedNumberAndDecimal,
          IxtTransform.transformWithFormat(NUM_DOT_DECIMAL_FORMAT, NUMBER_WITH_COMMA_AND_DECIMALS));

        Assert.assertEquals(expectedNumberAndDecimal,
          IxtTransform.transformWithFormat(NUM_DOT_DECIMAL_FORMAT, NUMBER_AND_DECIMALS));

    }

    @Test
    public void testTransformDecimalsOnly() {
        final String expectedNumberAndDecimal = ".23";
        Assert.assertEquals(expectedNumberAndDecimal,
          IxtTransform.transformWithFormat(NUM_DOT_DECIMAL_FORMAT, DECIMALS_ONLY));
    }
}
