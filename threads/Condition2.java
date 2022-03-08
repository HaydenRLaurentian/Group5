package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    waitList = new LinkedList<KThread>(); 
    thread = new KThread();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	//add thread to linkList of waiting threads, release lock,
	// disable interrupt, sleep thread
	KThread currentThread = KThread.currentThread();
	waitList.add(currentThread);
	conditionLock.release();
	boolean setStatus = Machine.interrupt().disable();
	KThread.sleep();
    //restore interrupt, aquire lock
	Machine.interrupt().restore(setStatus);
	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	//the idea here is if several threads were added to the
	//linked list the first thread added would be at the last
	//index in the list
	if(waitList.size() > 0){
		boolean setStatus = Machine.interrupt().disable();
		KThread thread = (KThread) waitList.removeLast();
		if(thread != null){
			thread.ready();
		}
	Machine.interrupt().restore(setStatus);
	}
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	for(int i = 0; i <= waitList.size(); i++){
		
		thread = (KThread) waitList.get(i);
		wake();
	}
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitList;
    KThread thread;
}
