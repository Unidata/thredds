/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.event;

/** Listeners for ActionValueEvents.
 * @author John Caron
 */
public interface ActionValueListener extends java.util.EventListener {
  void actionPerformed(ActionValueEvent e);
}

