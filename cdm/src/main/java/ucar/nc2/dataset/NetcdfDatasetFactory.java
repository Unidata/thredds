// $Id: NetcdfDatasetFactory.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * Used by NetcdfDatasetCache
 * @author john caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public interface NetcdfDatasetFactory {

  /**
   * Open the NetcdfDataset.
   * @param location location of the dataset
   * @param cancelTask allows user to cancel, may be null.
   * @return a valid NetcdfDataset
   * @throws java.io.IOException
   */
  public NetcdfDataset openDataset(String location, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException;
}