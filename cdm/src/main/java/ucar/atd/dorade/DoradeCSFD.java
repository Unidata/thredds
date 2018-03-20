/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import java.io.RandomAccessFile;
import java.util.Formatter;

class DoradeCSFD extends DoradeCELV {

  protected int nSegments;
  protected float rangeToFirstCell; // to center
  protected float[] segCellSpacing;
  protected short[] segNCells;

  public DoradeCSFD(RandomAccessFile file, boolean littleEndianData) throws DescriptorException {
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

    for (int seg = 0; seg < nSegments; seg++) {
      segCellSpacing[seg] = grabFloat(data, 16 + 4 * seg);
      segNCells[seg] = grabShort(data, 48 + 2 * seg);
      nCells += segNCells[seg];
    }

    ranges = new float[nCells];
    int cell = 0;
    float endOfPrevCell = 0.0f;

    for (int seg = 0; seg < nSegments; seg++) {
      for (int segCell = 0; segCell < segNCells[seg]; segCell++) {
        if (cell == 0) {
          ranges[cell++] = rangeToFirstCell;
          endOfPrevCell = rangeToFirstCell + segCellSpacing[seg] / 2;
        } else {
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
    Formatter f = new Formatter();
    f.format("CSFD  number of segments= %d", nSegments);
    for (int seg = 0; seg < nSegments; seg++) {
      f.format("  segment=%d # of cells=%d cell spacing=%f%n", seg, segNCells[seg], segCellSpacing[seg]);
    }
    return f.toString();
  }
}
