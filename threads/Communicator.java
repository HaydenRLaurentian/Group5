package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock = new Lock();
    	spkReady =  new Condition(lock);
    	lstnReady = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	spk++;

    	while(lstn==0){lstnReady.sleep();}
    	
    	lstn--;
    	msg = word;
    	spkReady.wake();
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {

    	lock.acquire();
    	lstn++;
    	lstnReady.wake();
    	
    	while(spk==0){spkReady.sleep();}
    	
    	spk--;
    	int ret = msg;
    	lock.release();
    	return ret;
    	
    }
    
    public static void selfTest(){
    	System.out.println("Communicator SelfTest Output");
        System.out.println("Test Speaker First #1");
        final Communicator tester = new Communicator();
    	
        new KThread(new Runnable(){
        	public void run(){
        		System.out.println("SPK: "+tester.spk+" LSTN: "+tester.lstn);
        		tester.listen();
        		System.out.println("SPK: "+tester.spk+" LSTN: "+tester.lstn);
        	}
        }).setName("Test Case 1").fork();
       
        tester.speak(0);
    }
    
    private Condition spkReady;
    private Condition lstnReady;
    private Lock lock;
    private int spk=0;
    private int lstn=0;
    private int msg;
    
    
}
