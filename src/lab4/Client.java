package lab4;

import abstractClient.*;

import java.io.*;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 *
 * @author Karen SRocha
 */
public class Client extends AbstractClient {

	/**
	 * The interface type variable. It allows the implementation of the display
	 * method in the client.
	 */
	ChatIF clientUI;

	/**
	 * The login ID used by the client to be identified by other clients and the
	 * server.
	 */
	private String loginID;
	private boolean hdb3;

	/**
	 * Constructs an instance of the chat client.
	 *
	 * @param host
	 *            The server to connect to.
	 * @param port
	 *            The port number to connect on.
	 * @param clientUI
	 *            The interface type variable.
	 * @param loginID
	 *            The login ID of the client.
	 * @throws IOException
	 */
	public Client(String loginID, String host, int port, ChatIF clientUI) {
		super(host, port); // Call the superclass constructor
		this.loginID = loginID;
		this.clientUI = clientUI;
		this.hdb3 = false;
		try {
			openConnection();
			sendToServer("login " + loginID);
		} catch (IOException e) {
			clientUI.display("Cannot open connection. Awaiting command.");
		}
	}

	/**
	 * Returns the login ID of the client.
	 * 
	 * @return The login ID of the client.
	 */
	public String getLoginID() {
		return loginID;
	}

	/**
	 * Sets the login ID of the client.
	 */
	public void setLoginID(String loginID) {
		this.loginID = loginID;
	}

	/**
	 * This method handles all data that comes in from the server.
	 *
	 * @param msg
	 *            The message from the server.
	 */
	public void handleMessageFromServer(Object msg) {
		clientUI.display(msg.toString());
	}

	/**
	 * This method handles all data coming from the UI
	 *
	 * @param message
	 *            The message from the UI.
	 */
	public void handleMessageFromClientUI(String message) {

		if (message.equals("hdb3")){
			hdb3 = true;
			try {
				sendToServer(message);
			} catch (IOException e) {
				clientUI.display("Could not send message to server. Terminating client.");
				quit();
			}
		} else if (hdb3) {
			if (testHDB3string(message)) {
				try {
					System.out.println("HDB3 is encoding message: " + message);
					String encoded = encodeHDB3(message);
					System.out.println("Sending encoded message: " + encoded);
					sendToServer(encoded);
				} catch (IOException e) {
				}
			} else {
				try {
					System.out.println("This is not a valid binary message");
					sendToServer("hdb3off");
					sendToServer(message);
					hdb3 = false;
				} catch (IOException e) {
				}
			}
		} else if (message.equals("quit")){
			quit();
		} else {
			try {
				sendToServer(message);
			} catch (IOException e) {
				clientUI.display("Could not send message to server. Terminating client.");
				quit();
			}
		}
	}

	/**
	 * This method terminates the client.
	 */
	public void quit() {
		if (isConnected()) {
			try {
				sendToServer("logoff");
				closeConnection();
			} catch (IOException e1) {
			}
		}
		clientUI.display("Terminating client.");
		System.exit(0);
	}

	/**
	 * This method displays a message if the server has shutdown and terminates
	 * the client.
	 */
	@Override
	protected void connectionException(Exception exception) {
		clientUI.display("Server has shutdown. Abnormal termination of connection.");
	}

	/**
	 * This method displays a message if the connection is closed.
	 */
	protected void connectionClosed() {
		clientUI.display("You have been logged off.");
	}

	/**
	 * This method tests if a string is an HDB3 string.
	 * 
	 * @param message
	 *            The message to be tested.
	 * @return True if it is and false if it is not.
	 */
	private boolean testHDB3string(String message) {
		boolean result = true;
		String[] charac = message.split("");
		for (String s : charac){
			if (!s.equals("0") && !s.equals("1") && !s.equals("+") && !s.equals("-")){
				result = false;
			}
		}
		return result;
	}

	/**
	 * This method encodes the message to HDB3.
	 * 
	 * @param msg
	 *            The message to be encoded.
	 * @return The encoded HDB3 message.
	 */
	private static String encodeHDB3(String msg) {
		String encodedMsg = ""; 
		String AMImsg = encodeAMI(msg);

		int indexOf4 = AMImsg.indexOf("0000");
		String precedingPulse = "+"; 
		int previousNumberOfPulses = 0;

		while (indexOf4 != -1) {
			
			String[] temp = AMImsg.split("");
			for (int i = 0; i < indexOf4; i++){
				if (temp[i].equals("+") || temp[i].equals("-")){
					precedingPulse = temp[i];
					previousNumberOfPulses++;
				}
			}
			if (indexOf4 != 0) encodedMsg += AMImsg.substring(0, indexOf4);
			
			if (precedingPulse.equals("-")) {
				if (previousNumberOfPulses%2 == 1){
					encodedMsg += "000-V";
				} else {
					encodedMsg += "+B00+V";
				}
			} else {
				if (previousNumberOfPulses%2 == 1){
					encodedMsg += "000+V";
				} else {
					encodedMsg += "-B00-V";
				}
			}
			AMImsg = AMImsg.substring(indexOf4+4);
			indexOf4 = AMImsg.indexOf("0000");
			previousNumberOfPulses = 0;
		}
		
		encodedMsg += AMImsg;
		return encodedMsg;
	}
	
	/**
	 * This method encodes the message to AMI.
	 * 
	 * @param msg
	 *            The message to be encoded.
	 * @return The encoded AMI message.
	 */
	private static String encodeAMI(String message){
		String encodedMsg = "";
		
		String pulse = "+";
		int indexOf1 = message.indexOf("1");
		
		while (indexOf1 != -1){
			encodedMsg += message.substring(0, indexOf1) + pulse + "1";
			if (pulse.equals("+")) pulse = "-";
			else pulse = "+";
			message = message.substring(indexOf1 + 1);
			indexOf1 = message.indexOf("1");
		}
		encodedMsg += message;
		return encodedMsg;
	}
}