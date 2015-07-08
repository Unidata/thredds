/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allow dimensions of length 1 to be removed
 *
 * @author caron
 * @since 12/16/13
 */
public class ReduceReader implements ProxyReader {
  private Variable orgClient;
  private List<Integer> dims;    // dimension index into original

  /**
   * Reduce 1 or more dimension of length 1
   * @param orgClient original variable
   * @param dims  index(es) in original variable of the dimension to reduce; dimension must be length 1.
   */
  ReduceReader(Variable orgClient, List<Integer> dims) {
    this.orgClient = orgClient;
    this.dims = new ArrayList<>(dims);
    Collections.sort(this.dims);
  }

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    Array data = orgClient._read();

    for (int i=dims.size()-1; i>=0; i--)
      data = data.reduce( dims.get(i));  // highest first
    return data;
  }

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    Section orgSection = new Section(section.getRanges());
    for (int dim : dims)
      orgSection.insertRange(dim, Range.ONE); // lowest first

    Array data = orgClient._read( orgSection);

    for (int i=dims.size()-1; i>=0; i--)
      data = data.reduce( dims.get(i));  // highest first

    return data;
  }
}
