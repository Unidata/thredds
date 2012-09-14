package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * The Data Representation section (5) for GRIB-2 files
 *
 * @author caron
 * @since 3/29/11
 */
@Immutable
public class Grib2SectionDataRepresentation {
  private final long startingPosition;
  private final int dataPoints;
  private final int dataTemplate;
  private int length; // dont have length in index

  public Grib2SectionDataRepresentation(RandomAccessFile raf) throws IOException {
    startingPosition = raf.getFilePointer();

    // octets 1-4 (Length of DRS)
    length = GribNumbers.int4(raf);

   // octet 5
    int section = raf.read();
    if (section != 5)
      throw new IllegalArgumentException("Not a GRIB-2 Data representation section");

    // octets 6-9 number of datapoints
    dataPoints = GribNumbers.int4(raf);

    // octet 10
    int dt = GribNumbers.uint2(raf);
    dataTemplate = (dt == 40000) ? 40 : dt; // ?? NCEP bug ??

    raf.seek(startingPosition+length);
  }

  public Grib2SectionDataRepresentation(long startingPosition, int dataPoints, int dataTemplate) {
    this.startingPosition = startingPosition;
    this.dataPoints = dataPoints;
    this.dataTemplate = dataTemplate;
  }

  public int getDataPoints() {
    return dataPoints;
  }

  public int getDataTemplate() {
    return dataTemplate;
  }

  public long getStartingPosition() {
    return startingPosition;
  }

  // debug
  public long getLength(RandomAccessFile raf) throws IOException {
    if (length == 0) {
      raf.seek(startingPosition);
      length = GribNumbers.int4(raf);

    }
    return length;
  }

  public Grib2Drs getDrs(RandomAccessFile raf) throws IOException {
    raf.seek(startingPosition+11);
    return Grib2Drs.factory(dataTemplate, raf);
  }
}
