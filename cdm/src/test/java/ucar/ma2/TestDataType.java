package ucar.ma2;

import org.junit.Test;

import java.math.BigInteger;

/**
 * Test ucar.ma2.DataType
 *
 * @author caron
 * @since 11/19/13
 */
public class TestDataType {

  private void doone(String org) {
    BigInteger biggy = new BigInteger(org);
    long convert = biggy.longValue(); // > 63 bits will become "negetive".
    String convertS = DataType.unsignedLongToString(convert);
    System.out.printf("org=%s == %d == %s%n", org, convert, convertS);
    assert org.equals(convertS);
  }

  @Test
  public void testUnsignedLongToString () {
    doone("18446744073709551615");
    doone("18446744073709551614");
    doone("18446744073709551613");
    doone(Long.toString(Long.MAX_VALUE));
    doone("0");
  }
}
