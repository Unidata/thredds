/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;
import java.io.IOException;

/**
 * A ProxyReader for slices.
 *
 * @author caron
 * @see Variable#section(Section subsection)
 */

class SectionReader implements ProxyReader {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SectionReader.class);
  private Variable orgVar;
  private Section orgSection;   // section of the original

  // section must be filled
  SectionReader(Variable orgVar, Section section) throws InvalidRangeException {
    this.orgVar = orgVar;
    this.orgSection = section.isImmutable() ? section : new Section(section.getRanges());
  }

  public Array read() throws IOException {
    try {
      return orgVar._read(orgSection);
    } catch (InvalidRangeException e) {
      log.error("InvalidRangeException in SectionReader, var="+orgVar.getName());
      throw new IllegalStateException(e.getMessage());
    }
  }

  public Array read(Section section) throws IOException, InvalidRangeException {
    Section want = orgSection.compose( section);
    return orgVar._read(want);
  }

}
