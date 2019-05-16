/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.prefs;

public interface FieldValidator {
  boolean validate(Field fld, Object editValue, StringBuffer errMessages);
}
