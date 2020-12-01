package de.unistgt.ipvs.vs.ex2.server;

import java.rmi.RemoteException;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

import de.unistgt.ipvs.vs.ex2.common.ICalculation;
import de.unistgt.ipvs.vs.ex2.common.ICalculationFactory;

/**
 * Change this class (implementation/signature/...) as necessary to complete the assignment.
 * You may also add some fields or methods.
 */

 public class CalculationImplFactory extends UnicastRemoteObject implements ICalculationFactory,Serializable {
 	private static final long serialVersionUID = 8409100566761383094L;
	
 	public CalculationImplFactory(String url) throws RemoteException {
		super();
	}
 	
 	
 	public ICalculation getSession() throws RemoteException{

		return new CalculationImpl();
 	}
 
 	
 
 }
