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
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;

import java.io.IOException;

/**
 * A ProxyReader for slices.
 *
 * @author caron
 * @see Variable#slice(int, int)
 */

class SliceReader implements ProxyReader {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SliceReader.class);

  private Variable orgClient;
  private int sliceDim;    // dimension index into original
  private Section slice;   // section of the original

  SliceReader(Variable orgClient, int dim, Section slice) throws InvalidRangeException {
   // LOOK could do check that slice is compatible with client

    this.orgClient = orgClient;
    this.sliceDim = dim;
    this.slice = slice;
  }

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Array data;
    try {
      data = orgClient._read( slice);
    } catch (InvalidRangeException e) {
      log.error("InvalidRangeException in slice, var="+ client);
      throw new IllegalStateException(e.getMessage());
    }
    data = data.reduce( sliceDim);
    return data;
  }

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    Section orgSection = new Section(section.getRanges());
    orgSection.insertRange(sliceDim, slice.getRange(sliceDim));
    Array data = orgClient._read( orgSection);
    data.reduce( sliceDim);
    return data;
  }

}
