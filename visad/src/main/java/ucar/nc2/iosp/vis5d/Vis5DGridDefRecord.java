/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.vis5d;


import ucar.nc2.iosp.grid.*;
import ucar.nc2.iosp.mcidas.McGridDefRecord;


/**
 * Class to hold the Vis5D grid navigation information.
 *
 * @author Unidata Development Team
 */
public class Vis5DGridDefRecord extends GridDefRecord {

  private static final int PROJ_GENERIC = 0;
  private static final int PROJ_LINEAR = 1;
  private static final int PROJ_CYLINDRICAL = 20;
  private static final int PROJ_SPHERICAL = 21;
  private static final int PROJ_LAMBERT = 2;
  private static final int PROJ_STEREO = 3;
  private static final int PROJ_ROTATED = 4;
  private int projection;

  //private Vis5DCoordinateSystem coord_sys;

  /**
   * Create a new grid nav block with the values
   *
   * @param Projection _more_
   * @param projargs   _more_
   * @param nr         _more_
   * @param nc         _more_
   */
  public Vis5DGridDefRecord(int Projection, double[] projargs, int nr, int nc) {
    projection = Projection;
    //coord_sys = new Vis5DCoordinateSystem(Projection, projargs, nr, nc);
    setParams(Projection, projargs);
    addParam(GDS_KEY, this.toString());
  }

  /**
   * Get a short name for this GDSKey for the netCDF group.
   * Subclasses should implement as a short description
   *
   * @return short name
   */
  public String getGroupName() {
    return getParam(PROJ) + "_" + getParam(NX) + "x" + getParam(NY);
  }

  public String toString() {
    // TODO: make this more unique
    return getParam(PROJ) + " X:" + getParam(NX) + " " + "Y:" + getParam(NY) + " Proj:" + projection;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vis5DGridDefRecord)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  public int hashCode() {
    return toString().hashCode();
  }



  private void setParams(int Projection, double[] projargs) {
    double NorthBound;
    double SouthBound;
    double WestBound;
    double EastBound;
    double RowInc;
    double ColInc;
    double Lat1;
    double Lat2;
    double PoleRow;
    double PoleCol;
    double CentralLat;
    double CentralLon;
    double CentralRow;
    double CentralCol;
    double Rotation;  /* radians */
    double Cone;
    double Hemisphere;
    double ConeFactor;
    double CosCentralLat;
    double SinCentralLat;
    double StereoScale;
    double InvScale;
    double CylinderScale;
    switch (Projection) {

      case PROJ_GENERIC:
        addParam(PROJ, "GENERIC");
      case PROJ_LINEAR:
        addParam(PROJ, "LINEAR");
      case PROJ_CYLINDRICAL:
        addParam(PROJ, "CYLINDRICAL");
      case PROJ_SPHERICAL:
        addParam(PROJ, "SPHERICAL");
        NorthBound = projargs[0];
        WestBound = -projargs[1];
        RowInc = projargs[2];
        ColInc = projargs[3];
        addParam(LA1, String.valueOf(NorthBound));
        addParam(LO1, String.valueOf(WestBound));
        addParam(DX, String.valueOf(ColInc));
        addParam(DY, String.valueOf(RowInc));
        break;

      case PROJ_ROTATED:
        addParam(PROJ, "ROTATED");
        NorthBound = projargs[0];
        WestBound = projargs[1];
        RowInc = projargs[2];
        ColInc = projargs[3];
        CentralLat = projargs[4];
        CentralLon = projargs[5];
        Rotation = projargs[6];
        break;

      case PROJ_LAMBERT:
        addParam(PROJ, "LAMBERT");
        Lat1 = projargs[0];
        Lat2 = projargs[1];
        PoleRow = projargs[2];
        PoleCol = projargs[3];
        CentralLon = projargs[4];
        ColInc = projargs[5];
        break;

      case PROJ_STEREO:
        addParam(PROJ, "STEREO");
        CentralLat = projargs[0];
        CentralLon = projargs[1];
        CentralRow = projargs[2];
        CentralCol = projargs[3];
        ColInc = projargs[4];
        break;

    }
  }

  /**
   * _more_
   *
   * @param Projection _more_
   * @param projargs   _more_
   */
  public static void printProjArgs(int Projection, double[] projargs) {
    double NorthBound;
    double SouthBound;
    double WestBound;
    double EastBound;
    double RowInc;
    double ColInc;
    double Lat1;
    double Lat2;
    double PoleRow;
    double PoleCol;
    double CentralLat;
    double CentralLon;
    double CentralRow;
    double CentralCol;
    double Rotation;  /* radians */
    double Cone;
    double Hemisphere;
    double ConeFactor;
    double CosCentralLat;
    double SinCentralLat;
    double StereoScale;
    double InvScale;
    double CylinderScale;
    switch (Projection) {

      case PROJ_GENERIC:
      case PROJ_LINEAR:
      case PROJ_CYLINDRICAL:
      case PROJ_SPHERICAL:
        NorthBound = projargs[0];
        WestBound = projargs[1];
        RowInc = projargs[2];
        ColInc = projargs[3];
        System.out.println("Generic, Linear, Cylindrical, Spherical:");
        System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                + ", rowInc: " + RowInc + ", colInc: "
                + ColInc);
        break;

      case PROJ_ROTATED:
        NorthBound = projargs[0];
        WestBound = projargs[1];
        RowInc = projargs[2];
        ColInc = projargs[3];
        CentralLat = projargs[4];
        CentralLon = projargs[5];
        Rotation = projargs[6];
        System.out.println("Rotated:");
        System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                + ", rowInc: " + RowInc + ", colInc: "
                + ColInc + ", clat: " + CentralLat
                + ", clon: " + CentralLon + ", rotation: "
                + Rotation);
        break;

      case PROJ_LAMBERT:
        Lat1 = projargs[0];
        Lat2 = projargs[1];
        PoleRow = projargs[2];
        PoleCol = projargs[3];
        CentralLon = projargs[4];
        ColInc = projargs[5];
        System.out.println("Lambert: ");
        System.out.println("lat1: " + Lat1 + ", lat2: " + Lat2
                + ", poleRow: " + PoleRow + ", PoleCol: "
                + PoleCol + ", clon: " + CentralLon
                + ", colInc: " + ColInc);
        break;

      case PROJ_STEREO:
        CentralLat = projargs[0];
        CentralLon = projargs[1];
        CentralRow = projargs[2];
        CentralCol = projargs[3];
        ColInc = projargs[4];
        System.out.println("Stereo: ");
        System.out.println("clat: " + CentralLat + ", clon: "
                + CentralLon + ", cRow: " + CentralRow
                + ", cCol: " + CentralCol + ", colInc: "
                + ColInc);
        break;

      default:
        System.out.println("Projection unknown");
    }
  }
}

