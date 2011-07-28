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


package ucar.nc2.iosp.gempak;


import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.List;


/**
 * Read a Gempak surface file
 */
public class GempakSurfaceFileReader extends AbstractGempakStationFileReader {

    /** Surface Text identifier */
    public static final String SFTX = "SFTX";

    /** Surface Data identifier */
    public static final String SFDT = "SFDT";

    /** Surface Data identifier */
    public static final String SFSP = "SFSP";

    /** standard surface file id */
    public static final String STANDARD = "standard";

    /** climate surface file id */
    public static final String CLIMATE = "climate";

    /** ship surface file id */
    public static final String SHIP = "ship";

    /**
     * Default ctor
     */
    GempakSurfaceFileReader() {}

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param raf   RandomAccessFile to read.
     * @param fullCheck  if true, check entire structure
     *
     * @return A GempakSurfaceFileReader
     * @throws IOException   problem reading file
     */
    public static GempakSurfaceFileReader getInstance(RandomAccessFile raf,
            boolean fullCheck)
            throws IOException {
        GempakSurfaceFileReader gsfr = new GempakSurfaceFileReader();
        gsfr.init(raf, fullCheck);
        return gsfr;
    }

    /**
     * Initialize this reader.  Read all the metadata
     *
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init() throws IOException {
        return init(true);
    }

    /**
     * Initialize this reader.  Get the Grid specific info
     *
     * @param fullCheck  check to make sure there are grids we can handle
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init(boolean fullCheck) throws IOException {

        if ( !super.init(fullCheck)) {
            return false;
        }

        // Modeled after SF_OFIL
        if (dmLabel.kftype != MFSF) {
            logError("not a surface data file ");
            return false;
        }


        int    numParams = 0;
        String partType  = ((dmLabel.kfsrce == 100) && (dmLabel.kprt == 1))
                           ? SFTX
                           : SFDT;

        DMPart part      = getPart(partType);

        if (part == null) {
            logError("No part named " + partType + " found");
            return false;
        } else {
            numParams = part.kparms;
        }

        if ( !readStationsAndTimes(true)) {
            logError("Unable to read stations and times");
            return false;
        }
        // since the reads are ob by ob, set buffer size small
        if (subType.equals(STANDARD)) rf.setBufferSize(256);
        return true;

    }

    /**
     * Make the list of dates.  Override superclass to make the
     * value based on the subtype
     * @param uniqueTimes  true to make a unique list
     *
     * @return the list of times
     */
    protected List<String> makeDateList(boolean uniqueTimes) {
        return super.makeDateList( !getFileSubType().equals(SHIP));
    }

    /**
     * Set the file subType.
     */
    protected void makeFileSubType() {
        // determine file type
        String latType = findKey(GempakStation.SLAT).type;
        if ( !(findKey(DATE).type.equals(latType))) {
            if (latType.equals(ROW)) {
                subType = CLIMATE;
            } else {
                subType = STANDARD;
            }
        } else {
            subType = SHIP;
        }
    }

    /**
     * Print the list of dates in the file
     *
     * @param row ob row
     * @param col ob column
     */
    public void printOb(int row, int col) {
        int           stnIndex = (getFileSubType().equals(CLIMATE))
                                 ? row
                                 : col;
        List<GempakStation> stations = getStations();
        if (stations.isEmpty() || stnIndex > stations.size()) {
            System.out.println("\nNo data available");
            return;
        }
        GempakStation station  = getStations().get(stnIndex - 1);
        StringBuilder builder  = new StringBuilder();
        builder.append("\nStation:\n");
        builder.append(station.toString());
        builder.append("\nObs\n\t");
        List<GempakParameter> params = getParameters(SFDT);
        for (GempakParameter parm : params) {
            builder.append(StringUtil.padLeft(parm.getName(), 7));
            builder.append("\t");
        }
        builder.append("\n");
        RData rd = null;
        try {
            rd = DM_RDTR(row, col, SFDT);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            rd = null;
        }
        if (rd == null) {
            builder.append("No Data Available");
        } else {
            builder.append("\t");
            float[] data = rd.data;
            for (int i = 0; i < data.length; i++) {
                builder.append(
                    StringUtil.padLeft(
                        Format.formatDouble(data[i], 7, 1), 7));
                builder.append("\t");
            }
            int[] header = rd.header;
            if (header.length > 0) {
                builder.append("\nOb Time = ");
                builder.append(header[0]);
            }
        }
        System.out.println(builder.toString());
    }


    /**
     * Get the type for this file
     * @return file type (CLIMATE, STANDARD, SHIP)
     */
    public String getSurfaceFileType() {
        return getFileSubType();
    }

    /**
     * Run the program
     *
     * @param args  [0] filename (required),
     *              [1] variable name (X for default),
     *              [2] X to not list grids
     *
     * @throws IOException problem reading the file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("need to supply a GEMPAK surface file name");
            System.exit(1);
        }
        try {
            GempakParameters.addParameters(
                "resources/nj22/tables/gempak/params.tbl");
        } catch (Exception e) {
            System.out.println("unable to init param tables");
        }

        GempakSurfaceFileReader gsfr = getInstance(getFile(args[0]), true);
        System.out.println("Type = " + gsfr.getSurfaceFileType());
        gsfr.printFileLabel();
        gsfr.printKeys();
        gsfr.printHeaders();
        gsfr.printParts();
        //gsfr.printDates();
        //gsfr.printStations(false);
        int row = 1;
        int col = 1;
        if (args.length > 1) {
            row = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            col = Integer.parseInt(args[2]);
        }
        gsfr.printOb(row, col);
    }

}

