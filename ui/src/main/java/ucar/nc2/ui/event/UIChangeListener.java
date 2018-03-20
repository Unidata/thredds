/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.event;
/** Listeners for UIChangeEvents.
 * @author John Caron
 */
public interface UIChangeListener extends java.util.EventListener {
    public void processChange( UIChangeEvent e);
}
