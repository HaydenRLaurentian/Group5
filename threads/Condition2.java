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

    
    public static void selfTest(){
    	final Lock l = new Lock();
    	final Condition2 m = new Condition2(l);
    
    	new KThread(new Runnable(){
    		public void run(){
    			l.acquire();
    			System.out.println("Before Sleep");
    			System.out.println(m.waitList.size());
    			m.sleep();
    		}
    	}).setName("Test Case 1").fork();
  
    	new KThread(new Runnable(){
    		public void run(){	
    			System.out.println(m.waitList.size());	
    			l.acquire();
    			m.wake();
    			System.out.println(m.waitList.size());
    			l.release();
    		}
    	}).setName("Test Case 1").fork();
    	
    	/*
    	 * Create 3 threads and put them to sleep and wake them all a the end.
    	 */

    	new KThread(new Runnable(){
    		public void run(){
    			l.acquire();
    			System.out.println("Before Sleep");
    			System.out.println(m.waitList.size());
    			m.sleep();
    		}
    	}).setName("Test Case 2").fork();
    	

    	new KThread(new Runnable(){
    		public void run(){
    			l.acquire();
    			System.out.println("Before Sleep");
    			System.out.println(m.waitList.size());
    			m.sleep();
    		}
    	}).setName("Test Case 2").fork();
    
    	new KThread(new Runnable(){
    		public void run(){
    			l.acquire();
    			System.out.println("Before Sleep");
    			System.out.println(m.waitList.size());
    			m.sleep();	
    		}
    	}).setName("Test Case 2").fork();
    	
    	new KThread(new Runnable(){
    		public void run(){
    			l.acquire();
    			System.out.println(m.waitList.size());
    			m.wakeAll();
    			System.out.println(m.waitList.size());
    		}
    	}).setName("Test Case 2").fork();
    	
    	
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
		KThread thread = waitList.removeFirst();
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
		while(!waitList.isEmpty()){
			wake();
		}
    }

    private Lock conditionLock;
    private LinkedList<KThread> waitList;
}
