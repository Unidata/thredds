package ucar.nc2.grib;

import static org.junit.Assert.*;
import static ucar.nc2.grib.GribNumbers.convertSignedByte;
import static ucar.nc2.grib.GribNumbers.convertSignedByte2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.ma2.DataType;

@RunWith(JUnit4.class)
public class TestGribNumbers {

  @Test
  public void testConvertSignedByte() {
    System.out.printf("byte == convertSignedByte == convertSignedByte2 == hex%n");
    for (int i = 125; i < 256; i++) {
      byte b = (byte) i;
      System.out.printf("%d == %d == %d == %s%n", b, convertSignedByte(b), convertSignedByte2(b),
          Long.toHexString((long) i));
      assertEquals(convertSignedByte(b), convertSignedByte2(b));
    }
  }

  @Test
  public void testConvertUnsigned() {
    int val = (int) DataType.unsignedByteToShort((byte) -200);
    int val2 = DataType.unsignedShortToInt((short) -200);
    assertNotEquals(val, val2);
  }

}
