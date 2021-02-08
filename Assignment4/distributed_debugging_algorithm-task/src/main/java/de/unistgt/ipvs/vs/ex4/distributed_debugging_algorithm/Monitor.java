package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//you are not allowed to change this class structure. However, you can add local functions!
public class Monitor implements Runnable {

	/**
	 * The state consists on vector timestamp and local variables of each process.
	 * In this class, a state is represented by messages (events) indices of each
	 * process. The message contains a local variable and vector timestamp, see
	 * Message class. E.g. if state.processesMessagesCurrentIndex contains {1, 2},
	 * it means that the state contains the second message (event) from process1 and
	 * the third message (event) from process2
	 */
	private class State {
		// Message indices of each process
		private int[] processesMessagesCurrentIndex;

		public State(int numberOfProcesses) {
			processesMessagesCurrentIndex = new int[numberOfProcesses];
		}

		public State(int[] processesMessagesCurrentIndex) {
			this.processesMessagesCurrentIndex = processesMessagesCurrentIndex;
		}

		{
			processesMessagesCurrentIndex = new int[numberOfProcesses];
		}

		public int[] getProcessesMessagesCurrentIndex() {
			return processesMessagesCurrentIndex;
		}

		public int getProcessMessageCurrentIndex(int processId) {
			return this.processesMessagesCurrentIndex[processId];
		}

		@Override
		public boolean equals(Object other) {
			State otherState = (State) other;

			// Iterate over processesMessagesCurrentIndex array
			for (int i = 0; i < numberOfProcesses; i++)
				if (this.processesMessagesCurrentIndex[i] != otherState.processesMessagesCurrentIndex[i])
					return false;

			return true;
		}

		public String toString() {
			String s = "State: ";
			for (int i = 0; i < numberOfProcesses; ++i) {
				s += processesMessagesCurrentIndex[i] + " ";
			}
			return s;
		}
	}

	private int numberOfProcesses;
	private final int numberOfPredicates = 4;

	// Count of still running processes. The monitor starts to check predicates
	// (build lattice) whenever runningProcesses equals zero.
	private AtomicInteger runningProcesses;
	/*
	 * Q1, Q2, ..., Qn It represents the processes' queue. See distributed debugging
	 * algorithm from global state lecture!
	 */
	private List<List<Message>> processesMessages;

	// list of states
	private LinkedList<State> states;

	// The predicates checking results
	private boolean[] possiblyTruePredicatesIndex;
	private boolean[] definitelyTruePredicatesIndex;

	public Monitor(int numberOfProcesses) {
		this.numberOfProcesses = numberOfProcesses;

		runningProcesses = new AtomicInteger();
		runningProcesses.set(numberOfProcesses);

		processesMessages = new ArrayList<>(numberOfProcesses);
		for (int i = 0; i < numberOfProcesses; i++) {
			List<Message> tempList = new ArrayList<>();
			processesMessages.add(i, tempList);
		}

		states = new LinkedList<>();

		possiblyTruePredicatesIndex = new boolean[numberOfPredicates];// there
																		// are
																		// three
		// predicates
		for (int i = 0; i < numberOfPredicates; i++)
			possiblyTruePredicatesIndex[i] = false;

		definitelyTruePredicatesIndex = new boolean[numberOfPredicates];
		for (int i = 0; i < numberOfPredicates; i++)
			definitelyTruePredicatesIndex[i] = false;
	}

	/**
	 * receive messages (events) from processes
	 *
	 * @param processId
	 * @param message
	 */
	public void receiveMessage(int processId, Message message) {
		synchronized (processesMessages) {
			processesMessages.get(processId).add(message);
		}
	}

	/**
	 * Whenever a process terminates, it notifies the Monitor. Monitor only starts
	 * to build lattice and check predicates when all processes terminate
	 *
	 * @param processId
	 */
	public void processTerminated(int processId) {
		runningProcesses.decrementAndGet();
	}

	public boolean[] getPossiblyTruePredicatesIndex() {
		return possiblyTruePredicatesIndex;
	}

	public boolean[] getDefinitelyTruePredicatesIndex() {
		return definitelyTruePredicatesIndex;
	}

	@Override
	public void run() {
		// wait till all processes terminate
		while (runningProcesses.get() != 0)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		// create initial state (S00)
		State initialState = new State(numberOfProcesses);

		// check predicates for part (b)
		for (int predicateNo = 0; predicateNo < 3; predicateNo++) {
			System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
			states.add(initialState); // add the initial state to states list
			buildLattice(predicateNo, 0, 1);
			states.clear();

		}

		if (numberOfProcesses > 2) {
			int predicateNo = 3;
			System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
			states.add(initialState); // add the initial state to states list
			buildLattice(predicateNo, 0, 2);
			states.clear();
		}

	}

	public void buildLattice(int predicateNo, int process_i_id, int process_j_id) {
		// TODO
		/*
		 * - implement this function to build the lattice of consistent states.- - The
		 * goal of building the lattice is to check a predicate if it is possibly or/and
		 * definitely True. Thus your function should stop whenever the predicate
		 * evaluates to both possibly and definitely True. NOTE1: this function should
		 * call findReachableStates and checkPredicate functions. NOTE2: predicateNo,
		 * process_i_id and process_j_id are described in checkPredicate function.
		 */
		this.possiblyTruePredicatesIndex[predicateNo] = this.checkPossibal(predicateNo, process_i_id, process_j_id);
		this.definitelyTruePredicatesIndex[predicateNo] = this.checkPredicate(predicateNo, process_i_id, process_j_id);
	}

	/**
	 * find all reachable states starting from a given state
	 *
	 * @param state
	 * @return list of all reachable states
	 */
	private LinkedList<State> findReachableStates(State state) {
		// TODO
		/*
		 * Given a state, implement a code that find all reachable states. The function
		 * should return a list of all reachable states
		 *
		 */
		LinkedList<State> reachables = new LinkedList<State>();

		for (int i = 0; i < numberOfProcesses; ++i) {
			int[] curIdxs = state.getProcessesMessagesCurrentIndex().clone();
			++curIdxs[i];
			if (curIdxs[i] < processesMessages.get(i).size()) {
				// Create new state;
				State s = new State(curIdxs);
				VectorClock iClock = processesMessages.get(i).get(s.getProcessMessageCurrentIndex(i)).getVectorClock();
				for (int j = 0; j < numberOfProcesses; ++j) {
					if (i == j)
						continue;
					VectorClock jClock = processesMessages.get(j).get(s.getProcessMessageCurrentIndex(j))
							.getVectorClock();
					if (jClock.checkConsistency(i, iClock) && !reachables.contains(s)) {
						reachables.add(s);
					}
				}
			}
		}

		System.out.println("The reachable state of " + state + " is ");
		for (int i = 0; i < reachables.size(); i++) {
			System.out.println(reachables.get(i));
		}
		System.out.println("____________________");

		return reachables;

	}

	/**
	 * - check a predicate and return true if the predicate is **definitely** True.
	 * - To simplify the code, we check the predicates only on local variables of
	 * two processes. Therefore, process_i_Id and process_j_id refer to the
	 * processes that have the local variables in the predicate. The predicate0,
	 * predicate1 and predicate2 contain the local variables from process1 and
	 * process2. whilst the predicate3 contains the local variables from process1
	 * and process3.
	 *
	 * @param predicateNo: which predicate to validate
	 * @return true if predicate is definitely true else return false
	 */
	private boolean checkPredicate(int predicateNo, int process_i_Id, int process_j_id) {
		// TODO
		/*
		 * - check if a predicate is possibly and/or definitely true. - iterate over all
		 * reachable states to check the predicates. NOTE: you can use the following
		 * code switch (predicateNo) { case 0: predicate =
		 * Predicate.predicate0(process_i_Message, process_j_Message); break; case 1:
		 * ... }
		 */
	

		//this part use to show the queue for the messages
		for (int i = 0; i < processesMessages.size(); i++) {
			for (int j = 0; j < processesMessages.get(i).size(); j++) {
				System.err.println("i is: " + i + " j is " + j);
				System.err.println(processesMessages.get(i).get(j).getLocalVariable());
				System.err.println(processesMessages.get(i).get(j).getVectorClock());
			}

		}
		
		boolean res = false;
		List<State> reachables = new LinkedList<State>();
		
		if(!this.gotPredictRes(predicateNo, processesMessages.get(process_i_Id).get(0), processesMessages.get(process_j_id).get(0))) {
			int[] init = new int[this.numberOfProcesses];
			Arrays.fill(init, 0);
			reachables.add(new State(init));
		}
		
		boolean flag = true;

		while (!reachables.isEmpty()) {
			List<State> nextReachables = new LinkedList<State>();
			

			for (int i = 0; i < reachables.size(); i++) {

				State state = reachables.get(i);
				boolean pred = false;

				System.out.println("x"+process_i_Id+" is " + processesMessages.get(process_i_Id)
						.get(state.getProcessMessageCurrentIndex(process_i_Id)).getLocalVariable());
				System.out.println("x"+process_j_id+" is "  + processesMessages.get(process_j_id)
						.get(state.getProcessMessageCurrentIndex(process_j_id)).getLocalVariable());

				Message processMessage_i = processesMessages.get(process_i_Id)
						.get(state.getProcessMessageCurrentIndex(process_i_Id));
				Message processMessage_j = processesMessages.get(process_j_id)
						.get(state.getProcessMessageCurrentIndex(process_j_id));
				pred =  this.gotPredictRes(predicateNo, processMessage_i, processMessage_j);
				
				flag = flag && pred;
				System.out.println("current state is: " + state);
				System.out.println("pred is: " + pred);
				System.out.println("flag is " + flag);
				
				
				
//				List<State> s = this.findReachableStates(state);
				
				if (!pred) {
					List<State> s = this.findReachableStates(state);
					for (int k = 0; k < s.size(); k++) {
						if (!nextReachables.contains(s.get(k)))
							nextReachables.add(s.get(k));
		
					}
				}

			}
			res = flag;
			reachables = nextReachables;
		}

		return res;

	}
	/*
	 * testMethode find all the linearization set then chaeck if is definitly
	 * 
	 * 
	 * 
	 * */
	
//	private boolean testMethode(int predicateNo, int process_i_id, int process_j_id) {
//		
//		List<State> reachables = new LinkedList<State>();
//		int[] init = new int[this.numberOfProcesses];
//		Arrays.fill(init, 0);
//		State initState=new State(init);
//		reachables.add(new State(init));
//		
//		List<State> nextReachables = new LinkedList<State>();
//		
//		while(!nextReachables.isEmpty()) {
//			for(int i=0;i<reachables.size();i++) {
//				
//			}
//		}
//
//		return true;
//		
//	}

	/*
	 * This function use to check possibly or not
	 * @param: predecateNo,which predictor we use
	 * 
	 * @return: true if the predictor is possibly

	 */
	private boolean checkPossibal(int predicateNo, int process_i_id, int process_j_id) {
		boolean res = false;
		
		List<State> reachables = new LinkedList<State>();
		int[] init = new int[this.numberOfProcesses];
		Arrays.fill(init, 0);

		reachables.add(new State(init));

		System.out.println(reachables.isEmpty());
		while (!res&&!reachables.isEmpty()) {
			List<State> nextReachables = new LinkedList<State>();
			for (int i = 0; i < reachables.size(); i++) {
				State state = reachables.get(i);
				Message processMessage_i = processesMessages.get(process_i_id)
						.get(state.getProcessMessageCurrentIndex(process_i_id));
				Message processMessage_j = processesMessages.get(process_j_id)
						.get(state.getProcessMessageCurrentIndex(process_j_id));
				res = res | this.gotPredictRes(predicateNo, processMessage_i, processMessage_j);
				
				List<State> s = this.findReachableStates(state);
				for (int k = 0; k < s.size(); k++) {
					if (!nextReachables.contains(s.get(k)))
						nextReachables.add(s.get(k));
				}
			}
			reachables=nextReachables;
		}
		return res;

	}

	/*
	 * this function to use switch case got diff predictor
	 * 
	 * @param: predicateNo, the number of predictor processMessage_i, x1
	 * processMessage_j, x2 
	 * 
	 * @return: pred, the result of predictor
	 * 
	 */
	private boolean gotPredictRes(int predicateNo, Message processMessage_i, Message processMessage_j) {
		boolean pred = false;
		switch (predicateNo) {
		case 0:
			pred = Predicate.predicate0(processMessage_i, processMessage_j);
			break;
		case 1:
			pred = Predicate.predicate1(processMessage_i, processMessage_j);
			break;
		case 2:
			pred = Predicate.predicate2(processMessage_i, processMessage_j);
			break;
		case 3:
			pred = Predicate.predicate3(processMessage_i, processMessage_j);
			break;
		}

		return pred;
	}

}
