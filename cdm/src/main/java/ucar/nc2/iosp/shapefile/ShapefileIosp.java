/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.shapefile;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Class Description.
 *
 * @author caron
 * @since 9/27/12
 */
public class ShapefileIosp extends AbstractIOServiceProvider {
  private final static int MAGIC = 9994;
  private final static int VERSION = 1000;

  @Override
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) throws IOException {
    // this is the first time we try to read the file - if there's a problem we get a IOException
    raf.seek(0);
    raf.order(ByteOrder.BIG_ENDIAN);
    int fileCode = raf.readInt();
    if (fileCode != MAGIC)
      return false;

    raf.seek(28);
    raf.order(ByteOrder.LITTLE_ENDIAN);
    int version = raf.readInt();
    if (version != VERSION)
      return false;
    return true;
  }

  @Override
  public void open( RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask ) throws IOException {
    super.open(raf, ncfile, cancelTask);
  }

  //////////////////
  /*
 Position Field Value Type Order
 Byte 0 File Code 9994 Integer Big
 Byte 4 Unused 0 Integer Big
 Byte 8 Unused 0 Integer Big
 Byte 12 Unused 0 Integer Big
 Byte 16 Unused 0 Integer Big
 Byte 20 Unused 0 Integer Big
 Byte 24 File Length File Length Integer Big
 Byte 28 Version 1000 Integer Little
 Byte 32 Shape Type Shape Type Integer Little
 Byte 36 Bounding Box Xmin Double Little
 Byte 44 Bounding Box Ymin Double Little
 Byte 52 Bounding Box Xmax Double Little
 Byte 60 Bounding Box Ymax Double Little
 Byte 68* Bounding Box Zmin Double Little
 Byte 76* Bounding Box Zmax Double Little
 Byte 84* Bounding Box Mmin Double Little
 Byte 92* Bounding Box Mmax Double Little
  */
  enum Type {none, point, polyline, polygon, multipoint}
  private Type type;
  private ProjectionRect bb;

  private void readHeader() throws IOException {
    raf.seek(32);
    raf.order(ByteOrder.LITTLE_ENDIAN);
    int itype = raf.readInt();
    type = assignType(itype);
    bb = readBoundingBox();
  }

  private Type assignType(int type) {
    switch (type) {
      case 1 : return Type.point;
      case 3 : return Type.polyline;
      case 5 : return Type.polygon;
      case 8 : return Type.multipoint;
      default:
        throw new RuntimeException("shapefile type "+type+" not supported");
    }
  }

  private ProjectionRect readBoundingBox() throws IOException {
    double xMin = raf.readDouble();
    double yMin = raf.readDouble();
    double xMax = raf.readDouble();
    double yMax = raf.readDouble();
    return new ProjectionRect(xMin, yMin, xMax, yMax);
  }


  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public String getFileTypeId() {
    return "Shapefile";
  }

  @Override
  public String getFileTypeDescription() {
    return "ESRI shapefile";
  }

  static public void main(String args[]) throws IOException {
    String fname = "C:\\data\\g4g/EcoAtlas_modern_baylands.shp";
    ShapefileIosp iosp = new ShapefileIosp();
    RandomAccessFile raf = new RandomAccessFile(fname, "r");
    System.out.printf("%s%n",iosp.isValidFile(raf));
  }
}
