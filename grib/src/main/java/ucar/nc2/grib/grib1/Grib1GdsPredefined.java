/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;


/**
 * Helper class for pre-defined grid definition section (GDS) .
 * These are NCEP.
 *
 * @see "http://www.nco.ncep.noaa.gov/pmb/docs/on388/tableb.html"
 */

public class Grib1GdsPredefined {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1GdsPredefined.class);

  /**
   * Constructs a Grib1Gds object from a pds and predefined tables.
   *
   * @param center center id
   * @param gridNumber from pds (octet 7)
   * @return predefined GDS
   */
  public static Grib1Gds factory(int center, int gridNumber) {
    if (center == 7) {
      return factoryNCEP(gridNumber);
    } else
      throw new IllegalArgumentException("Dont have predefined GDS " + gridNumber + " from " + center);
  }

  // 21-26, 61-64: International Exchange and Family of Services (FOS) grids. So may be more general than NCEP

  private static Grib1Gds factoryNCEP(int gridNumber) {
    switch (gridNumber) {
      case 21:
        return new NcepLatLon(gridNumber, 37, 36, 0.0F, 0.0F, 90.0F, 180.0F, 5.0F, 2.5F, (byte) 0x88, (byte) 64);
      case 22:
        return new NcepLatLon(gridNumber, 37, 36, 0.0F, -180.0F, 90.0F, 0.0F, 5.0F, 2.5F, (byte) 0x88, (byte) 64);
      case 23:
        return new NcepLatLon(gridNumber, 37, 36, -90.0F, 0.0F, 180.0F, 0.0F, 5.0F, 2.5F, (byte) 0x88, (byte) 64);
      case 24:
        return new NcepLatLon(gridNumber, 37, 36, -90.0F, -180.0F, 0.0F, 0.0F, 5.0F, 2.5F, (byte) 0x88, (byte) 64);
      case 25:
        return new NcepLatLon(gridNumber, 72, 18, 0.0F, 0.0F, 90.0F, 355.0F, 5.0F, 5.0F, (byte) 0x88, (byte) 64);
      case 26:
        return new NcepLatLon(gridNumber, 72, 18, -90.0F, 0.0F, 0.0F, 355.0F, 5.0F, 5.0F, (byte) 0x88, (byte) 64);
      case 61:
        return new NcepLatLon(gridNumber, 91, 45, 0.0F, 0.0F, 90.0F, 180.0F, 2.0F, 2.0F, (byte) 0x88, (byte) 64);
      case 62:
        return new NcepLatLon(gridNumber, 91, 45, -90.0F, 0.0F, 0.0F, 180.0F, 2.0F, 2.0F, (byte) 0x88, (byte) 64);
      case 63:
        return new NcepLatLon(gridNumber, 91, 45, -90.0F, 0.0F, 0.0F, 180.0F, 2.0F, 2.0F, (byte) 0x88, (byte) 64);
      case 64:
        return new NcepLatLon(gridNumber, 91, 45, -90.0F, -180.0F, 0.0F, 0.0F, 2.0F, 2.0F, (byte) 0x88, (byte) 64);
      case 87:
        return new NcepPS(gridNumber, 81, 62, 22.8756F, 239.5089F, 255.0F, 68153.0F, 68153.0F, (byte) 0x08, (byte) 64);
    }
    throw new IllegalArgumentException("Dont have predefined GDS " + gridNumber + " from NCEP (center 7)");
  }

  private static class NcepLatLon extends Grib1Gds.LatLon {
    int gridNumber;

    NcepLatLon(int gridNumber, int nx, int ny, float la1, float lo1, float la2, float lo2, float deltaLon, float deltaLat,
               byte resolution, byte scan) {
      super(1000 * gridNumber);
      this.gridNumber = gridNumber;
      this.nx = nx;
      this.ny = ny;
      this.la1 = la1;
      this.lo1 = lo1;
      this.la2 = la2;
      this.lo2 = lo2;
      this.deltaLon = deltaLon;
      this.deltaLat = deltaLat;
      this.resolution = resolution;
      this.scanMode = scan;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      NcepLatLon that = (NcepLatLon) o;
      if (gridNumber != that.gridNumber) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return gridNumber;
    }
  }

  private static class NcepPS extends Grib1Gds.PolarStereographic {
    int gridNumber;

    NcepPS(int gridNumber, int nx, int ny, float la1, float lo1, float lov, float dX, float dY,
           byte resolution, byte scan) {
      super(1000 * gridNumber);
      this.gridNumber = gridNumber;
      this.nx = nx;
      this.ny = ny;
      this.la1 = la1;
      this.lo1 = lo1;
      this.lov = lov;
      this.dX = dX;
      this.dY = dY;
      this.resolution = resolution;
      this.scanMode = scan;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      NcepPS that = (NcepPS) o;
      if (gridNumber != that.gridNumber) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return gridNumber;
    }
  }

}


