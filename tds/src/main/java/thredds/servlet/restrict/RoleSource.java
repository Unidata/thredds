/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.servlet.restrict;

/**
 * This tells whether a user has the named role.
 *
 * @author caron
 */
public interface RoleSource {
   public boolean hasRole(String username, String role);
}
