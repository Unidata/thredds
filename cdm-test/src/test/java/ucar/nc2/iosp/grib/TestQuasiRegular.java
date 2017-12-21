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

package ucar.nc2.iosp.grib;

/**
 * from https://github.com/lost-carrier 6/12/2014
 */
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.QuasiRegular;

public class TestQuasiRegular {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final double x1d = 1.0e30;  /* derivative of the first end point */
    private static final double xnd = 1.0e30;  /* derivative of the nth end point */

	@Test
	public void testCubicSpline() throws IOException {
		float[] in = new float[]{0.0f, 9.0f, 18.0f, 27.0f};

		double[] d2 = new double[4];
		QuasiRegular.secondDerivative(in, 0, 4, x1d, xnd, d2);

		float[] out = new float[8];
		QuasiRegular.cubicSpline(in, 0, d2, 0.0, out, 0);
		assertEquals(0.0, out[0], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 0.5, out, 1);
		assertEquals(4.5, out[1], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 1.0, out, 2);
		assertEquals(9.0, out[2], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 1.5, out, 3);
		assertEquals(13.5, out[3], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 2.0, out, 4);
		assertEquals(18.0, out[4], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 2.5, out, 5);
		assertEquals(22.5, out[5], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 3.0, out, 6);
		assertEquals(27.0, out[6], 0.0);
		QuasiRegular.cubicSpline(in, 0, d2, 3.5, out, 7);
		assertEquals(13.5, out[7], 0.0);
	}

}
