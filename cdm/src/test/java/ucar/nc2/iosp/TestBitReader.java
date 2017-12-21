/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
