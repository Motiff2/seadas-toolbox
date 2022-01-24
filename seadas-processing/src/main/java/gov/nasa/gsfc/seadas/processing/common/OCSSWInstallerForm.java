package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.swing.TableLayout;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamUtils;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import gov.nasa.gsfc.seadas.processing.preferences.SeadasToolboxDefaults;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_TAG_PROPERTY;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_SRC_DIR_NAME;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/13/13
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OCSSWInstallerForm extends JPanel implements CloProgramUI {


    //private FileSelector ocsswDirSelector;
    JTextField fileTextField;
    //private SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    ProcessorModel processorModel;
    private AppContext appContext;
    private JPanel dirPanel;
    private JPanel tagPanel;
    private JPanel missionPanel;
    private JPanel otherPanel;

    //private JPanel superParamPanel;

    public static final String INSTALL_DIR_OPTION_NAME = "--install_dir";
    public final static String VALID_TAGS_OPTION_NAME = "--tag";
    public final static String CURRENT_TAG_OPTION_NAME = "--current_tag";

    public String missionDataDir;
    public OCSSW ocssw;

    private static final Set<String> MISSIONS = new HashSet<String>(Arrays.asList(
            new String[]{"AQUARIUS",
                    "AVHRR",
                    "CZCS",
                    "GOCI",
                    "HAWKEYE",
                    "HICO",
                    "MERIS",
                    "MODISA",
                    "MODIST",
                    "MOS",
                    "OCM1",
                    "OCM2",
                    "OCRVC",
                    "OCTS",
                    "OLI",
                    "OSMI",
                    "SGLI",
                    "SEAWIFS",
                    "VIIRSN",
                    "VIIRSJ1"}
    ));
    private static final Set<String> DEFAULT_MISSIONS = new HashSet<String>(Arrays.asList(
            new String[]{
                    //"GOCI",
                    //"HICO",
                    "OCRVC"
            }
    ));


    ArrayList<String> validOCSSWTagList = new ArrayList<>();
    String tagDefault = "V2022.0"; // a hard default which get replace by JSON value if internet connection


    HashMap<String, Boolean> missionDataStatus;


    public OCSSWInstallerForm(AppContext appContext, String programName, String xmlFileName, OCSSW ocssw) {
        this.appContext = appContext;
        this.ocssw = ocssw;
        getSeaDASVersionTags();


        // set default
        if (validOCSSWTagList != null && validOCSSWTagList.size() >= 1) {
            tagDefault = validOCSSWTagList.get(0);
        }

        for (String tag : validOCSSWTagList) {
            System.out.println("tag=" + tag);
        }

        processorModel = ProcessorModel.valueOf(programName, xmlFileName, ocssw);
        processorModel.setReadyToRun(true);
        setMissionDataDir(OCSSWInfo.getInstance().getOcsswDataDirPath());
        init();
        updateMissionValues();
        createUserInterface();
        processorModel.updateParamInfo(INSTALL_DIR_OPTION_NAME, getInstallDir());
        processorModel.addPropertyChangeListener(INSTALL_DIR_OPTION_NAME, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setMissionDataDir(processorModel.getParamValue(INSTALL_DIR_OPTION_NAME) + File.separator + OCSSWInfo.OCSSW_DATA_DIR_SUFFIX);
                updateMissionStatus();
                updateMissionValues();
                createUserInterface();
            }
        });
    }

    String getMissionDataDir() {
        return missionDataDir;
    }

    void setMissionDataDir(String currentMissionDataDir) {
        missionDataDir = currentMissionDataDir;
    }

    abstract void updateMissionStatus();

    abstract void updateMissionValues();

    String getInstallDir() {
        return OCSSWInfo.getInstance().getOcsswRoot();
    }

    abstract void init();

    public ProcessorModel getProcessorModel() {
        return processorModel;
    }

    public File getSelectedSourceProduct() {
        return null;
    }

    public boolean isOpenOutputInApp() {
        return false;
    }

    public String getParamString() {
        return processorModel.getParamList().getParamString();
    }

    public void setParamString(String paramString) {
        processorModel.getParamList().setParamString(paramString);
    }

    protected void createUserInterface() {

        this.setLayout(new GridBagLayout());

        JPanel paramPanel = new ParamUIFactory(processorModel).createParamPanel();
        reorganizePanel(paramPanel);

        add(dirPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        add(tagPanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));

        add(missionPanel,
                new GridBagConstraintsCustom(0, 2, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        add(otherPanel,
                new GridBagConstraintsCustom(0, 3, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));

        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    public JPanel getParamPanel() {
        JPanel newPanel = new JPanel(new GridBagLayout());
        newPanel.add(missionPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        newPanel.add(otherPanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 2));
        return newPanel;
    }
    //ToDo: missionDataDir test should be differentiated for local and remote servers

    protected void reorganizePanel(JPanel paramPanel) {
        final Preferences preferences = Config.instance("seadas").load().preferences();
        String ocsswTagString = preferences.get(SEADAS_OCSSW_TAG_PROPERTY, tagDefault);

        dirPanel = new JPanel();
        tagPanel = new JPanel();
        missionPanel = new JPanel(new TableLayout(5));
        missionPanel.setBorder(BorderFactory.createTitledBorder("Mission Data"));

        otherPanel = new JPanel();
        TableLayout otherPanelLayout = new TableLayout(3);
        otherPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        otherPanel.setLayout(otherPanelLayout);
        otherPanel.setBorder(BorderFactory.createTitledBorder("Others"));
        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();

        JScrollPane jsp = (JScrollPane) paramPanel.getComponent(0);
        JPanel panel = (JPanel) findJPanel(jsp, "param panel");
        Component[] options = panel.getComponents();
        String tmpString;
        for (Component option : options) {
            if (option.getName().equals("boolean field panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                for (Component c : bps) {
                    tmpString = ParamUtils.removePreceedingDashes(c.getName()).toUpperCase();
                    if (MISSIONS.contains(tmpString)) {
                        if (!DEFAULT_MISSIONS.contains(tmpString)) {
                            if (ocssw.isMissionDirExist(tmpString) ||
                                    missionDataStatus.get(tmpString)) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            } else {
                                ((JPanel) c).getComponents()[0].setEnabled(true);
                            }
                            missionPanel.add(c);
                        }
                    } else {
                        if (tmpString.equals("SRC")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("Source Code");
                            if (new File(ocsswInfo.getOcsswRoot() + System.getProperty("file.separator") + OCSSW_SRC_DIR_NAME).exists()) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            }
                        } else if (tmpString.equals("CLEAN")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("Clean Install");
                            ((JPanel) c).getComponents()[0].setEnabled(true);
                        } else if (tmpString.equals("VIIRSDEM")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("VIIRS DEM files");
                            if (new File(ocsswInfo.getOcsswRoot() + System.getProperty("file.separator") +
                                    "share" + System.getProperty("file.separator") + "viirs" +
                                    System.getProperty("file.separator") + "dem").exists()) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            }
                        }
                        otherPanel.add(c);
                        otherPanel.add(new JLabel("      "));
                    }
                }
            } else if (option.getName().equals("file parameter panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                for (Component c : bps) {
                    dirPanel = (JPanel) c;

                }
                if (!ocsswInfo.getOcsswLocation().equals(ocsswInfo.OCSSW_LOCATION_LOCAL)) {
                    //if ocssw is not local, then disable the button to choose ocssw installation directory
                    ((JLabel) dirPanel.getComponent(0)).setText("Remote install_dir");
                } else {
                    ((JLabel) dirPanel.getComponent(0)).setText("Local install_dir");
                }
                ((JLabel) dirPanel.getComponent(0)).setToolTipText("This directory can be set in SeaDAS-OCSSW > OCSSW Configuration");
                ((JTextField) dirPanel.getComponent(1)).setEditable(false);
                ((JTextField) dirPanel.getComponent(1)).setFocusable(false);
                ((JTextField) dirPanel.getComponent(1)).setEnabled(false);
                ((JTextField) dirPanel.getComponent(1)).setBorder(null);
                ((JTextField) dirPanel.getComponent(1)).setDisabledTextColor(Color.BLUE);
                ((JTextField) dirPanel.getComponent(1)).setForeground(Color.BLUE);
                ((JTextField) dirPanel.getComponent(1)).setBackground(dirPanel.getBackground());
//                ((JTextField) dirPanel.getComponent(1)).setBackground(new Color(250,250,250));
                ((JTextField) dirPanel.getComponent(1)).setToolTipText("This directory can be set in SeaDAS-OCSSW > OCSSW Configuration");
                dirPanel.getComponent(2).setVisible(false);

            } else if (option.getName().equals("text field panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                JPanel tempPanel1, tempPanel2;
                for (Component c : bps) {
                    if (c.getName().equals(VALID_TAGS_OPTION_NAME)) {
                        tempPanel1 = (JPanel) c;


                        try {
                            Version latestReleaseVersion = readVersionFromStream(new URL(SystemUtils.getApplicationRemoteVersionUrl()).openStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String currentVersion = SystemUtils.getReleaseVersion();


                        if (tagDefault.equals(ocsswTagString)) {
                            ((JLabel) tempPanel1.getComponent(0)).setText("OCSSW Tags: latest tag installed ");
                            ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.BLACK);
                        } else {
                            ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tags: <br>WARNING!! latest tag is " + tagDefault + "</html>");
                            ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.RED);
                        }


                        JComboBox tags = ((JComboBox) tempPanel1.getComponent(1));
                        tags.setMaximumRowCount(15);
                        JLabel tmp = new JLabel("12345678901234567890");
                        tags.setMinimumSize(tmp.getPreferredSize());


                        //This segment of code is to disable tags that are not compatible with the current SeaDAS version
                        ArrayList<String> validOcsswTags = ocssw.getOcsswTagsValid4CurrentSeaDAS();
                        Font f1 = tags.getFont();
                        Font f2 = new Font("Tahoma", 0, 14);

                        if (ocsswTagString != null) {
                            tags.setSelectedItem(ocsswTagString);
                        }

                        tags.setToolTipText("Latest tag for this release is " + ocsswTagString);



                        tags.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                          int index, boolean isSelected, boolean cellHasFocus) {
                                if (value instanceof JComponent)
                                    return (JComponent) value;

                                boolean itemEnabled = validOCSSWTagList.contains((String) value);
                                super.getListCellRendererComponent(list, value, index,
                                        isSelected && itemEnabled, cellHasFocus);


                                if (itemEnabled) {
                                    if (ocsswTagString.equals(value.toString().trim())) {
                                        list.setToolTipText(value.toString() + " is latest operational tag for this release");
                                    } else {
                                        list.setToolTipText(value.toString() + " is NOT the latest operational tag for this release");
                                    }
                                    if (isSelected) {
                                        setBackground(Color.blue);
                                        setForeground(Color.white);
                                    } else {
                                        setBackground(Color.white);
                                        setForeground(Color.black);
                                    }
                                } else {
                                    if (value.toString().toUpperCase().startsWith("V")) {
                                        list.setToolTipText(value.toString() + " is NOT an optimum operational tag for this release");
                                    } else {
                                        list.setToolTipText(value.toString() + " is NOT an operational tag");
                                    }
                                    if (isSelected) {
                                        setBackground(Color.darkGray);
                                        setForeground(Color.white);
                                    } else {
                                        setBackground(Color.white);
                                        setForeground(Color.gray);
                                    }
                                }

                                return this;
                            }
                        });

                        // code segment ends here
                        tagPanel.add(tempPanel1);
                        ;
                    } else if (c.getName().contains(CURRENT_TAG_OPTION_NAME)) {
                        //|| CURRENT_TAG_OPTION_NAME.contains(c.getName())) {
                        tempPanel2 = (JPanel) c;
                        ((JLabel) tempPanel2.getComponent(0)).setText("Last Installed OCSSW Tag:");
                        tagPanel.add(tempPanel2);
                    }
                }
            }
        }

    }


    private static Version readVersionFromStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            line = reader.readLine();
            if (line != null) {
                return Version.parseVersion(line.toUpperCase());
            }
        }
        return null;
    }




    private Component findJPanel(Component comp, String panelName) {
        if (comp.getClass() == JPanel.class) return comp;
        if (comp instanceof Container) {
            Component[] components = ((Container) comp).getComponents();
            for (int i = 0; i < components.length; i++) {
                Component child = findJPanel(components[i], components[i].getName());
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }


    private void getSeaDASVersionTags() {

        JSONParser jsonParser = new JSONParser();
        try {

            URL tagsURL = new URL("https://oceandata.sci.gsfc.nasa.gov/manifest/seadasVersions.json");
            URLConnection tagsConnection = tagsURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(tagsConnection.getInputStream()));

            //Read JSON file
            Object obj = jsonParser.parse(in);

            JSONArray validSeaDASTags = (JSONArray) obj;
            //System.out.println(validSeaDASTags);

            //Iterate over seadas tag array
            validSeaDASTags.forEach(tagObject -> parseValidSeaDASTagObject((JSONObject) tagObject));
            in.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void parseValidSeaDASTagObject(JSONObject tagObject) {
        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        String seadasToolboxVersion = String.valueOf(seadasProcessingModuleInfo.getSpecificationVersion());

        String seadasToolboxVersionJson = (String) tagObject.get("seadas");

        if (seadasToolboxVersionJson.equals(seadasToolboxVersion)) {
            //Get corresponding ocssw tags for seadas
            JSONArray ocsswTags = (JSONArray) tagObject.get("ocssw");
            if (ocsswTags != null) {
                for (int i = 0; i < ocsswTags.size(); i++) {
                    try {
                        validOCSSWTagList.add((String) ocsswTags.get(i));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}