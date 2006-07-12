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
 * @version $Revision$ $Date$
 */
/* $Id$ */

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
