/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package gov.nasa.gsfc.seadas.watermask.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.imgfilter.model.Filter;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * The watermask operator is a GPF-Operator. It takes the geographic bounds of the input product and creates a new
 * product with the same bounds. The output product contains a single band, which is a land/water fraction mask.
 * For each pixel, it contains the fraction of water; a value of 0.0 indicates land, a value of 100.0 indicates water,
 * and every value in between indicates a mixed pixel.
 * <br/>
 * The water mask is based on data given by SRTM-shapefiles between 60° north and 60° south, and by the GlobCover world
 * map above 60° north.
 * Since the base data may exhibit a higher resolution than the input product, a subsampling &ge;1 may be specified;
 * therefore, mixed pixels may occur.
 *
 * @author Daniel Knowles, Marco Peters, Thomas Storm
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "CoastlineLandWaterMasks",
        category = "Raster/Masks",
        version = "1.0",
        authors = "Daniel Knowles, Marco Peters",
        copyright = "",
        description = "Operator creating a target product with a single band containing a land/water-mask," +
                " which is based on SRTM-shapefiles (between 60° north and 60° south) and the " +
                "GlobCover world map (above 60° north) and therefore very accurate.")
public class WatermaskOp extends Operator {

    public static final String LAND_WATER_FRACTION_BAND_NAME = "water_fraction";
    public static final String LAND_WATER_FRACTION_SMOOTHED_BAND_NAME = "water_fraction_mean";
    public static final String COAST_BAND_NAME = "coast";
    @SourceProduct(alias = "source", description = "The Product the land/water-mask shall be computed for.",
            label = "Name")
    private Product sourceProduct;

    @Parameter(description = "Specifies on which resolution the water mask shall be based.  This needs to match data worldSourceDataFilename resolution", unit = "m/pixel",
            label = "Resolution", defaultValue = "1000", valueSet = {"50", "150", "1000", "10000"}, notNull = false)
    private int resolution;

    @Parameter(description = "Data source file for determining land/water: if default then determined based on selected resolution",
            label = "worldSourceDataFilename", defaultValue = "DEFAULT",
            valueSet = {"DEFAULT", "50m.zip", "150m.zip", "GSHHS_water_mask_1km.zip", "GSHHS_water_mask_10km.zip"}, notNull = false)
    private String worldSourceDataFilename;

    @Parameter(description = "Specifies the factor to divide up a target pixel when determining match with land source data" +
            ". A value of '1' means no subsampling at all.",
            label = "Supersampling factor", defaultValue = "3", notNull = false)
    private int superSamplingFactor;


    @Parameter(description = "Specifies the watermaskClassifier mode: uses SRTM_GC for the 50m and 150m files",
            label = "Mode", defaultValue = "DEFAULT", valueSet = {"DEFAULT", "GSHHS", "SRTM_GC"}, notNull = false)
    private WatermaskClassifier.Mode mode;

    @Parameter(description = "Output file is copy of source file with land data added",
            label = "Copy Source File", defaultValue = "true", notNull = false)
    private boolean copySourceFile;

    @Parameter(description = "Specifies a filter grid size to apply to determine the coastal mask.  (e.g. 3 = 3x3 matrix)",
            label = "Coastal grid box size", defaultValue = "3", notNull = false)
    private int coastalGridSize;

    @Parameter(description = "Specifies percent of coastal grid matrix to mask",
            label = "Coastal size tolerance", defaultValue = "50", notNull = false)
    private int coastalSizeTolerance;

    @Parameter(description = "Color of water mask",
            label = "Color of water mask", defaultValue = "0, 125, 255", notNull = false)
    private Color waterMaskColor;

    @Parameter(description = "Color of coastal mask",
            label = "Color of coastal mask", defaultValue = "0, 0, 0", notNull = false)
    private Color coastalMaskColor;

    @Parameter(description = "Color of land mask",
            label = "Color of land mask", defaultValue = "51, 51, 51", notNull = false)
    private Color landMaskColor;

    @Parameter(description = "Includes the masks (otherwise only land band is created)",
            label = "includeMasks", defaultValue = "true", notNull = false)
    private boolean includeMasks;

    @Parameter(description = "Water mask transparency",
            label = "Water mask transparency", defaultValue = "0", notNull = false)
    private double waterMaskTransparency;

    @Parameter(description = "Land mask transparency",
            label = "Land mask transparency", defaultValue = "0", notNull = false)
    private double landMaskTransparency;

    @Parameter(description = "Coastal mask transparency",
            label = "Coastal mask transparency", defaultValue = "0", notNull = false)
    private double coastalMaskTransparency;


//    @Parameter(description = "Specifies the resolutionInfo which contains resolution, mode",
//            label = "Resolution Info", defaultValue = "1 km GSHHS", notNull = true)
//    private SourceFileInfo resolutionInfo;


    @TargetProduct
    private Product targetProduct;
    private WatermaskClassifier classifier;

    @Override
    public void initialize() throws OperatorException {
        validateParameter();
        validateSourceProduct();
        initTargetProduct();

        try {
            classifier = new WatermaskClassifier(resolution, mode, worldSourceDataFilename);
        } catch (IOException e) {
            throw new OperatorException("Error creating class WatermaskClassifier.", e);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();
        try {
            final String targetBandName = targetBand.getName();
            final PixelPos pixelPos = new PixelPos();
            final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    pixelPos.x = x;
                    pixelPos.y = y;
                    int dataValue = 0;
                    if (targetBandName.equals(LAND_WATER_FRACTION_BAND_NAME)) {
                        dataValue = classifier.getWaterMaskFraction(geoCoding, pixelPos,
                                superSamplingFactor,
                                superSamplingFactor);
                    } else if (targetBandName.equals(COAST_BAND_NAME)) {
                        final boolean coastline = isCoastline(geoCoding, pixelPos,
                                superSamplingFactor,
                                superSamplingFactor);
                        dataValue = coastline ? 1 : 0;
                    }
                    targetTile.setSample(x, y, dataValue);
                }
            }

        } catch (Exception e) {
            throw new OperatorException("Error computing tile '" + targetTile.getRectangle().toString() + "'.", e);
        }
    }

    private boolean isCoastline(GeoCoding geoCoding, PixelPos pixelPos, int superSamplingX, int superSamplingY) {
        double xStep = 1.0 / superSamplingX;
        double yStep = 1.0 / superSamplingY;
        final GeoPos geoPos = new GeoPos();
        final PixelPos currentPos = new PixelPos();
        for (int sx = 0; sx < superSamplingX; sx++) {
            currentPos.x = (float) (pixelPos.x + sx * xStep);
            for (int sy = 0; sy < superSamplingY; sy++) {
                currentPos.y = (float) (pixelPos.y + sy * yStep);
                geoCoding.getGeoPos(currentPos, geoPos);
                // Todo: Implement coastline algorithm here
                //
            }
        }
        return false;
    }

    private void validateParameter() {
        if (resolution != WatermaskClassifier.RESOLUTION_50m &&
                resolution != WatermaskClassifier.RESOLUTION_150m &&
                resolution != WatermaskClassifier.RESOLUTION_1km &&
                resolution != WatermaskClassifier.RESOLUTION_10km) {
            throw new OperatorException(String.format("Resolution needs to be either %d, %d, %d or %d.",
                    WatermaskClassifier.RESOLUTION_50m,
                    WatermaskClassifier.RESOLUTION_150m,
                    WatermaskClassifier.RESOLUTION_1km,
                    WatermaskClassifier.RESOLUTION_10km));
        }
        if (superSamplingFactor < 1) {
            String message = MessageFormat.format(
                    "Supersampling factor needs to be greater than or equal to 1; was: ''{0}''.", superSamplingFactor);
            throw new OperatorException(message);
        }
    }

    private void validateSourceProduct() {
        final GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }
        if (!geoCoding.canGetGeoPos()) {
            throw new OperatorException("The geo-coding of the source product can not be used.\n" +
                    "It does not provide the geo-position for a pixel position.");
        }
    }

    private void copySourceToTarget() {
//        final HashMap<String, Object> copyOpParameters = new HashMap<String, Object>();
//
//
//        HashMap<String, Product> copyOpProducts = new HashMap<String, Product>();
//        copyOpProducts.put("source", sourceProduct);
//        targetProduct = GPF.createProduct("Copy", copyOpParameters, copyOpProducts);

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(sourceProduct.getName() + "_LW-Mask", "CoastlineLandWaterMasks", width, height);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        copyFlagCodingsIfPossible(sourceProduct, targetProduct);
        copyIndexCodingsIfPossible(sourceProduct, targetProduct);
        // copying GeoCoding from product to product, bands which do not have a GC yet will be geo-coded afterwards
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setDescription(sourceProduct.getDescription());

        if (sourceProduct.getStartTime() != null && sourceProduct.getEndTime() != null) {
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());
        }


    }

    private static void copyFlagCodingsIfPossible(Product source, Product target) {
        int numCodings = source.getFlagCodingGroup().getNodeCount();
        for (int n = 0; n < numCodings; n++) {
            FlagCoding sourceFlagCoding = source.getFlagCodingGroup().get(n);
            final String sourceFlagCodingName = sourceFlagCoding.getName();
            if (target.containsBand(sourceFlagCodingName) &&
                    target.getBand(sourceFlagCodingName).hasIntPixels()) {
                ProductUtils.copyFlagCoding(sourceFlagCoding, target);
                target.getBand(sourceFlagCodingName).setSampleCoding(sourceFlagCoding);
            }
        }
    }

    private static void copyIndexCodingsIfPossible(Product source, Product target) {
        int numCodings = source.getIndexCodingGroup().getNodeCount();
        for (int n = 0; n < numCodings; n++) {
            IndexCoding sourceIndexCoding = source.getIndexCodingGroup().get(n);
            final String sourceIndexCodingName = sourceIndexCoding.getName();
            if (target.containsBand(sourceIndexCodingName) &&
                    target.getBand(sourceIndexCodingName).hasIntPixels()) {
                ProductUtils.copyIndexCoding(sourceIndexCoding, target);
                target.getBand(sourceIndexCodingName).setSampleCoding(sourceIndexCoding);
            }
        }
    }

    private void initTargetProduct() {
        if (copySourceFile) {
            copySourceToTarget();
        } else {
            targetProduct = new Product("LW-Mask", ProductData.TYPESTRING_UINT8, sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());
        }


        final Band waterBand = targetProduct.addBand(LAND_WATER_FRACTION_BAND_NAME, ProductData.TYPE_FLOAT32);
        waterBand.setNoDataValue(WatermaskClassifier.INVALID_VALUE);
        waterBand.setNoDataValueUsed(true);

//        final Kernel arithmeticMean3x3Kernel = new Kernel(3, 3, 1.0 / 9.0,
//                new double[]{
//                        +1, +1, +1,
//                        +1, +1, +1,
//                        +1, +1, +1,
//                });


        if (includeMasks) {

            final Filter meanFilter = new Filter("Mean " + Integer.toString(coastalGridSize) + "x" + Integer.toString(coastalGridSize), "mean" + Integer.toString(coastalGridSize), Filter.Operation.MEAN, coastalGridSize, coastalGridSize);
            final Kernel meanKernel = new Kernel(meanFilter.getKernelWidth(),
                    meanFilter.getKernelHeight(),
                    meanFilter.getKernelOffsetX(),
                    meanFilter.getKernelOffsetY(),
                    1.0 / meanFilter.getKernelQuotient(),
                    meanFilter.getKernelElements());

            int count = 1;
//        final ConvolutionFilterBand filteredCoastlineBand = new ConvolutionFilterBand(
//                LAND_WATER_FRACTION_SMOOTHED_BAND_NAME,
//                waterBand,
//                arithmeticMean3x3Kernel, count);


            String filteredCoastlineBandName = LAND_WATER_FRACTION_SMOOTHED_BAND_NAME + Integer.toString(coastalGridSize);
            final FilterBand filteredCoastlineBand = new GeneralFilterBand(filteredCoastlineBandName, waterBand, GeneralFilterBand.OpType.MEAN, meanKernel, count);
            if (waterBand instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) waterBand, filteredCoastlineBand);
            }


            targetProduct.addBand(filteredCoastlineBand);

            final ProductNodeGroup<Mask> maskGroup = targetProduct.getMaskGroup();

            double min = 50 - coastalSizeTolerance / 2;
            double max = 50 + coastalSizeTolerance / 2;
            String coastlineMaskExpression = filteredCoastlineBandName + " > " + Double.toString(min) + " and " + filteredCoastlineBandName + " < " + Double.toString(max);

            Mask coastlineMask = Mask.BandMathsType.create(
                    "CoastalMask",
                    "Coastal masked pixels",
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight(),
                    coastlineMaskExpression,
                    coastalMaskColor,
                    coastalMaskTransparency);
            maskGroup.add(coastlineMask);


            Mask landMask = Mask.BandMathsType.create(
                    "LandMask",
                    "Land masked pixels",
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight(),
                    LAND_WATER_FRACTION_BAND_NAME + "== 0",
                    landMaskColor,
                    landMaskTransparency);

            maskGroup.add(landMask);

            Mask waterMask = Mask.BandMathsType.create(
                    "WaterMask",
                    "Water masked pixels",
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight(),
                    LAND_WATER_FRACTION_BAND_NAME + "> 0",
                    waterMaskColor,
                    waterMaskTransparency);
            maskGroup.add(waterMask);


            String[] bandNames = targetProduct.getBandNames();
            for (String bandName : bandNames) {
                RasterDataNode raster = targetProduct.getRasterDataNode(bandName);
            }


        }
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }


    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WatermaskOp.class);
        }
    }
}
