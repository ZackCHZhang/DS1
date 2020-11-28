package de.unistgt.ipvs.vs.ex1.server;
import de.unistgt.ipvs.vs.ex1.server.CalculationSession;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * Extend the run-method of this class as necessary to complete the assignment.
 * You may also add some fields, methods, or further classes.
 */
public class CalcSocketServer extends Thread {
	private ServerSocket srvSocket;
	private int port;
	
	public CalcSocketServer(int port) {
		this.srvSocket = null;
		this.port = port;
	}
	
	@Override
	public void interrupt() {
		try {
			if (srvSocket != null) srvSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
           
		if (port <= 0) {
			System.err.println("Wrong number of arguments.\nUsage: SocketServer <listenPort>\n");
			System.exit(-1);
		}

		// Start listening server socket ..
		Socket mySocket=null;
		int count=0;
		try {
			this.srvSocket=new ServerSocket(this.port);
			while(true) {
				mySocket=this.srvSocket.accept();
				count++;
				System.out.println("got a new conncet at "+new Date()+".now have"+count+"Scocket. now the Socket is:"+mySocket.hashCode());
				Runnable calcSession=new CalculationSession(mySocket);
				new Thread(calcSession).start();
			}
			
			
		}catch(IOException e){
			e.getStackTrace();
			
		}
	}
        
        public void waitUnitlRunnig(){
            while(this.srvSocket == null){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                }
            }
        }
}