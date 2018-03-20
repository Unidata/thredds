/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;
/**
 * Listeners for "Cursor Move" events.
 * @author John Caron
 */
public interface CursorMoveEventListener extends java.util.EventListener {
    public void actionPerformed( CursorMoveEvent e);
}
