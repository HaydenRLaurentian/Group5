package nachos.threads;

import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.*;

public class ReactWater{
	
		private static Queue<KThread> hWait = new LinkedList<KThread>();
		private static Queue<KThread> oWait = new LinkedList<KThread>();
		
		private int hydrogenCount, oxygenCount;
		private Lock lock = new Lock();

    /** 
     *   Constructor of ReactWater
     **/
    public ReactWater(Lock lock) {
    	
    	hydrogenCount=0;
    	oxygenCount=0;
    	this.lock = lock;

    } // end of ReactWater()

    /** 
     *   When H element comes, if there already exist another H element 
     *   and an O element, then call the method of Makewater(). Or let 
     *   H element wait in line. 
     **/ 
    public void hReady() {
    	hydrogenCount++;
    	hWait.add(KThread.currentThread());
    	Makewater();
    } // end of hReady()
 
    /** 
     *   When O element comes, if there already exist another two H
     *   elements, then call the method of Makewater(). Or let O element
     *   wait in line. 
     **/ 
    public void oReady() {
    	oxygenCount++;
    	oWait.add(KThread.currentThread());
    	Makewater();
    } // end of oReady()
    
    /** 
     *   Print out the message of "water was made!".
     **/
    public void Makewater() {
    	
    	Lib.assertTrue(lock.isHeldByCurrentThread());
    	while(oxygenCount > 0 && hydrogenCount > 1){
    		System.out.println("Water has been made");
    		oxygenCount--;
    		hydrogenCount = hydrogenCount - 2;
    		((LinkedList<KThread>) oWait).removeFirst();
    		((LinkedList<KThread>) hWait).removeFirst();
    		((LinkedList<KThread>) hWait).removeFirst();
    		
    	}
    	
    } // end of Makewater()
    
    public static void selfTest(){
		final Lock testLock = new Lock();
		final ReactWater react = new ReactWater(testLock);
		KThread water1 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 1: 1 Hydrogen Atom");
                react.hReady();    
                System.out.println("Test Case 1: No water was made " + react.hydrogenCount + " hydrogen atoms and " + react.oxygenCount + " oxygen atoms."); 
				System.out.println("Test Case 1: Complete");
				testLock.release();
				
        } } ).setName("Test 1");
		water1.fork();
		
		KThread water2 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 2: 2 Hydrogen Atoms");
                react.hReady();    
                System.out.println("Test Case 2: No water was made " + react.hydrogenCount + " hydrogen atoms and " + react.oxygenCount + " oxygen atoms."); 
				System.out.println("Test Case 2: Complete");
				testLock.release();
				
        } } ).setName("Test 2");
		water2.fork();
		water1.join();
		
		KThread water3 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 3: 3 Hydrogen Atoms");
                react.hReady();    
                System.out.println("Test Case 3: No water was made  " + react.hydrogenCount + " hydrogen atoms and " + react.oxygenCount + " oxygen atoms."); 
				System.out.println("Test Case 3: Complete");
				testLock.release();
				
        } } ).setName("Test 3");
		water3.fork();
		
		
		KThread water4 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 4: 3 Hydrogen Atoms, 1 Oxygen Atom");
                react.oReady();    
                System.out.println("Test Case 4: Water should be made " + react.hydrogenCount + " hydrogen atoms and " + react.oxygenCount + " oxygen atoms left over."); 
				System.out.println("Test Case 4: Complete");
				testLock.release();
				
        } } ).setName("Test 4");
		water4.fork();
		water1.join();
		water2.join();	
		
		
	}

} // end of class ReactWater
