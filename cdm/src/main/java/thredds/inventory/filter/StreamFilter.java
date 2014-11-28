package thredds.inventory.filter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * A java.nio.file.DirectoryStream.Filter using a regexp on the last entry of the Path
 *
 * @author John
 * @since 1/28/14
 */
public class StreamFilter implements DirectoryStream.Filter<Path> {
  private Pattern pattern;
  public StreamFilter(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean accept(Path entry) throws IOException {
    String last = entry.getName(entry.getNameCount()-1).toString();
    java.util.regex.Matcher matcher = this.pattern.matcher(last);
    return matcher.matches();
  }
}
