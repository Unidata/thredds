/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
