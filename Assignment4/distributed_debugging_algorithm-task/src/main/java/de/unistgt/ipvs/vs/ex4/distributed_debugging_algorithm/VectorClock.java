package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

//you are not allowed to change this class structure
public class VectorClock {

	protected int[] vectorClock;
	private int processId;
	private int numberOfProcesses;

	public VectorClock(int processId, int numberOfProcesses) {
		vectorClock = new int[numberOfProcesses];
		this.numberOfProcesses = numberOfProcesses;
		this.processId = processId;
	}

	VectorClock(VectorClock other) {
		vectorClock = other.vectorClock.clone();
		processId = other.processId;
		numberOfProcesses = other.numberOfProcesses;

	}

	public void increment() {
		// TODO
		/*
		 * Complete a code to increment the local clock component
		 */
		this.vectorClock[this.processId]+=1;

	}

	public int[] get() {
		// TODO
		// Complete a code to return the vectorClock value
		return this.vectorClock;
	}

	public void update(VectorClock other) {
		// TODO
		/*
		 * Implement Supermum operation
		 */
		int [] otherVectorClock=other.get();
		for(int i=0;i<this.numberOfProcesses;i++) {
			if(this.vectorClock[i]<otherVectorClock[i]) {
				this.vectorClock[i]=otherVectorClock[i];
			}
		}
		
	}

	public boolean checkConsistency(int otherProcessId, VectorClock other) {
		//TODO
		/*
		 * Implement a code to check if a state is consist regarding two vector clocks (i.e. this and other).
		 * See slide 41 from global state lecture.
		 */

		int [] otherVectorClock=other.get();
		return this.vectorClock[this.processId]>= otherVectorClock[this.processId] && this.vectorClock[otherProcessId]<= otherVectorClock[otherProcessId];

	}
	
	public String toString() {
		String s="VC: ";
		for(int i=0;i<this.numberOfProcesses;i++) {
			s+=this.vectorClock[i]+" ";
		}
		return s;
	}

}
