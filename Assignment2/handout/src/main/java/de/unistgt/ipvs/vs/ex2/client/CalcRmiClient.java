package de.unistgt.ipvs.vs.ex2.client;

import de.unistgt.ipvs.vs.ex2.common.ICalculation;
import de.unistgt.ipvs.vs.ex2.common.ICalculationFactory;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;


/**
 * Implement the getCalcRes-, init-, and calculate-method of this class as
 * necessary to complete the assignment. You may also add some fields or methods.
 */
public class CalcRmiClient {
	private ICalculation calc = null;

	public CalcRmiClient() {
		this.calc = null;
	}

	public int getCalcRes() {
		int res=Integer.MAX_VALUE;
		
		try {
			res=calc.getResult();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}

	public boolean init(String url) {
		try {
//			for(String s: Naming.list(url)) {
//				System.out.println(s+"\n");
//			}
			
//			System.out.println(Naming.list(url));
			Registry registry = LocateRegistry.getRegistry("localhost",Registry.REGISTRY_PORT);
			ICalculationFactory factory=(ICalculationFactory) Naming.lookup(url);
			this.calc=factory.getSession();
//			this.calc=(ICalculation) Naming.lookup(url);
			System.out.println("creat session");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}

	public boolean calculate(CalculationMode calcMode, Collection<Integer> numbers) {
		for (Integer number : numbers) {
			// Perform appropriate operation according to calcMode
			switch (calcMode) {
			case ADD:
				try {
					calc.add(number);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
				System.out.println("Added number: " + number);
				break;
			case SUB:
				try {
					calc.subtract(number);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Subtracted number: " + number);
				break;
			case MUL:
				try {
					calc.multiply(number);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Multiplied number: " + number);
				break;
			}
		}
		
		return true;
	}
}
