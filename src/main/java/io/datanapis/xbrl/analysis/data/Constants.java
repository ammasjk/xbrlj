package io.datanapis.xbrl.analysis.data;

import java.io.File;
import java.util.Set;

public class Constants {
    public static final String FOLDER_PREFIX;
    static {
        FOLDER_PREFIX = System.getProperty("folder.prefix", System.getProperty("user.home") + File.separator + "Downloads");
    }

    public static final Set<String> T2012_ALLOWED_DUPLICATES = Set.of(
            "us-gaap:RealizedInvestmentGainsLossesAbstract",
            "us-gaap:GainLossOnSecuritizationOfFinancialAssets",
            "us-gaap:GainLossOnSaleOfSecuritiesNet",
            "us-gaap:GainLossOnSalesOfLoansNet",
            "us-gaap:GainLossOnSaleOfCapitalLeasesNet",
            "us-gaap:GainLossOnSaleOfLeasedAssetsNetOperatingLeases",
            "us-gaap:GainsLossesOnSalesOfInvestmentRealEstate",
            "us-gaap:GainsLossesOnSalesOfOtherAssets",
            "us-gaap:RealizedInvestmentGainsLosses"
    );
}
