/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * from https://github.com/lost-carrier 6/12/2014
 */
public class TestBitReader {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Test
	public void testUcar() throws IOException {
		BitReader bu = new BitReader(new byte[] {-1,2,4,8});
	    assertEquals(127, bu.bits2UInt(7));
	    assertEquals(1, bu.bits2UInt(1));
	}

	@Test
	public void testSignedPositive() throws IOException {
		BitReader bu = new BitReader(new byte[] {32,0,0,0});
		assertEquals(2, (int) bu.bits2SInt(4));
	}

	@Test
	public void testSignedNegative() throws IOException {
		BitReader bu = new BitReader(new byte[] {(byte)160,0,0,0});
		int binary = (int) bu.bits2SInt(4);
		assertEquals(-2, binary);
	}

	@Test
	public void testSignedNegative2() throws IOException {
		BitReader bu = new BitReader(new byte[] {(byte)71,(byte)200,(byte)235,(byte)216,(byte)128,(byte)0});
		assertEquals(574, (int) bu.bits2UInt(11));
		assertEquals(570, (int) bu.bits2UInt(11));
		assertEquals(-945, (int) bu.bits2SInt(11));
	}

	@Test
	@Ignore("why is this failing?")
	public void testSignedNegative3() throws IOException {
		BitReader bu = new BitReader(new byte[] {(byte)199,(byte)242,(byte)0,(byte)0,(byte)6,(byte)6});
		assertEquals(799, (int) bu.bits2UInt(10));
		assertEquals(800, (int) bu.bits2UInt(10));
		assertEquals(-344, (int) bu.bits2SInt(10));
	}

}
