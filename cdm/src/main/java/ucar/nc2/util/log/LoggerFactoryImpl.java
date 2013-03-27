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
