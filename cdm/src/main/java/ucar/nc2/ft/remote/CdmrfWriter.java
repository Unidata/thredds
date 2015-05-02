/* Copyright */
package ucar.nc2.ft.remote;

import ucar.nc2.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.stream.NcStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Describe
 *
 * @author caron
 * @since 5/1/2015
 */
public class CdmrfWriter {
    //  must start with this "CDFF"
  static public final byte[] MAGIC_START = new byte[]{0x43, 0x44, 0x46, 0x46};

  CdmrGridAdapter gridDataset;
  String location;

  public CdmrfWriter(CdmrGridAdapter gridDataset, String location) throws IOException {
    this.gridDataset = gridDataset;
    this.location = location;
  }

  private boolean show = true;
  public long sendHeader(OutputStream out) throws IOException {
    long size = 0;

    CdmfRemoteProto.GridCoverage.Builder headerBuilder = encodeHeader(gridDataset);
    CdmfRemoteProto.GridCoverage header = headerBuilder.build();

    // header message
    size += NcStream.writeBytes(out, NcStream.MAGIC_HEADER);
    byte[] b = header.toByteArray();
    size += NcStream.writeVInt(out, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);

    // payload
    size += NcStream.writeBytes(out, b);
    if (show) System.out.println(" header size=" + size);

    return size;
  }

  CdmfRemoteProto.GridCoverage.Builder encodeHeader(CdmrGridAdapter gridDataset) {
    CdmfRemoteProto.GridCoverage.Builder builder = CdmfRemoteProto.GridCoverage.newBuilder();
    builder.setName(location);

    for (Attribute att : gridDataset.getAttributes())
      builder.addAtts(NcStream.encodeAtt(att));

    for (GridDatatype grid : gridDataset.getGrids()) {
        builder.addGrids(encodeGrid(grid));
    }

    return builder;
  }

  /*
  message Grid {
    required string name = 1; // short name
    required DataType dataType = 2;
    optional bool unsigned = 3 [default = false];
    repeated Attribute atts = 4;
    required CoordSys coordSys = 5;
  }
   */
  CdmfRemoteProto.Grid.Builder encodeGrid(GridDatatype grid) {
    CdmfRemoteProto.Grid.Builder builder = CdmfRemoteProto.Grid.newBuilder();
    builder.setName(grid.getName());
    builder.setDataType(NcStream.encodeDataType(grid.getDataType()));
    builder.setCoordSys(encodeCoordSys(grid));

    for (Attribute att : grid.getAttributes())
      builder.addAtts(NcStream.encodeAtt(att));

    return builder;
  }

  /*
   * message CoordSys {
     required string name = 1;
     repeated string coordVar = 2;
     repeated CoordTransform transforms = 3;
     repeated CoordSys components = 4;
   } */
  CdmfRemoteProto.CoordSys.Builder encodeCoordSys(GridDatatype grid) {
    CdmfRemoteProto.CoordSys.Builder builder = CdmfRemoteProto.CoordSys.newBuilder();
    GridCoordSystem gcs = grid.getCoordinateSystem();
    builder.setName(gcs.getName());
    return builder;
  }

}
