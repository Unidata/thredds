/*
 * Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grads;


import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;


/**
 * IOSP for GrADS Binary data files.  This IOSP only handles the binary formatted grids,
 * most other GrADS data types can be read directly through other IOSPs
 *
 * @author         Don Murray - CU/CIRES
 */
public class GradsBinaryGridServiceProvider extends AbstractIOServiceProvider {

    /** GrADS file reader */
    protected GradsDataDescriptorFile gradsDDF;

    /** GrADS binary file */
    private RandomAccessFile dataFile;

    /** the netCDF file */
    private NetcdfFile ncFile;

    /** the size of the x dimension */
    private int sizeX = 0;

    /** the size of the y dimension */
    private int sizeY = 0;

    /** the number of xy header bytes */
    private int xyHeaderBytes = 0;

    /** the sequential record bytes */
    private int sequentialRecordBytes = 0;

    /** the number of file header bytes */
    private int fileHeaderBytes = 0;

    /** the number of time header bytes */
    private int timeHeaderBytes = 0;

    /** the number of time trailer bytes */
    private int timeTrailerBytes = 0;

    /** The name for the ensemble varaible */
    private static final String ENS_VAR = "ensemble";

    /** The name for the time variable */
    private static final String TIME_VAR = "time";

    /** The name for the z dimension variable */
    private static final String Z_VAR = "level";

    /** The name for the y dimension variable */
    private static final String Y_VAR = "latitude";

    /** The name for the x dimension variable */
    private static final String X_VAR = "longitude";

    /** The dimension names */
    private String[] dimNames = { GradsDataDescriptorFile.EDEF,
                                  GradsDataDescriptorFile.TDEF,
                                  GradsDataDescriptorFile.ZDEF,
                                  GradsDataDescriptorFile.YDEF,
                                  GradsDataDescriptorFile.XDEF };

    /** The corresponding variable names */
    private String[] dimVarNames = { ENS_VAR, TIME_VAR, Z_VAR, Y_VAR,
                                     X_VAR, };

    /** The word size in bytes */
    private int wordSize = 4;

    /**
     * Is this a valid file?  For this GrADS IOSP, the valid file must be:
     * <ul>
     * <li>raw binary grid (not GRIB, netCDF, HDF, etc)
     * <li>not a cross section (x and y > 1)
     * <li>not an ensemble definded by EDEF/ENDEDEF (need examples)
     * </ul>
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid GrADS grid file of the type listed above
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        try {
            gradsDDF = new GradsDataDescriptorFile(raf.getLocation());
            GradsDimension x = gradsDDF.getXDimension();
            GradsDimension y = gradsDDF.getYDimension();
            //J-
            
            return  gradsDDF.getDataType() == null && // only handle raw binary
                    gradsDDF.getDataFile() != null && 
                    !gradsDDF.hasProjection() &&  // can't handle projections
                    !gradsDDF.getVariables().isEmpty() &&  // must have valid entries
                    !gradsDDF.getDimensions().isEmpty() && 
                    (x.getSize() > 1) && (y.getSize() > 1);  // can't handle cross sections
//J+
        } catch (Exception ioe) {
            return false;
        }
    }

    /**
     * Get the file type id
     *
     * @return  the file type id
     */
    public String getFileTypeId() {
        return "GradsBinaryGrid";
    }

    /**
     * Get the file type description
     *
     * @return the file type description
     */
    public String getFileTypeDescription() {
        return "GrADS Binary Gridded Data";
    }

    /**
     * Open the service provider for reading.
     * @param raf  file to read from
     * @param ncfile  netCDF file we are writing to (memory)
     * @param cancelTask  task for cancelling
     *
     * @throws IOException  problem reading file
     */
    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask)
            throws IOException {
        super.open(raf, ncfile, cancelTask);
        this.ncFile = ncfile;
        // debugProj = true;
        if (gradsDDF == null) {
            gradsDDF = new GradsDataDescriptorFile(raf.getLocation());
        }
        xyHeaderBytes    = gradsDDF.getXYHeaderBytes();
        fileHeaderBytes  = gradsDDF.getFileHeaderBytes();
        timeHeaderBytes  = gradsDDF.getTimeHeaderBytes();
        timeTrailerBytes = gradsDDF.getTimeTrailerBytes();
        // get the first file so we can calculate the sequentialRecordBytes
        dataFile = getDataFile(0, 0);
        dataFile.order(getByteOrder());
        // assume all files are the same as the first
        if (gradsDDF.isSequential()) {
            GradsDimension     ensDim   = gradsDDF.getEnsembleDimension();
            int numens = ((ensDim != null) && !gradsDDF.isTemplate())
                         ? ensDim.getSize()
                         : 1;
            GradsTimeDimension timeDim  = gradsDDF.getTimeDimension();
            int                numtimes = 0;
            if (gradsDDF.isTemplate()) {
                int[] timesPerFile = 
                	gradsDDF.getTimeStepsPerFile(dataFile.getLocation());
                numtimes = timesPerFile[0];
            } else {
                numtimes = timeDim.getSize();
            }
            int gridsPerTimeStep        = gradsDDF.getGridsPerTimeStep();
            int numrecords              = numens * numtimes
                                          * gridsPerTimeStep;
            int                xlen     = gradsDDF.getXDimension().getSize();
            int                ylen     = gradsDDF.getYDimension().getSize();
            long               fileSize = dataFile.length();
            // calculate record indicator length
            long dataSize = fileHeaderBytes
                            + (xlen * ylen * 4l + xyHeaderBytes) * numrecords;
            // add on the bytes for the time header/trailers
            dataSize += numtimes * (timeHeaderBytes + timeTrailerBytes);
            int leftovers = (int) (fileSize - dataSize);
            sequentialRecordBytes = (leftovers / numrecords) / 2;
            if (sequentialRecordBytes < 0) {
            	throw new IOException("Incorrect sequential record byte size: " + 
            			sequentialRecordBytes);
            }
        }

        buildNCFile();
    }

    /**
     * Get the byte order from the data descriptor file
     *
     * @return  the byte order
     */
    private int getByteOrder() {
        return (gradsDDF.isBigEndian())
               ? RandomAccessFile.BIG_ENDIAN
               : RandomAccessFile.LITTLE_ENDIAN;
    }

    /**
     * Build the netCDF file
     *
     * @throws IOException   problem reading the file
     */
    protected void buildNCFile() throws IOException {
        ncFile.empty();
        fillNCFile();
        ncFile.finish();
        //System.out.println(ncfile);
    }

    /**
     * Get the variable name for the given dimension
     *
     * @param dim  the dimension
     *
     * @return  the variable name
     */
    private String getVarName(GradsDimension dim) {
        for (int i = 0; i < dimNames.length; i++) {
            if (dim.getName().equalsIgnoreCase(dimNames[i])) {
                return dimVarNames[i];
            }
        }
        return dim.getName();
    }

    /**
     * Fill out the netCDF file
     *
     * @throws IOException  problem reading or writing stuff
     */
    private void fillNCFile() throws IOException {

        List<GradsVariable>  vars  = gradsDDF.getVariables();
        List<GradsAttribute> attrs = gradsDDF.getAttributes();
        //TODO: ensembles
        List<GradsDimension>       dims = gradsDDF.getDimensions();
        Variable                   v;
        int                        numZ  = 0;
        HashMap<String, Dimension> zDims = new HashMap<String, Dimension>();
        for (GradsDimension dim : dims) {
            String    name  = getVarName(dim);
            int       size  = dim.getSize();
            Dimension ncDim = new Dimension(name, size, true);
            ncFile.addDimension(null, ncDim);
            if (name.equals(ENS_VAR)) {
                v = new Variable(ncFile, null, null, name, DataType.STRING,
                                 name);
                v.addAttribute(new Attribute("standard_name", "ensemble"));
                v.addAttribute(new Attribute(_Coordinate.AxisType,
                                             AxisType.Ensemble.toString()));
                List<String> names =
                    gradsDDF.getEnsembleDimension().getEnsembleNames();
                String[] nameArray = new String[names.size()];
                for (int i = 0; i < nameArray.length; i++) {
                    nameArray[i] = names.get(i);
                }
                Array dataArray = Array.factory(DataType.STRING,
                                      new int[] { nameArray.length },
                                      nameArray);
                v.setCachedData(dataArray, false);
            } else {
                double[] vals = dim.getValues();
                v = new Variable(ncFile, null, null, name, DataType.DOUBLE,
                                 name);
                v.addAttribute(new Attribute("units", dim.getUnit()));
                if (name.equals(Y_VAR)) {
                    v.addAttribute(new Attribute("long_name", "latitude"));
                    v.addAttribute(new Attribute("standard_name",
                            "latitude"));
                    v.addAttribute(new Attribute("axis", "Y"));
                    sizeY = dim.getSize();
                    v.addAttribute(new Attribute(_Coordinate.AxisType,
                            AxisType.Lat.toString()));
                } else if (name.equals(X_VAR)) {
                    v.addAttribute(new Attribute("long_name", "longitude"));
                    v.addAttribute(new Attribute("standard_name",
                            "longitude"));
                    v.addAttribute(new Attribute("axis", "X"));
                    v.addAttribute(new Attribute(_Coordinate.AxisType,
                            AxisType.Lon.toString()));
                    sizeX = dim.getSize();
                } else if (name.equals(Z_VAR)) {
                    numZ = size;
                    zDims.put(name, ncDim);
                    v.addAttribute(new Attribute("long_name", "level"));
                    addZAttributes(dim, v);
                } else if (name.equals(TIME_VAR)) {
                    v.addAttribute(new Attribute("long_name", "time"));
                    v.addAttribute(new Attribute(_Coordinate.AxisType,
                            AxisType.Time.toString()));
                }
                ArrayDouble.D1 varArray = new ArrayDouble.D1(size);
                for (int i = 0; i < vals.length; i++) {
                    varArray.set(i, vals[i]);
                }
                v.setCachedData(varArray, false);
            }
            ncFile.addVariable(null, v);
        }
        if (numZ > 0) {
            GradsDimension zDim = gradsDDF.getZDimension();
            double[]       vals = zDim.getValues();
            for (GradsVariable var : vars) {
                int nl = var.getNumLevels();
                if ((nl > 0) && (nl != numZ)) {
                    String name = Z_VAR + nl;
                    if (zDims.get(name) == null) {
                        Dimension ncDim = new Dimension(name, nl, true);
                        ncFile.addDimension(null, ncDim);
                        Variable vz = new Variable(ncFile, null, null, name,
                                          DataType.DOUBLE, name);
                        vz.addAttribute(new Attribute("long_name", name));
                        vz.addAttribute(new Attribute("units",
                                zDim.getUnit()));
                        addZAttributes(zDim, vz);
                        ArrayDouble.D1 varArray = new ArrayDouble.D1(nl);
                        for (int i = 0; i < nl; i++) {
                            varArray.set(i, vals[i]);
                        }
                        vz.setCachedData(varArray, false);
                        ncFile.addVariable(null, vz);
                        zDims.put(name, ncDim);
                    }
                }
            }
        }
        zDims = null;
        for (GradsVariable var : vars) {
            String coords = "latitude longitude";
            int    nl     = var.getNumLevels();
            if (nl > 0) {
                if (nl == numZ) {
                    coords = "level " + coords;
                } else {
                    coords = Z_VAR + nl + " " + coords;
                }
            }
            coords = "time " + coords;
            if (gradsDDF.getEnsembleDimension() != null) {
                coords = "ensemble " + coords;
            }
            v = new Variable(ncFile, null, null, var.getName(),
                             DataType.FLOAT, coords);
            v.addAttribute(new Attribute("long_name", var.getDescription()));
            if (var.getUnitName() != null) {
                v.addAttribute(new Attribute("units", var.getUnitName()));
            }
            v.addAttribute(
                new Attribute(
                    "_FillValue", new Float(gradsDDF.getMissingValue())));
            v.addAttribute(
                new Attribute(
                    "missing_value", new Float(gradsDDF.getMissingValue())));
            for (GradsAttribute attr : attrs) {
                if (attr.getVariable().equalsIgnoreCase(var.getName())) {
                    // TODO: what to do about a UINT16/32
                    if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.STRING)) {
                        v.addAttribute(new Attribute(attr.getName(),
                                attr.getValue()));
                    } else if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.BYTE)) {
                        try {
                            v.addAttribute(new Attribute(attr.getName(),
                                    new Byte(attr.getValue())));
                        } catch (NumberFormatException nfe) {}
                    } else if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.INT16)) {
                        try {
                            v.addAttribute(new Attribute(attr.getName(),
                                    new Short(attr.getValue())));
                        } catch (NumberFormatException nfe) {}
                    } else if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.INT32)) {
                        try {
                            v.addAttribute(new Attribute(attr.getName(),
                                    new Integer(attr.getValue())));
                        } catch (NumberFormatException nfe) {}
                    } else if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.FLOAT32)) {
                        try {
                            v.addAttribute(new Attribute(attr.getName(),
                                    new Float(attr.getValue())));
                        } catch (NumberFormatException nfe) {}
                    } else if (attr.getType().equalsIgnoreCase(
                            GradsAttribute.FLOAT64)) {
                        try {
                            v.addAttribute(new Attribute(attr.getName(),
                                    new Double(attr.getValue())));
                        } catch (NumberFormatException nfe) {}
                    }
                }
            }
            ncFile.addVariable(null, v);
        }
        // Global Attributes
        ncFile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
        ncFile.addAttribute(
            null,
            new Attribute(
                "history",
                "Direct read of GrADS binary grid into NetCDF-Java 4 API"));
        String title = gradsDDF.getTitle();
        if ((title != null) && !title.isEmpty()) {
            ncFile.addAttribute(null, new Attribute("title", title));
        }
        for (GradsAttribute attr : attrs) {
            if (attr.getVariable().equalsIgnoreCase(GradsAttribute.GLOBAL)) {
                ncFile.addAttribute(null,
                                    new Attribute(attr.getName(),
                                        attr.getValue()));
            }
        }

    }

    /**
     * Add the appropriate attributes for a Z dimension
     * @param zDim  The GrADS Z dimension
     * @param v     the variable to augment
     */
    private void addZAttributes(GradsDimension zDim, Variable v) {
        if (zDim.getUnit().indexOf("Pa") >= 0) {
            v.addAttribute(new Attribute("positive", "down"));
            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         AxisType.Pressure.toString()));
        } else {
            v.addAttribute(new Attribute("positive", "up"));
            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         AxisType.Height.toString()));
        }
    }

    /**
     * Read the grid
     *
     * @param index  the index of the grid
     *
     * @return  the grid data
     *
     * @throws IOException  problem reading stuff
     */
    private float[] readGrid(int index) throws IOException {
        //System.out.println("grid number: " + index);
        // NB: RandomAccessFile.skipBytes only takes an int.   For files larger than 
        // 2GB, that is problematic, so we use offset as a long
        long offset = 0;
        dataFile.seek(offset);
        // skip over the file header
        //dataFile.skipBytes(fileHeaderBytes);
        offset += fileHeaderBytes;
        // The full record structure of a Fortran sequential binary file is:
        //  [Length] [Record 1 data] [Length]
        //  [Length] [Record 2 data] [Length]
        //  [Length] [Record 3 data] [Length]
        //  ...
        //  [End of file] 
        // so we have to add 2*sequentialRecordBytes for each record we skip, 
        offset += (sizeX * sizeY * wordSize + xyHeaderBytes
                   + 2l * sequentialRecordBytes) * (long) index;
        //System.out.println("offset to grid = " + offset);

        // TODO: make sure this works - need an example
        int curTimeStep = index / gradsDDF.getGridsPerTimeStep();
        // add time headers
        offset += (curTimeStep + 1) * timeHeaderBytes;
        // add time trailers
        offset += curTimeStep * timeTrailerBytes;
        // and then 1 sequentialRecordBytes for the record itself (+ xyHeader)
        offset += (xyHeaderBytes + sequentialRecordBytes);
        //dataFile.skipBytes(offset);
        dataFile.seek(offset);
        float[] data = new float[sizeX * sizeY];
        dataFile.readFloat(data, 0, sizeX * sizeY);
        if (gradsDDF.isYReversed()) {
            int     newLoc = 0;
            float[] temp   = new float[sizeX * sizeY];
            for (int y = sizeY - 1; y >= 0; y--) {
                for (int x = 0; x < sizeX; x++) {
                    int oldLoc = y * sizeX + x;
                    temp[newLoc++] = data[oldLoc];
                }
            }
            data = temp;
        }
        return data;
    }

    /**
     * Close this IOSP and associated files
     *
     * @throws IOException problem closing files
     */
    public void close() throws IOException {
        if (dataFile != null) {
            dataFile.close();
        }
        dataFile = null;
        super.close();
    }

    /**
     * Find the GradsVariable associated with the netCDF variable
     *
     * @param v2 the netCDF variable
     *
     * @return  the corresponding GradsVariable
     */
    private GradsVariable findVar(Variable v2) {
        List<GradsVariable> vars    = gradsDDF.getVariables();
        String              varName = v2.getFullName();
        for (GradsVariable var : vars) {
            if (var.getName().equals(varName)) {
                return var;
            }
        }

        return null;  // can't happen?
    }


    /**
     * Read the data for the variable
     *
     * @param v2      Variable to read
     * @param section section infomation
     * @return Array of data
     * @throws IOException           problem reading from file
     * @throws InvalidRangeException invalid Range
     */
    public Array readData(Variable v2, Section section)
            throws IOException, InvalidRangeException {

        Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
        GradsVariable gradsVar = findVar(v2);

        // Canonical ordering is ens, time, level, lat, lon
        int           rangeIdx  = 0;
        Range         ensRange  = (gradsDDF.getEnsembleDimension() != null)
                                  ? section.getRange(rangeIdx++)
                                  : new Range(0, 0);
        Range         timeRange = (section.getRank() > 2)
                                  ? section.getRange(rangeIdx++)
                                  : new Range(0, 0);
        Range         levRange  = (gradsVar.getNumLevels() > 0)
                                  ? section.getRange(rangeIdx++)
                                  : new Range(0, 0);
        Range         yRange    = section.getRange(rangeIdx++);
        Range         xRange    = section.getRange(rangeIdx);

        IndexIterator ii        = dataArray.getIndexIterator();

        // loop over ens
        for (int ensIdx = ensRange.first(); ensIdx <= ensRange.last();
                ensIdx += ensRange.stride()) {
            //loop over time
            for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last();
                    timeIdx += timeRange.stride()) {
                //loop over level
                for (int levelIdx = levRange.first();
                        levelIdx <= levRange.last();
                        levelIdx += levRange.stride()) {
                    readXY(v2, ensIdx, timeIdx, levelIdx, yRange, xRange, ii);
                }
            }
        }

        return dataArray;
    }

    /**
     * read one YX array
     *
     * @param v2      variable to put the data into
     * @param ensIdx  ensemble index
     * @param timeIdx time index
     * @param levIdx  level index
     * @param yRange  x range
     * @param xRange  y range
     * @param ii      index iterator
     * @throws IOException           problem reading the file
     * @throws InvalidRangeException invalid range
     */
    private void readXY(Variable v2, int ensIdx, int timeIdx, int levIdx,
                        Range yRange, Range xRange, IndexIterator ii)
            throws IOException, InvalidRangeException {

        //System.out.println("ens: " + ensIdx + " , time = " + timeIdx
        //                   + ", lev = " + levIdx);


        dataFile = getDataFile(ensIdx, timeIdx);
        List<GradsVariable> vars = gradsDDF.getVariables();
        // if it's an ensemble template, then all data is in this file
        int numEns =
            ((gradsDDF.getTemplateType()
              == GradsDataDescriptorFile.ENS_TEMPLATE) || (gradsDDF
                  .getTemplateType() == GradsDataDescriptorFile
                  .ENS_TIME_TEMPLATE))
            ? 0
            : ensIdx;
        // if it's a time template figure out how many previous times we should use
        int numTimes = gradsDDF.getTimeDimension().getSize();
        if ((gradsDDF.getTemplateType() == GradsDataDescriptorFile
                .TIME_TEMPLATE) || (gradsDDF
                .getTemplateType() == GradsDataDescriptorFile
                .ENS_TIME_TEMPLATE)) {
            int[] tpf = gradsDDF.getTimeStepsPerFile(dataFile.getLocation());
            numTimes = tpf[0];
            timeIdx  = (timeIdx - tpf[1]) % numTimes;
        }
        int gridNum = numEns * numTimes * gradsDDF.getGridsPerTimeStep();
        // loop up to  the last time in the last ensemble
        for (int t = 0; t < timeIdx; t++) {
            for (GradsVariable var : vars) {
                int numVLevels = var.getNumLevels();
                if (numVLevels == 0) {
                    numVLevels = 1;
                }
                for (int l = 0; l < numVLevels; l++) {
                    gridNum++;
                }
            }
        }
        // loop up to  the last level in the last time in the last ensemble
        for (GradsVariable var : vars) {
            int numVLevels = var.getNumLevels();
            if (numVLevels == 0) {
                numVLevels = 1;
            }
            if (var.getName().equals(v2.getFullName())) {
                gridNum += levIdx;

                break;
            }
            for (int l = 0; l < numVLevels; l++) {
                gridNum++;
            }
        }

        // TODO: Flip grid if Y is reversed
        float[] data = readGrid(gridNum);

        // LOOK can improve with System.copy ??
        for (int y = yRange.first(); y <= yRange.last();
                y += yRange.stride()) {
            for (int x = xRange.first(); x <= xRange.last();
                    x += xRange.stride()) {
                int index = y * sizeX + x;
                ii.setFloatNext(data[index]);
            }
        }
    }

    /**
     * Get the data file to use (if this is a template)
     *
     * @param eIndex ensemble index
     * @param tIndex time index
     *
     * @return  the current file
     *
     * @throws IOException  couldn't open the current file
     */
    private RandomAccessFile getDataFile(int eIndex, int tIndex)
            throws IOException {

        String dataFilePath = gradsDDF.getFileName(eIndex, tIndex);
        if ( !gradsDDF.isTemplate()) {  // we only have one file
            if (dataFile != null) {
                return dataFile;
            }
        }
        if (dataFile != null) {
            String path = dataFile.getLocation();
            if (path.equals(dataFilePath)) {
                return dataFile;
            } else {
                dataFile.close();
            }
        }
        dataFile = new RandomAccessFile(dataFilePath, "r");
        dataFile.order(getByteOrder());
        return dataFile;
    }
}

