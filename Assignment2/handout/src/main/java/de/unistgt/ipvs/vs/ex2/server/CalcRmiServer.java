package de.unistgt.ipvs.vs.ex2.server;

import de.unistgt.ipvs.vs.ex2.common.ICalculation;
import de.unistgt.ipvs.vs.ex2.common.ICalculationFactory;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement the run-method of this class to complete
 * the assignment. You may also add some fields or methods.
 */
public class CalcRmiServer extends Thread {
	private String regHost;
	private String objName;
        String url; //Please use this variable to bind the object.
	
	public CalcRmiServer(String regHost, String objName) {
		this.regHost = regHost;
		this.objName = objName;
        this.url = "rmi://" + regHost + "/" + objName;
        try {
			Registry registry=LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		if (regHost == null || objName == null) {
			System.err.println("<registryHost> or <objectName> not set!");
			return;
		}
                
                //Add solution here
//		CalculationImpl calMethode=null;
		try {
//			System.setProperty("java.rmi.server.codebase", ICalculationFactory.class.getProtectionDomain().getCodeSource().getLocation().toString());
//
//		    System.setProperty("java.security.policy", "/java.policy");
//			System.setProperty("java.rmi.server.hostname",regHost);
//			CalcRmiServer obj=new CalcRmiServer(this.regHost,this.objName);
			
//			System.err.println(LocateRegistry.getRegistry(Registry.REGISTRY_PORT));
			
			ICalculationFactory fac=new CalculationImplFactory(url);
			
			java.rmi.Naming.bind(this.url, fac);
			System.out.println("Methode Naming");
//			System.err.println(LocateRegistry.getRegistry(Registry.REGISTRY_PORT));
//			System.err.println(Naming.list(url));
//			for(String s: Naming.list(url)) {
//				System.err.println(s+"\n");
//			}
			
		}catch(Exception e) {
			System.err.println(e.getMessage());
			e.getMessage();
		}
		
		
	}

        public void stopServer(){
            try {
                Naming.unbind(url);
            } catch (RemoteException ex) {
                Logger.getLogger(CalcRmiServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(CalcRmiServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(CalcRmiServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
}
