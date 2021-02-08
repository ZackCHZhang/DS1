package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

//you are not allowed to change this class structure
public class Predicate {

	static public boolean predicate0(Message process_i_Message, Message process_j_Message) {
		System.out.println("p0 cal is "+Math.abs(3 * process_i_Message.getLocalVariable() - 4 * process_j_Message.getLocalVariable()) );
		if (Math.abs(3 * process_i_Message.getLocalVariable() - 4 * process_j_Message.getLocalVariable()) == 25)
			return true;
		else
			return false;
	}

	static public boolean predicate1(Message process_i_Message, Message process_j_Message) {
		// TODO
		/*
		 * Add you code here to implement predicate2 x1 - x2= 15
		 */
	 	return process_i_Message.getLocalVariable()-process_j_Message.getLocalVariable()==15;
	}

	static public boolean predicate2(Message process_i_Message, Message process_j_Message) {
		// TODO
		/*
		 * Add you code here to implement predicate2 x1 + x2= 30
		 */

		return process_i_Message.getLocalVariable()-process_j_Message.getLocalVariable()==30;
	}

	static public boolean predicate3(Message process_i_Message, Message process_j_Message) {
		// TODO
		/*
		 * Add you code here to implement predicate1 x1- x3=8
		 */

		return process_i_Message.getLocalVariable()-process_j_Message.getLocalVariable()==8;
	}

}
