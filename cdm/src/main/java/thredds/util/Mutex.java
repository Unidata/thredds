/*
  File: Mutex.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
  25Sep2001  JCaron           track which thread has lock
*/

package thredds.util;

/**
 * A simple non-reentrant mutual exclusion lock.
 * The lock is free upon construction. Each acquire gets the
 * lock, and each release frees it. Releasing a lock that
 * is already free has no effect.
 * <p>
 * This implementation makes no attempt to provide any fairness
 * or ordering guarantees. If you need them, consider using one of
 * the Semaphore implementations as a locking mechanism.
 * <p>
 * <b>Sample usage</b><br>
 * <p>
 * Mutex can be useful in constructions that cannot be
 * expressed using java synchronized blocks because the
 * acquire/release pairs do not occur in the same method or
 * code block. For example, you can use them for hand-over-hand
 * locking across the nodes of a linked list. This allows
 * extremely fine-grained locking,  and so increases
 * potential concurrency, at the cost of additional complexity and
 * overhead that would normally make this worthwhile only in cases of
 * extreme contention.
 * <p>
 * JOHN CARON ADDITIONS:
 *   Track which Thread has acquired the mutex, throw RunTimeException if wrong Thread
 *   tries to release. You must match each release with exactly one acquire
 *
 * <p>[<a href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
**/

public class Mutex {

  /** The lock status **/
  private boolean inuse_ = false;
  private int who = 0;
  public boolean isLocked() { return inuse_; }
  public int getWho() { return who; }

  public synchronized boolean isLockedByMe() {
    return inuse_ && (who == Thread.currentThread().hashCode());
  }

  public void acquire() throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    synchronized(this) {
      try {
        while (inuse_) wait();
        inuse_ = true;
        who = Thread.currentThread().hashCode();
      }
      catch (InterruptedException ex) {
        notify();
        throw ex;
      }
    }
  }

  public synchronized void release()  {
    if (who != Thread.currentThread().hashCode())
      throw new RuntimeException("ERROR: MUTEX illegal released by different Thread than acquired "
      +" "+who+" != "+Thread.currentThread().hashCode());

    inuse_ = false;
    who = 0;
    notify();
  }


  public boolean attempt(long msecs) throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    synchronized(this) {
      if (!inuse_) {
        inuse_ = true;
        who = Thread.currentThread().hashCode();
        return true;
      }
      else if (msecs <= 0)
        return false;
      else {
        long waitTime = msecs;
        long start = System.currentTimeMillis();
        try {
          for (;;) {
            wait(waitTime);
            if (!inuse_) {
              inuse_ = true;
              who = Thread.currentThread().hashCode();
              return true;
            }
            else {
              waitTime = msecs - (System.currentTimeMillis() - start);
              if (waitTime <= 0)
                return false;
            }
          }
        }
        catch (InterruptedException ex) {
          notify();
          throw ex;
        }
      }
    }
  }

}

