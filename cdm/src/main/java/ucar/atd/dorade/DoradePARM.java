/*
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
package ucar.atd.dorade;

import java.io.RandomAccessFile;

/**
 * <p>Title: DoradePARM</p>
 * <p>Description: DORADE parameter descriptor</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradePARM.java 51 2006-07-12 17:13:13Z caron $ */

public class DoradePARM extends DoradeDescriptor {

    /**
     * Bad data value flag returned by getParamValues().
     */
    public static final float BAD_VALUE = Float.MAX_VALUE;

    private String paramName;
    private String paramDescription;
    private String unitName;
    private short usedPRTs;
    private short usedFrequencies;
    private float rcvrBandwidth; // MHz
    private short pulseWidth; // m
    private short polarization; // 0 horizontal, 1 vertical, 2 circular,
                                // 3 elliptical
    private short nSamples;
    private short binaryFormat; // 1 8-bit integer, 2 16-bit integer,
                                // 3 24-bit integer, 4 32-bit float,
                                // 5 16-bit float


    /**
     * 8-bit signed integer format.
     */
    public static final int FORMAT_8BIT_INT = 1;
    /**
     * 16-bit signed integer format.
     */
    public static final int FORMAT_16BIT_INT = 2;
    /**
     * 32-bit signed integer format.
     */
    public static final int FORMAT_32BIT_INT = 3;
    /**
     * 32-bit IEEE float format.
     */
    public static final int FORMAT_32BIT_FLOAT = 4;
    /**
     * 16-bit IEEE float format.
     */
    public static final int FORMAT_16BIT_FLOAT = 5;

    private String thresholdParamName;
    private float thresholdValue;
    private float scale;
    private float bias;
    private int badDataFlag;
    private DoradeRADD myRADD;

    public DoradePARM(RandomAccessFile file, boolean littleEndianData,
                      DoradeRADD radd)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "PARM");
        myRADD = radd;

        //
        // unpack
        //
        paramName = new String(data, 8, 8).trim();
        paramDescription = new String(data, 16, 40).trim();
        unitName = new String(data, 56, 8).trim();
        usedPRTs = grabShort(data, 64);
        usedFrequencies = grabShort(data, 66);
        rcvrBandwidth = grabFloat(data, 68);
        pulseWidth = grabShort(data, 72);
        polarization = grabShort(data, 74);
        nSamples = grabShort(data, 76);
        binaryFormat = grabShort(data, 78);
        thresholdParamName = new String(data, 80, 8).trim();
        thresholdValue = grabFloat(data, 88);
        scale = grabFloat(data, 92);
        bias = grabFloat(data, 96);
        badDataFlag = grabInt(data, 100);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "PARM\n";
        s += "  param name: " + paramName + "\n";
        s += "  param description: " + paramDescription + "\n";
        s += "  unit name: " + unitName + "\n";
        s += "  used PRTs: " + usedPRTs + "\n";
        s += "  used frequencies: " + usedFrequencies + "\n";
        s += "  receiver bandwidth: " + rcvrBandwidth + "\n";
        s += "  pulse width: " + pulseWidth + "\n";
        s += "  polarization: " + polarization + "\n";
        s += "  number of samples: " + nSamples + "\n";
        s += "  binary format: " + binaryFormat + "\n";
        s += "  threshold parameter: " + thresholdParamName + "\n";
        s += "  threshold value: " + thresholdValue + "\n";
        s += "  scale: " + scale + "\n";
        s += "  bias: " + bias + "\n";
        s += "  bad data flag: " + badDataFlag;
        return s;
    }

    /**
     * Get the name of this parameter.
     * @return the name of the parameter
     */
    public String getName() {
        return paramName;
    }

    // unidata added
    public int getBadDataFlag() {
        return badDataFlag;
    }

    // unidata added
    public float getThresholdValue() {
        return thresholdValue;
    }

    // unidata added
    public int getPolarization() {
        return polarization;
    }

    // unidata added
    public float getScale() {
        return scale;
    }

    // unidata added
    public String getUnitName() {
        return unitName;
    }

    // unidata added
    public int getusedPRTs() {
        return usedPRTs;
    }

    // unidata added
    public int getusedFrequencies() {
        return usedFrequencies;
    }

    // unidata added
    public int getnSamples() {
        return nSamples;
    }

    // unidata added
    public String getthresholdParamName() {
        return thresholdParamName;
    }

    /**
     * Get the units string for this parameter.
     * @return the units string
     */
    public String getUnits() {
        return unitName;
    }

    /**
     * Get the long description for this parameter.
     * @return the description string
     */
    public String getDescription() {
        return paramDescription;
    }

    /**
     * Get the binary format used for encoding this parameter.
     * Legal values are:
     * <li><code>FORMAT_8BIT_INT</code>
     * <li><code>FORMAT_16BIT_INT</code>
     * <li><code>FORMAT_32BIT_INT</code>
     * <li><code>FORMAT_16BIT_FLOAT</code>
     * <li><code>FORMAT_32BIT_FLOAT</code>
     * @return the binary format for this parameter
     */
    public int getBinaryFormat() {
        return binaryFormat;
    }

    /**
     * Get the number of cells in a ray.
     * @return  the number of cells in a ray
     */
    public int getNCells() {
        return myRADD.getNCells();
    }

    /**
     * Get the cell spacing.  An exception is thrown if the cell spacing
     * is not constant.
     * @return  the cell spacing, in meters
     * @throws DescriptorException if the cell spacing is not constant.
     */
    public float getCellSpacing() throws DescriptorException {
        return myRADD.getCellSpacing();
    }


    /**
     * Get the unpacked data values for a selected parameter.
     * @param rdat the name of the desired parameter
     * @return the unpacked data values for all cells, using BAD_VALUE
     * for bad data cells
     * @throws DescriptorException
     */
    public float[] getParamValues(DoradeRDAT rdat)
            throws DescriptorException {
	return getParamValues(rdat, null);
    }

    /**
     * Get the unpacked data values for a selected parameter.
     * @param rdat the name of the desired parameter
     * @param workingArray If non-null and the same length as needed then use this.
     * @return the unpacked data values for all cells, using BAD_VALUE
     * for bad data cells
     * @throws DescriptorException
     */
    public float[] getParamValues(DoradeRDAT rdat,float[] workingArray)
            throws DescriptorException {


        if (! paramName.equals(rdat.getParamName()))
            throw new DescriptorException("parameter name mismatch");

        byte[] paramData = rdat.getRawData();

        int nCells = myRADD.getNCells();

        float[] values;
	if(workingArray!=null && workingArray.length == nCells) {
	    values = workingArray;
	} else {
	    values = new float[nCells];
	}

        short[] svalues = null;
        if (myRADD.getCompressionScheme() == DoradeRADD.COMPRESSION_HRD) {
            if (binaryFormat != DoradePARM.FORMAT_16BIT_INT) {
                throw new DescriptorException("Cannot unpack " +
                           "compressed data with binary format " +
                           binaryFormat);
            }
            svalues = uncompressHRD(paramData, nCells);
        }

        for (int cell = 0; cell < nCells; cell++) {
            switch (binaryFormat) {
                case DoradePARM.FORMAT_8BIT_INT:
                    byte bval = paramData[cell];
                    values[cell] = (bval == badDataFlag) ?
                                   BAD_VALUE : (bval - bias) / scale;
                    break;
                case DoradePARM.FORMAT_16BIT_INT:
                    short sval = (svalues != null) ?
                                 svalues[cell] : grabShort(paramData, 2 * cell);
                    values[cell] = (sval == badDataFlag) ?
                                   BAD_VALUE : (sval - bias) / scale;
                    break;
                case DoradePARM.FORMAT_32BIT_INT:
                    int ival = grabInt(paramData, 4 * cell);
                    values[cell] = (ival == badDataFlag) ?
                                   BAD_VALUE : (ival - bias) / scale;
                    break;
                case DoradePARM.FORMAT_32BIT_FLOAT:
                    float fval = grabFloat(paramData, 4 * cell);
                    values[cell] = (fval == badDataFlag) ?
                                   BAD_VALUE : (fval - bias) / scale;
                    break;
                case DoradePARM.FORMAT_16BIT_FLOAT:
                    throw new DescriptorException("can't unpack 16-bit " +
                                                        "float data yet");
                default:
                    throw new DescriptorException("bad binary format (" +
                                                        binaryFormat + ")");
            }
        }
        return values;
    }

    /**
     * Unpack MIT/HRD-compressed data into an array of exactly nCells shorts.
     * @param compressedData the raw HRD-compressed data array
     * @return an array of nCells unpacked short values
     * @throws DescriptorException
     */
    private short[] uncompressHRD(byte[] compressedData, int nCells)
            throws DescriptorException {
        short[] svalues = new short[nCells];

        int cPos = 0; // position in the compressed data, in bytes

        int nextCell = 0;
        int runLength;
        for (;; nextCell += runLength) {
            //
            // Each run begins with a 16-bin run descriptor.  The
            // high order bit is set if the run consists of bad flags.
            // The remaining 15 bits tell the length of the run.
            // A run length of 1 indicates the end of compressed data.
            //
            short runDescriptor = grabShort(compressedData, cPos);
            cPos += 2;

            boolean runHasGoodValues = ((runDescriptor & 0x8000) != 0);
            runLength = runDescriptor & 0x7fff;

            if (runLength == 1)
                break;
            //
            // Sanity check on run length
            //
            if ((nextCell + runLength) > nCells)
                throw new DescriptorException("attempt to unpack " +
                                                    "too many cells");
            //
            // If the run contains good values, then the next runLength
            // values in the compressed data stream are real values.  Otherwise
            // we need to fill with runLength bad value flags.
            //
            for (int cell = nextCell; cell < nextCell + runLength; cell++) {
                if (runHasGoodValues) {
                    svalues[cell] = grabShort(compressedData, cPos);
                    cPos += 2;
                } else {
                    svalues[cell] = (short)badDataFlag;
                }
            }
        }
        //
        // Fill the remainder of the array (if any) with bad value flags
        //
        for (int cell = nextCell; cell < nCells; cell++)
            svalues[cell] = (short)badDataFlag;

        return svalues;
    }


}
