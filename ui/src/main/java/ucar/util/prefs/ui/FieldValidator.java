package ucar.util.prefs.ui;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Aug 20, 2005
 * Time: 2:30:11 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FieldValidator {
  public boolean validate( Field fld, Object editValue, StringBuffer errMessages);
}
