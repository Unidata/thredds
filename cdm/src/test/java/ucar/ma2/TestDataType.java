package ucar.ma2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

/**
 * Test ucar.ma2.DataType
 *
 * @author caron
 * @since 11/19/13
 */
public class TestDataType {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static void doone(String org) {
    BigInteger biggy = new BigInteger(org);
    long convert = biggy.longValue(); // > 63 bits will become "negative".
    String convertS = DataType.unsignedLongToString(convert);
    Assert.assertEquals(org, convertS);
  }

  @Test
  public void testUnsignedLongToString () {
    doone("18446744073709551615");
    doone("18446744073709551614");
    doone("18446744073709551613");
    doone(Long.toString(Long.MAX_VALUE));
    doone("0");
  }

  @Test
  public void testUnsignedLongToBigInt() {
    Assert.assertEquals("The long round-trips as expected.",
            Long.MAX_VALUE, DataType.unsignedLongToBigInt(Long.MAX_VALUE).longValueExact());

    BigInteger overflowAsBigInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    Assert.assertEquals(new BigInteger("9223372036854775808"), overflowAsBigInt);

    long overflowAsLong = overflowAsBigInt.longValue();
    Assert.assertEquals(-9223372036854775808L, overflowAsLong);

    Assert.assertEquals("Interpret signed long as unsigned long",
            overflowAsBigInt, DataType.unsignedLongToBigInt(overflowAsLong));
  }
}
