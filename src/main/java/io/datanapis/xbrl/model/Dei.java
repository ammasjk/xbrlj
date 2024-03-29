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
package io.datanapis.xbrl.model;

import io.datanapis.xbrl.XbrlInstance;
import io.datanapis.xbrl.utils.EdgarUtils;
import io.datanapis.xbrl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Dei {
    private static final Logger log = LoggerFactory.getLogger(Dei.class);
    private static final Pattern TICKER = Pattern.compile("^[A-Z0-9].*$");

    private LocalDate dateFiled;
    private LocalDate estimatedPeriodEndDate;
    private boolean amendmentFlag = false;
    private String amendmentDescription = null;
    private MonthDay yearEndDate = null;
    private Context primaryContext = null;
    private final DocumentInformation documentInformation = new DocumentInformation();
    private final EntityInformation entityInformation = new EntityInformation();
    private final ContextMap<Long> sharesOutstandingMap = new ContextMap<>();
    private final ContextMap<SecurityInformation> securityInformationMap = new ContextMap<>();

    public Dei(LocalDate dateFiled) {
        this.dateFiled = dateFiled;
    }

    public void clear() {
        sharesOutstandingMap.clear();
        securityInformationMap.clear();
    }

    public LocalDate getDateFiled() {
        return dateFiled;
    }
    public void setDateFiled(LocalDate dateFiled) {
        this.dateFiled = dateFiled;
    }

    public LocalDate getEstimatedPeriodEndDate() {
        return this.estimatedPeriodEndDate;
    }
    public void setEstimatedPeriodEndDate(LocalDate endDate) {
        this.estimatedPeriodEndDate = endDate;
    }

    public boolean isAmendmentFlag() {
        return amendmentFlag;
    }
    public String getAmendmentDescription() {
        return amendmentDescription;
    }

    public String getFiscalPeriod() {
        return documentInformation.getFiscalPeriod();
    }

    public int getFiscalYear() {
        return documentInformation.getFiscalYear();
    }

    public String getCIK() {
        return entityInformation.getCentralIndexKey();
    }

    public String getRegistrantName() {
        return entityInformation.getRegistrantName();
    }

    public MonthDay getYearEndDate() {
        return yearEndDate;
    }

    public LocalDate getPeriodEndDate() {
        return documentInformation.getPeriodEndDate();
    }

    public Context getPrimaryContext() {
        return primaryContext;
    }

    public ContextMap<Long> getSharesOutstanding() {
        return sharesOutstandingMap;
    }

    public ContextMap<SecurityInformation> getSecurityInformation() {
        return securityInformationMap;
    }

    public List<String> getTickers() {
        List<String> tickers = new ArrayList<>();
        for (Map.Entry<Context,SecurityInformation> entry : securityInformationMap.entrySet()) {
            Context context = entry.getKey();
            if (context.hasDimensions()) {
                ExplicitMember explicitMember = context.getDimensions().iterator().next();
                if (!explicitMember.getMember().getQualifiedName().startsWith("us-gaap:Common"))
                    continue;
            }

            SecurityInformation si = entry.getValue();
            if (si.getTradingSymbol() != null) {
                tickers.add(si.getTradingSymbol());;
            }
        }

        return tickers;
    }

    public DocumentInformation getDocumentInformation() {
        assert (primaryContext != null);
        return documentInformation;
    }

    public EntityInformation getEntityInformation() {
        assert (primaryContext != null);
        return entityInformation;
    }

    public void setPropertiesFrom(XbrlInstance instance, Fact fact) {
        String name = fact.getConcept().getName();
        String contextId = fact.getContext().getId();
        Context context = instance.getContext(contextId);
        String value = fact.getValue();

        switch (name) {
            /* This is a must-have */
            case "AmendmentFlag":
                amendmentFlag = Boolean.parseBoolean(value);
                break;
            case "AmendmentDescription":
                amendmentDescription = value;
                break;
            /* This is a must-have */
            case "CurrentFiscalYearEndDate":
                yearEndDate = EdgarUtils.parseMonthDay(value);
                break;
            /* This is a must-have */
            case "EntityCommonStockSharesOutstanding":
                /* Not all shares counts are longs. We are truncating for simplicity! */
                if (fact.getLongValue() != null) {
                    sharesOutstandingMap.put(context, fact.getLongValue());
                } else if (fact.getDoubleValue() != null) {
                    double shareCount = fact.getDoubleValue();
                    sharesOutstandingMap.put(context, (long)shareCount);
                } else {
                    log.info("Common shares outstanding [{}] is neither long nor double", fact.getValue());
                }
                break;
            case "AuditorName":
            case "AuditorFirmId":
            case "AuditorLocation":
                /* TODO Need to handle this */
                break;
            default:
                if (SecurityInformation.isMember(name)) {
                    SecurityInformation securityInformation =
                            securityInformationMap.computeIfAbsent(context, k -> new SecurityInformation());
                    securityInformation.setPropertiesFrom(fact);
                } else if (DocumentInformation.isMember(name)) {
                    if (!context.hasDimensions()) {
                        documentInformation.setPropertiesFrom(fact);
                        if (documentInformation.documentType != null) {
                            primaryContext = context;
                        }
                    }
                } else if (EntityInformation.isMember(name)) {
                    if (!context.hasDimensions()) {
                        entityInformation.setPropertiesFrom(fact);
                    }
                } else {
                    log.info("Unsupported field {} in class {}", name, this.getClass().getName());
                }
                break;
        }
    }

    private static final int ALLOWANCE = 12;

    public static int guessFiscalYear(Dei dei, LocalDate periodEndDate) {
        MonthDay yearEndDate = dei.getYearEndDate();

        /* this will be the case most of the time, e.g. Q1 ends on yyyy-03-31 whereas the year-end is --12-31 */
        if (periodEndDate.getMonthValue() <= yearEndDate.getMonthValue()) {
            return periodEndDate.getYear();
        }

        /* this can happen if the fiscal year ends in the middle of the year, e.g. Apple, September 30 */
        return periodEndDate.getYear() + 1;
    }

    public static String guessFiscalPeriod(Dei dei, LocalDate periodEndDate) {
        MonthDay yearEndDate = dei.getYearEndDate();
        LocalDate fiscalEnd;
        try {
            /* this logic won't work for 02/29. Hopefully, this won't be a common occurrence */
            fiscalEnd = LocalDate.of(periodEndDate.getYear(), yearEndDate.getMonth(), yearEndDate.getDayOfMonth());
        } catch (DateTimeException e) {
            log.info("DateTimeException constructing LocalDate({}, {}, {}). [{}]",
                    periodEndDate.getYear(), yearEndDate.getMonth(), yearEndDate.getDayOfMonth(), e.getMessage());
            return dei.getFiscalPeriod();
        }

        long daysBetween = ChronoUnit.DAYS.between(periodEndDate, fiscalEnd);
        if (daysBetween < 0 && Math.abs(daysBetween) >= ALLOWANCE) {
            /* Can happen when fiscal year ends in the middle of a year, e,g. Apple's fiscal end is the end of September. */
            fiscalEnd = fiscalEnd.plusYears(1);
            daysBetween = ChronoUnit.DAYS.between(periodEndDate, fiscalEnd);
        }

        String fiscalPeriod = null;
        if (Math.abs(daysBetween - 270) < ALLOWANCE) {
            fiscalPeriod = "Q1";
        } else if (Math.abs(daysBetween - 180) < ALLOWANCE) {
            fiscalPeriod = "Q2";
        } else if (Math.abs(daysBetween - 90) < ALLOWANCE) {
            fiscalPeriod = "Q3";
        } else if (Math.abs(daysBetween) < ALLOWANCE) {
            fiscalPeriod = "FY";
        }

        if (fiscalPeriod == null)
            return dei.getFiscalPeriod();

        return fiscalPeriod;
    }

    public static final class DocumentInformation {
        private String fiscalPeriod;
        private int fiscalYear;
        /* This information is not always set. Using the document type to deduce this */
        private boolean annualReport;
        private boolean quarterlyReport;
        private boolean transitionReport;
        private String documentType;
        private LocalDate periodEndDate;

        public String getFiscalPeriod() {
            if (Objects.isNull(fiscalPeriod))
                return "";
            return fiscalPeriod;
        }

        public int getFiscalYear() {
            return fiscalYear;
        }

        public boolean isAnnualReport() {
            return documentType.startsWith("10-K");
        }

        public boolean isQuarterlyReport() {
            return documentType.startsWith("10-Q");
        }

        public boolean isTransitionReport() {
            return transitionReport;
        }

        public String getDocumentType() {
            return documentType;
        }

        public LocalDate getPeriodEndDate() {
            return periodEndDate;
        }

        private static boolean isMember(String name) {
            return name.startsWith("Document");
        }

        private DocumentInformation() {}

        @Override
        public String toString() {
            return String.format("%s, %d %s, ending on %s", documentType, fiscalYear, fiscalPeriod, periodEndDate);
        }

        private void setPropertiesFrom(Fact fact) {
            String name = fact.getConcept().getName();
            String value = fact.getValue();

            switch (name) {
                /* This is a must-have, however, this can be incorrect */
                case "DocumentFiscalPeriodFocus":
                    fiscalPeriod = Utils.trim(value);
                    break;
                /* This is a must-have, however, this can be incorrect */
                case "DocumentFiscalYearFocus":
                    fiscalYear = Utils.asInt(value);
                    break;
                case "DocumentAnnualReport":
                    annualReport = Boolean.parseBoolean(value);
                    break;
                case "DocumentQuarterlyReport":
                    quarterlyReport = Boolean.parseBoolean(value);
                    break;
                case "DocumentTransitionReport":
                    transitionReport = Boolean.parseBoolean(value);
                    break;
                /* This is a must-have */
                case "DocumentType":
                    documentType = Utils.trim(value);
                    break;
                /* This is a must-have */
                case "DocumentPeriodEndDate":
                    try {
                        periodEndDate = Utils.asDate(value);
                    } catch (DateTimeParseException e) {
                        /* If we are unable to parse the date value from the fact, we are going to guess it from the fact's context */
                        Period contextPeriod = fact.getContext().getPeriod();
                        if (contextPeriod instanceof Duration d) {
                            periodEndDate = d.getEndDate();
                            log.info("Setting periodEndDate to [{}] since [{}] is not parseable", periodEndDate.toString(), value);
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                default:
                    log.info("Unsupported field {} in class {}", name, this.getClass().getName());
                    break;
            }
        }
    }

    public static final class EntityInformation {
        private String centralIndexKey;
        private String fileNumber;
        private String registrantName;
        private String incorporationStateCountryCode;
        private String taxIdentificationNumber;
        private String addressLine1;
        private String cityOrTown;
        private String stateOrProvince;
        private String postalZipCode;
        private String country;
        private String countryCode;
        private String areaCode;
        private String localPhoneNumber;
        private String currentReportingStatus;
        private String filerCategory;
        private boolean emergingGrowthCompany;
        private String wellKnownSeasonedIssuer;
        private String voluntaryFilers;
        private long publicFloat;
        private boolean icfrAuditorAttestationFlag;

        @Override
        public String toString() {
            return String.format("%s, %s", centralIndexKey, registrantName);
        }

        public String getCentralIndexKey() {
            return centralIndexKey;
        }

        public String getFileNumber() {
            return fileNumber;
        }

        public String getRegistrantName() {
            return registrantName;
        }

        public String getIncorporationStateCountryCode() {
            return incorporationStateCountryCode;
        }

        public String getTaxIdentificationNumber() {
            return taxIdentificationNumber;
        }

        public String getAddressLine1() {
            return addressLine1;
        }

        public String getCityOrTown() {
            return cityOrTown;
        }

        public String getStateOrProvince() {
            return stateOrProvince;
        }

        public String getPostalZipCode() {
            return postalZipCode;
        }

        public String getCountry() {
            return country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getAreaCode() {
            return areaCode;
        }

        public String getLocalPhoneNumber() {
            return localPhoneNumber;
        }

        public String getCurrentReportingStatus() {
            return currentReportingStatus;
        }

        public String getFilerCategory() {
            return filerCategory;
        }

        public boolean isEmergingGrowthCompany() {
            return emergingGrowthCompany;
        }

        public String getWellKnownSeasonedIssuer() {
            return wellKnownSeasonedIssuer;
        }

        public String getVoluntaryFilers() {
            return voluntaryFilers;
        }

        public long getPublicFloat() {
            return publicFloat;
        }

        public boolean getIcfrAuditorAttestationFlag() {
            return icfrAuditorAttestationFlag;
        }

        private EntityInformation() {}

        private static boolean isMember(String name) {
            return name.startsWith("Entity") ||
                    name.equals("CountryRegion") ||
                    name.equals("CityAreaCode") ||
                    name.equals("LocalPhoneNumber") ||
                    name.equals("IcfrAuditorAttestationFlag");
        }

        private void setPropertiesFrom(Fact fact) {
            String name = fact.getConcept().getName();
            String value = fact.getValue();

            switch (name) {
                /* This is a must have */
                case "EntityCentralIndexKey":
                    centralIndexKey = Utils.trim(value);
                    break;
                case "EntityFileNumber":
                    fileNumber = Utils.trim(value);
                    break;
                /* This is a must have */
                case "EntityRegistrantName":
                    registrantName = Utils.trim(value);
                    break;
                case "EntityIncorporationStateCountryCode":
                    incorporationStateCountryCode = Utils.trim(value);
                    break;
                case "EntityTaxIdentificationNumber":
                    taxIdentificationNumber = Utils.trim(value);
                    break;
                case "EntityAddressAddressLine1":
                    addressLine1 = Utils.trim(value);
                    break;
                case "EntityAddressCityOrTown":
                    cityOrTown = Utils.trim(value);
                    break;
                case "EntityAddressStateOrProvince":
                    stateOrProvince = Utils.trim(value);
                    break;
                case "EntityAddressPostalZipCode":
                    postalZipCode = Utils.trim(value);
                    break;
                case "EntityAddressCountry":
                    country = Utils.trim(value);
                    break;
                case "CountryRegion":
                    countryCode = Utils.trim(value);
                    break;
                case "CityAreaCode":
                    areaCode = Utils.trim(value);
                    break;
                case "LocalPhoneNumber":
                    localPhoneNumber = Utils.trim(value);
                    break;
                /* This is a must have for a 10-K, optional for a 10-Q */
                case "EntityCurrentReportingStatus":
                    currentReportingStatus = Utils.trim(value);
                    break;
                /* This is a must have */
                case "EntityFilerCategory":
                    filerCategory = Utils.trim(value);
                    break;
                /* This is a must have */
                case "EntityEmergingGrowthCompany":
                    emergingGrowthCompany = Boolean.parseBoolean(value);
                    break;
                /* This is a must have for a 10-K, optional for a 10-Q */
                case "EntityWellKnownSeasonedIssuer":
                    wellKnownSeasonedIssuer = Utils.trim(value);
                    break;
                /* This is a must have for a 10-K, optional for a 10-Q */
                case "EntityVoluntaryFilers":
                    voluntaryFilers = Utils.trim(value);
                    break;
                /* This is a must have for a 10-K, optional for a 10-Q */
                case "EntityPublicFloat":
                    if (fact.getLongValue() != null) {
                        publicFloat = fact.getLongValue();
                    } else if (fact.getDoubleValue() != null) {
                        double pf = fact.getDoubleValue();
                        publicFloat = (long)pf;
                    } else {
                        log.info("Public float [{}] is neither long nor double", fact.getValue());
                    }
                    break;
                case "IcfrAuditorAttestationFlag":
                    icfrAuditorAttestationFlag = Boolean.parseBoolean(value);
                    break;
                default:
                    /* This is not a big deal. We are storing all Dei info as facts as well. Information is not lost */
                    break;
            }
        }
    }

    public static final class SecurityInformation {
        private String security12bTitle;
        private String security12gTitle;
        private String tradingSymbol;
        private String securityExchangeName;
        private boolean noTradingSymbolFlag;

        public String getSecurity12bTitle() {
            return security12bTitle;
        }

        public String getSecurity12gTitle() {
            return security12gTitle;
        }

        public String getTradingSymbol() {
            return tradingSymbol;
        }

        public String getSecurityExchangeName() {
            return securityExchangeName;
        }

        public boolean getNoTradingSymbolFlag() {
            return noTradingSymbolFlag;
        }

        private static boolean isMember(String name) {
            switch (name) {
                case "NoTradingSymbolFlag":
                case "Security12bTitle":
                case "Security12gTitle":
                case "TradingSymbol":
                case "SecurityExchangeName":
                    return true;
                default:
                    return false;
            }
        }

        private void setPropertiesFrom(Fact fact) {
            String name = fact.getConcept().getName();
            String value = fact.getValue();

            switch (name) {
                case "NoTradingSymbolFlag":
                    noTradingSymbolFlag = Boolean.parseBoolean(value);
                    break;
                case "Security12bTitle":
                    security12bTitle = Utils.trim(value);
                    break;
                case "Security12gTitle":
                    security12gTitle = Utils.trim(value);
                    break;
                case "TradingSymbol":
                    tradingSymbol = Utils.trim(value);
                    break;
                case "SecurityExchangeName":
                    securityExchangeName = Utils.trim(value);
                    break;
                default:
                    log.info("Unsupported field {} in class {}", name, this.getClass().getName());
                    break;
            }
        }

        private SecurityInformation() {
        }
    }
}
