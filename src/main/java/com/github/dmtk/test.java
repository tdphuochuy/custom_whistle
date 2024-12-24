package com.github.dmtk;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class test{
	public static int count = 0;
	public static void main(String [] args) throws InterruptedException, IOException
	{
	   System.out.println("Enter order number:");
	   Scanner scanner = new Scanner(System.in);
	   String orderNum = scanner.nextLine();
       Telnet telnet = new Telnet("167.110.212.137", 23, System.out);
       Thread myThready = new Thread(new Runnable() {
           @Override
           public void run() {

               try {
                   telnet.execute();
               } catch (Exception ex) {
            	   ex.printStackTrace();
               }
           }
       });
       myThready.start();
       initialize(telnet);
       Thread.sleep(1000);
       outer:while(true)
       {
    	   if(checkCondition(telnet,"Order # [[0;7m") && !checkCondition(telnet,"Prod [[0;7m"))
    	   {
    		   telnet.sendCommand(orderNum + "\n");
    	   }
    	   System.out.println("Waiting for production to be ready...");
           //waitResponse(telnet,"Prod [[0;7m");
           try {
           while(true)
           {
   				String response = telnet.getResponse();
   				if(response.contains("Prod [[0;7m"))
   				{
   					break;
   				} else if (response.contains("ReportProd"))
   				{
   					System.out.println("OOPS!! returning to production...");
   					telnet.sendCommand("1");
   					Thread.sleep(300);
   					continue outer;
   				}
   	    	   Thread.sleep(300);
           }
           
	       System.out.println("Enter product code");
	       String prodNum = scanner.nextLine();
	       System.out.println("Enter quantity");
	       String quantity = scanner.nextLine();
	       System.out.println("Enter sequence");
	       String sequenceInput = scanner.nextLine();
	       if(prodNum.equals("skip") || quantity.equals("skip") || sequenceInput.equals("skip")) {
	    	   continue;
	       }
	       int sequenceInteger = 1000 + Integer.parseInt(sequenceInput);
	       String sequence = String.valueOf(sequenceInteger);
	       
	                                                                                      
	       System.out.println("Filling product number");
	       telnet.sendCommand(prodNum + "\n");
	       waitResponse(telnet,"Prod [" + prodNum);
	       System.out.println("Setting quantity");
	       setQuantity(telnet,quantity);
	       Thread.sleep(300);
	       setDate(telnet);
	       //Thread.sleep(300);
	       waitResponse(telnet,"Hour");
	       setHour(telnet);
	       waitResponse(telnet,"Sequence");
	       setSequence(telnet,sequence);
	       if(prodNum.equals("22486"))
	       {
	    	   String copiesNum = "4";
	    	   setCopiesQuantity(telnet,copiesNum);
	    	   Thread.sleep(300);
	       }
	       System.out.println("Bulding label");
	       buildLabel(telnet);
           } catch (Exception e)
           {
        	   e.printStackTrace();
           }
	    }
	}
	
	public static void buildLabel(Telnet telnet) throws InterruptedException, IOException
	{
		while(!checkCondition(telnet,"([0;7m  OK"))
		{
			if(checkCondition(telnet,"([0;7m  Yes"))
			{
				telnet.sendCommand("\n");
			} else {
				telnet.sendCommand(getArrowKey("up"));
			}
			Thread.sleep(300);
		}
		Thread.sleep(300);
		telnet.sendCommand("\n");
		Thread.sleep(300);
		System.out.println("Checking ready");
		String buildResponse = checkBuildResponse(telnet);
		if(buildResponse.equals("ready"))
		{
			System.out.println("Ready!!!!!!!!!");
			telnet.sendCommand("\n");
			setKillDate(telnet);
			while(!checkCondition(telnet,"Order # [[0;7m"))
   			{
				System.out.println("Looking for new order");
   		    	telnet.sendCommand("\n");
   		    	Thread.sleep(300);
   			}
			System.out.println("Done!!!");
			count = count + 1;
		    System.out.println("Successful builds: " + count);  
		} else if (buildResponse.equals("Backflush")){
			reset(telnet);
		} else {
			System.out.println(buildResponse);
		}
	}
	
	public static void setKillDate(Telnet telnet) throws InterruptedException, IOException
	{
		System.out.println("Setting kill date");
		waitResponse(telnet,"Kill Date");
		if(checkCondition(telnet,"Enter kill date"))
		{
			telnet.sendCommand(getDate("MMdd"));
			Thread.sleep(300);
			//telnet.sendCommand("");
		} else {
			Thread.sleep(200);
			while(checkCondition(telnet,"[[0;7m     "))
			{
				telnet.sendCommand(getDate("yyyy-MM-dd"));
				Thread.sleep(200);
			}
			System.out.println("Kill date set!!!!");
			waitResponse(telnet,"Kill Date [[0;7m" + getDate("yyyy-MM-dd"));
		}
	}
	
	public static void setCopiesQuantity(Telnet telnet,String copiesNum) throws IOException, InterruptedException
	{
		System.out.println("Setting copies quantity");
		while(!checkCondition(telnet,"Copies [[0;7m"))
		{
			System.out.println("Looking for copies quantity input");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
		}
	    telnet.sendCommand(copiesNum + "\n");
	}
	
	public static String checkBuildResponse(Telnet telnet) throws InterruptedException
	{
		while(true)
		{
			System.out.println("Checking build response...");
			String response = telnet.getResponse();
			if(response.contains("Ready to build"))
			{
				return "ready";
			} else if(response.contains("Backflush"))
			{
				return "Backflush";
			} else if (response.contains("([0;7m  OK"))
			{
				telnet.sendCommand("\n");
			}
			Thread.sleep(300);
		}
	}
	
	
	public static void setSequence(Telnet telnet,String sequence) throws InterruptedException, IOException
	{
		System.out.println("Setting sequence");
		telnet.sendCommand(sequence);
	    Thread.sleep(300);
	    while(!checkCondition(telnet,"([0;7mOkay"))
		{
	    	System.out.println("Confirming sequence");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
		}
		telnet.sendCommand("\n");
		Thread.sleep(300);
		System.out.println("Finalizing sequence");
		if(checkCondition(telnet,"Sequence ["))
		{
			//issue
			setSequence(telnet,sequence);
		}
	}
	
	public static void setHour(Telnet telnet) throws InterruptedException
	{
		System.out.println("Setting hour");
	    telnet.sendCommand(getHour() + "\n");
	    Thread.sleep(300);
	    telnet.sendCommand("\n");
	}
	
	public static String getHour()
	{
		LocalTime currentTime = LocalTime.now();

        // Get the current hour in 24-hour format
        int currentHour = currentTime.getHour();

        // Adjust the hour by adding 24
        if(currentHour < 3)
        {        	
        	currentHour += 24;
        }
        
        return String.valueOf(currentHour);

	}
	
	public static void setDate(Telnet telnet) throws InterruptedException, IOException
	{
		System.out.println("Setting prod date");
		while(!checkCondition(telnet,"Prod Date [[0;7m"))
		{
			System.out.println("Looking for prod date");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
			if(checkCondition(telnet,"Full pallet is"))
		    {
		    	System.out.println("CONFIRMING PALLET QUANTITY...");
		    	 while(!checkCondition(telnet,"([0;7m  Yes"))
		 		{
		 		    telnet.sendCommand(getArrowKey("up"));
		 			Thread.sleep(300);
		 		}
			    telnet.sendCommand("\n");
		    } else if (checkCondition(telnet,"([0;7m  Yes"))
		    {
			    telnet.sendCommand("\n");
	 			Thread.sleep(500);
		    }
		}
		Thread.sleep(300);
	    telnet.sendCommand(getDate("MMdd") + "\n");
	    //telnet.sendCommand("\n");
	}
	
	public static String getDate(String dateFormat)
	{
		LocalDate today;
		LocalTime currentTime = LocalTime.now();
        int currentHour = currentTime.getHour();
		if(currentHour < 3)
		{
			today = LocalDate.now().minusDays(1);
		} else {
			today= LocalDate.now();
		}
		
        // Define the formatter for MMDD
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

        // Format the date
        String formattedDate = today.format(formatter);
        
        return formattedDate;
	}
	
	public static void setQuantity(Telnet telnet,String quantity) throws InterruptedException, IOException
	{
		System.out.println("Setting quantity");
		while(true)
		{
			if(checkCondition(telnet,"Quantity [[0;7m") && !checkCondition(telnet,"Track #"))
			{
				break;
			}
			System.out.println("Looking for quantity input");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
		}
		System.out.println("Setting quantity input of " + quantity);
	    telnet.sendCommand(quantity + "\n");
	    Thread.sleep(300);
	    if(checkCondition(telnet,"Full pallet is"))
	    {
	    	System.out.println("CONFIRMING PALLET QUANTITY...");
	    	 while(!checkCondition(telnet,"([0;7m  Yes"))
	 		{
	 		    telnet.sendCommand(getArrowKey("up"));
	 			Thread.sleep(300);
	 		}
		    telnet.sendCommand("\n");
	    }
	}
	
	public static void reset(Telnet telnet) throws IOException, InterruptedException
	{
		while(!checkCondition(telnet,"Order # [[0;7m"))
		{
			while(!checkCondition(telnet,"ReportProd"))
			{
			       telnet.sendCommand(getArrowKey("esc"));
			       if(checkCondition(telnet,"Inventory"))
			       {
					    telnet.sendCommand("1");
			       }
			       Thread.sleep(500);
			}
		    telnet.sendCommand("1");
		    Thread.sleep(300);
		}
	}
	
	public static void initialize(Telnet telnet) throws InterruptedException
	{
	       Thread.sleep(1000);
	       telnet.sendCommand("pdgwinterm7\n");
	       Thread.sleep(300);
	       telnet.sendCommand("Lucky7isthenumber!\n");
	       Thread.sleep(300);
	       telnet.sendCommand("poultry\n");
	       waitResponse(telnet,"Logon");
	       Thread.sleep(1000);
	       telnet.sendCommand("pmambo\n");
	       Thread.sleep(300);
	       telnet.sendCommand("4292\n");
	       Thread.sleep(300);
	       telnet.sendCommand("\n");
	       waitResponse(telnet,"Split Pallet");
	        Thread.sleep(300);
	       telnet.sendCommand("2");
	       Thread.sleep(300);
	       telnet.sendCommand("1");
	       waitResponse(telnet,"Order #");

	}
	
	public static boolean checkCondition(Telnet telnet,String condition)
	{
		String response = telnet.getResponse();
		if(response.contains(condition))
		{
			return true;
		}		
		return false;
	}
	
	public static void waitResponse(Telnet telnet,String condition) throws InterruptedException
	{
		outer:while(true)
        {
				String response = telnet.getResponse();
				if(response.contains(condition))
				{
					break outer;
				}
	    	   Thread.sleep(300);
        }
	}
	
	public static String getArrowKey(String arrowKey) throws IOException {
	    String arrowCommand;
	    switch (arrowKey.toLowerCase()) {
	        case "up":
	            arrowCommand = "\u001B[A";   // Up Arrow
	            break;
	        case "down":
	            arrowCommand = "\u001B[B"; // Down Arrow
	            break;
	        case "right":
	            arrowCommand = "\u001B[C"; // Right Arrow
	            break;
	        case "left":
	            arrowCommand = "\u001B[D";  // Left Arrow
	            break;
	        case "esc":
	            arrowCommand = "\u001B";  // Left Arrow
	            break;
	        default:
	            throw new IllegalArgumentException("Invalid arrow key: " + arrowKey);
	    }

	    return arrowCommand;
	}

}