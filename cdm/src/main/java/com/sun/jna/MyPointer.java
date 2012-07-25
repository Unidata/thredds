package com.sun.jna;

/**
 * Need access to com.sun.jna.Pointer
 *
 * @author caron
 * @since 7/24/12
 */
public class MyPointer extends Pointer {
    public MyPointer(long peer) {
      this.peer = peer;
    }
}
