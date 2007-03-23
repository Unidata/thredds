// $Id: $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2;

/**
 * An estimate of the cost of performing a data access operation.
 * @author caron
 */
public interface DataCost {

  /**
   * Estimated number of data objects this operation will return
   * @return number of data objects or -1 if unknown
   */
  int estimateDataCount();

  /**
   * Estimated number of seconds this operation will take.
   * Zero means data is memory resident.
   * @return number of seconds or -1 if unknown
   */
  int estimateTimeCostInSeconds();
}