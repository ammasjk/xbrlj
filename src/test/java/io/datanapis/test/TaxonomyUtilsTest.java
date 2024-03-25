package io.datanapis.test;

import io.datanapis.xbrl.analysis.data.XbrlTaxonomyPath;
import io.datanapis.xbrl.utils.TaxonomyUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class TaxonomyUtilsTest {
    private static final String FOLDER_PREFIX;
    static {
        FOLDER_PREFIX = System.getProperty("folder.prefix", System.getProperty("user.home") + File.separator + "Downloads");
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testReaderUtils() {
        Map<String,byte[]> contentMap = TaxonomyUtils.buildCacheFromZip(Path.of(FOLDER_PREFIX, "us-gaap"));
    }

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testTaxonomyRoot() {
        Path root = Path.of(XbrlTaxonomyPath.T2018.toString());
        System.out.println(TaxonomyUtils.getGaapTaxonomyBasePath(root));
        Map<String,byte[]> contentMap = TaxonomyUtils.buildCacheFromRootXsd(root, TaxonomyUtils::getGaapTaxonomyBasePath);
        if (contentMap != null)
            System.out.printf("Found [%d] files\n", contentMap.size());
    }
}
