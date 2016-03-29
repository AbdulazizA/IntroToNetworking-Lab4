package lab4;

import java.io.IOException;

import abstractServer.AbstractServer;
import abstractServer.ConnectionToClient;
import lab4.ChatIF;

/**
 * This class overrides some of the methods in the abstract superclass in order
 * to give more functionality to the server.
 *
 * @author Karen SRocha
 */
public class Server extends AbstractServer {

	/**
	 * The interface type variable. It allows the implementation of the display
	 * method in the server.
	 */
	ChatIF serverUI;
	private boolean hdb3;

	/**
	 * Constructs an instance of the echo server.
	 *
	 * @param port
	 *            The port number to connect on.
	 * @param serverUI
	 *            The interface type variable.
	 */
	public Server(int port, ChatIF serverUI) {
		super(port);
		this.serverUI = serverUI;
		this.hdb3 = false;
	}

	/**
	 * This method handles any messages received from the client.
	 *
	 * @param msg
	 *            The message received from the client.
	 * @param client
	 *            The connection from which the message originated.
	 */
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {

		String message = (String) msg;

		if (message.startsWith("login")){
			if (client.getInfo("LoginID") == null) {
				String loginID = message.split(" ")[1];
				client.setInfo("LoginID", loginID);
				sendToAllClients(loginID + " has logged on.");
				System.out.println(loginID + " has logged on.");
			} else {
				try {
					client.sendToClient("Error: You cannot set another login.");
					client.close();
				} catch (IOException e) {
				}
			}
		}
		else if (message.equals("hdb3")) {
			try {
				client.sendToClient("Mode HDB3 ON");
				client.sendToClient("Please send an encoded message to be decoded:");
				hdb3 = true;
			} catch (IOException e) {
			}
			
		} else if (message.equals("hdb3off")){
			hdb3 = false;
			try {
				client.sendToClient("Mode HDB3 OFF");
			} catch (IOException e) {
			}
		} else if (message.equals("logoff")) {
			sendToAllClients(client.getInfo("LoginID") + " has disconnected.");
			System.out
					.println(client.getInfo("LoginID") + " has disconnected.");
			
		} else if (hdb3) {
			try {
				String decoded = decodeHDB3((String) msg);
				client.sendToClient("Message has been decoded to: " + decoded);
			} catch (IOException e) {
			}
			
		} else {
			System.out.println("Message received from "
					+ client.getInfo("LoginID") + ": " + msg);
		}
	}

	/**
	 * This method handles all data coming from the UI
	 *
	 * @param message
	 *            The message from the UI.
	 */
	public void handleMessageFromServerUI(String message) {

		if (message.startsWith("#")) {

			String[] messages = message.split(" ");

			String message1 = messages[0];

			switch (message1.toLowerCase()) {
			case "#quit":
				try {
					close();
				} catch (IOException e) {
					serverUI.display("Could not close. Try again later.");
				}
				break;

			case "#stop":
				stopListening();
				break;

			case "#close":
				stopListening();
				for (Thread client : getClientConnections()) {
					try {
						((ConnectionToClient) client).close();
					} catch (IOException e) {
					}
				}
				break;

			case "#setport":
				if (messages.length > 1) {
					if (!isListening() && getNumberOfClients() == 0) {
						String message2 = messages[1];
						try {
							setPort(Integer.parseInt(message2));
						} catch (NumberFormatException e) {
							serverUI.display("Error: enter a valid port number.");
						}
						serverUI.display("Port set to: " + getPort());
					} else {
						serverUI.display("Error: please close the server to set port.");
					}
				} else {
					serverUI.display("Error: enter a port number after command (#setport ####).");
				}
				break;

			case "#start":
				if (!isListening()) {
					try {
						listen();
					} catch (IOException e) {
					}
				} else {
					serverUI.display("Error: please stop the server to start.");
				}
				break;

			case "#getport":
				serverUI.display("" + getPort());
				break;

			default:
				serverUI.display("This command is invalid, try another command.");
				break;
			}

		} else {
			String serverMsg = "SERVER MSG> " + message;
			sendToAllClients(serverMsg);
			serverUI.display(serverMsg);
		}
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * starts listening for connections.
	 */
	protected void serverStarted() {
		System.out.println("Server listening for connections on port "
				+ getPort());
	}

	/**
	 * This method overrides the one in the superclass. Called when the server
	 * stops listening for connections.
	 */
	protected void serverStopped() {
		sendToAllClients("WARNING - The server has stopped listening for connections");
		System.out.println("Server has stopped listening for connections.");
	}

	/**
	 * This method is called each time a new client connection is accepted.
	 * 
	 * @param client
	 *            the connection connected to the client.
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		System.out.println("A client has connected.");

	}

	/**
	 * This method is called each time a client is disconnection from the
	 * server.
	 *
	 * @param client
	 *            the connection with the client.
	 */
	@Override
	synchronized protected void clientDisconnected(ConnectionToClient client) {
		System.out.println(client.getInfo("LoginID") + " has disconnected.");
	}

	/**
	 * This method decodes the HDB3 message.
	 * 
	 * @param msg
	 *            The message to be decoded.
	 * @return The decoded message.
	 */
	private static String decodeHDB3(String msg) {
		String msgWithoutB = "";
		int indexOfB = msg.indexOf("B");
		indexOfB--;

		while (indexOfB > -1) {

			if (indexOfB > 0)
				msgWithoutB += msg.substring(0, indexOfB);

			msgWithoutB += "0000";

			msg = msg.substring(indexOfB + 6);
			indexOfB = msg.indexOf("B");
			indexOfB--;
		}
		msgWithoutB += msg;

		String decodedToAMI = "";

		int indexOfV = msgWithoutB.indexOf("V");
		indexOfV -= 4;

		while (indexOfV > -1) {

			if (indexOfV > 0)
				decodedToAMI += msgWithoutB.substring(0, indexOfV);

			decodedToAMI += "0000";

			msgWithoutB = msgWithoutB.substring(indexOfV + 5);
			indexOfV = msgWithoutB.indexOf("V");
			indexOfV -= 4;
		}
		decodedToAMI += msgWithoutB;
		String decodedMsg = decodeAMI(decodedToAMI);
		return decodedMsg;
	}

	/**
	 * This method decodes the AMI message.
	 * 
	 * @param msg
	 *            The message to be decoded.
	 * @return The decoded message.
	 */
	private static String decodeAMI(String message) {
		String decodedMsg = "";

		for (char c : message.toCharArray()) {
			if (c != '+' && c != '-') {
				decodedMsg += c;
			}
		}
		return decodedMsg;
	}

}