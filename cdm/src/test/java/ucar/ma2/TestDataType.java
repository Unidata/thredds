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
  
  @Test
  public void convertUnsignedByte() {
    // Widen regardless
    assertSameNumber((short) 12, DataType.widenNumber((byte) 12));
    
    // Widen only if negative.
    assertSameNumber((byte)  12,  DataType.widenNumberIfNegative((byte) 12));
    assertSameNumber((short) 155, DataType.widenNumberIfNegative((byte) 155));
    assertSameNumber((short) 224, DataType.widenNumberIfNegative((byte) -32));
  }
  
  @Test
  public void convertUnsignedShort() {
    // Widen regardless
    assertSameNumber((int) 3251, DataType.widenNumber((short) 3251));
    
    // Widen only if negative.
    assertSameNumber((short) 3251,  DataType.widenNumberIfNegative((short) 3251));
    assertSameNumber((int)   40000, DataType.widenNumberIfNegative((short) 40000));
    assertSameNumber((int)   43314, DataType.widenNumberIfNegative((short) -22222));
  }
  
  @Test
  public void convertUnsignedInt() {
    // Widen regardless
    assertSameNumber((long) 123456, DataType.widenNumber((int) 123456));
    
    // Widen only if negative.
    assertSameNumber((int)  123456,      DataType.widenNumberIfNegative((int) 123456));
    assertSameNumber((long) 3500000000L, DataType.widenNumberIfNegative((int) 3500000000L));
    assertSameNumber((long) 4289531025L, DataType.widenNumberIfNegative((int) -5436271));
  }
  
  @Test
  public void convertUnsignedLong() {
    // The maximum signed long is 9223372036854775807.
    // So, the number below fits in an unsigned long, but not a signed long.
    BigInteger tenQuintillion = new BigInteger("10000000000000000000");  // Nineteen zeros.
    
    // Widen regardless
    assertSameNumber(BigInteger.valueOf(3372036854775L), DataType.widenNumber((long) 3372036854775L));
    
    // Widen only if negative.
    assertSameNumber(3372036854775L,                         DataType.widenNumberIfNegative((long) 3372036854775L));
    assertSameNumber(tenQuintillion,                         DataType.widenNumberIfNegative(tenQuintillion.longValue()));
    assertSameNumber(new BigInteger("18446620616920539271"), DataType.widenNumberIfNegative((long) -123456789012345L));
  }
  
  // Asserts tha numbers have the same type and value.
  private static void assertSameNumber(Number expected, Number actual) {
    Assert.assertEquals(expected.getClass(), actual.getClass());
    Assert.assertEquals(expected, actual);
  }
}
