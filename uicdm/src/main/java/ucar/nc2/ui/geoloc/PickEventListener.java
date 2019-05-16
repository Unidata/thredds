/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;
/**
 * Listeners for "Pick Object" events.
 * @author John Caron
 */
public interface PickEventListener extends java.util.EventListener {
    void actionPerformed(PickEvent e);
}
