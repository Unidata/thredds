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
package ucar.nc2.dt.radial;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;

/**
 * Factory to create RadialDatasets
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @deprecated use ucar.nc2.dt.TypedDatasetFactory

 */
public class RadialDatasetSweepFactory {

  private StringBuffer log;
  public String getErrorMessages() { return log == null ? "" : log.toString(); }

  public RadialDatasetSweep open( String location, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException {
    log = new StringBuffer();
    NetcdfDataset ncd = NetcdfDataset.acquireDataset( location, cancelTask);
    return open( ncd);
  }

  public RadialDatasetSweep open( NetcdfDataset ncd) {

    String convention = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals(_Coordinate.Convention)) {
      String format = ncd.findAttValueIgnoreCase(null, "Format", null);
      if (format.equals("Unidata/netCDF/Dorade"))
        return new Dorade2Dataset( ncd);
      if (format.equals("ARCHIVE2")  || format.equals("AR2V0001"))
        return new LevelII2Dataset( ncd);
      if (format.equals("Level3/NIDS") )
        return new Nids2Dataset( ncd);
    }

    return null;
  }

}
