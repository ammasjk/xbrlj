# xbrlj

Core Java library that implements XBRL parsing and analysis. To get started,

1. Construct a new XbrlReader as follows

        XbrlReader reader = new XbrlReader();

2. Get XbrlInstance from the reader

        //
        // url can be a http url to the root .xml document, 
        //     e.g. https://www.sec.gov/Archives/edgar/data/773840/000093041315000621/hon-20141231.xml for Honeywell, FY 2014
        // It can also be a file url, a zip url to the entire xbrl instance or a zip url to an iXBRL instance
        // There are other variations of this basic method. Look at the class for more details or XbrlTest in src/test
        //
        XbrlInstance instance = reader.getInstance(url)

3. Introspect the instance. The simplest introspection is printing the instance to a text file as follows

        PrintWriter writer = new PrintWriter(new FileWriter(path));
        PrettyPrinter printer = new PrettyPrinter(writer, true, true, false);
        PresentationNetwork fileWriter = new PresentationNetwork(instance, printer);

        DiscoverableTaxonomySet dts = instance.getTaxonomy();
        Collection<RoleType> roles = dts.getReportableRoleTypes();
        for (RoleType roleType : roles) {
            fileWriter.process(roleType);
        }
   
   Here is a sample output in json (https://datanapis.io/JPM_20210930_ZIP.json) and text (https://datanapis.io/JPM_20210930_ZIP.txt)


5. You can do many more things such as exploring the concepts and facts used by the XbrlInstance, exploring the DefinitionNetwork, the CalculationNetwork, validating calculations, exploring the Discoverable Taxonomy Set, etc.

I have run this on 305,676 XBRL instances submitted to Edgar starting from 2012 with 99.59% success rate.
You can download these statistics from here - https://datanapis.io/xbrl_results.csv

The software for downloading from Edgar and processing all those instances is not part of this repository.

Before you can start to download XBRL instances from Edgar, you may need to set the property user.agent to a valid email. See the Edgar documentation for more details.
