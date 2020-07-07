package gov.nasa.gsfc.seadas.processing.common;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import org.esa.snap.ui.AppContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 1/20/15
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class OCSSWInstallerFormLocal extends OCSSWInstallerForm {

    public OCSSWInstallerFormLocal(AppContext appContext, String programName, String xmlFileName, OCSSW ocssw) {
        super(appContext, programName, xmlFileName, ocssw);

    }

    void updateMissionStatus() {
        missionDataStatus = new HashMap<String, Boolean>();
        missionDataStatus.put("SEAWIFS", ocssw.isMissionDirExist("seawifs"));
        missionDataStatus.put("MODISA", ocssw.isMissionDirExist("modisa"));
        missionDataStatus.put("MODIST", ocssw.isMissionDirExist("modist"));
        missionDataStatus.put("VIIRSN", ocssw.isMissionDirExist("viirsn"));
        missionDataStatus.put("VIIRSJ1", ocssw.isMissionDirExist("viirsj1"));
        missionDataStatus.put("MERIS", ocssw.isMissionDirExist("meris"));
        missionDataStatus.put("CZCS", ocssw.isMissionDirExist("czcs"));
        missionDataStatus.put("AQUARIUS", ocssw.isMissionDirExist("aquarius"));
        missionDataStatus.put("OCTS", ocssw.isMissionDirExist("octs"));
        missionDataStatus.put("OLI", ocssw.isMissionDirExist("oli"));
        missionDataStatus.put("OSMI", ocssw.isMissionDirExist("osmi"));
        missionDataStatus.put("MOS", ocssw.isMissionDirExist("mos"));
        missionDataStatus.put("OCM2", ocssw.isMissionDirExist("ocm2"));
        missionDataStatus.put("OCM1", ocssw.isMissionDirExist("ocm1"));
        missionDataStatus.put("AVHRR", ocssw.isMissionDirExist("avhrr"));
        missionDataStatus.put("HICO", ocssw.isMissionDirExist("hico"));
        missionDataStatus.put("GOCI", ocssw.isMissionDirExist("goci"));
    }

    void init(){
        updateMissionStatus();
    }

    void updateMissionValues() {

        for (Map.Entry<String, Boolean> entry : missionDataStatus.entrySet()) {
            String missionName = entry.getKey();
            Boolean missionStatus = entry.getValue();

            if (missionStatus) {
                processorModel.setParamValue("--" + missionName.toLowerCase(), "1");
            } else {
                processorModel.setParamValue("--" + missionName.toLowerCase(), "0");
            }

        }
        if (new File(OCSSWInfo.getInstance().getOcsswRoot(), "ocssw-src").exists()) {
            processorModel.setParamValue("--src", "1");
        }
        if (new File(OCSSWInfo.getInstance().getOcsswRoot(), "share/viirs/dem").exists()) {
            processorModel.setParamValue("--viirsdem", "1");
        }
    }
}
