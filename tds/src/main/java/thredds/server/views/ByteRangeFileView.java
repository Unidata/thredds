package thredds.server.views;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ByteRangeFileView extends AbstractView
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( ByteRangeFileView.class );

  protected void renderMergedOutputModel( Map model, HttpServletRequest request, HttpServletResponse response ) throws Exception
  {
  }
}
