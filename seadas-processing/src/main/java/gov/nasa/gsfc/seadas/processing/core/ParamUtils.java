package gov.nasa.gsfc.seadas.processing.core;

import gov.nasa.gsfc.seadas.processing.common.XmlReader;
import gov.nasa.gsfc.seadas.processing.preferences.SeadasToolboxDefaults;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/19/12
 * Time: 8:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParamUtils {

    private String OCDATAROOT = System.getenv("OCDATAROOT");

    public static final String PAR = "par";
    public static final String GEOFILE = "geofile";
    public static final String SPIXL = "spixl";
    public static final String EPIXL = "epixl";
    public static final String DPIXL = "dpixl";
    public static final String SLINE = "sline";
    public static final String ELINE = "eline";
    public static final String DLINE = "dline";
    public static final String NORTH = "north";
    public static final String SOUTH = "south";
    public static final String WEST = "west";
    public static final String EAST = "east";
    public static final String IFILE = "ifile";
    public static final String OFILE = "ofile";

    public static final String OPTION_NAME = "name";
    public static final String OPTION_TYPE = "type";

    public static final String XML_ELEMENT_HAS_GEO_FILE = "hasGeoFile";
    public static final String XML_ELEMENT_HAS_PAR_FILE = "hasParFile";

    public static final String NO_XML_FILE_SPECIFIED = "No XML file Specified";

    public final String INVALID_IFILE_EVENT = "INVALID_IFILE_EVENT";
    public final String PARFILE_CHANGE_EVENT = "PARFILE_CHANGE_EVENT";

    public final String WAVE_LIMITER_CHANGE_EVENT = "WAVE_LIMITER_EVENT";

    public final String DEFAULTS_CHANGED_EVENT = "DEFAULTS_CHANGED_EVENT";

    public final static String DEFAULT_PAR_FILE_NAME = "par";
    public final static String DEFAULT_PROGRESS_REGEX = "Processing scan .+?\\((\\d+) of (\\d+)\\)";

    private static int longestIFileNameLength;

    public enum nullValueOverrides {
        IFILE, OFILE, PAR, GEOFILE
    }

    public static Set<String> getPrimaryOptions(String parXMLFileName) {

        Set<String> primaryOptions = new HashSet<String>();
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("primaryOption");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("primaryOptions does not exist!");
            primaryOptions.add("ifile");
            primaryOptions.add("ofile");
            return primaryOptions;
        }
        for (int i = 0; i < optionNodelist.getLength(); i++) {
            Element optionElement = (Element) optionNodelist.item(i);
            String name = optionElement.getFirstChild().getNodeValue();
            primaryOptions.add(name);
        }
        return primaryOptions;
    }

    public static String getParFileOptionName(String parXMLFileName) {

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("parFileOptionName");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("par file option name is not specified in the xml file. 'par' is used as a default name.");
            return DEFAULT_PAR_FILE_NAME;
        }
        return optionNodelist.item(0).getFirstChild().getNodeValue();
    }

    public static String getProgressRegex(String parXMLFileName) {

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("progressRegex");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("progress meter regular expression is not specified in the xml file.");
            return DEFAULT_PROGRESS_REGEX;
        }
        return optionNodelist.item(0).getFirstChild().getNodeValue();
    }

    public static boolean getOptionStatus(String parXMLFileName, String elementName) {

        boolean optionStatus = false;
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName(elementName);
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning(elementName + " exist: " + optionStatus);
            return optionStatus;
        }
        Element metaDataElement = (Element) optionNodelist.item(0);

        String name = metaDataElement.getTagName();
        //SeadasLogger.getLogger().fine("tag name: " + name);
        //   if (name.equals(elementName)) {
        optionStatus = Boolean.parseBoolean(metaDataElement.getFirstChild().getNodeValue());
        //SeadasLogger.getLogger().fine(name + " value = " + metaDataElement.getFirstChild().getNodeValue() + " " + optionStatus);
        //  }

        return optionStatus;
    }


    public static int getLongestIFileNameLength() {
        return longestIFileNameLength;
    }

    public static ArrayList<ParamInfo> computeParamList(String paramXmlFileName) {

        if (paramXmlFileName.equals(NO_XML_FILE_SPECIFIED)) {
            return getDefaultParamList();
        }

        final ArrayList<ParamInfo> paramList = new ArrayList<ParamInfo>();

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(paramXmlFileName);
        if (paramStream == null) {
            Dialogs.showError("XML file " + paramXmlFileName + " not found.");
            return null;
        }

        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        if (rootElement == null) {
            Dialogs.showError("XML file " + paramXmlFileName + " root element not found.");
            return null;
        }
        NodeList optionNodelist = rootElement.getElementsByTagName("option");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            return null;
        }

        longestIFileNameLength = 0;

        for (int i = 0; i < optionNodelist.getLength(); i++) {

            Element optionElement = (Element) optionNodelist.item(i);

            String name = XmlReader.getTextValue(optionElement, OPTION_NAME);
            debug("option name: " + name);
            String tmpType = XmlReader.getAttributeTextValue(optionElement, OPTION_TYPE);
            debug("option type: " + tmpType);

            ParamInfo.Type type = ParamInfo.Type.HELP;

            if (tmpType != null) {
                if (tmpType.toLowerCase().equals("boolean")) {
                    type = ParamInfo.Type.BOOLEAN;
                } else if (tmpType.toLowerCase().equals("int")) {
                    type = ParamInfo.Type.INT;
                } else if (tmpType.toLowerCase().equals("float")) {
                    type = ParamInfo.Type.FLOAT;
                } else if (tmpType.toLowerCase().equals("string")) {
                    type = ParamInfo.Type.STRING;
                } else if (tmpType.toLowerCase().equals("ifile")) {
                    type = ParamInfo.Type.IFILE;
                    if (name.length() > longestIFileNameLength) {
                        longestIFileNameLength = name.length();
                    }
                } else if (tmpType.toLowerCase().equals("ofile")) {
                    type = ParamInfo.Type.OFILE;
                } else if (tmpType.toLowerCase().equals("help")) {
                    type = ParamInfo.Type.HELP;
                } else if (tmpType.toLowerCase().equals("dir")) {
                    type = ParamInfo.Type.DIR;
                } else if (tmpType.toLowerCase().equals("flags")) {
                    type = ParamInfo.Type.FLAGS;
                } else if (tmpType.toLowerCase().equals("button")) {
                    type = ParamInfo.Type.BUTTON;
                }
            }

            String value = XmlReader.getTextValue(optionElement, "value");

            if (name != null) {
                String nullValueOverrides[] = {ParamUtils.IFILE, ParamUtils.OFILE, ParamUtils.PAR, ParamUtils.GEOFILE};
                for (String nullValueOverride : nullValueOverrides) {
                    if (name.equals(nullValueOverride)) {
                        value = ParamInfo.NULL_STRING;
                    }
                }
            }

            String description = XmlReader.getTextValue(optionElement, "description");
            String source = XmlReader.getTextValue(optionElement, "source");
            String order = XmlReader.getTextValue(optionElement, "order");
            String usedAs = XmlReader.getTextValue(optionElement, "usedAs");
            String defaultValue = XmlReader.getTextValue(optionElement, "default");
            // set the value and the default to the current value from the XML file
            //ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, value) : new ParamInfo(name, value, type, value);
            ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, defaultValue) : new ParamInfo(name, value, type, defaultValue);
            paramInfo.setDescription(description);
            paramInfo.setSource(source);

            if (order != null) {
                paramInfo.setOrder(new Integer(order).intValue());
            }

            if (usedAs != null) {
                paramInfo.setUsedAs(usedAs);
            }

            NodeList validValueNodelist = optionElement.getElementsByTagName("validValue");

            if (validValueNodelist != null && validValueNodelist.getLength() > 0) {
                for (int j = 0; j < validValueNodelist.getLength(); j++) {

                    Element validValueElement = (Element) validValueNodelist.item(j);

                    String validValueValue = XmlReader.getTextValue(validValueElement, "value");
                    String validValueDescription = XmlReader.getTextValue(validValueElement, "description");

                    ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(validValueValue);

                    paramValidValueInfo.setDescription(validValueDescription);
                    paramInfo.addValidValueInfo(paramValidValueInfo);
                }

            }

            final String optionName = ParamUtils.removePreceedingDashes(paramInfo.getName());

            if ("l3mapgen.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case "product":
                        paramInfo.setValue(l3mapgenPreferenceProduct());
                        break;
                    case "projection":
                        paramInfo.setValue(l3mapgenPreferenceProjection());
                        break;
                    case "resolution":
                        paramInfo.setValue(l3mapgenPreferenceResolution());
                        break;
                    case "interp":
                        paramInfo.setValue(l3mapgenPreferenceInterp());
                        break;
                    case "north":
                        paramInfo.setValue(l3mapgenPreferenceNorth());
                        break;
                    case "south":
                        paramInfo.setValue(l3mapgenPreferenceSouth());
                        break;
                    case "west":
                        paramInfo.setValue(l3mapgenPreferenceWest());
                        break;
                    case "east":
                        paramInfo.setValue(l3mapgenPreferenceEast());
                        break;
                }
            }


            paramList.add(paramInfo);

        }

        return paramList;
    }



    private static String l3mapgenPreferenceProduct() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
    }

    private static String l3mapgenPreferenceProjection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
    }

    private static String l3mapgenPreferenceResolution() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
    }

    private static String l3mapgenPreferenceInterp() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_DEFAULT);
    }

    private static String l3mapgenPreferenceNorth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_DEFAULT);
    }

    private static String l3mapgenPreferenceSouth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
    }

    private static String l3mapgenPreferenceWest() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_DEFAULT);
    }

    private static String l3mapgenPreferenceEast() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_DEFAULT);
    }



    /**
     * Create a default array list with ifile, ofile,  spixl, epixl, sline, eline options
     *
     * @return
     */
    public static ArrayList<ParamInfo> getDefaultParamList() {
        ArrayList<ParamInfo> defaultParamList = new ArrayList<ParamInfo>();
        return defaultParamList;
    }

    static void debug(String debugMessage) {
        //System.out.println(debugMessage);
    }

    public static String removePreceedingDashes(String optionName) {
        return optionName.replaceAll("--", "");
    }


}
