package com.realeigenvalue.img_recon_server;

import java.util.Scanner;

// TODO: Auto-generated Javadoc
/**
 * The Class ServerConsoleApp.
 */
public class ServerConsoleApp {
	
	/**
	 * Instantiates a new server console app.
	 */
	public ServerConsoleApp() {
		ServerInstance s1 = new ServerInstance();
		new Thread(s1).start();
		Scanner reader = new Scanner(System.in);
		while(reader.hasNext()) {
			String command = reader.nextLine();
			if(command.equals("graceful_shutdown")) {
				if(!s1.graceful_shutdown()) {
					System.out.println("imgRecon Server is busy cannot shutdown !!!");
					continue;
				}
			} else if(command.equals("force_shutdown")) {
				s1.force_shutdown();
			} else {
				System.out.println("Invalid Server Command !!!");
			}
		}
	}
}
