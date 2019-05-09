/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;
/**
 * Listeners for new world bounding box events.
 * @author John Caron
 */
public interface NewMapAreaListener extends java.util.EventListener {
    void actionPerformed(NewMapAreaEvent e);
}
