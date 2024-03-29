/*
 * Copyright 2009 Jonathan Grochowski
 * 
 * This file is part of onsie.
 * 
 * onsie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * onsie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with onsie.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.jgrocho.uno;

import java.io.*;
import java.net.*;

import java.util.List;
import java.util.ArrayList;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Iterator;

class ServerThread extends Thread implements ServerThreadListener {

    private enum Awaiting { NONE, USERNAME };

    private List<ServerThreadListener> listeners;
    private Queue<Message> messages;

    boolean running;
    private boolean connected;
    private boolean clientReady;
    private Socket socket;

    private Awaiting awaiting;
    private int position;
    private String username;

    /*
    private PrintWriter socketOut;
    private BufferedReader socketIn;
    */
    private ObjectOutputStream socketObjOut;
    private ObjectInputStream socketObjIn;

    public ServerThread(Socket socket, int position) {
	super("Server Thread-" + position);

	listeners = new ArrayList<ServerThreadListener>();
	messages = new ConcurrentLinkedQueue<Message>();

	this.socket = socket;
	this.position = position;

	/*
	socketOut = null;
	socketIn = null;
	*/
	socketObjOut = null;
	socketObjIn = null;

	try {
	    /*
	    socketOut = new PrintWriter(socket.getOutputStream());
	    socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    */
	    socketObjOut = new ObjectOutputStream(socket.getOutputStream());
	    socketObjIn = new ObjectInputStream(socket.getInputStream());

	    running = true;
	    connected = true;
	    System.out.println("Connection opened to " +
			       socket.getInetAddress() + 
			       " on port " + socket.getPort());

	} catch (IOException eStream) {
	    System.err.println("Error creating IO streams or setting timeout.");
	    eStream.printStackTrace();
	    try {
		socket.close();
	    } catch (IOException eSocket) {
		System.out.println("Error closing socket.");
		eSocket.printStackTrace();
	    } finally {
		connected = false;
	    }
	}

	addListener(this);
    }

    public void run() {
	while (running) {
	    while (! messages.isEmpty()) {
		Message message = messages.poll();
		if (message.getType() == Message.Type.PROTOCOL) {
		    try {
			socketObjOut.writeObject(message.getProtocol());
			socketObjOut.flush();
		    } catch (IOException e) {
			close();
		    }
		    /*
		    socketOut.println(message.getProtocol());
		    socketOut.flush();
		    if (socketOut.checkError())
			close();
		    */
		} else if (message.getType() == Message.Type.OBJECT) {
		    try {
			socketObjOut.writeObject(message.getObject());
			socketObjOut.flush();
		    } catch (IOException e) {
			close();
		    }
		}

		System.out.println("SEND:: " + 
				   message + " sent to " + 
				   socket.getInetAddress() + " from " + 
				   this.getName());
	    }
	}

	while (! messages.isEmpty()) {
	    Message message = messages.poll();
	    if (message.getType() == Message.Type.PROTOCOL) {
		try {
		    socketObjOut.writeObject(message.getProtocol());
		    socketObjOut.flush();
		} catch (IOException e) {
		    close();
		}
		/*
		  socketOut.println(message.getProtocol());
		  socketOut.flush();
		  if (socketOut.checkError())
		  close();
		*/
	    } else if (message.getType() == Message.Type.OBJECT) {
		try {
		    socketObjOut.writeObject(message.getObject());
		    socketObjOut.flush();
		} catch (IOException e) {
		    close();
		}
	    }

	    System.out.println("SEND:: " + 
			       message + " sent to " + 
			       socket.getInetAddress() + " from " + 
			       this.getName());
	}

	close();
    }

    public void close() {
	try {
	    //socketOut.close();
	    //socketIn.close();
	    socketObjOut.close();
	    socketObjIn.close();
	    socket.close();
	} catch (IOException e) {
	} finally {
	    connected = false;
	}
    }

    public void setup(Game game) {
	Hand hand = game.getHand(position);

	clientReady = true;
	receive(Protocol.Ready);

	receive(Protocol.Username);
	receiveObject();

	send(Protocol.Hand);
	sendObject(hand);

	send(Protocol.PlayOrder);
	sendObject((Integer) position);
    }

    public void send(String protocol) {
	messages.offer(new Message(protocol));
    }

    public void sendObject(Object object) {
	messages.offer(new Message(object));
    }

    public void receive(String protocol) {
	String line = "";

	while (! line.equals(protocol))
	    line = readSocketInput();

	fireMessageReceived(new Message(protocol));
    }

    public void receiveObject() {
	Object object = readSocketObjInput();

	fireMessageReceived(new Message(object));
    }

    private String readSocketInput() {
	String line = null;

	try {
	    //line = socketIn.readLine();
	    line = (String) socketObjIn.readObject();
	} catch (SocketException e) {
	    System.err.println("Socket Error.");
	    e.printStackTrace();
	    close();
	} catch (IOException e) {
	    System.out.println("Error reading input.");
	    e.printStackTrace();
	    close();
	} catch (ClassNotFoundException e) {
	    System.err.println("Cannot find class for STRING");
	    System.err.println("Are you running Java?");
	    e.printStackTrace();
	    connected = false;
	}

	if (line == null)
	    close();

	return line;
    }

    private Object readSocketObjInput() {
	Object object = null;

	try {
	    object = socketObjIn.readObject();
	} catch (SocketException e) {
	    System.err.println("Error reading object from socket");
	    e.printStackTrace();
	    close();
	} catch (IOException e) {
	    System.err.println("Error reading object");
	    e.printStackTrace();
	    close();
	} catch (ClassNotFoundException e) {
	    System.err.println("Cannot find class for object");
	    e.printStackTrace();
	}

	return object;
    }

    public boolean isConnected() {
	return connected;
    }

    public boolean isReady() {
	return clientReady;
    }

    public int getPosition() {
	return position;
    }

    public String getUsername() {
	return username;
    }

    public void addListener(ServerThreadListener listener) {
	listeners.add(listener);
    }

    public void removeListener(ServerThreadListener listener) {
	listeners.remove(listener);
    }

    private void fireMessageReceived(Message message) {
	ReceiveEvent event = new ReceiveEvent(message);
	for (Iterator<ServerThreadListener> i =
		 new ArrayList<ServerThreadListener>(listeners).iterator();
	     i.hasNext();) {
	    ServerThreadListener listener = i.next();
	    listener.messageReceived(event);
	}

	System.out.println("RECEIVE:: " +
			   message + " received from " + 
			   socket.getInetAddress() + " to " +
			   this.getName());

    }

    public void messageReceived(ReceiveEvent event) {
	Message message = event.getMessage();
	if (message.getType() == Message.Type.PROTOCOL) {
	    String protocol = message.getProtocol();
	    if (protocol.equals(Protocol.Username))
		awaiting = Awaiting.USERNAME;
	} else if (message.getType() == Message.Type.OBJECT) {
	    if (awaiting == Awaiting.USERNAME) {
		username = (String) message.getObject();
		awaiting = Awaiting.NONE;
	    }
	}
    }
}
