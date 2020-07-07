package gov.nasa.gsfc.seadas.processing.ocssw;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.about.SnapAboutBox;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.UIUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSW.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;


/**
 * @author Aynur Abdurazik
 * @author Bing Yang
 *
 */
// May 2020 - Yang - print out relevant system info for SeaDAS or OCSSW troubleshooting

public class GetSysInfoGUI {

    final String PANEL_NAME = "Seadas Configuration";
    final String HELP_ID = "getSysInfo";
    String sysInfoText;

    ModalDialog modalDialog;

//    PropertyContainer pc = new PropertyContainer();

    boolean windowsOS;

    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        GetSysInfoGUI getSysInfoGUI = new GetSysInfoGUI();
        getSysInfoGUI.init(parent);
    }

    public void init(Window parent) {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem.toLowerCase().contains("windows")) {
            windowsOS = true;
        } else {
            windowsOS = false;
        }


        JPanel mainPanel = GridBagUtils.createPanel();

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_CANCEL_HELP, HELP_ID){
            @Override

            protected void onOK(){
                SystemUtils.copyToClipboard(sysInfoText);
            }
        };

        modalDialog.getButton(ModalDialog.ID_OK).setText("Copy To Clipboard");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Close");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("Help");

        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        JPanel sysInfoPanel = sysInfoPanel();
        mainPanel.add(sysInfoPanel, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel fillerPanel = new JPanel();
        fillerPanel.setMinimumSize(fillerPanel.getPreferredSize());
        mainPanel.add(fillerPanel, gbc);


        mainPanel.setMinimumSize(mainPanel.getMinimumSize());


        modalDialog.getButton(ModalDialog.ID_OK).setMinimumSize(modalDialog.getButton(ModalDialog.ID_OK).getPreferredSize());

        // Specifically set sizes for dialog here
        Dimension minimumSizeAdjusted = adjustDimension(modalDialog.getJDialog().getMinimumSize(), 25, 25);
        Dimension preferredSizeAdjusted = adjustDimension(modalDialog.getJDialog().getPreferredSize(), 25, 25);
        modalDialog.getJDialog().setMinimumSize(minimumSizeAdjusted);
        modalDialog.getJDialog().setPreferredSize(preferredSizeAdjusted);

        modalDialog.getJDialog().pack();


        int dialogResult;

        boolean finish = false;
        while (!finish) {
            dialogResult = modalDialog.show();
            if (dialogResult == ModalDialog.ID_CANCEL) {
                finish = true;
            } else {
                finish = true;
            }

//            finish = true;
        }

        return;

    }

    protected JPanel sysInfoPanel(){

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

        JTextArea sysInfoTextarea = new JTextArea();
        sysInfoTextarea.setEditable(false);
        JScrollPane scroll = new JScrollPane(sysInfoTextarea);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

//        final Preferences preferences = Config.instance("seadas").load().preferences();
//        String lastOcsswLocation = preferences.get(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);

        SnapApp snapapp = SnapApp.getDefault();
        String appNameVersion = snapapp.getInstanceName();
        String appName = SystemUtils.getApplicationName();
        String appReleaseVersionFromPOM = SystemUtils.getReleaseVersion();
        File appHomeDir = SystemUtils.getApplicationHomeDir();
        File appDataDir = SystemUtils.getApplicationDataDir();
        Path appBinDir = appHomeDir.toPath().resolve("bin");
        Path appConfig = null;

        if (appName != null && appName.toUpperCase().contains("SEADAS")) {
            appConfig = appBinDir.resolve("seadas.properties");
        } else {
            appConfig = appBinDir.resolve("snap.properties");
        }

        Path vmOptions = appBinDir.resolve("pconvert.vmoptions");
        Path vmOptionsGpt = appBinDir.resolve("gpt.vmoptions");

        String jre = System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version");
        String jvm = System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor");
        String memory = Math.round(Runtime.getRuntime().maxMemory() /1024. /1024.) + " MiB";

        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);


//        System.out.println("\nMain Application Platform:");
//        System.out.println("Application Name Version: " + appNameVersion);
//        System.out.println("Application Home Directory: " + appHomeDir.toString());
//        System.out.println("Application Data Directory: " + appDataDir.toString());
//        System.out.println("Application Configuration: " + appConfig.toString());
//        System.out.println("Virtual Memory Configuration: " + vmOptions.toString());
//        System.out.println("Virtual Memory Configuration (gpt): " + vmOptionsGpt.toString());
//        System.out.println("Desktop Specification Version: " + desktopModuleInfo.getSpecificationVersion());
//        System.out.println("Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion());
//        System.out.println("Engine Specification Version: " + engineModuleInfo.getSpecificationVersion());
//        System.out.println("Engine Implementation Version: " + engineModuleInfo.getImplementationVersion());
//        System.out.println("JRE: " + jre);
//        System.out.println("JVM: " + jvm);
//        System.out.println("Memory: " + memory);
//
//        System.out.println("SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion());
//        System.out.println("SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion());
//


        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        String ocsswLogDir = ocsswInfo.getLogDirPath();
        String ocsswLocation = ocsswInfo.getOcsswLocation();

        //        System.out.println("appDir = " + installDir);
        //        System.out.println("ocsswRootOcsswInfo = " + ocsswRootOcsswInfo);
        //        System.out.println("OCSSW Log Directory = " + ocsswLogDir);
        //        System.out.println("OCSSW Location = " + lastOcsswLocation);

        sysInfoText = "Main Application Platform: " + "\n";

        sysInfoText += "Application Name Version: " + appNameVersion + "\n";
        sysInfoText += "Application Home Directoy: " + appHomeDir.toString() + "\n";
        sysInfoText += "Application Data Directory: " + appDataDir.toString() + "\n";
        sysInfoText += "Application Configuration: " + appConfig.toString() + "\n";
        sysInfoText += "Virtual Memory Configuration: " + vmOptions.toString() + "\n";
        sysInfoText += "Virtual Memory Configuration (gpt): " + vmOptionsGpt.toString() + "\n";
        sysInfoText += "Desktop Specification Version: " + desktopModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText += "Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion() + "\n";
        sysInfoText += "Engine Specification Version: " + engineModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText += "Engine Implementation Version: " + engineModuleInfo.getImplementationVersion() + "\n";
        sysInfoText += "JRE: " + jre + "\n";
        sysInfoText += "JVM: " + jvm + "\n";
        sysInfoText += "Memory: " + memory + "\n";

        sysInfoText += "\n" + "SeaDAS Toolbox: " + "\n";

        sysInfoText += "SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText += "SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion() + "\n";
        sysInfoText += "OCSSW Root Directory: " + ocsswRootOcsswInfo + "\n";
        sysInfoText += "OCSSW Log Directory: " + ocsswLogDir + "\n";
        sysInfoText += "OCSSW Location: " + ocsswLocation + "\n" +"\n";

//        String appDir = Config.instance().installDir().toString();

        String command = ocsswRootOcsswInfo + "/scripts/ocssw_runner --ocsswroot " + ocsswRootOcsswInfo
                + " " + ocsswRootOcsswInfo + "/scripts/seadas_info.py";

        String seadasProg = ocsswRootOcsswInfo + "/scripts/seadas_info2.py";
        if (!Files.isExecutable(Paths.get(seadasProg))) {
            sysInfoText += "NASA Science Processing (OCSSW): " + "\n";
            sysInfoText += "Processors not installed" + "\n" +"\n";
            sysInfoText += "General System and Software: " + "\n";
            sysInfoText += "These fields could possibly be determined by java?";
            sysInfoTextarea.setText(sysInfoText);
        } else {
//            System.out.println("command is: " + command);

            try {
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    sysInfoText += line + "\n";
                }

                sysInfoTextarea.setText(sysInfoText);
                reader.close();

                process.destroy();

                if (process.exitValue() != 0) {
                    System.out.println("Abnormal process termination");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scroll, gbc);

        return panel;
    }

    public File getDir() {

        File selectedFile = null;
        JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = jFileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = jFileChooser.getSelectedFile();
            //System.out.println(selectedFile.getAbsolutePath());
        }
        return selectedFile;
    }


    public static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        return gbc;
    }

    private Dimension adjustDimension(Dimension dimension, int widthAdjustment, int heightAdjustment) {

        if (dimension == null) {
            return null;
        }

        int width = dimension.width + widthAdjustment;
        int height = dimension.height + heightAdjustment;

        return new Dimension(width, height);
    }
}