package com.github.dmtk;

import java.util.List;
import java.util.Scanner;

public class TelnetManager{
	public static String username = "pmambo";
	public static String password = "4292";
	public static boolean autoSequence = true;
	public static int workerNum = 3;
	public static List<whistleWorker> workerList;
	public static void main(String [] args)
	{
		System.out.println("Enter order number:");
		Scanner scanner = new Scanner(System.in);
		String orderNum = scanner.nextLine();
		SequenceGetter getter = new SequenceGetter(username,password);
		for(int i = 0; i < workerNum;i++)
		{
			whistleWorker worker = new whistleWorker(orderNum,username,password,getter,autoSequence);
			workerList.add(worker);
			Thread initThread = new Thread(() -> {
				try {
	                worker.initialize();
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
			});
	
	        initThread.start();
		}
		while(true)
		{
			System.out.println("Enter product code");
			String prodNum = scanner.nextLine();
			System.out.println("Enter quantity");
			String quantity = scanner.nextLine();
			String sequenceInput = "1";
			if(!autoSequence)
			{
				System.out.println("Enter sequence");
				sequenceInput = scanner.nextLine();
			}
			for(whistleWorker worker: workerList)
			{
				if(worker.isReady())
				{
					worker.setLabelData(prodNum, quantity, sequenceInput);
					Thread workerThread = new Thread(worker);
			        workerThread.start();
			        break;
				}
			}
		}
	}
}