package ucar.atd.dorade;

import java.io.RandomAccessFile;

/**
 * <p>Title: DoradeCELV</p>
 * <p>Description: DORADE cell range vector</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision: 1.1 $ $Date: 2004/12/09 20:18:51 $
 */
/* $Id: DoradeCELV.java,v 1.1 2004/12/09 20:18:51 jeffmc Exp $ */

class DoradeCELV extends DoradeDescriptor {

    protected int nCells;
    protected float[] ranges;

    public DoradeCELV(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "CELV");

        //
        // unpack
        //
        nCells = grabInt(data, 8);
        ranges = new float[nCells];
        for (int i = 0; i < nCells; i++)
            ranges[i] = grabFloat(data, 12 + 4 * i);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    protected DoradeCELV() {}

    public String toString() {
        String s = "CELV\n";
        s += "  number of cells: " + nCells + "\n";
        s += "  ranges: " + ranges[0] + ", " + ranges[1] + ", ..., " +
             ranges[nCells - 1];
        return s;
    }

    /**
     * Get the number of cells
     * @return the number of cells
     */
    public int getNCells() {
        return nCells;
    }

    /**
     * Get the array of ranges to cell centers
     * @return  array of ranges to cell centers, in meters
     */
    public float[] getCellRanges() {
        return ranges;
    }
}
