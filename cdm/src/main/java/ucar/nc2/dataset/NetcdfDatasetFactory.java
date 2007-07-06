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
package ucar.nc2.dataset;

/**
 * 	A factory for opening a NetcdfDataset.
 * @author john caron
 */
public interface NetcdfDatasetFactory {

  /**
   * Open the NetcdfDataset.
   * @param location location of the dataset
   * @param  buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param  cancelTask allow task to be cancelled; may be null.
   * @param  spiObject sent to iosp.setSpecial() if not null
   * @return a valid NetcdfDataset
   * @throws java.io.IOException on error
   */
  public NetcdfDataset openDataset(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws java.io.IOException;
}