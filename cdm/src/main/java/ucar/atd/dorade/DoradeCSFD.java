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
 * <p>Title: DoradeCSFD</p>
 * <p>Description: DORADE "cell spacing floating-point" descriptor,
 *    containing up to eight groups of cells with fixed cell spacing within
 *    each group.  This descriptor can be used in place of the CELV
 *    descriptor, so is implemented as a subclass of DoradeCELV.</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeCSFD.java 51 2006-07-12 17:13:13Z caron $ */

class DoradeCSFD extends DoradeCELV {

    protected int nSegments;
    protected float rangeToFirstCell; // to center
    protected float[] segCellSpacing;
    protected short[] segNCells;

    public DoradeCSFD(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
	//
	// The CSFD descriptor:
	//	descriptor name			char[4]	= "CSFD"
	//	descriptor len			int	= 64
	//	number of segments (<= 8)	int
	//	distance to first cell, m	float
	//	segment cell spacing		float[8]
	//	cells in segment		short[8]
	//
        byte[] data = readDescriptor(file, littleEndianData, "CSFD");

        //
        // unpack
        //
	nSegments = grabInt(data, 8);
	rangeToFirstCell = grabFloat(data, 12);

	segCellSpacing = new float[nSegments];
	segNCells = new short[nSegments];

	nCells = 0;

	for (int seg = 0; seg < nSegments; seg++)
	{
	    segCellSpacing[seg] = grabFloat(data, 16 + 4 * seg);
	    segNCells[seg] = grabShort(data, 48 + 2 * seg);
	    nCells += segNCells[seg];
	}

	ranges = new float[nCells];
	int cell = 0;
	float endOfPrevCell = 0.0f;
	
	for (int seg = 0; seg < nSegments; seg++)
	{
	    for (int segCell = 0; segCell < segNCells[seg]; segCell++)
	    {
		if (cell == 0)
		{
		    ranges[cell++] = rangeToFirstCell;
		    endOfPrevCell = rangeToFirstCell + segCellSpacing[seg] / 2;
		}
		else
		{
		    ranges[cell++] = endOfPrevCell + segCellSpacing[seg] / 2;
		    endOfPrevCell += segCellSpacing[seg];
		}
	    }
	}

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "CSFD\n";
        s += "  number of segments: " + nSegments;
	for (int seg = 0; seg < nSegments; seg++)
	{
	    s += "\n";
	    s += "  segment " + seg + "\n";
	    s += "    # of cells: " + segNCells[seg] + "\n";
	    s += "    cell spacing: " + segCellSpacing[seg] + " m";
	}
        return s;
    }
}
