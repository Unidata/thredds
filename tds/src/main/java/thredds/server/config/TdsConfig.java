package thredds.server.config;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsConfig.class );

  private TdsConfigHtml tdsConfigHtml;
  private TdsCatConfig tdsCatConfig;
  private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
  private final Lock r = rwl.readLock();
  private final Lock w = rwl.writeLock();

  public void setTdsConfigHtml( TdsConfigHtml tdsConfigHtml)
  {
    this.tdsConfigHtml = tdsConfigHtml;
  }

  public TdsConfigHtml getTdsConfigHtml()
  {
    return this.tdsConfigHtml;
  }

  public void setTdsCatConfig( TdsCatConfig tdsCatConfig )
  {
    w.lock();
    try {
      this.tdsCatConfig = tdsCatConfig; }
    finally { w.unlock(); }
  }

  public TdsCatConfig getTdsCatConfig()
  {
    r.lock();
    try {
      return this.tdsCatConfig; }
    finally { r.unlock(); }
  }
}
