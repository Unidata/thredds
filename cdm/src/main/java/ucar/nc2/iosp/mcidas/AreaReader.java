/*
 *
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.Calibrator;
import edu.wisc.ssec.mcidas.CalibratorException;
import edu.wisc.ssec.mcidas.CalibratorFactory;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;

import ucar.nc2.units.DateFormatter;

import ucar.unidata.geoloc.ProjectionImpl;

import ucar.unidata.io.RandomAccessFile;

import ucar.unidata.util.Parameter;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * Class to read an AREA file and create a netCDF data structure
 * from it.
 *
 * @author Don Murray
 */
public class AreaReader {


    /** The AREA file */
    private AreaFile af;

    /** The AREA navigation */
    private AREAnav nav;

    /** The raw AREA directory */
    private int[] dirBlock;

    /** The raw nav block */
    private int[] navBlock;

    /** The AREA directory */
    private AreaDirectory ad;

    /** The calibrator */
    Calibrator calibrator = null;

    /** list  of bands */
    int[] bandMap = null;

    /** calibration scale */
    private float calScale = 1.f;

    /** calibration scale */
    private String calUnit;

    /**
     * Default ctor
     */
    public AreaReader() {}


    /**
     * initialize with the file
     *
     * @param raf   the AREA file to open
     * @param ncfile  the netCDF file to fill out
     *
     * @return true if successful
     *
     * @throws AreaFileException problem opening the area file
     */
    public boolean init(RandomAccessFile raf, NetcdfFile ncfile)
            throws AreaFileException {

        af = new AreaFile(raf.getLocation());

        //read metadata
        dirBlock = af.getDir();
        ad       = af.getAreaDirectory();
        int numElements = ad.getElements();
        int numLines    = ad.getLines();
        int numBands    = ad.getNumberOfBands();
        bandMap  = ad.getBands();
        navBlock = af.getNav();
        Date          nomTime = ad.getNominalTime();
        DateFormatter df      = new DateFormatter();
        try {
            nav = AREAnav.makeAreaNav(navBlock, af.getAux());
        } catch (McIDASException me) {
            throw new AreaFileException(me.getMessage());
        }
        int    sensor  = dirBlock[af.AD_SENSORID];
        String calName = McIDASUtil.intBitsToString(dirBlock[af.AD_CALTYPE]);
        int    calType = getCalType(calName);

        // TODO:  Need to support calibrated data.
        if ((af.getCal() != null)
                && CalibratorFactory.hasCalibrator(sensor)) {
            //System.out.println("can calibrate");
            try {
                calibrator = CalibratorFactory.getCalibrator(sensor, calType,
                        af.getCal());
            } catch (CalibratorException ce) {
                // System.out.println("can't make calibrator");
                calibrator = null;
            }
            //System.out.println("calibrator = " + calibrator);

        }
        calUnit  = ad.getCalibrationUnitName();
        calScale = (1.0f / ad.getCalibrationScaleFactor());

        // make the dimensions
        Dimension elements     = new Dimension("elements", numElements, true);
        Dimension       lines  = new Dimension("lines", numLines, true);
        Dimension       bands  = new Dimension("bands", numBands, true);
        Dimension       time   = new Dimension("time", 1, true);
        Dimension       dirDim = new Dimension("dirSize", af.AD_DIRSIZE,
                                     true);
        Dimension navDim = new Dimension("navSize", navBlock.length, true);
        List<Dimension> image  = new ArrayList<Dimension>();
        image.add(time);
        image.add(bands);
        image.add(lines);
        image.add(elements);
        ncfile.addDimension(null, elements);
        ncfile.addDimension(null, lines);
        ncfile.addDimension(null, bands);
        ncfile.addDimension(null, time);
        ncfile.addDimension(null, dirDim);
        ncfile.addDimension(null, navDim);


        Array varArray;

        // make the variables

        // time
        Variable timeVar = new Variable(ncfile, null, null, "time");
        timeVar.setDataType(DataType.INT);
        timeVar.setDimensions("time");
        timeVar.addAttribute(new Attribute("units",
                                           "seconds since "
                                           + df.toDateTimeString(nomTime)));
        timeVar.addAttribute(new Attribute("long_name", "time"));
        varArray = new ArrayInt.D1(1);
        ((ArrayInt.D1) varArray).set(0, 0);
        timeVar.setCachedData(varArray, false);
        ncfile.addVariable(null, timeVar);


        // lines and elements
        Variable lineVar = new Variable(ncfile, null, null, "lines");
        lineVar.setDataType(DataType.INT);
        lineVar.setDimensions("lines");
        //lineVar.addAttribute(new Attribute("units", "km"));
        lineVar.addAttribute(new Attribute("standard_name",
                                           "projection_y_coordinate"));
        varArray = new ArrayInt.D1(numLines);
        for (int i = 0; i < numLines; i++) {
            int pos = nav.isFlippedLineCoordinates()
                      ? i
                      : numLines - i - 1;
            ((ArrayInt.D1) varArray).set(i, pos);
        }
        lineVar.setCachedData(varArray, false);
        ncfile.addVariable(null, lineVar);

        Variable elementVar = new Variable(ncfile, null, null, "elements");
        elementVar.setDataType(DataType.INT);
        elementVar.setDimensions("elements");
        //elementVar.addAttribute(new Attribute("units", "km"));
        elementVar.addAttribute(new Attribute("standard_name",
                "projection_x_coordinate"));
        varArray = new ArrayInt.D1(numElements);
        for (int i = 0; i < numElements; i++) {
            ((ArrayInt.D1) varArray).set(i, i);
        }
        elementVar.setCachedData(varArray, false);
        ncfile.addVariable(null, elementVar);


        // TODO: handle bands and calibrations
        Variable bandVar = new Variable(ncfile, null, null, "bands");
        bandVar.setDataType(DataType.INT);
        bandVar.setDimensions("bands");
        bandVar.addAttribute(new Attribute("long_name",
                                           "spectral band number"));
        bandVar.addAttribute(new Attribute("axis", "Z"));
        Array bandArray = new ArrayInt.D1(numBands);
        for (int i = 0; i < numBands; i++) {
            ((ArrayInt.D1) bandArray).set(i, bandMap[i]);
        }
        bandVar.setCachedData(bandArray, false);
        ncfile.addVariable(null, bandVar);

        // the image
        Variable imageVar = new Variable(ncfile, null, null, "image");
        imageVar.setDataType(DataType.INT);
        imageVar.setDimensions(image);
        setCalTypeAttributes(imageVar, getCalType(calName));
        imageVar.addAttribute(new Attribute(getADDescription(af.AD_CALTYPE),
                                            calName));
        imageVar.addAttribute(new Attribute("bands", bandArray));
        imageVar.addAttribute(new Attribute("grid_mapping", "AREAnav"));
        ncfile.addVariable(null, imageVar);


        Variable dirVar = new Variable(ncfile, null, null, "areaDirectory");
        dirVar.setDataType(DataType.INT);
        dirVar.setDimensions("dirSize");
        setAreaDirectoryAttributes(dirVar);
        ArrayInt.D1 dirArray = new ArrayInt.D1(AreaFile.AD_DIRSIZE);
        for (int i = 0; i < AreaFile.AD_DIRSIZE; i++) {
            dirArray.set(i, dirBlock[i]);
        }
        dirVar.setCachedData(dirArray, false);
        ncfile.addVariable(null, dirVar);

        Variable navVar = new Variable(ncfile, null, null, "navBlock");
        navVar.setDataType(DataType.INT);
        navVar.setDimensions("navSize");
        setNavBlockAttributes(navVar);
        ArrayInt.D1 navArray = new ArrayInt.D1(navBlock.length);
        for (int i = 0; i < navBlock.length; i++) {
            navArray.set(i, navBlock[i]);
        }
        navVar.setCachedData(navArray, false);
        ncfile.addVariable(null, navVar);


        // projection variable
        ProjectionImpl projection = new McIDASAreaProjection(af);
        Variable       proj = new Variable(ncfile, null, null, "AREAnav");
        proj.setDataType(DataType.CHAR);
        proj.setDimensions("");

        List params = projection.getProjectionParameters();
        for (int i = 0; i < params.size(); i++) {
            Parameter p = (Parameter) params.get(i);
            proj.addAttribute(new Attribute(p));
        }

        // For now, we have to overwrite the parameter versions of thes
        proj.addAttribute(
            new Attribute(
                "grid_mapping_name", McIDASAreaProjection.GRID_MAPPING_NAME));
        /*
        proj.addAttribute(new Attribute(McIDASAreaProjection.ATTR_AREADIR,
                                        dirArray));
        proj.addAttribute(new Attribute(McIDASAreaProjection.ATTR_NAVBLOCK,
                                        navArray));
        */
        varArray = new ArrayChar.D0();
        ((ArrayChar.D0) varArray).set(' ');
        proj.setCachedData(varArray, false);

        ncfile.addVariable(null, proj);

        // add the attributes
        ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
        ncfile.addAttribute(null, new Attribute("netCDF-Java", "4.0"));
        ncfile.addAttribute(null,
                            new Attribute("nominal_image_time",
                                          df.toDateTimeString(nomTime)));
        String encStr = "netCDF encoded on "
                        + df.toDateTimeString(new Date());
        ncfile.addAttribute(null, new Attribute("history", encStr));

        //Lastly, finish the file
        ncfile.finish();
        return true;
    }

    /**
     * Check to see if this is a valid AREA file.
     *
     * @param raf  the file in question
     *
     * @return true if it is an AREA file.
     */
    public static boolean isValidFile(RandomAccessFile raf) {
        String fileName = raf.getLocation();
        try {
            AreaFile af = new AreaFile(fileName);
            return true;
        } catch (AreaFileException e) {
            return false;
        }
    }


    /**
     * Read the values for a variable
     *
     * @param v2  the variable
     * @param section the section info (time,x,y range);
     *
     * @return the data
     *
     * @throws IOException problem reading file
     * @throws InvalidRangeException  range doesn't match data
     */
    public Array readVariable(Variable v2, Section section)
            throws IOException, InvalidRangeException {

        Range timeRange = null;
        Range bandRange = null;
        Range geoXRange = null;
        Range geoYRange = null;
        if (section != null & section.getRank() > 0) {
            if (section.getRank() > 3) {
                timeRange = (Range) section.getRange(0);
                bandRange = (Range) section.getRange(1);
                geoYRange = (Range) section.getRange(2);
                geoXRange = (Range) section.getRange(3);
            } else if (section.getRank() > 2) {
                timeRange = (Range) section.getRange(0);
                geoYRange = (Range) section.getRange(1);
                geoXRange = (Range) section.getRange(2);
            } else if (section.getRank() > 1) {
                geoYRange = (Range) section.getRange(0);
                geoXRange = (Range) section.getRange(1);
            } else {
                geoXRange = (Range) section.getRange(0);
            }
        }

        String varname = v2.getName();

        Array dataArray =
            Array.factory(v2.getDataType().getPrimitiveClassType(),
                          section.getShape());

        Index dataIndex = dataArray.getIndex();

        if (varname.equals("latitude") || varname.equals("longitude")) {
            double[][] pixel = new double[2][1];
            double[][] latLon;
            double[][][] latLonValues =
                new double[geoXRange.length()][geoYRange.length()][2];

            // Use Range object, which calculates requested i, j 
            // values and incorporates stride
            for (int i = 0; i < geoXRange.length(); i++) {
                for (int j = 0; j < geoYRange.length(); j++) {
                    pixel[0][0] = (double) geoXRange.element(i);
                    pixel[1][0] = (double) geoYRange.element(j);
                    latLon      = nav.toLatLon(pixel);

                    if (varname.equals("lat")) {
                        dataArray.setFloat(dataIndex.set(j, i),
                                           (float) (latLon[0][0]));
                    } else {
                        dataArray.setFloat(dataIndex.set(j, i),
                                           (float) (latLon[1][0]));
                    }
                }
            }
        }

        if (varname.equals("image")) {
            try {
                int[][] pixelData = new int[1][1];
                if (bandRange != null) {
                    for (int k = 0; k < bandRange.length(); k++) {
                        int bandIndex = bandRange.element(k) + 1;  // band numbers in McIDAS are 1 based
                        for (int j = 0; j < geoYRange.length(); j++) {
                            for (int i = 0; i < geoXRange.length(); i++) {
                                pixelData = af.getData(geoYRange.element(j),
                                        geoXRange.element(i), 1, 1,
                                        bandIndex);
                                dataArray.setInt(dataIndex.set(0, k, j, i),
                                        (pixelData[0][0]));
                            }
                        }
                    }

                } else {
                    for (int j = 0; j < geoYRange.length(); j++) {
                        for (int i = 0; i < geoXRange.length(); i++) {
                            pixelData = af.getData(geoYRange.element(j),
                                    geoXRange.element(i), 1, 1);
                            dataArray.setInt(dataIndex.set(0, j, i),
                                             (pixelData[0][0]));
                        }
                    }

                }
            } catch (AreaFileException afe) {
                throw new IOException(afe.toString());
            }

        }

        return dataArray;
    }

    /**
     * Set the area directory attributes on the variable
     *
     * @param v  the variable to set them on
     */
    private void setAreaDirectoryAttributes(Variable v) {
        if ((dirBlock == null) || (ad == null)) {
            return;
        }
        for (int i = 1; i < 14; i++) {
            if (i == 7) {
                continue;
            }
            v.addAttribute(new Attribute(getADDescription(i),
                                         new Integer(dirBlock[i])));
        }
    }

    /**
     * Set the navigation block attributes on the variable
     *
     * @param v  the variable to set them on
     */
    private void setNavBlockAttributes(Variable v) {
        if ((navBlock == null) || (ad == null)) {
            return;
        }
        v.addAttribute(
            new Attribute(
                "navigation_type", McIDASUtil.intBitsToString(navBlock[0])));
    }

    // TODO: Move to use edu.wisc.ssec.mcidas.AreaDirectory.getDescription
    // once it's released

    /**
     * Get a description for a particular Area Directory entry
     *
     * @param index  the index
     *
     * @return  a description
     */
    private String getADDescription(int index) {

        String desc = "dir(" + index + ")";
        switch (index) {

          case AreaFile.AD_STATUS :
              desc = "relative position of the image object in the ADDE dataset";
              break;

          case AreaFile.AD_VERSION :
              desc = "AREA version";
              break;

          case AreaFile.AD_SENSORID :
              desc = "SSEC sensor source number";
              break;

          case AreaFile.AD_IMGDATE :
              desc = "nominal year and Julian day of the image (yyyddd)";
              break;

          case AreaFile.AD_IMGTIME :
              desc = "nominal time of the image (hhmmss)";
              break;

          case AreaFile.AD_STLINE :
              desc = "upper-left image line coordinate";
              break;

          case AreaFile.AD_STELEM :
              desc = "upper-left image element coordinate";
              break;

          case AreaFile.AD_NUMLINES :
              desc = "number of lines in the image";
              break;

          case AreaFile.AD_NUMELEMS :
              desc = "number of data points per line";
              break;

          case AreaFile.AD_DATAWIDTH :
              desc = "number of bytes per data point";
              break;

          case AreaFile.AD_LINERES :
              desc = "line resolution";
              break;

          case AreaFile.AD_ELEMRES :
              desc = "element resolution";
              break;

          case AreaFile.AD_NUMBANDS :
              desc = "number of spectral bands";
              break;

          case AreaFile.AD_PFXSIZE :
              desc = "length of the line prefix";
              break;

          case AreaFile.AD_PROJNUM :
              desc = "SSEC project number used when creating the file";
              break;

          case AreaFile.AD_CRDATE :
              desc = "year and Julian day the image file was created (yyyddd)";
              break;

          case AreaFile.AD_CRTIME :
              desc = "image file creation time (hhmmss)";
              break;

          case AreaFile.AD_BANDMAP :
              desc = "spectral band map: bands 1-32";
              break;

          case AreaFile.AD_DATAOFFSET :
              desc = "byte offset to the start of the data block";
              break;

          case AreaFile.AD_NAVOFFSET :
              desc = "byte offset to the start of the navigation block";
              break;

          case AreaFile.AD_VALCODE :
              desc = "validity code";
              break;

          case AreaFile.AD_STARTDATE :
              desc = "actual image start year and Julian day (yyyddd)";
              break;

          case AreaFile.AD_STARTTIME :
              desc = "actual image start time (hhmmss) in milliseconds for POES data";
              break;

          case AreaFile.AD_STARTSCAN :
              desc = "actual image start scan";
              break;

          case AreaFile.AD_DOCLENGTH :
              desc = "length of the prefix documentation";
              break;

          case AreaFile.AD_CALLENGTH :
              desc = "length of the prefix calibration";
              break;

          case AreaFile.AD_LEVLENGTH :
              desc = "length of the prefix band list";
              break;

          case AreaFile.AD_SRCTYPE :
              desc = "source type";
              break;

          case AreaFile.AD_CALTYPE :
              desc = "calibration type";
              break;

          case AreaFile.AD_SRCTYPEORIG :
              desc = "original source type";
              break;

          case AreaFile.AD_CALTYPEUNIT :
              desc = "calibration unit";
              break;

          case AreaFile.AD_CALTYPESCALE :
              desc = "calibration scaling";
              break;

          case AreaFile.AD_AUXOFFSET :
              desc = "byte offset to the supplemental block";
              break;

          case AreaFile.AD_CALOFFSET :
              desc = "byte offset to the calibration block";
              break;

          case AreaFile.AD_NUMCOMMENTS :
              desc = "number of comment cards";
              break;

        }
        desc = desc.replaceAll("\\s", "_");
        return desc;

    }

    /**
     * Get the calibration type from the name
     *
     * @param calName calibration name
     *
     * @return the Calibrator class type
     */
    private int getCalType(String calName) {
        int calTypeOut = Calibrator.CAL_NONE;
        if (calName.trim().equals("ALB")) {
            calTypeOut = Calibrator.CAL_ALB;
        } else if (calName.trim().equals("BRIT")) {
            calTypeOut = Calibrator.CAL_BRIT;
        } else if (calName.trim().equals("RAD")) {
            calTypeOut = Calibrator.CAL_RAD;
        } else if (calName.trim().equals("RAW")) {
            calTypeOut = Calibrator.CAL_RAW;
        } else if (calName.trim().equals("TEMP")) {
            calTypeOut = Calibrator.CAL_TEMP;
        }
        return calTypeOut;
    }

    /**
     * Set the long name and units for the calibration type
     * @param image  image variable
     * @param calType calibration type
     */
    private void setCalTypeAttributes(Variable image, int calType) {
        String longName = "image values";
        String unit     = "";
        switch (calType) {

          case Calibrator.CAL_ALB :
              longName = "albedo";
              //unit     = "%";
              break;

          case Calibrator.CAL_BRIT :
              longName = "brightness values";
              break;

          case Calibrator.CAL_TEMP :
              longName = "temperature";
              //unit     = "K";
              break;

          case Calibrator.CAL_RAD :
              longName = "pixel radiance values";
              //unit     = "mW/m2/sr/cm-1";
              break;

          case Calibrator.CAL_RAW :
              longName = "raw image values";
              break;

          default :
              break;
        }
        image.addAttribute(new Attribute("long_name", longName));
        if (calUnit != null) {
            image.addAttribute(new Attribute("units", calUnit));
        }
        if (calScale != 1.f) {
            image.addAttribute(new Attribute("scale_factor", calScale));
        }

    }

}

