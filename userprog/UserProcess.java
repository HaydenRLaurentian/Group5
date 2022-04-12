package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
    	
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
	lock.acquire();//??????????????????????????????????????????????????????????
	
	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

//PHASE 2---------------------------------------------------------------------------
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallExit:
		handleExit(a0);
		break;
	case syscallExec:
		return handleExec(a0,a1,a2);
	case syscallJoin:
		return handleJoin(a0,a1);
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0,a1,a2);
	case syscallWrite:
		return handleWrite(a0,a1,a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }
    
    
    /**
     * handleOpen
     * 
     * Handle open(int *fileAddress) sys call
     * check for invalid address, attempt to read fileName
     * find available file slot 1-16 if none of these return an error
     * open file.  if there's an error opening the file or the file 
     * is null return error
     * 
     * @param fileAddress the address of the file to open
     * @return an int value either -1 if an error occurred or index value  
     *         representing the file slot # within the fileDescriptorTable
     */
    
    private int handleOpen(int fileAddress){
    	
    	if (fileAddress <= 0){
    		Lib.debug(dbgProcess, "handleOpen: Invalid address");
    		return -1;
    	}
    	
    	String fileName = readVirtualMemoryString(fileAddress, 256);
    	
    	if (fileName == null){
    		Lib.debug(dbgProcess,  "handleOpen: Illegal Filename");
    		return -1;
    	}
    	
    	//check for available file slot 1-16
    	
    	int emptyIndex = -1;
    	
    	for(int i=2; i < 16 || emptyIndex > 0; i++){  
    		if(fileDescriptorTable[i] == null){
    			emptyIndex = i;
    		}
    	}
    	if (emptyIndex == -1){
    		Lib.debug(dbgProcess, "handleOpen: No available file slot");
    		return -1;
    	}
    	
    	
    	//open file: if error show at which index
    	try{
    		OpenFile fileToOpen = ThreadedKernel.fileSystem.open(fileName, false);
    		//or use:Filesystem fs = Machine.stubFileSystem(); 
    		//       OpenFile file = fs.open(fileName, createOnOpen); ????????????????????????

    		if (fileToOpen == null){
    			Lib.debug(dbgProcess, "handleOpen: could not open file at index: " + emptyIndex);
    			return - 1;
    		}
    		else{
    			fileDescriptorTable[emptyIndex] = fileToOpen;
    			return emptyIndex; 
    		}
    		
    	}catch (Exception ex){
    		Lib.debug(dbgProcess, "handleOpen: index: " + emptyIndex + " exception: " + ex);
    		return -1;
    	}
    }
    
    
    /**
     * handleCreate
     * 
     * handle create() sys call
     * creates the file virtAddr. if the file exists open it
     * @param virtAddr the filename of the file to be created
     * @return int value of fileDescriptorTable index if file 
     * 		   was created, -1 if there's an error
     */
    
    private int handleCreate(int virtAddr){
    	
    	//check name validity
    	if(virtAddr < 0){
    		Lib.debug(dbgProcess, "handleCreate: invalid virtual address");
    		return -1;    		
    	}
    	
    	String fileName = readVirtualMemoryString(virtAddr, 256);
    	
    	if (fileName == null){
    		Lib.debug(dbgProcess, "handleCreate: Illegal Filename");
    		return -1;
    	}
    	
    	//check for available file slot 1-16
    	
    	int emptyIndex = -1;
    	
    	for(int i=2; i < 16 || emptyIndex > 0; i++){   
    		if(fileDescriptorTable[i] == null){
    			emptyIndex = i;
    		}
    	}
    	if (emptyIndex == -1){
    		Lib.debug(dbgProcess, "handleCreate: No available file slot");
    		return -1;
    	}  	

    	//create file: if cannot create file, show error
    	try{
    		OpenFile fileToCreate = ThreadedKernel.fileSystem.open(fileName, true);
    		//or use:Filesystem fs = Machine.stubFileSystem(); 
    		//OpenFile file = fs.open(fileName, true); ????????????????????????

    		if (fileToCreate == null){
    			Lib.debug(dbgProcess, "handleCreate: could not create file");
    			return - 1;
    		}
    		else{
    			fileDescriptorTable[emptyIndex] = fileToCreate;
    			return emptyIndex; 
    		}
    	}catch (Exception ex){
    		Lib.debug(dbgProcess, "handleCreate: index: " + emptyIndex + " exception: " + ex);
    		return -1;
    	}    	
    }
    
/**
 *  handleClose
 *  
 *  get the file index from fileDescriptorTable
 *  close the file & set fileDescriptorTable[index] to null
 *  @param index the index of the file to be removed from fileDescriptorTable
 *  @return status 0 if close was successful, -1 otherwise
 */    
    private int handleClose(int index){
    	
    	int status = 0;
    	
    	//ensure index is greater than 0 and less than 16
    	//ensure fileDescriptorTable[index] isnt already null
    try{
    	if (index < 0 || index > 16 || fileDescriptorTable[index] == null){
    		Lib.debug(dbgProcess, "handleClose: index out of bounds or already null");
    		return -1;
    	}
    	else{
    		fileDescriptorTable[index].close();
    		fileDescriptorTable[index] = null;
    		return status;
    	}
    }catch (Exception ex){
    	Lib.debug(dbgProcess, "handleClose: could not close file: " + ex);
    	return -1;
    }
    }
    
    /**
     *  handleUnlink
     *  
     *  get fileName at address sent as parameter
     *  if fileName is null return error else remove from ThreadedKernel.filesystem
     *  @param nameAddr virtual memory address
     *  @return int value 0 if remove was success, -1 otherwise  
     */
    
    private int handleUnlink(int nameAddr){
    	
    	String fileName = readVirtualMemoryString(nameAddr, 256);
    	
    	if (fileName == null){
    		Lib.debug(dbgProcess, "handleUnlink: no file to unlink");
    		return -1;
    	}else{
    		boolean succeeded = ThreadedKernel.fileSystem.remove(fileName);
    		
    		if (!succeeded){
    			return -1;
    		}
    		else {
    			return 0;
    		}
    	}
    }
    
    /**
     *  handleRead
     *  
     *  check fileDescriptor value, create array to save data to, check if 
     *  inputFile at fileDescript index is null.  if Not null read file
     *  
     *  @param fileDescript is the file descriptor from which to read
     *  @param memAddr memory address to store data
     *  @param numBytes number of bytes to write
     *  @return -1 if error, fileDescript+data+offset+bytesRead to writeVirtualMemory
     */
    private int handleRead(int fileDescript, int memAddr, int numBytes){
    	  
    	if (fileDescript < 0 || fileDescript > 16){
    		Lib.debug(dbgProcess, "handleRead: fileDescriptor out of bounds");
    		return -1; 
    	}
    try{
    	byte[] data = new byte[numBytes];
    	
    	OpenFile inputFile = fileDescriptorTable[fileDescript];
    	
    	if (inputFile == null){
    		return -1;
    	}
    	int bytesRead = inputFile.read(data, 0, data.length);
    	
    	if (bytesRead <= 0){
    		return -1;
    	}
    	//read data from memory 
    	int bytesWritten = writeVirtualMemory(fileDescript, data, 0, bytesRead);
    	return bytesWritten; 
    	
    }catch (Exception ex){
    	Lib.debug(dbgProcess, "handleRead: could not read file: " + ex);
    	return -1;
    }
    }
    
    /**
     * handleWrite
     * 
     *  writes data bytes in numBytes located at memAddr
     *  
     *  @param fileDescript is the file descriptor from which to read
     *  @param memAddr memory address to store data
     *  @param numBytes number of bytes to write
     *  @return -1 if error, number of bytes written otherwise 
     */
    private int handleWrite(int fileDescript, int memAddr, int numBytes){
    	
    	if (fileDescript < 0 || fileDescript > 16){
    		Lib.debug(dbgProcess, "handleWrite: fileDescriptor out of bounds");
    		return -1; 
    	}
    try {
    	
    	byte[] data = new byte[numBytes];
    	
    	int readBytes = readVirtualMemory(memAddr, data, 0, numBytes);
    	
    	if (readBytes != numBytes){
    		Lib.debug(dbgProcess, "handleWrite: error reading bytes");
    		return -1; 
    	}
    	
    	OpenFile outputFile = fileDescriptorTable[fileDescript];
    	
    	if (outputFile == null){
    		Lib.debug(dbgProcess, "handleWrite: no file at fileDescriptorTable index");
    		return -1;
    	}
    	
    	//write data to memory 
    	int writtenBytes = outputFile.write(data, 0, data.length);
    	
    	if (writtenBytes <= data.length){
    		Lib.debug(dbgProcess, "handleWrite: bytes written < total data bytes");
    		return -1;
    	}
    	return writtenBytes;
    	
    }catch (Exception ex){
    	Lib.debug(dbgProcess, "handleWrite: could not write file: " + ex);
    	return -1;
    }
    }
    
    /**
     *  handleExec
     *  
     *  Execute program from file with args in child process with unique process ID
     *  checks virtual address for file and checks file for the .coff extension
     *  get string file name and checks for null value, read contents of file increases
     *  address variable to new address, creates child process and adds it to childProcessList
     *  
     *  @param fileNameVaddr the virtual address of the .coff file with executable instr
     *  @param argNum number of arguments 
     *  @param argOffset the virtual address offset
     *  @return -1 if error, child processID otherwise
     */
    private int handleExec(int fileNameVaddr, int argNum, int argOffset){
    
    	//check if fileNameVaddr is less than 0
    	if (fileNameVaddr < 0){
    		Lib.debug(dbgProcess, "handleExec: invalid address");
    		return -1; 
    	}
    	//Check string name for file
    	String fileName = readVirtualMemoryString(fileNameVaddr, 256);
    	
    	if(fileName == null || !fileName.endsWith(".coff")) {
    		Lib.debug(dbgProcess, "handleExec: error regarding the file name or extension");
    		return -1;
    	}
    	
    	String[] vMemArgs = new String[argNum];
    	String vMemString;
    	int currentVaddr = fileNameVaddr; 
    	
    	for(int i = 0; i < argNum; i++){
    		byte[] data = new byte[4];
    	
    		int readBytes = readVirtualMemory(currentVaddr, data);
    	
    		if (readBytes != data.length){
    			return -1;
    		}
    		int ptrToVaddr = Lib.bytesToInt(data, 0);
    		
    		if (ptrToVaddr == 0){
    			Lib.debug(dbgProcess, "handleExec: error converting bytes to int");
    			return -1;
    		}else{
    			vMemString = readVirtualMemoryString(ptrToVaddr, 256);
    			
    			if (vMemString == null){
    				Lib.debug(dbgProcess, "handleExec: error reading memory string");
    				return -1;
    			}
    		}
    		vMemArgs[i] = vMemString;
    		currentVaddr += 4;
    	}
    		UserProcess child = UserProcess.newUserProcess();
    		if (child.execute(fileName,vMemArgs)){
    			this.childProcessList.add(child);
    			child.parent = this;
    			return child.processID;
    		}else{
    			Lib.debug(dbgProcess, "handleExec: error executing the process");
    			return -1;
    		}
    }
    
    /**
     *  handleJoin
     *  
     *  ensure thread is NOT currentThread, get child process from childProcessList
     *  if child processID from childProcessList is equal to processId sent as arg it 
     *  becomes joinChild and removed from childProcessList then joined and written to 
     *  memory
     *  
     *  @param processId the ID of the process to be joined
     *  @param addr the address of the file to be executed
     *  @return -1 if error, 1 if process is successful 
     *  
     */
    private int handleJoin(int processId, int addr){
    	
    	if (this.processID == processId){
    		Lib.debug(dbgProcess,  "handleJoin: cant join itself");
    		return -1;
    	}
    	
    	UserProcess joinChild = null;
    	int index = -1;
    	boolean gotProcess = false;
    	
    //lock.acquire();
    boolean machineStatus = Machine.interrupt().disable();
    
    	for(int i = 0; i < childProcessList.size()|| gotProcess == true; i++) {
    	
    		if (this.childProcessList.get(i).processID == processId){
    			joinChild = this.childProcessList.get(i);
    			gotProcess = true;
    			index++;
    		}
    	}
    	gotProcess = false;
    	
    	//if childProcessList.get didnt work or didnt get anything
    	if(joinChild == null || index == -1){
    		Lib.debug(dbgProcess,  "handleJoin: error getting process from list ");
    //lock.release();
    Machine.interrupt().restore(machineStatus);
    		return -1;
    	}
    	
    	if( joinChild.processID == this.processID){
    		joinChild.thread.join();
    		Lib.debug(dbgProcess,  "handleJoin: joining thread: " + toString());
    		joinedThreadIdList.add(joinChild.processID);
    	}
    	//remove joinChild from childProcessList
    	this.childProcessList.remove(index);
    	joinChild.parent = null;
    	
    	 //need to check state of process for null (if process is finished)...???????????????????
    	//if(this.status == statusFinished)
    	if (this.thread.schedulingState == null){
    		
    		byte[] data = new byte[4];
    		int writtenBytes = writeVirtualMemory(addr, data);
    		
    		if (writtenBytes != 4){
    			Lib.debug(dbgProcess,  "handleJoin: error writing bytes");
    //lock.release();
    Machine.interrupt().restore(machineStatus);			
    			return -1;    			
    		}else{
    //lock.release();
    			thread.sleep(); //???????????????????????????????????????????????
    Machine.interrupt().restore(machineStatus);			
    			return 1;
    		}    		
    	}else{
    		Lib.debug(dbgProcess,  "handleJoin: error regarding thread state");
    		return -1; 
    	}
    }

    /**
     * handleExit
     * 
     * terminates current process. Clear childProcessList and any
     * parent processes, file descriptors and process ids associated
     * 
     * @param status status of process... why is it never used??????????????????????????
     * 
     */
    private void handleExit(int status){
    	
    	if (parent != null){
    		
    		lock.acquire();
    		
    		for (int i = 0; i < childProcessList.size(); i++){
    			handleClose(i);
    			childProcessList.get(i).parent = null;
    		}
    		childProcessList.clear();
    		
    		lock.release();
    		
    		Lib.debug(dbgProcess,  "handleExit: removed child processes...now terminating");
    		
    		if (this.processID == 0) {
    			Kernel.kernel.terminate();
    		}else{
    			KThread.finish();
    		}  		
    		Lib.debug(dbgProcess,  "handleExit: good bye");
    	}
    }
    
    
    
    
    
    
    
    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
   
//GLOBAL VARIABLES FOR PHASE 2........................
    
    // for handleOpen()
    private OpenFile[] fileDescriptorTable;
    
    //for loadSections( )
    private Lock lock = new Lock();
    
    //for handleExec( )
    private LinkedList<UserProcess> childProcessList;
    private UserProcess parent;
    private int processID;
    
    //for handleJoin( )
    private UThread thread;
    private LinkedList<Integer> joinedThreadIdList;
    
    
   
}
