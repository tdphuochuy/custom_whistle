package com.github.dmtk;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

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
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		// Start WebSocket Client in a separate thread with auto-reconnect
        executor.submit(() -> {
            while (true) {
                try {
                    WebSocketClient client = new WebSocketClient(new URI("wss://projectmbymoneymine.com:8082")) {
                        @Override
                        public void onOpen(ServerHandshake handshakedata) {
                            
                        }

                        @Override
                        public void onMessage(String message) {
                            
                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {
                            System.out.println("WebSocket connection closed: " + reason);
                        }

                        @Override
                        public void onError(Exception ex) {
                            System.err.println("WebSocket Error: " + ex.getMessage());
                        }
                    };
                    client.connectBlocking(); // Block until connected
                    while (client.isOpen()) {
                        Thread.sleep(5000); // Keep the connection alive
                    }
                } catch (URISyntaxException | InterruptedException e) {
                    System.err.println("WebSocket client error: " + e.getMessage());
                }
                System.out.println("Reconnecting WebSocket in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        });
		
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
			if(prodNum.length() == 0 || quantity.length() == 0) {
		    	   System.out.println("Missing required info, skipping...");
		    	   continue;
		    }
			manager.addCommand(new Command(prodNum,quantity,sequence));
		}
	}
}