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
package ucar.nc2.ft.point;

import ucar.nc2.Structure;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;

import java.io.IOException;

/**
 * Use contiguous or linked lists to iterate over members of a Structure
 * @author caron
 * @since Mar 26, 2008
 */
public class StructureDataIteratorLinked implements StructureDataIterator {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDataIteratorLinked.class);

  private Structure s;
  private int firstRecord, nextRecno, numRecords;
  private String linkVarName;
  private int currRecno;

  public StructureDataIteratorLinked(Structure s, int firstRecord, int numRecords, String linkVarName) throws IOException {
    this.s = s;
    this.firstRecord = firstRecord;
    this.nextRecno = firstRecord;
    this.numRecords = numRecords; // contiguous only
    this.linkVarName = linkVarName;
  }

  @Override
  public StructureData next() throws IOException {
    StructureData sdata;
    currRecno = nextRecno;
    try {
      sdata = s.readStructure( currRecno);
    } catch (ucar.ma2.InvalidRangeException e) {
      log.error("StructureDataLinkedIterator.nextStructureData recno=" + currRecno, e);
      throw new IOException(e.getMessage());
    }

    if (numRecords > 0) { // contiguous case
      nextRecno++;
      if (nextRecno >= firstRecord + numRecords)
        nextRecno = -1;

    } else {
      nextRecno = sdata.getScalarInt(linkVarName);
    }

    return sdata;
  }

  @Override
  public boolean hasNext() throws IOException {
    return nextRecno >= 0;
  }

  @Override
  public StructureDataIterator reset() {
    this.nextRecno = firstRecord;
    return this;
  }

  @Override
  public void setBufferSize(int bytes) {}

  @Override
  public int getCurrentRecno() {
    return currRecno;
  }
  
}
