package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.validators.NotNullValidator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik
 * Date: 3/30/12
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class OutputFileSelectorModel {


    private static final String ENVISAT_FORMAT_NAME = "ENVISAT";
    // used by object binding
    private String productName;
    private boolean saveToFileSelected;
    private boolean openInAppSelected;
    private File productDir;
    private String formatName;
    private String[] formatNames;

    private final PropertyContainer propertyContainer;

    public OutputFileSelectorModel() {
        this(ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings());
    }

    public OutputFileSelectorModel(String[] formatNames) {
        propertyContainer = PropertyContainer.createObjectBacked(this);
        propertyContainer.addPropertyChangeListener("saveToFileSelected", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!(Boolean) evt.getNewValue()) {
                    setOpenInAppSelected(true);
                }
            }
        });
        propertyContainer.addPropertyChangeListener("openInAppSelected", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!(Boolean) evt.getNewValue()) {
                    setSaveToFileSelected(true);
                }
            }
        });
        PropertyDescriptor productNameDescriptor = propertyContainer.getDescriptor("productName");
        productNameDescriptor.setValidator(new ProductNameValidator());
        productNameDescriptor.setDisplayName("target product name");

        PropertyDescriptor productDirDescriptor = propertyContainer.getDescriptor("productDir");
        productDirDescriptor.setValidator(new NotNullValidator());
        productDirDescriptor.setDisplayName("target product directory");

        setOpenInAppSelected(true);
        setSaveToFileSelected(true);
        this.formatNames = formatNames;
        if (StringUtils.contains(this.formatNames, ProductIO.DEFAULT_FORMAT_NAME)) {
            setFormatName(ProductIO.DEFAULT_FORMAT_NAME);
        } else {
            setFormatName(formatNames[0]);
        }
    }

//    public static OutputFileSelectorModel createEnvisatTargetProductSelectorModel() {
//        return new EnvisatTargetProductSelectorModel();
//    }

    public String getProductName() {
        return productName;
    }

    public boolean isSaveToFileSelected() {
        return saveToFileSelected;
    }

    public boolean isOpenInAppSelected() {
        return openInAppSelected;
    }

    public File getProductDir() {
        return productDir;
    }

    public File getProductFile() {
        if (productName == null) {
            return null;
        }
        return new File(productDir, productName);
    }

    String getProductFileName() {
        String productFileName = productName;
//        Iterator<ProductWriterPlugIn> iterator = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
//        if (iterator.hasNext()) {
//            final ProductWriterPlugIn writerPlugIn = iterator.next();
//
//            boolean ok = false;
//            for (String extension : writerPlugIn.getDefaultFileExtensions()) {
//                if (productFileName.endsWith(extension)) {
//                    ok = true;
//                    break;
//                }
//            }
//            if (!ok) {
//                productFileName = productFileName.concat(writerPlugIn.getDefaultFileExtensions()[0]);
//            }
//        }
        return productFileName;
    }

    public String getFormatName() {
        return formatName;
    }

    public String[] getFormatNames() {
        return formatNames;
    }

    public void setProductName(String productName) {
        setValueContainerValue("productName", productName);
    }

    public void setSaveToFileSelected(boolean saveToFileSelected) {
        setValueContainerValue("saveToFileSelected", saveToFileSelected);
    }

    public void setOpenInAppSelected(boolean openInAppSelected) {
        setValueContainerValue("openInAppSelected", openInAppSelected);
    }

    public void setProductDir(File productDir) {
        setValueContainerValue("productDir", productDir);
    }

    public void setFormatName(String formatName) {
        setValueContainerValue("formatName", formatName);
    }

    public PropertyContainer getValueContainer() {
        return propertyContainer;
    }

    private void setValueContainerValue(String name, Object value) {
        propertyContainer.setValue(name, value);
    }

    private static class ProductNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String name = (String) value;
            if (!ProductNode.isValidNodeName(name)) {
                final String message = MessageFormat.format("The product name ''{0}'' is not valid.\n\n"
                                                            + "Names must not start with a dot and must not\n"
                                                            + "contain any of the following characters: \\/:*?\"<>|",
                                                            name);
                throw new ValidationException(message);
            }
        }
    }

    public static class SeadasTargetProductSelectorModel extends OutputFileSelectorModel {

        private SeadasTargetProductSelectorModel() {
            super(createFormats());
        }

        @Override
        public File getProductFile() {
            if (!ENVISAT_FORMAT_NAME.equals(getFormatName())) {
                return super.getProductFile();
            }
            final String productName = getProductName();
            return new File(getProductDir(), FileUtils.ensureExtension(productName, ".N1"));

        }

        private static String[] createFormats() {
            final String[] productWriterFormatStrings = ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings();
            final String[] formatNames = Arrays.copyOf(productWriterFormatStrings,
                                                       productWriterFormatStrings.length + 1);
            formatNames[formatNames.length - 1] = ENVISAT_FORMAT_NAME;
            return formatNames;
        }
    }
}
