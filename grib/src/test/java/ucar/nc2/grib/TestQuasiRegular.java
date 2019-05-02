/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

/**
 * from https://github.com/lost-carrier 6/12/2014
 */
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
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
