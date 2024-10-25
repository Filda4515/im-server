package utb.fai;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SocketHandler {
	/** mySocket je socket, o který se bude tento SocketHandler starat */
	Socket mySocket;

	/** client ID je řetězec ve formátu <IP_adresa>:<port> */
	String clientID;

	String name = null;

	/**
	 * activeHandlers je reference na množinu všech právě běžících SocketHandlerů.
	 * Potřebujeme si ji udržovat, abychom mohli zprávu od tohoto klienta
	 * poslat všem ostatním!
	 */
	ActiveHandlers activeHandlers;

	/**
	 * messages je fronta příchozích zpráv, kterou musí mít každý klient svoji
	 * vlastní - pokud bude přetížená nebo nefunkční klientova síť,
	 * čekají zprávy na doručení právě ve frontě messages
	 */
	ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<String>(20);

	/**
	 * startSignal je synchronizační závora, která zařizuje, aby oba tasky
	 * OutputHandler.run() a InputHandler.run() začaly ve stejný okamžik.
	 */
	CountDownLatch startSignal = new CountDownLatch(2);

	/** outputHandler.run() se bude starat o OutputStream mého socketu */
	OutputHandler outputHandler = new OutputHandler();
	/** inputHandler.run() se bude starat o InputStream mého socketu */
	InputHandler inputHandler = new InputHandler();
	/**
	 * protože v outputHandleru nedovedu detekovat uzavření socketu, pomůže mi
	 * inputFinished
	 */
	volatile boolean inputFinished = false;

	public SocketHandler(Socket mySocket, ActiveHandlers activeHandlers) {
		this.mySocket = mySocket;
		clientID = mySocket.getInetAddress().toString() + ":" + mySocket.getPort();
		this.activeHandlers = activeHandlers;
	}

	class OutputHandler implements Runnable {
		public void run() {
			OutputStreamWriter writer;
			try {
				System.err.println("DBG>Output handler starting for " + clientID);
				startSignal.countDown();
				startSignal.await();
				System.err.println("DBG>Output handler running for " + clientID);
				writer = new OutputStreamWriter(mySocket.getOutputStream(), "UTF-8");
				writer.write("\nYou are connected from " + clientID + "\n");
				writer.flush();
				writer.write("Enter your name:\n");
				writer.flush();
				while (!inputFinished) {
					String m = messages.take();// blokující čtení - pokud není ve frontě zpráv nic, uspi se!
					writer.write(m + "\r\n"); // pokud nějaké zprávy od ostatních máme,
					writer.flush(); // pošleme je našemu klientovi
					System.err.println("DBG>Message sent to " + clientID + ":" + m + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.err.println("DBG>Output handler for " + clientID + " has finished.");

		}
	}

	class InputHandler implements Runnable {
		public void run() {
			try {
				System.err.println("DBG>Input handler starting for " + clientID);
				startSignal.countDown();
				startSignal.await();
				System.err.println("DBG>Input handler running for " + clientID);
				String message = "";
				/**
				 * v okamžiku, kdy nás Thread pool spustí, přidáme se do množiny
				 * všech aktivních handlerů, aby chodily zprávy od ostatních i nám
				 */
				activeHandlers.add(SocketHandler.this);
				BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream(), "UTF-8"));
				while ((message = reader.readLine()) != null) {
					// name setting
					if (name == null) {
						message = message.trim();
						if (message.contains(" ")) {
							message = "[Error] >> Name cannot contain spaces.";
						} else if (activeHandlers.isNameTaken(message)) {
							message = "[Error] >> This name is already taken.";
						} else {
							name = message;
							message = "[Info] >> Your name was set to: " + name;
						}
						System.out.println(message);
						activeHandlers.sendMessageToSelf(SocketHandler.this, message);
						continue;
					}
					// #setMyName <name>
					if (message.startsWith("#setMyName")) {
						String[] args = message.trim().split(" ", 2);
						if (args.length < 2) {
							message = "[Error] >> Syntax error: #setMyName <name>";
							System.out.println(message);
							activeHandlers.sendMessageToSelf(SocketHandler.this, message);
						} else if (args[1].contains(" ")) {
							message = "[Error] >> Name cannot contain spaces.";
				 		} else if (args[1].equals(name)) {
							message = "[Error] >> You are already using this name.";
						} else if (activeHandlers.isNameTaken(args[1])) {
							message = "[Error] >> This name is already taken.";
						} else {
							name = args[1];
							message = "[Info] >> Your name was set to: " + name;
						}
						System.out.println(message);
						activeHandlers.sendMessageToSelf(SocketHandler.this, message);
						continue;
					}
					// #sendPrivate <name> <message>
					if (message.startsWith("#sendPrivate")) {
						String[] args = message.trim().split(" ", 3);
						if (args.length < 3) {
							message = "[Error] >> Syntax error: #sendPrivate <name> <message>";
							System.out.println(message);
							activeHandlers.sendMessageToSelf(SocketHandler.this, message);
						} else {
							message = "[" + name + "] >> " + args[2];
							System.out.println(message);
							if (!activeHandlers.sendMessageToName(message, args[1])) {
								message = "[Error] >> Client with name '" + args[1] + "' doesn't exist.";
								System.out.println(message);
								activeHandlers.sendMessageToSelf(SocketHandler.this, message);
							}
						}
						continue;
					}
					message = "[" + name + "] >> " + message;
					System.out.println(message);
					activeHandlers.sendMessageToAll(SocketHandler.this, message);
				}
				inputFinished = true;
				messages.offer("OutputHandler, wakeup and die!");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				// remove yourself from the set of activeHandlers
				synchronized (activeHandlers) {
					activeHandlers.remove(SocketHandler.this);
				}
			}
			System.err.println("DBG>Input handler for " + clientID + " has finished.");
		}

	}
}
