/*
 * 
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.*;

import ucar.nc2.iosp.grid.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.*;


/**
 *  Read grid(s) from a McIDAS grid file
 */
public class McIDASGridReader {

    /** The file */
    protected RandomAccessFile rf;

    /** An error message */
    private String errorMessage;

    /** Grid index */
    private GridIndex gridIndex;

    /** swap flag */
    protected boolean needToSwap = false;

    /** hashMap of GridDefRecords */
    private HashMap<String, McGridDefRecord> gdsMap = new HashMap<String,
                                                          McGridDefRecord>();

    /**
     * Bean ctor
     */
    public McIDASGridReader() {}

    /**
     * Create a McIDASGrid Reader from the file
     *
     * @param filename  filename
     *
     * @throws IOException problem reading file
     */
    public McIDASGridReader(String filename) throws IOException {
        this(new RandomAccessFile(filename, "r", 2048));
    }

    /**
     * Create a McIDASGrid Reader from the file
     *
     * @param raf  RandomAccessFile
     *
     * @throws IOException problem reading file
     */
    public McIDASGridReader(RandomAccessFile raf) throws IOException {
        init(raf);
    }

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param raf   RandomAccessFile to read.
     *
     * @throws IOException   problem reading file
     */
    public final void init(RandomAccessFile raf) throws IOException {
        rf = raf;
        raf.order(RandomAccessFile.BIG_ENDIAN);
        boolean ok = init();
        if ( !ok) {
            throw new IOException("Unable to open McIDAS Grid file: "
                                  + errorMessage);
        }
    }

    /**
     * Initialize this reader.  Get the Grid specific info
     *
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init() throws IOException {
        if (rf == null) {
            logError("File is null");
            return false;
        }
        gridIndex = new GridIndex();

        rf.order(RandomAccessFile.BIG_ENDIAN);
        int numEntries = Math.abs(readInt(10));
        if (numEntries > 10000000) {
            needToSwap = true;
            numEntries = Math.abs(McIDASUtil.swbyt4(numEntries));
            if (numEntries > 10000000) {
                return false;
            }
        }
        // System.out.println("need to Swap = " + needToSwap);
        // System.out.println("number entries="+numEntries);

        // go back to the beginning
        rf.seek(0);
        // read the fileheader
        String label = rf.readString(32);
        //System.out.println("label = " + label);

        int project = readInt(8);
        //System.out.println("Project = " + project);

        int date = readInt(9);
        //System.out.println("date = " + date);

        int[] entries = new int[numEntries];
        for (int i = 0; i < numEntries; i++) {
            entries[i] = readInt(i + 11);
            // sanity check that this is indeed a McIDAS Grid file
            if (entries[i] < -1) {
                logError("bad grid offset " + i + ": " + entries[i]);
                return false;
            }
        }

        // Don't swap:
        rf.order(RandomAccessFile.BIG_ENDIAN);
        for (int i = 0; i < numEntries; i++) {
            if (entries[i] == -1) {
                continue;
            }
            int[] header = new int[64];
            rf.seek(entries[i] * 4);
            rf.readInt(header, 0, 64);
            if (needToSwap) {
                swapGridHeader(header);
            }
            try {

                McIDASGridRecord gr = new McIDASGridRecord(entries[i],
                                          header);
                if (gr.getGridDefRecordId().equals("CONF X:93 Y:65")) {
                    //if (gr.getGridDefRecordId().equals("CONF X:54 Y:47")) {
                    // figure out how to handle Mercator projections
                    // if ( !(gr.getGridDefRecordId().startsWith("MERC"))) {
                    gridIndex.addGridRecord(gr);
                    if (gdsMap.get(gr.getGridDefRecordId()) == null) {
                        McGridDefRecord mcdef = gr.getGridDefRecord();
                        //System.out.println("new nav " + mcdef.toString());
                        gdsMap.put(mcdef.toString(), mcdef);
                        gridIndex.addHorizCoordSys(mcdef);
                    }
                }
            } catch (McIDASException me) {
                logError("problem creating grid dir");
                return false;
            }
        }
        // check to see if there are any grids that we can handle
        if (gridIndex.getGridRecords().isEmpty()) {
            logError("no grids found");
            return false;
        }
        return true;
    }

    /**
     * Swap the grid header, avoiding strings
     *
     * @param gh   grid header to swap
     */
    private void swapGridHeader(int[] gh) {
        McIDASUtil.flip(gh, 0, 5);
        McIDASUtil.flip(gh, 7, 7);
        McIDASUtil.flip(gh, 9, 10);
        McIDASUtil.flip(gh, 12, 14);
        McIDASUtil.flip(gh, 32, 51);
    }

    /**
     * Read the grid
     *
     * @param gr  the grid record
     *
     * @return the data
     */
    public float[] readGrid(McIDASGridRecord gr) {

        float[] data = null;
        try {
            int te   = (gr.getOffsetToHeader() + 64) * 4;
            int rows = gr.getRows();
            int cols = gr.getColumns();
            rf.seek(te);

            float scale = (float) gr.getParamScale();

            data = new float[rows * cols];
            rf.order(needToSwap
                     ? rf.LITTLE_ENDIAN
                     : rf.BIG_ENDIAN);
            int n = 0;
            // store such that 0,0 is in lower left corner...
            for (int nc = 0; nc < cols; nc++) {
                for (int nr = 0; nr < rows; nr++) {
                    int temp = rf.readInt();  // check for missing value
                    data[(rows - nr - 1) * cols + nc] = (temp
                            == McIDASUtil.MCMISSING)
                            ? Float.NaN
                            : ((float) temp) / scale;
                }
            }
            rf.order(rf.BIG_ENDIAN);
        } catch (Exception esc) {
            System.out.println(esc);
        }
        return data;
    }

    /**
     * to get the grid header corresponding to the last grid read
     *
     * @return McIDASGridDirectory of the last grid read
     */
    public GridIndex getGridIndex() {
        return gridIndex;
    }

    /**
     * Read an integer
     * @param word   word in file (0 based) to read
     *
     * @return  int read
     *
     * @throws IOException   problem reading file
     */
    public int readInt(int word) throws IOException {
        if (rf == null) {
            throw new IOException("no file to read from");
        }
        rf.seek(word * 4);
        // set the order
        if (needToSwap) {
            rf.order(RandomAccessFile.LITTLE_ENDIAN);  // swap
        } else {
            rf.order(RandomAccessFile.BIG_ENDIAN);
        }
        int idata = rf.readInt();
        rf.order(RandomAccessFile.BIG_ENDIAN);
        return idata;
    }

    /**
     * Log an error
     *
     * @param errMsg message to log
     */
    private void logError(String errMsg) {
        errorMessage = errMsg;
    }

    /**
     * for testing purposes
     *
     * @param args   file name
     *
     * @throws IOException  problem reading file
     */
    public static void main(String[] args) throws IOException {
        String file = "GRID2001";
        if (args.length > 0) {
            file = args[0];
        }
        McIDASGridReader mg        = new McIDASGridReader(file);
        GridIndex        gridIndex = mg.getGridIndex();
        List             grids     = gridIndex.getGridRecords();
        System.out.println("found " + grids.size() + " grids");
        int num = Math.min(grids.size(), 10);
        for (int i = 0; i < num; i++) {
            System.out.println(grids.get(i));
        }

    }
}

