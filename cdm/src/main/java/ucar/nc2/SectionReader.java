/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.*;
import ucar.nc2.util.CancelTask;

import java.io.IOException;

/**
 * A ProxyReader for logical sections of a Variable.
 *
 * @author caron
 * @see Variable#section(Section subsection)
 */

class SectionReader implements ProxyReader {
  private Section orgSection;   // section of the original
  private Variable orgClient;

  // section must be filled
  SectionReader(Variable orgClient, Section section) throws InvalidRangeException {
    this.orgClient = orgClient;
    this.orgSection = section.isImmutable() ? section : new Section(section.getRanges());
  }

  @Override
  public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
    try {
      return orgClient._read( orgSection);
    } catch (InvalidRangeException e) {
      try {
        orgClient._read( orgSection); // debug
      } catch (InvalidRangeException e1) {
        e1.printStackTrace();
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    Section want = orgSection.compose( section);
    return orgClient._read( want);
  }

}
