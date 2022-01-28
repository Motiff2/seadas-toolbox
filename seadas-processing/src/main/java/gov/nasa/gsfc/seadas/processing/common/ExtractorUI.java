package gov.nasa.gsfc.seadas.processing.common;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import org.esa.snap.rcp.util.Dialogs;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 5/3/12
 * Time: 3:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtractorUI extends ProgramUIFactory {

    public static final String START_LINE_PARAM_NAME = "sline";
    public static final String END_LINE_PARAM_NAME = "eline";
    public static final String START_PIXEL_PARAM_NAME = "spixl";
    public static final String END_PIXEL_PARAM_NAME = "epixl";

    public static final String GEO_LOCATE_PROGRAM_NAME_VIIRS = "geolocate_viirs";
    public static final String GEO_LOCATE_PROGRAM_NAME_MODIS = "modis_GEO";

    private ProcessorModel lonlat2pixline;
    private JPanel pixelPanel;
    private JPanel newsPanel;
    private JPanel paramPanel;

    private ParamUIFactory paramUIFactory;

    private boolean initiliazed = false;


    HashMap<String, Boolean> paramCounter;
    //OCSSW ocssw;

    public ExtractorUI(String programName, String xmlFileName, OCSSW ocssw) {
        super(programName, xmlFileName, ocssw);
        paramCounter = new HashMap<String, Boolean>();
        initiliazed = true;
        //this.ocssw = ocssw;
    }

    private void initLonLatProcessor() {
        lonlat2pixline = ProcessorModel.valueOf("lonlat2pixline", "lonlat2pixline.xml", ocssw);
        lonlat2pixline.addPropertyChangeListener(lonlat2pixline.getAllparamInitializedPropertyName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                processorModel.updateParamInfo(START_PIXEL_PARAM_NAME, lonlat2pixline.getParamValue(START_PIXEL_PARAM_NAME));
                processorModel.updateParamInfo(END_PIXEL_PARAM_NAME, lonlat2pixline.getParamValue(END_PIXEL_PARAM_NAME));
                processorModel.updateParamInfo(START_LINE_PARAM_NAME, lonlat2pixline.getParamValue(START_LINE_PARAM_NAME));
                processorModel.updateParamInfo(END_LINE_PARAM_NAME, lonlat2pixline.getParamValue(END_LINE_PARAM_NAME));
            }
        });

        processorModel.addPropertyChangeListener(processorModel.getPrimaryInputFileOptionName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (processorModel.getProgramName() != null) {
                    lonlat2pixline.updateIFileInfo(getLonLattoPixelsIFileName(processorModel.getParamInfo(processorModel.getPrimaryInputFileOptionName()).getValue().trim(), processorModel.getProgramName()));
                }
            }
        });
        if (processorModel.getParamInfo(processorModel.getPrimaryInputFileOptionName()).getValue().trim().length() > 0) {
            lonlat2pixline.updateIFileInfo(processorModel.getParamInfo(processorModel.getPrimaryInputFileOptionName()).getValue().trim());
        }
    }

    private void initStaticPanels() {
        newsPanel = new ParamUIFactory(lonlat2pixline).createParamPanel(lonlat2pixline);
        newsPanel.setBorder(BorderFactory.createTitledBorder("Lon/Lat"));
        newsPanel.setName("newsPanel");
    }

    @Override
    public JPanel getParamPanel() {

        if (!initiliazed) {
            initLonLatProcessor();
            initStaticPanels();
        }

        SeadasFileUtils.debug("updating ofile change listener ...  processorModel   " + processorModel.getPrimaryOutputFileOptionName());
        paramUIFactory = new ExtractorParamUI(processorModel);
        pixelPanel = paramUIFactory.createParamPanel(processorModel);
        pixelPanel.setBorder(BorderFactory.createTitledBorder("Pixels"));
        pixelPanel.setName("pixelPanel");
        paramPanel = new JPanel(new GridBagLayout());
        paramPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        paramPanel.setPreferredSize(new Dimension(700, 400));
        paramPanel.add(newsPanel,
                new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        paramPanel.add(Box.createRigidArea(new Dimension(100, 50)),
                new GridBagConstraintsCustom(0, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        paramPanel.add(pixelPanel,
                new GridBagConstraintsCustom(0, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        return paramPanel;
    }


    private String getLonLattoPixelsIFileName(String ifileName, String programName) {

        try {
            String geoFileName = null;
            FileInfo iFileInfo = new FileInfo(ifileName);
            FileInfo geoFileInfo = FilenamePatterns.getGeoFileInfo(iFileInfo, ocssw);
            if (geoFileInfo.getFile().exists()) {
                geoFileName = geoFileInfo.getFile().getName();
                return geoFileName;
            }
            if (programName.contains("l1aextract_modis")) {
                String suffix = ".GEO";
                geoFileName = (ifileName.substring(0, ifileName.lastIndexOf("."))).concat(suffix);

            } else if (programName.contains("l1aextract_viirs")) {
                String suffix = null;
                if (ifileName.contains("JPSS1")) {
                    suffix = ".GEO-M_JPSS1";
                } else if (ifileName.contains("SNPP")) {
                    suffix = ".GEO-M_SNPP";
                }

                if (suffix != null) {
                    geoFileName = (ifileName.substring(0, ifileName.lastIndexOf(".L"))).concat(suffix);
                }

            }

            if (geoFileName != null) {
                if (new File(geoFileName).exists()) {
                    return geoFileName;

                } else {
                    geoFileName = geoFileName + ".nc";

                    if (new File(geoFileName).exists()) {
                        return geoFileName;
                    } else {
                        Dialogs.showError(ifileName + " requires a GEO file to be extracted. " + geoFileName + " does not exist.");
                        return null;
                    }
                }
            }


        } catch (Exception e) {

        }
        return ifileName;
    }

    private String getGeoLocateProgramName(String programName) {

        String geoLocateProgramName = null;

        if (programName.contains("modis")) {
            geoLocateProgramName = GEO_LOCATE_PROGRAM_NAME_MODIS;
        } else if (programName.contains("viirs")) {
            geoLocateProgramName = GEO_LOCATE_PROGRAM_NAME_VIIRS;

        }
        return geoLocateProgramName;
    }
}
