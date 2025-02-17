package com.github.dmtk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

public class Main{
	public static String username = "pmambo";
	public static String password = "4292";
	public static boolean autoSequence = true;
	public static void main(String [] args) throws InterruptedException
	{
		System.out.println("Enter order number:");
		Scanner scanner = new Scanner(System.in);
		String orderNum = scanner.nextLine();
		TelnetManager manager = new TelnetManager(orderNum, username, password, autoSequence);
		while(true)
		{
			System.out.println("Enter product code");
			String prodNum = scanner.nextLine();
			System.out.println("Enter quantity");
			String quantity = scanner.nextLine();
			String sequence = "1";
			if(!autoSequence)
			{
				System.out.println("Enter sequence");
				sequence = scanner.nextLine();
			}
			manager.addCommand(new Command(prodNum,quantity,sequence));
		}
	}
}