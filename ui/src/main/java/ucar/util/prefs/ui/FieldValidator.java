/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util.prefs.ui;

public interface FieldValidator {
  public boolean validate( Field fld, Object editValue, StringBuffer errMessages);
}
