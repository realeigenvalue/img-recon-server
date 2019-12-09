//Java JDBC Tutorial https://www.tutorialspoint.com/jdbc/jdbc-sample-code.htm
package com.realeigenvalue.img_recon_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import aws.rekognition.image.ImageComparator;

// TODO: Auto-generated Javadoc
/**
 * The Class Server.
 */
public class Server {
	
	/** The Constant TITLE. */
	private static final String TITLE = "img-recon-server";
	
	/** The Constant DEFAULT_IP. */
	private static final String DEFAULT_IP = "127.0.0.1";
	
	/** The Constant DEFAULT_PORT. */
	private static final int DEFAULT_PORT = 1024;
	
	/** The Constant COLLECTION_PATH. */
	private static final String COLLECTION_PATH = "C:/Users/" + System.getProperty("user.name") + "/imgRecon_collection/";
	
	/** The ip address. */
	private String ipAddress;
	
	/** The port number. */
	private int portNumber;
	
	/** The server socket. */
	private ServerSocket serverSocket;
	
	/** The active connections. */
	private AtomicInteger activeConnections;
	
	/**
	 * The Class JDBC.
	 */
	public class JDBC {
		
		/** The Constant JDBC_DRIVER. */
		public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
		
		/** The Constant DB_URL. */
		public static final String DB_URL = "jdbc:mysql://localhost:3306/database";
		
		/** The Constant USER. */
		public static final String USER = "root";
		
		/** The Constant PASSWORD. */
		public static final String PASSWORD = "password";
	}
	
	/**
	 * Instantiates a new server.
	 */
	public Server() {
		this.ipAddress = Server.DEFAULT_IP;
		this.portNumber = Server.DEFAULT_PORT;
		serverSocket = null;
		activeConnections = new AtomicInteger();
		setupLogFile();
	}
	
	/**
	 * Instantiates a new server.
	 *
	 * @param ipAddress the ip address
	 * @param portNumber the port number
	 */
	public Server(String ipAddress, int portNumber) {
		this.ipAddress = ipAddress;
		this.portNumber = portNumber;
		serverSocket = null;
		activeConnections = new AtomicInteger();
		setupLogFile();
	}
	
	/**
	 * Gets the active connections.
	 *
	 * @return the active connections
	 */
	public synchronized int getActiveConnections() {
		return activeConnections.get();
	}
	
	/**
	 * Setup log file.
	 */
	private void setupLogFile() {
		File file = new File("log.txt");
		FileOutputStream fos = null;
		PrintStream ps = null;
		try {
			fos = new FileOutputStream(file);
			ps = new PrintStream(fos);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		}
		System.setErr(ps);
	}
	
	/**
	 * Start.
	 */
	public void start() {
		System.out.println("Starting " + Server.TITLE + " at " + ipAddress + " on port " + portNumber);
		try {
			serverSocket = new ServerSocket(portNumber, 50, InetAddress.getByName(ipAddress));
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		while(true) {
			try {
				Socket clientSocket = serverSocket.accept();
				new RequestThread(clientSocket).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
	}
	
	/**
	 * The Class RequestThread.
	 */
	private class RequestThread extends Thread {
		
		/** The socket. */
		private Socket socket;
		
		/** The output. */
		private DataOutputStream output;
		
		/** The input. */
		private DataInputStream input;
				
		/**
		 * Instantiates a new request thread.
		 *
		 * @param socket the socket
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public RequestThread(Socket socket) throws IOException {
			activeConnections.incrementAndGet();
			this.socket = socket;
			output = new DataOutputStream (socket.getOutputStream());
			input = new DataInputStream(socket.getInputStream());
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				String clientResponse = receiveMessageFromClient();
				if(clientResponse == null) {
					activeConnections.decrementAndGet();
					return;
				}
				System.out.println("New Request! from " + socket.getInetAddress().getHostName() + " " + clientResponse);
				String[] parseResponse = clientResponse.split("\\|");
				String command = parseResponse[0];
				if(command.equals("ADD_TO_DATABASE")) {
					byte[] imageBytes = receiveBytesFromClient();
					File tempImage = createTempFile(parseResponse[2], imageBytes);
					ADD_TO_DATABASE(parseResponse[1], tempImage.getAbsolutePath());
					if(tempImage != null) {
						tempImage.delete();
					}
				} else if(command.equals("ADD_TO_SAMPLES")) {
					byte[] imageBytes = receiveBytesFromClient();
					File tempImage = createTempFile(parseResponse[2], imageBytes);
					ADD_TO_SAMPLES(parseResponse[1], tempImage.getAbsolutePath());
					if(tempImage != null) {
						tempImage.delete();
					}				
				} else if(command.equals("REMOVE_FROM_DATABASE")) {
					REMOVE_FROM_DATABASE(parseResponse[1]);
				} else if(command.equals("RECOGNIZE")) {
					byte[] imageBytes = receiveBytesFromClient();
					File tempImage = createTempFile(parseResponse[1], imageBytes);
					RECOGNIZE(tempImage.getAbsolutePath());
					if(tempImage != null) {
						tempImage.delete();
					}				
				} else {
					System.out.println("Invalid Command!");
				}
				output.close();
				input.close();
				socket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
			}
			activeConnections.decrementAndGet();
		}
		
		/**
		 * Adds the to database.
		 *
		 * @param name the name
		 * @param imageURL the image URL
		 */
		public void ADD_TO_DATABASE(String name, String imageURL) {
			Connection conn = null;
			Statement stmt = null;
			try {
				Class.forName(JDBC.JDBC_DRIVER);
				conn = DriverManager.getConnection(JDBC.DB_URL, JDBC.USER, JDBC.PASSWORD);
				stmt = conn.createStatement();
				name = name.toLowerCase();
				String folder_location = Server.COLLECTION_PATH + name; 
				String sql = "INSERT INTO data(name, folder_location) " +
							 "VALUES (\'" + name + "\', \'" + folder_location + "\')";
				stmt.executeUpdate(sql);
			} catch(Exception e) {
				// TODO Auto-generated catch block
			} finally {
				// TODO Auto-generated catch block
			    try { stmt.close(); } catch (Exception e) {}
			    try { conn.close(); } catch (Exception e) {}
			}
			File newFolder = new File(toWindowsPathFormat(Server.COLLECTION_PATH + name));
			newFolder.mkdirs();
			File imageFile = new File(imageURL);
			copyFile(imageURL, newFolder.getAbsolutePath() + "\\" + imageFile.getName());
			sendMessageToClient("DONE|TRUE");
		}
		
		/**
		 * Adds the to samples.
		 *
		 * @param name the name
		 * @param imageURL the image URL
		 */
		public void ADD_TO_SAMPLES(String name, String imageURL) {
			String response = "DONE|FALSE";
			name = name.toLowerCase();
			if(existsName(name)) {
				File folder = new File(toWindowsPathFormat(Server.COLLECTION_PATH + name));
				folder.mkdirs();
				File imageFile = new File(imageURL);
				copyFile(imageURL, folder.getAbsolutePath() + "\\" + imageFile.getName());
				response = "DONE|TRUE";
			}
			sendMessageToClient(response);
		}
		
		/**
		 * Removes the from database.
		 *
		 * @param name the name
		 */
		public void REMOVE_FROM_DATABASE(String name) {
			String response = "DONE|FALSE";
			if(existsName(name)) {
				Connection conn = null;
				Statement stmt = null;
				try {
					Class.forName(JDBC.JDBC_DRIVER);
					conn = DriverManager.getConnection(JDBC.DB_URL, JDBC.USER, JDBC.PASSWORD);
					stmt = conn.createStatement();
					name = name.toLowerCase();
					String sql = "DELETE from data " +
								 "WHERE name=\'" + name + "\'";
					stmt.executeUpdate(sql);
				} catch(Exception e) {
					// TODO Auto-generated catch block
				} finally {
					// TODO Auto-generated catch block
				    try { stmt.close(); } catch (Exception e) {}
				    try { conn.close(); } catch (Exception e) {}
				}
				File directory = new File(toWindowsPathFormat(Server.COLLECTION_PATH + name));
				removeDirectory(directory);
				response = "DONE|TRUE";
			}
			sendMessageToClient(response);
		}
		
		/**
		 * Recognize.
		 *
		 * @param imageURL the image URL
		 */
		public void RECOGNIZE(String imageURL) {
			String response = "DONE|FALSE";
			Connection conn = null;
			Statement stmt = null;
			try {
				Class.forName(JDBC.JDBC_DRIVER);
				conn = DriverManager.getConnection(JDBC.DB_URL, JDBC.USER, JDBC.PASSWORD);
				stmt = conn.createStatement();
				String sql = "SELECT name, folder_location FROM data";
				ResultSet rs = stmt.executeQuery(sql);
				File sourceImage = new File(imageURL);
				ArrayList<String> people = new ArrayList<String>();
				ArrayList<Float> confidenceLevels = new ArrayList<Float>();
				ImageComparator comparator = new ImageComparator();
				comparator.compareStageSource(sourceImage.getAbsolutePath());
				while(rs.next()) {
					String name = rs.getString("name");
					String folder_location = rs.getString("folder_location");
					File imageDirectory = new File(toWindowsPathFormat(folder_location));
					File[] images = imageDirectory.listFiles();
					if(images != null) {
						for(File targetImage : images) {
							comparator.compareStageTarget(targetImage.getAbsolutePath());
							people.add(name);
							confidenceLevels.add(comparator.getConfidenceLevel());
						}
					}
				}
				int bestIndex = findBestIndex(people, confidenceLevels);
				String bestMatch = people.get(bestIndex);
				Float bestConfidence = confidenceLevels.get(bestIndex);
				response = "DONE|TRUE" + "|" + bestMatch + "|" + bestConfidence;
			} catch(Exception e) {
				// TODO Auto-generated catch block
			} finally {
				// TODO Auto-generated catch block
			    try { stmt.close(); } catch (Exception e) {}
			    try { conn.close(); } catch (Exception e) {}
			}
			sendMessageToClient(response);
		}
		
		/**
		 * Send message to client.
		 *
		 * @param message the message
		 */
		public void sendMessageToClient(String message) {
			try {
				output.writeInt(message.length());
				output.writeBytes(message);
				output.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
		
		/**
		 * Receive message from client.
		 *
		 * @return the string
		 */
		public String receiveMessageFromClient() {
			String result = null;
			byte[] bytes = receiveBytesFromClient();
			if(bytes != null) {
				result = new String(bytes);
			}
			return result;
		}
		
		/**
		 * Receive bytes from client.
		 *
		 * @return the byte[]
		 */
		public byte[] receiveBytesFromClient() {
			byte[] bytes = null;
			try {
				int length = input.readInt();
				bytes = new byte[length];
				int bytesRead = 0;
				while(bytesRead < length) {
					bytes[bytesRead++] = input.readByte();
				}
				return bytes;
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
			return bytes;
		}
		
		/**
		 * Creates the temp file.
		 *
		 * @param fullName the full name
		 * @param fileBytes the file bytes
		 * @return the file
		 */
		public File createTempFile(String fullName, byte[] fileBytes) {
			File result = null;
			String[] name_ext = fullName.split("\\.");
			String name = name_ext[0];
			String ext = "." + name_ext[1];
			try {
				File tempFile = File.createTempFile(name, ext);
				Files.write(tempFile.toPath(), fileBytes);
				result = tempFile;
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
			return result;
		}
		
		/**
		 * Find best index.
		 *
		 * @param people the people
		 * @param confidenceLevels the confidence levels
		 * @return the int
		 */
		public int findBestIndex(ArrayList<String> people, ArrayList<Float> confidenceLevels) {
			// TODO Auto-generated method stub
			if(people.size() != confidenceLevels.size()) { return -1; };
			int size = people.size();
			Float maxValue = Float.MIN_VALUE;
			int maxIndex = 0;
			for(int i = 0; i < size; i++) {
				if(confidenceLevels.get(i) > maxValue) {
					maxValue = confidenceLevels.get(i);
					maxIndex = i;
				}
			}
			return maxIndex; 
		}
		
		/**
		 * Copy file.
		 *
		 * @param sourceURL the source URL
		 * @param destinationURL the destination URL
		 */
		public void copyFile(String sourceURL, String destinationURL) {
			File source = new File(sourceURL);
			File destination = new File(destinationURL);
			InputStream in = null; 
			OutputStream out = null;
			try {
				in = new FileInputStream(source);
				out = new FileOutputStream(destination);
				byte[] buffer = new byte[1024];
				int bytesRead;
				while((bytesRead = in.read(buffer)) > 0) {
					out.write(buffer, 0, bytesRead);
				}
				in.close();
				out.close();
			} catch(Exception e) {
				// TODO Auto-generated catch block
			}
		}
		
		/**
		 * Removes the directory.
		 *
		 * @param directory the directory
		 * @return true, if successful
		 */
		public boolean removeDirectory(File directory) {
			File[] files = directory.listFiles();
			if(files != null) {
				for(File file : files) {
					removeDirectory(file);
				}
			}
			return directory.delete();
		}
		
		/**
		 * To windows path format.
		 *
		 * @param path the path
		 * @return the string
		 */
		public String toWindowsPathFormat(String path) {
			return path.replace('/', '\\');
		}
	
		
		/**
		 * Exists name.
		 *
		 * @param name the name
		 * @return true, if successful
		 */
		public boolean existsName(String name) {
			boolean result = false;
			Connection conn = null;
			Statement stmt = null;
			try {
				Class.forName(JDBC.JDBC_DRIVER);
				conn = DriverManager.getConnection(JDBC.DB_URL, JDBC.USER, JDBC.PASSWORD);
				stmt = conn.createStatement();
				String sql = "SELECT name, folder_location FROM data";
				ResultSet rs = stmt.executeQuery(sql);
				name = name.toLowerCase();
				while(rs.next()) {
					if(name.equals(rs.getString("name"))) {
						result = true;
						break;
					}
				}
			} catch(Exception e) {
				// TODO Auto-generated catch block
			} finally {
				// TODO Auto-generated catch block
			    try { stmt.close(); } catch (Exception e) {}
			    try { conn.close(); } catch (Exception e) {}
			}
			return result;
		}
	}
}
