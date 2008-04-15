package thredds.server.cataloggen;

import java.util.List;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenConfig.class );

  private boolean isValid = true;
  private String invalidMsg;

  private List<CatGenTaskConfig> taskConfigList;

  CatGenConfig()
  {
    taskConfigList = Collections.emptyList();
  }

  CatGenConfig( String invalidMsg )
  {
    this.isValid = false;
    this.invalidMsg = invalidMsg;

    taskConfigList = Collections.emptyList();
  }

  CatGenConfig( List<CatGenTaskConfig> taskConfigList )
  {
    if ( taskConfigList == null )
      this.taskConfigList = Collections.emptyList();
    else
      this.taskConfigList = taskConfigList;
  }

  public List<CatGenTaskConfig> getTaskInfoList()
  {
    return this.taskConfigList;
  }

  public boolean isValid()
  {
    return isValid;
  }

  public String getInvalidMsg()
  {
    return invalidMsg;
  }
}
