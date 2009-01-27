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
 * <p>Title: DoradeCELV</p>
 * <p>Description: DORADE cell range vector</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeCELV.java 51 2006-07-12 17:13:13Z caron $ */

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
