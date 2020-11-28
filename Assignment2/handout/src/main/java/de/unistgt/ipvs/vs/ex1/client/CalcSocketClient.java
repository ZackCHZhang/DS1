package de.unistgt.ipvs.vs.ex1.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * Implement the connectTo-, disconnect-, and calculate-method of this class as
 * necessary to complete the assignment. You may also add some fields or
 * methods.
 */
public class CalcSocketClient {
	private Socket cliSocket;
	private int rcvdOKs; // --> Number of valid message contents
	private int rcvdErs; // --> Number of invalid message contents
	private int calcRes; // --> Calculation result (cf. 'RES')

	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	public CalcSocketClient() {
		this.cliSocket = null;
		this.rcvdOKs = 0;
		this.rcvdErs = 0;
		this.calcRes = 0;
	}

	public int getRcvdOKs() {
		return rcvdOKs;
	}

	public int getRcvdErs() {
		return rcvdErs;
	}

	public int getCalcRes() {
		return calcRes;
	}

	public boolean connectTo(String srvIP, int srvPort) {

		// Solution here
		try {
			System.out.println("Clinet is connecting");
			this.cliSocket = new Socket(srvIP, srvPort);

			oos = new ObjectOutputStream(this.cliSocket.getOutputStream());
			ois = new ObjectInputStream(this.cliSocket.getInputStream());

		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		String resMsg = null;
		try {
			resMsg = (String) ois.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (resMsg.equalsIgnoreCase("<08:RDY>")) {
			System.out.println("Already Connect");
		}

		return true;
	}

	public boolean disconnect() {

		// Solution here
		try {
			this.ois.close();
			this.oos.close();
			this.cliSocket.close();
			System.out.println("Connect closed");
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

		return true;
	}

	public boolean calculate(String request) {

		if (cliSocket == null) {
			System.err.println("Client not connected!");
			return false;
		}

		// Solution here


		String resMsg = null;
		this.sendRequest(request);

		while(true) {
			try {
				resMsg=(String) ois.readObject();
				System.out.println(resMsg);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			if(this.checkResponse(resMsg)) {
				StringTokenizer st=new StringTokenizer(resMsg.substring(4,resMsg.length()-1));
				if(st.hasMoreElements()) {
					String s=st.nextToken();
					
					if(s.equalsIgnoreCase("FIN")) {
						System.out.println("Calculate finished");
						break;
					}else if(s.equalsIgnoreCase("ERR")) {
						this.rcvdErs++;

						System.err.print("oops there is a error "+resMsg+"\n");
					}else if(s.equalsIgnoreCase("OK")) {
						this.rcvdOKs++;
						if(st.hasMoreTokens()) {
							String secondS=st.nextToken();
							if(secondS.equalsIgnoreCase("RES")) {
								String res=st.nextToken();
								this.calcRes=Integer.valueOf(res).intValue();
								System.out.println("we got a result");
								break;
							}
						}
					}else {
						System.err.println("ERROR: "+resMsg);
					}
				}
			}

		}
		return true;
	}
	
	/*
	 * the function is used for send a message to server. If there is no respose <07:OK>. This mean the server is not recived the messge
	 * Then wait 0.5s resend the request
	 * @param: req, The message taht user want to sent
	 * @return: null
	 */
	
	private void sendRequest(String req) {
		String resMsg = null;
		while (true) {
			//send request
			try {
				oos.writeObject(req);
				
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			System.out.println("Send request");
			try {
				resMsg=(String) ois.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//System.err.println(resMsg);
			if(resMsg.equalsIgnoreCase("<07:OK>")) {
				this.rcvdOKs++;
				System.out.println("Server recived");
				break;
			}else {
				System.out.println("No response try to send request one more");
			}
			try {
				Thread.currentThread().sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	/*
	 * This function is used to check the response from the server valid or not
	 * @param: res, the response from server
	 * @return: true,the message is valid
	 */
	private boolean checkResponse(String res) {
		String[] temp;
		temp = res.split(":");
		int msgSize = Integer.valueOf(temp[0].substring(1)).intValue();
		//System.out.println(res.length());
		if (msgSize != res.length()) {
			System.err.println("client:"+res.length());
			return false;
		}
		return true;
		
	}
	
}
