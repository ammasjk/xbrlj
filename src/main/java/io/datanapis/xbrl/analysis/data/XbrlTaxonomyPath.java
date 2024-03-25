package io.datanapis.xbrl.analysis.data;

import java.io.File;
import java.util.Set;

public enum XbrlTaxonomyPath {
    T2012(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2012-01-31/entire/us-gaap-entryPoint-all-2012-01-31.xsd", 2012, Constants.T2012_ALLOWED_DUPLICATES),
    T2013(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2013-01-31/entire/us-gaap-entryPoint-all-2013-01-31.xsd", 2013, null),
    T2014(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2014-01-31/entire/us-gaap-entryPoint-all-2014-01-31.xsd", 2014, null),
    T2015(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2015-01-31/entire/us-gaap-entryPoint-all-2015-01-31.xsd", 2015, null),
    T2016(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2016-01-31/entire/us-gaap-entryPoint-all-2016-01-31.xsd", 2016, null),
    T2017(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2017-01-31/entire/us-gaap-entryPoint-all-2017-01-31.xsd", 2017, null),
    T2018(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2018-01-31/entire/us-gaap-entryPoint-all-2018-01-31.xsd", 2018, null),
    T2019(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2019-01-31/entire/us-gaap-entryPoint-all-2019-01-31.xsd", 2019, null),
    T2020(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2020-01-31/entire/us-gaap-entryPoint-all-2020-01-31.xsd", 2020, null),
    T2021(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2021-01-31/entire/us-gaap-entryPoint-all-2021-01-31.xsd", 2021, null),
    T2022(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2022/entire/us-gaap-entryPoint-all-2022.xsd", 2022, null),
    T2023(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2023/entire/us-gaap-entryPoint-all-2023.xsd", 2023, null),
    T2024(Constants.FOLDER_PREFIX + File.separator + "us-gaap/us-gaap-2024/entire/us-gaap-entryPoint-all-2024.xsd", 2024, null);

    private final String path;
    private final int year;
    private final Set<String> allowedDuplicates;

    XbrlTaxonomyPath(String path, int year, Set<String> allowedDuplicates) {
        this.path = path;
        this.year = year;
        this.allowedDuplicates = allowedDuplicates;
    }

    public String path() {
        return this.path;
    }
    public int year() {
        return this.year;
    }
    public Set<String> allowedDuplicates() {
        return this.allowedDuplicates;
    }

    public String getFilePath(String suffix) {
        return Constants.FOLDER_PREFIX + File.separator + "Output" + File.separator + "taxonomy" + File.separator + this.name() + "." + suffix;
    }

    public String getFilePath() {
        return getFilePath("output");
    }

    public String getCSVPath() {
        return getFilePath("csv");
    }

    public String getJsonPath() {
        return getFilePath("json");
    }

    public String getJsonGzPath() {
        return getFilePath("json.gz");
    }

    @Override
    public String toString() {
        return this.path;
    }
}
