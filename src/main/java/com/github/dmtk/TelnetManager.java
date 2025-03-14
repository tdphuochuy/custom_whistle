package com.github.dmtk;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class TelnetManager{
	private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private whistleWorker worker;
    private SequenceGetter sequenceGetter;
	public TelnetManager(String orderNum,String username,String password,boolean autoSequence) throws InterruptedException
	{
		sequenceGetter = new SequenceGetter(username,password);
		worker = new whistleWorker(orderNum, username, password, sequenceGetter,autoSequence);
        executor.submit(this::processCommands);
	}
	
	public void addCommand(Command command) {
        queue.offer(command);
    }
	
	public String getOrderHTML(String orderNum)
	{
		return sequenceGetter.getOrderHTML(orderNum);
	}
	
	 private void processCommands() {
	        while (true) {
	            try {
	                Command command = queue.take();
	                worker.process(command);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                break;
	            }
	        }
	    }
}