/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: VocabTranslator.java 48 2006-07-12 16:15:40Z caron $

package thredds.catalog.dl;

/**
 * @author john
 */
public interface VocabTranslator {
  public String translate( String from);
}