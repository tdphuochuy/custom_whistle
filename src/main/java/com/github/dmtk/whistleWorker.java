package com.github.dmtk;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class whistleWorker{
	public int count = 0;
	public boolean autoSequence;
	public String username;
	public String password;
	public String orderNum;
    public String prodNum;
    public String quantity;
    public String sequenceInput;
    private Telnet telnet;
	private boolean backflush;
	private boolean notfound;
    public SequenceGetter sequenceGetter;
    public whistleWorker(String orderNum,String username,String password,SequenceGetter sequenceGetter,boolean autoSequence) throws InterruptedException{
		this.orderNum = orderNum;
		this.username = username;
		this.password = password;
		this.sequenceGetter = sequenceGetter;
		this.autoSequence = autoSequence;
		initialize();
	}
    
    public void initialize() throws InterruptedException
    {
    	//take 10-20 secs to start
    	telnet = new Telnet("167.110.212.137", 23, System.out);
		Thread myThread = new Thread(new Runnable() {
           @Override
           public void run() {
               try {
                   telnet.execute();
               } catch (Exception ex) {
            	   ex.printStackTrace();
               }
           }
       });
		myThread.start();
		initialize(telnet);
		Thread.sleep(1000);
    }
    
	public void process(Command command)
	{
	   setData(command);
	   backflush = false;
	   notfound = false;
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
	   					reset(telnet);
	   					Thread.sleep(300);
	   					continue outer;
	   				}
	   	    	   Thread.sleep(300);
	           }
		       System.out.println("Filling product number");
		       telnet.sendCommand(prodNum + "\n");
		       while(true)
	           {
	   				String response = telnet.getResponse();
	   				if(response.contains("Item [[0;7m"))
	   				{
	   					break;
	   				} else if (response.contains("Product not found on order"))
	   				{
	   					notfound = true;
	   					reset(telnet);
	   					break;
	   				}
	   	    	   Thread.sleep(300);
	           }
		       if(notfound)
		       {
		    	   break;
		       }
		       String[] itemPack = getItemPack(telnet);
		       String itemPackNum = itemPack[0].trim() + itemPack[1].trim();
		       if(autoSequence)
		       {
			       System.out.println("Item: " + itemPack[0]);
			       System.out.println("Pack: " + itemPack[1]);
			       sequenceInput =String.valueOf(sequenceGetter.getSequence(orderNum,itemPack[0].trim(),itemPack[1].trim()));
		       }
		       int sequenceInteger = 1000 + Integer.parseInt(sequenceInput);
		       String sequence = String.valueOf(sequenceInteger);
		       
		       System.out.println("Setting quantity");
		       if(!setQuantity(telnet,quantity))
		       {
		    	   continue;   
		       }
		       Thread.sleep(300);
		       if(!setDate(telnet))
		       {
		    	   continue;   
		       }
		       //Thread.sleep(300);
		       if(!waitResponseCount(telnet,"Hour"))
		       {
		    	   continue;
		       }
		       String hour = setHour(telnet);
		       waitResponse(telnet,"Sequence");
		       if(!setSequence(telnet,sequence))
		       {
		    	   continue;
		       }
		       if(prodNum.equals("22486"))
		       {
		    	   String copiesNum = "4";
		    	   setCopiesQuantity(telnet,copiesNum);
		    	   Thread.sleep(300);
		       }
		       System.out.println("Bulding label");
		       boolean success = buildLabel(telnet);
		       if(success)
		       {
		    	   sequenceGetter.updateSequence(itemPackNum, Integer.valueOf(hour) , Integer.valueOf(sequenceInput));
		    	   System.out.println(sequenceGetter.getSequenceMap());
		    	   break;
		       } else {
	    		   reset(telnet);
		    	   if(backflush)
		    	   {
		    		   break;
		    	   } else {
		    		   continue;
		    	   }
		       }
           } catch (Exception e)
           {
        	   e.printStackTrace();
           }
	    }
	}
	
	public boolean buildLabel(Telnet telnet) throws InterruptedException, IOException
	{
		while(!checkCondition(telnet,"([0;7m  OK"))
		{
			if(checkCondition(telnet,"([0;7m  Yes"))
			{
				telnet.sendCommand("\n");
			} else {
				if(prodNum.equals("22486"))
				{
					telnet.sendCommand(getArrowKey("down"));
				} else {
					telnet.sendCommand(getArrowKey("up"));
				}
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
			if(!setKillDate(telnet))
			{
				return false;
			}
			while(!checkCondition(telnet,"Order # [[0;7m"))
   			{
				if(checkCondition(telnet,"Lot Table"))
				{
					System.out.println("Updating lot table...");
				} else if (checkCondition(telnet,"Entry must appear"))
				{
					telnet.sendCommand("\n");
					telnet.sendCommand(getArrowKey("backspace"));
					Thread.sleep(500);
					setKillDate(telnet);
				}
				System.out.println("Looking for new order");
   		    	telnet.sendCommand("\n");
   		    	Thread.sleep(300);
   			}
			System.out.println("Done!!!");
			count = count + 1;
		    System.out.println("Successful builds: " + count);
		    return true;
		} else if (buildResponse.equals("Backflush")){
			backflush = true;
			return false;
		} else {
			System.out.println(buildResponse);
			return false;
		}
	}
	
	public boolean setKillDate(Telnet telnet) throws InterruptedException, IOException
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
			int count = 0;
			while(true)
			{
				count++;
				System.out.println("Confirming kill date...");
				if(checkCondition(telnet,"Kill Date [[0;7m" + getDate("yyyy-MM-dd")))
				{
					break;
				}
				if(count > 30)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public void setCopiesQuantity(Telnet telnet,String copiesNum) throws IOException, InterruptedException
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
	
	public String checkBuildResponse(Telnet telnet) throws InterruptedException
	{
		int count = 0;
		while(true)
		{
			count++;
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
			if(count > 30)
			{
				return "timeout";
			}
		}
	}
	
	
	public boolean setSequence(Telnet telnet,String sequence) throws InterruptedException, IOException
	{
		System.out.println("Setting sequence");
		if(prodNum.equals("12623"))
		{
			telnet.sendCommand("0");
		} else {
			telnet.sendCommand(sequence);
		}
	    Thread.sleep(300);
	    int count = 0;
	    while(!checkCondition(telnet,"([0;7mOkay"))
		{
	    	System.out.println("Confirming sequence");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
			count++;
			if(count > 30)
			{
				reset(telnet);
				return false;
			}
		}
		telnet.sendCommand("\n");
		Thread.sleep(300);
		System.out.println("Finalizing sequence");
		if(checkCondition(telnet,"Sequence ["))
		{
			//issue
			return setSequence(telnet,sequence);
		}
		return true;
	}
	
	public String setHour(Telnet telnet) throws InterruptedException
	{
		String hour = getHour();
		System.out.println("Setting hour");
		if(prodNum.equals("12623"))
		{
			telnet.sendCommand("98\n");
		} else {
			telnet.sendCommand(hour + "\n");
		}
	    Thread.sleep(300);
	    telnet.sendCommand("\n");
	    return hour;
	}
	
	public String getHour()
	{
		LocalTime currentTime = LocalTime.now();

        // Get the current hour in 24-hour format
        int currentHour = currentTime.getHour();

        // Adjust the hour by adding 24
        if(currentHour < 4)
        {        	
        	currentHour += 24;
        }
        
        return String.valueOf(currentHour);

	}
	
	
	public String[] getItemPack(Telnet telnet) throws InterruptedException
	{
		String[] array = new String[2];
		String response = telnet.getResponse();
		int itemIndex = response.indexOf("Item [[0;7m") + 12;
		array[0] = response.substring(itemIndex,itemIndex + 8);
		if(response.contains("Pack ["))
		{
			int packIndex = response.indexOf("Pack [") + 6;
			array[1] = response.substring(packIndex,packIndex + 6);
		} else {
			array[1] = "";
		}
		return array;
	}
	
	public boolean setDate(Telnet telnet) throws InterruptedException, IOException
	{
		System.out.println("Setting prod date");
		int count = 0;
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
			count++;
			if(count > 30)
			{
				reset(telnet);
				return false;
			}
		}
		Thread.sleep(300);
	    telnet.sendCommand(getDate("MMdd") + "\n");
	    //telnet.sendCommand("\n");
	    return true;
	}
	
	public String getDate(String dateFormat)
	{
		LocalDate today;
		LocalTime currentTime = LocalTime.now();
        int currentHour = currentTime.getHour();
		if(currentHour < 5)
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
	
	public boolean setQuantity(Telnet telnet,String quantity) throws InterruptedException, IOException
	{
		System.out.println("Setting quantity");
		int count = 0;
		while(true)
		{
			if(checkCondition(telnet,"Quantity [[0;7m") && !checkCondition(telnet,"Track #"))
			{
				break;
			}
			System.out.println("Looking for quantity input");
		    telnet.sendCommand(getArrowKey("up"));
			Thread.sleep(300);
			count++;
			if(count > 30)
			{
				reset(telnet);
				return false;
			}
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
	    return true;
	}
	
	public void reset(Telnet telnet) throws IOException, InterruptedException
	{
		while(!checkCondition(telnet,"Order # [[0;7m"))
		{
			while(!checkCondition(telnet,"ReportProd"))
			{
			       telnet.sendCommand(getArrowKey("esc"));
			       Thread.sleep(700);
			       if(checkCondition(telnet,"Inventory"))
			       {
					    telnet.sendCommand("1");
			       } else if (checkCondition(telnet,"Do you really wish to log out"))
			       {
				       telnet.sendCommand(getArrowKey("esc"));
				       Thread.sleep(700);
					    telnet.sendCommand("2");
			       } else if (checkCondition(telnet,"Yes"))
			       {
				       telnet.sendCommand(getArrowKey("esc"));
				       Thread.sleep(700);
					    telnet.sendCommand("2");
			       }
			       Thread.sleep(700);
			}
		    telnet.sendCommand("1");
		    Thread.sleep(700);
		}
	}
	
	public void initialize(Telnet telnet) throws InterruptedException
	{
		   System.out.println("Loggining in (1)");
	       Thread.sleep(1000);
	       telnet.sendCommand("pdgwinterm7\n");
	       Thread.sleep(300);
	       telnet.sendCommand("Lucky7isthenumber!\n");
	       Thread.sleep(300);
	       telnet.sendCommand("poultry\n");
	       waitResponse(telnet,"Logon");
	       Thread.sleep(1000);
		   System.out.println("Loggining in (2): " + username);
	       telnet.sendCommand(username + "\n");
	       Thread.sleep(300);
	       telnet.sendCommand(password + "\n");
	       Thread.sleep(300);
	       telnet.sendCommand("\n");
	       waitResponse(telnet,"Split Pallet");
	        Thread.sleep(300);
	       telnet.sendCommand("2");
	       Thread.sleep(300);
	       telnet.sendCommand("1");
	       waitResponse(telnet,"Order #");
	       System.out.println("Worker ready!");
	}
	
	public boolean checkCondition(Telnet telnet,String condition)
	{
		String response = telnet.getResponse();
		if(response.contains(condition))
		{
			return true;
		}		
		return false;
	}
	
	public void waitResponse(Telnet telnet,String condition) throws InterruptedException
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
	
	public boolean waitResponseCount(Telnet telnet,String condition) throws InterruptedException, IOException
	{
		int count = 0;
		while(true)
        {
				count++;
				String response = telnet.getResponse();
				if(response.contains(condition))
				{
					return true;
				}
	    	   Thread.sleep(300);
	    	   if(count > 30)
	    	   {		
					reset(telnet);
					return false;
	    	   }
        }
	}
	
	public String getArrowKey(String arrowKey) throws IOException {
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
	            arrowCommand = "\u001B";  // Esc key
	            break;
	        case "backspace": //backspace
	        	arrowCommand = "\u0008";
	        	break;
	        default:
	            throw new IllegalArgumentException("Invalid arrow key: " + arrowKey);
	    }

	    return arrowCommand;
	}
	
	public void setData(Command command)
	{
		this.prodNum = command.getProdNum();
		this.quantity = command.getQuantity();
		this.sequenceInput = command.getSequence();
	}

}