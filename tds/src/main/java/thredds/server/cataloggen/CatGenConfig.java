package thredds.server.cataloggen;

import java.util.List;

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

  private List<CatGenTaskInfo> taskInfoList;

  CatGenConfig() {}
  void setTaskInfoList( List<CatGenTaskInfo> taskInfoList )
  {
    this.taskInfoList = taskInfoList;
  }
}
