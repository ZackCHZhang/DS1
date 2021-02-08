package de.uni_stuttgart.ipvs.ids.replication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import de.uni_stuttgart.ipvs.ids.communication.MessageWithSource;
import de.uni_stuttgart.ipvs.ids.communication.NonBlockingReceiver;
import de.uni_stuttgart.ipvs.ids.communication.ReadRequestMessage;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseReadLock;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseWriteLock;
import de.uni_stuttgart.ipvs.ids.communication.RequestReadVote;
import de.uni_stuttgart.ipvs.ids.communication.RequestWriteVote;
import de.uni_stuttgart.ipvs.ids.communication.ValueResponseMessage;
import de.uni_stuttgart.ipvs.ids.communication.Vote;
import de.uni_stuttgart.ipvs.ids.communication.Vote.State;
import de.uni_stuttgart.ipvs.ids.communication.WriteRequestMessage;

public class MajorityConsensus<T> {

	protected Collection<SocketAddress> replicas;

	protected DatagramSocket socket;
	protected NonBlockingReceiver nbio;

	final static int TIMEOUT = 1000;

	public MajorityConsensus(Collection<SocketAddress> replicas, int port) throws SocketException {
		this.replicas = replicas;
		SocketAddress address = new InetSocketAddress("127.0.0.1", port);
		this.socket = new DatagramSocket(address);
		this.nbio = new NonBlockingReceiver(socket);
	}

	/**
	 * Part c) Implement this method.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected Collection<MessageWithSource<Vote>> requestReadVote()
			throws QuorumNotReachedException, IOException, ClassNotFoundException {
		// TODO: Implement me!
		RequestReadVote message = new RequestReadVote();
		for (SocketAddress iterReplicas : replicas) {
			this.sendMessage(iterReplicas, message);
		}

		Vector<DatagramPacket> rescivedMsg = this.nbio.receiveMessages(TIMEOUT, this.replicas.size());
		
		Collection<MessageWithSource<Vote>> messages = NonBlockingReceiver.unpack(rescivedMsg);
		return this.checkQuorum(messages);

	}


	/**
	 * Part c) Implement this method.
	 * 
	 * @throws IOException
	 */
	protected void releaseReadLock(Collection<SocketAddress> lockedReplicas) throws IOException {
		// TODO: Implement me!
		ReleaseReadLock message = new ReleaseReadLock();
		for (SocketAddress iterReplicas : replicas) {
			this.sendMessage(iterReplicas, message);
		}
		// 这是干嘛的？
		this.nbio.receiveMessages(TIMEOUT, this.replicas.size());

	}

	/**
	 * Part d) Implement this method.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected Collection<MessageWithSource<Vote>> requestWriteVote()
			throws QuorumNotReachedException, IOException, ClassNotFoundException {
		// TODO: Implement me!

		RequestWriteVote message = new RequestWriteVote();
		for (SocketAddress iterReplicas : replicas) {
			this.sendMessage(iterReplicas, message);
		}

		Vector<DatagramPacket> rescivedMsg = this.nbio.receiveMessages(TIMEOUT, this.replicas.size());

		Collection<MessageWithSource<Vote>> messages = NonBlockingReceiver.unpack(rescivedMsg);

		return messages;
	}

	/**
	 * Part d) Implement this method.
	 * 
	 * @throws IOException
	 */
	protected void releaseWriteLock(Collection<SocketAddress> lockedReplicas) throws IOException {
		// TODO: Implement me!
		ReleaseWriteLock message = new ReleaseWriteLock();
		for (SocketAddress iterReplicas : replicas) {
			this.sendMessage(iterReplicas, message);
		}
		// 这是干嘛的？
		this.nbio.receiveMessages(TIMEOUT, this.replicas.size());
	}

	/**
	 * Part c) Implement this method.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected T readReplica(SocketAddress replica) throws IOException, ClassNotFoundException {
		// TODO: Implement me!
		ReadRequestMessage msg = new ReadRequestMessage();
		this.sendMessage(replica, msg);

		byte[] buf = new byte[4096];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		this.socket.receive(packet);

		ByteArrayInputStream bStream = new ByteArrayInputStream(packet.getData());
		ObjectInputStream iStream = new ObjectInputStream(bStream);

		ValueResponseMessage<T> message = (ValueResponseMessage<T>) iStream.readObject();

		return message.getValue();
	}

	/**
	 * Part d) Implement this method.
	 * 
	 * @throws IOException
	 */
	protected void writeReplicas(Collection<SocketAddress> lockedReplicas, VersionedValue<T> newValue)
			throws IOException {
		// TODO: Implement me!
		WriteRequestMessage<T> message = new WriteRequestMessage<T>(newValue);
		for (SocketAddress iterReplicas : lockedReplicas) {
			this.sendMessage(iterReplicas, message);
		}
		this.nbio.receiveMessages(TIMEOUT, lockedReplicas.size());
	}

	/**
	 * Part c) Implement this method (and checkQuorum(), see below) to read the
	 * replicated value using the majority consensus protocol.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public VersionedValue<T> get() throws QuorumNotReachedException, ClassNotFoundException, IOException {
		// TODO: Implement me!
		Collection<MessageWithSource<Vote>> lockedRep = this.requestReadVote();
		Collection<SocketAddress> lockedRepAdress = new ArrayList<SocketAddress>();
		int maxVersion = -1;
		VersionedValue<T> res = null;
		for (MessageWithSource<Vote> iterLockedRep : lockedRep) {
			lockedRepAdress.add(iterLockedRep.getSource());
			
			System.err.println("IterLockedREP version: "+iterLockedRep.getMessage().getVersion());
			
			if (iterLockedRep.getMessage().getVersion() > maxVersion && iterLockedRep != null) {
				
				maxVersion=iterLockedRep.getMessage().getVersion();
				res = new VersionedValue<T>(iterLockedRep.getMessage().getVersion(),
						this.readReplica(iterLockedRep.getSource()));
				
				System.err.println("??????????????????");
			}
		}
		System.out.println("maxVersion is "+maxVersion);
		System.out.println("the value is : "+res.getValue());
		this.releaseReadLock(lockedRepAdress);
		return res;
	}

	/**
	 * Part d) Implement this method to set the replicated value using the majority
	 * consensus protocol.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void set(T value) throws QuorumNotReachedException, ClassNotFoundException, IOException {
		// TODO: Implement me!
		Collection<MessageWithSource<Vote>> lockedRep = this.requestWriteVote();
		Collection<MessageWithSource<Vote>> lockedRepAdress = this.checkQuorum(lockedRep);

		Collection<SocketAddress> writeAdress = new ArrayList<SocketAddress>();
		int maxVersion = -1;

		for (MessageWithSource<Vote> iterLockedRepAdress : lockedRepAdress) {
			writeAdress.add(iterLockedRepAdress.getSource());
			if (iterLockedRepAdress.getMessage().getVersion() > maxVersion) {
				maxVersion = iterLockedRepAdress.getMessage().getVersion();
			}
		}

		this.writeReplicas(writeAdress, new VersionedValue(maxVersion + 1, value));

		this.releaseWriteLock(replicas);
	}

	/**
	 * Part c) Implement this method to check whether a sufficient number of replies
	 * were received. If a sufficient number was received, this method should return
	 * the {@link MessageWithSource}s of the locked {@link Replica}s. Otherwise, a
	 * QuorumNotReachedException must be thrown.
	 * 
	 * @throws QuorumNotReachedException
	 */
	protected Collection<MessageWithSource<Vote>> checkQuorum(Collection<MessageWithSource<Vote>> replies)
			throws QuorumNotReachedException {
		// TODO: Implement me!
		Collection<MessageWithSource<Vote>> res = new ArrayList<MessageWithSource<Vote>>();
		System.err.println(replies.size());
//		int count=0;
		for (MessageWithSource<Vote> iterReplies : replies) {
//			count++;
//			System.err.println("The "+count+" loopp and state is "+iterReplies.getMessage().getState());
			if (iterReplies.getMessage().getState() == State.YES) {
				res.add(iterReplies);
			}
		}
		int suffNumber = (int) (this.replicas.size() / 2) + 1;
		System.err.println(res.size());
		if (res.size() < suffNumber) {
			Collection<SocketAddress> achieved = new ArrayList<SocketAddress>();
			for (MessageWithSource<Vote> iterRes : res) {
				achieved.add(iterRes.getSource());

			}
			throw new QuorumNotReachedException(suffNumber, achieved);
		}
		return res;
	}

	/*
	 * This function is intergiert serializemessage and send message
	 * 
	 * @param: address message
	 * 
	 */
	private void sendMessage(SocketAddress address, Object message) throws IOException {
		
		byte[] buf=null;
		
		try (ByteArrayOutputStream bStream = new ByteArrayOutputStream()) {
            try (ObjectOutput oo = new ObjectOutputStream(bStream)) {
                oo.writeObject(message);
            }
           buf= bStream.toByteArray();
        }
		

		this.socket.send(new DatagramPacket(buf, buf.length, address));
	}

	/*
	 * Help Function to recive message
	 * 
	 * 
	 * /
	 */
	private T reciveMessage() {
		return null;
	}

}
