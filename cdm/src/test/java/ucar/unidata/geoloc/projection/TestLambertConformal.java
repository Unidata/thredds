/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.projection.LambertConformal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.invoke.MethodHandles;

/**
 *  Some basic tests of the Lambert conformal conic projection class,
 *  checking that it accepts or rejects certain projection parameters.
 */
public class TestLambertConformal
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
