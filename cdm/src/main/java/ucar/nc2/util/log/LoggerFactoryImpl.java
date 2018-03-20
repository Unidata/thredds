/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.log;

import org.slf4j.Logger;

/**
 * Default LoggerFactory uses org.slf4j.LoggerFactory
 *
 * @author caron
 * @since 3/27/13
 */
public class LoggerFactoryImpl implements LoggerFactory {

  @Override
  public Logger getLogger(String name) {
    return org.slf4j.LoggerFactory.getLogger(name);
  }
}
