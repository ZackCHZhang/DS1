package de.unistgt.ipvs.vs.ex1.server;

import de.unistgt.ipvs.vs.ex1.common.ICalculation;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringTokenizer;

/**
 * Add fields and methods to this class as necessary to fulfill the assignment.
 */
public class CalculationSession implements Runnable {
	private Socket socket;
	private ObjectInputStream oisIn;
	private ObjectOutputStream oosOut;
	private CalculationImpl calMethode;
	private int operator;

	private enum ValidMessage {
		ADD, SUB, MUL, RES, RDY, OK, ERR, FIN;

		public static boolean contains(String type) {
			for (ValidMessage s : ValidMessage.values()) {
				if (s.name().equals(type)) {
					return true;
				}
			}
			return false;
		}
	}

	public CalculationSession(Socket mySocket) {
		// TODO Auto-generated constructor stub
		this.operator=-1;
		this.socket = mySocket;
		try {
			this.oisIn = new ObjectInputStream(this.socket.getInputStream());
			this.oosOut = new ObjectOutputStream(this.socket.getOutputStream());
			this.calMethode = new CalculationImpl();

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		try {
			this.calMethode=new CalculationImpl();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Send init ok message back
	private void sendReadyInit() {
		String msg = "<08:RDY>";
		try {
			this.oosOut.writeObject(msg);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	// Send Ok back when got message
	private void sendOk() {
		String msg = "<07:OK>";
		try {
			this.oosOut.writeObject(msg);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	// Send FIN back after process meaasge
	private void sendFin() {
		String msg = "<08:Fin>";
		try {
			this.oosOut.writeObject(msg);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * Send ERR and the invalid string back
	 * 
	 * @param invalidContent the invalidContent
	 */
	private void sendErr(String invalidContent) {
		int msgLength = invalidContent.length() + 9;
		String msg = "<" + msgLength + ":ERR " + invalidContent + ">";
		try {
			this.oosOut.writeObject(msg);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * send ok with valid message this mean <xx:OK_Content>
	 * 
	 * @param�� msg the valid message not process
	 * 
	 * @return: null
	 */
	private void ackValidMessage(String validContent) {
		try {
			String s = validContent;
			int msgSize=s.length()+8;
			String backSize=String.format("%02d", msgSize);
			
			String msg = "<" + backSize + ":OK " + s + ">";
			this.oosOut.writeObject(msg);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * send result back like: <xx:OK_RES_Result>
	 * 
	 * @param: res the calculation result
	 * 
	 * @return null
	 */
	private void sendResult(int res) {
		try {
			String resNumber = Integer.toString(res);
			int msgLength = resNumber.length() + 12;
			String msg = "<" + msgLength + ":OK RES " + resNumber + ">";
			this.oosOut.writeObject(msg);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * choose message
	 * 
	 * @param: msg, the msg from the client
	 * 
	 * @return : string that inside the < >
	 */
	private String chooseMessage(String msg) {
		String res;
		int startPos = 0;
		int endPos = 0;
		for (int i = 0; i < msg.length(); i++) {
			if (msg.charAt(i) == '<') {
				startPos = i;
			}
			if (msg.charAt(i) == '>') {
				endPos = i;
			}
		}
		if (startPos >= endPos) {
			this.sendErr(msg);
			return null;
		} else {
			return msg.substring(startPos, endPos);
		}
	}

	/*
	 * check the message is valid or not
	 * 
	 * @param msg:the message from the client.
	 * 
	 * @return true, when the message is valid false send ERR message and return
	 * false
	 */
	private boolean checkMeaasge(String msg) {
		// at first check the size of msg
		String[] temp;
		temp = msg.split(":");
		int msgSize = Integer.valueOf(temp[0].substring(1)).intValue();
		if (msgSize != msg.length()+1) {
			System.err.println("Server:"+msg.length());
			this.sendErr("SIZE");
			return false;
		}
		// Second check the contains
		StringTokenizer st=new StringTokenizer(msg.substring(4, msg.length() ));
//		temp = msg.substring(4, msg.length() ).split(" ");
		while(st.hasMoreTokens()) {
//			s.trim();
			String s=st.nextToken();
			//System.err.println(s);
			if (ValidMessage.contains(s) || Character.isDigit(s.charAt(0)) || s == null||s.charAt(0)=='-') {
				
				return true;
			}else {
				this.sendErr(s);
			}
		}
		
		return true;
	}

	/*
	 * the function process the message, 1. choose meassage, ignore the bytes out of
	 * the"< >". 2. check message, check if this meassgae have invalid symbol. If
	 * its ok send back ackvlaidmessage 3. put every useful message in a FIFO queue
	 * 4. when get a RES, pull out every word and calculate 5. when the queue is
	 * empty send FIN back 6. send RES back to client
	 * 
	 * @param: msg, the massage from client
	 * 
	 * @return: null
	 */
	private void processMessage(String msg) {
		Queue<String> msgQueue = new LinkedList<String>();
		

		String s = this.chooseMessage(msg);
		if (this.checkMeaasge(s)) {
			//this.ackValidMessage(s.substring(4, s.length()));
			StringTokenizer msgSt = new StringTokenizer(s.substring(4, s.length()));
			while (msgSt.hasMoreTokens()) {
				msgQueue.add(msgSt.nextToken());
			}
			for (String iterString : msgQueue) {
				
				
				if (!Character.isDigit(iterString.charAt(0))&&iterString.charAt(0)!='-') {
					if (iterString.equalsIgnoreCase("ADD")) {
						this.operator = 0;
						this.ackValidMessage(iterString);
					} else if (iterString.equalsIgnoreCase("SUB")) {
						this.operator = 1;
						this.ackValidMessage(iterString);
					} else if (iterString.equalsIgnoreCase("MUL")) {
						this.operator = 2;
						this.ackValidMessage(iterString);
					}else if(iterString.equalsIgnoreCase("RES")){
						this.operator=-1;
						try {
							this.sendResult(this.calMethode.getResult());
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						};
						this.sendFin();
					} else {
						this.sendErr(iterString);
						System.err.println("the op is wrong");
					}

				} else {
					
					
					int calNumber = Integer.valueOf(iterString).intValue();
					switch (operator) {
					case 0:
						try {
							this.calMethode.add(calNumber);
							this.ackValidMessage(iterString);
							
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case 1:
						try {
							this.calMethode.subtract(calNumber);
							this.ackValidMessage(iterString);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case 2:
						try {
							this.calMethode.multiply(calNumber);
							this.ackValidMessage(iterString);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					default:
							break;
					}
					//
					try {
						System.err.println(this.calMethode.getResult());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//
				}
			}
			this.sendFin();
		} else {
			this.sendErr("WRONG");
		}
	}

	/*
	 * 
	 * 
	 */

	public void run() {
		this.sendReadyInit();

		while (true) {
			//boolean resFlag = false;
			String msg = null;
			try {
				msg = (String) this.oisIn.readObject();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println(msg);
			this.sendOk();
			if (!msg.equalsIgnoreCase("<08:RES>") ) {
				this.processMessage(msg);
			} else {
				try {
					this.operator=-1;
					this.sendResult(this.calMethode.getResult());
					this.sendFin();
					
					break;
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		this.sendFin();
	}
}