/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc.projection;

import ucar.unidata.geoloc.projection.LambertConformal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *  Some basic tests of the Lambert conformal conic projection class,
 *  checking that it accepts or rejects certain projection parameters.
 */
public class TestLambertConformal
{
/**
 *  LambertConformal should accept latitude-at-origin that is at either pole.
 */
    @Test
    public void acceptCenterLatAtAPole()
    {
        LambertConformal sp = new LambertConformal (-90., 0., 45., 45.);
        LambertConformal np = new LambertConformal ( 90., 0., 45., 45.);
    }

/**
 *  LambertConformal should reject latitude-at-origin less than -90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectCenterLatTooNegative()
    {
        LambertConformal lc = new LambertConformal (-91., 0., 30., 45.);
    }

/**
 *  LambertConformal should reject latitude-at-origin greater than +90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectCenterLatTooPositive()
    {
        LambertConformal lc = new LambertConformal (+91., 0., 30., 45.);
    }

/**
 *  LambertConformal should reject standard parallel #1 set to -90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectParallel1AtSouthPole()
    {
        LambertConformal lc = new LambertConformal (45., 0., -90., -45.);
    }

/**
 *  LambertConformal should reject standard parallel #1 set to +90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectParallel1AtNorthPole()
    {
        LambertConformal lc = new LambertConformal (45., 0., 90., 45.);
    }

/**
 *  LambertConformal should reject standard parallel #2 set to -90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectParallel2AtSouthPole()
    {
        LambertConformal lc = new LambertConformal (45., 0., -45., -90.);
    }

/**
 *  LambertConformal should reject standard parallel #2 set to +90.
 */
    @Test(expected=IllegalArgumentException.class)
    public void rejectParallel2AtNorthPole()
    {
        LambertConformal lc = new LambertConformal (45., 0., 45., 90.);
    }
}
