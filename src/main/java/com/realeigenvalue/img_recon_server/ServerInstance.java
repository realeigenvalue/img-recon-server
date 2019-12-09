package com.realeigenvalue.img_recon_server;

// TODO: Auto-generated Javadoc
/**
 * The Class ServerInstance.
 */
public class ServerInstance implements Runnable {
	
	/** The server. */
	private Server server;
	
	/**
	 * Instantiates a new server instance.
	 */
	public ServerInstance() {
		server = new Server();
	}
	
	/**
	 * Instantiates a new server instance.
	 *
	 * @param ipAddress the ip address
	 * @param portNumber the port number
	 */
	public ServerInstance(String ipAddress, int portNumber) {
		server = new Server(ipAddress, portNumber);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		server.start();
	}
	
	/**
	 * Graceful shutdown.
	 *
	 * @return true, if successful
	 */
	public boolean graceful_shutdown() {
		if(server.getActiveConnections() == 0) {
			System.out.println("Shutting down imgRecon Server Instance ...");
			System.exit(0);
		}
		return false;
	}
	
	/**
	 * Force shutdown.
	 */
	public void force_shutdown() {
		System.out.println("Warning !!! There may be data loss and data corruption !!!");
		System.out.println("Shutting down imgRecon Server Instance ...");
		System.exit(1);
	}
}
