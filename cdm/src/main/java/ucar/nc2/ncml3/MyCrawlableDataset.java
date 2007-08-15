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
package ucar.nc2.ncml3;

import thredds.crawlabledataset.CrawlableDataset;

import java.util.Date;

/**
 * Encapsolate a CrawlableDataset.
 * @author caron
 * @since Aug 10, 2007
 */
class MyCrawlableDataset {
  Scanner dir;
  CrawlableDataset file;

  Date dateCoord; // will have both or neither
  String dateCoordS;

  Date runDate; // fmrcHourly only
  Double offset;

  MyCrawlableDataset(Scanner dir, CrawlableDataset file) {
    this.dir = dir;
    this.file = file;
  }

  // MyCrawlableDataset with the same CrawlableDataset.path are equal
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof MyCrawlableDataset)) return false;
    MyCrawlableDataset other = (MyCrawlableDataset) oo;
    return file.getPath().equals(other.file.getPath());
  }

  public int hashCode() {
    return file.getPath().hashCode();
  }
}
