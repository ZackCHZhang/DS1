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
import java.util.Random;

import de.uni_stuttgart.ipvs.ids.communication.ReadRequestMessage;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseReadLock;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseWriteLock;
import de.uni_stuttgart.ipvs.ids.communication.RequestReadVote;
import de.uni_stuttgart.ipvs.ids.communication.RequestWriteVote;
import de.uni_stuttgart.ipvs.ids.communication.ValueResponseMessage;
import de.uni_stuttgart.ipvs.ids.communication.Vote;
import de.uni_stuttgart.ipvs.ids.communication.WriteRequestMessage;

public class Replica<T> extends Thread {

	public enum LockType {
		UNLOCKED, READLOCK, WRITELOCK
	};

	private int id;

	private double availability;
	private VersionedValue<T> value;

	protected DatagramSocket socket = null;

	protected LockType lock;

	/**
	 * This address holds the addres of the client holding the lock. This variable
	 * should be set to NULL every time the lock is set to UNLOCKED.
	 */
	protected SocketAddress lockHolder;

	public Replica(int id, int listenPort, double availability, T initialValue) throws SocketException {
		super("Replica:" + listenPort);
		this.id = id;
		SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", listenPort);
		this.socket = new DatagramSocket(socketAddress);
		this.availability = availability;
		this.value = new VersionedValue<T>(0, initialValue);
		this.lock = LockType.UNLOCKED;
	}

	/**
	 * Part a) Implement this run method to receive and process request messages. To
	 * simulate a replica that is sometimes unavailable, it should randomly discard
	 * requests as long as it is not locked. The probability for discarding a
	 * request is (1 - availability).
	 * 
	 * For each request received, it must also be checked whether the request is
	 * valid. For example: - Does the requesting client hold the correct lock? - Is
	 * the replica unlocked when a new lock is requested?
	 */
	public void run() {
		// TODO: Implement me!
		byte[] buf = new byte[4096];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		Random random = new Random();
		// TODO: 这个循环改改有点蠢
		while (true) {
			try {

				Object msg = this.getObjectFromMessage(packet);
				System.err.println(msg.getClass());
				
//				if(random.nextDouble()<availability) {
//					throw new QuorumNotReachedException(this.id, null);
//				}
				
//				socket.receive(packet);
//				ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
//				Object msg = objStream.readObject();
//				objStream.close();

				if (this.lock == LockType.UNLOCKED && random.nextDouble() > availability) {
					continue;
				}

				if (msg instanceof RequestReadVote) {
					
					if (lock == LockType.UNLOCKED) {
						this.lock = LockType.READLOCK;
						this.lockHolder = packet.getSocketAddress();
						this.sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if (msg instanceof ReleaseReadLock) {
					if (lock == LockType.READLOCK && lockHolder.equals(packet.getSocketAddress())) {
						lock = LockType.UNLOCKED;
						this.lockHolder = null;
						this.sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if (msg instanceof ReadRequestMessage) {
					if (lock == LockType.READLOCK && lockHolder.equals(packet.getSocketAddress())) {
						System.err.println("~~~~~~~~~~~");
						ValueResponseMessage<T> resMsg = new ValueResponseMessage<T>(value.value);

						ByteArrayOutputStream byteout = new ByteArrayOutputStream();
						ObjectOutputStream oStream = null;

						oStream = new ObjectOutputStream(byteout);
						oStream.writeObject(resMsg);
						byte[] resBuf = byteout.toByteArray();
						
						System.err.println(oStream);
						
						this.socket.send(new DatagramPacket(resBuf, resBuf.length, packet.getSocketAddress()));
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if (msg instanceof RequestWriteVote) {

					if (lock == LockType.UNLOCKED) {
						this.lock = LockType.WRITELOCK;
						this.lockHolder = packet.getSocketAddress();
						this.sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if (msg instanceof ReleaseWriteLock) {
					if (lock == LockType.WRITELOCK && lockHolder.equals(packet.getSocketAddress())) {
						lock = LockType.UNLOCKED;
						this.lockHolder = null;
						this.sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if (msg instanceof WriteRequestMessage) {
					if (lock == LockType.WRITELOCK && lockHolder.equals(packet.getSocketAddress())
							&& ((VersionedValue) msg).version > value.version) {
						this.value = (VersionedValue) msg;
						// TODO: 这个类型转化会不会有问题
						this.sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					} else {
						this.sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
//			catch (QuorumNotReachedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

		}

	}

	/**
	 * This is a helper method. You can implement it if you want to use it or just
	 * ignore it. Its purpose is to send a Vote (YES/NO depending on the state) to
	 * the given address.
	 */
	protected void sendVote(SocketAddress address, Vote.State state, int version) throws IOException {
		// TODO: Implement me!
		ByteArrayOutputStream byteout = new ByteArrayOutputStream();

		Vote vote = new Vote(state, version);

		ObjectOutput oStream = new ObjectOutputStream(byteout);
		oStream.writeObject(vote);
//		oStream.flush();
		oStream.close();

		byte[] buf = byteout.toByteArray();
		DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, address);

		socket.send(datagramPacket);

	}

	/**
	 * This is a helper method. You can implement it if you want to use it or just
	 * ignore it. Its purpose is to extract the object stored in a DatagramPacket.
	 */
	protected Object getObjectFromMessage(DatagramPacket packet) throws IOException, ClassNotFoundException {
		// TODO: Implement me!
//
//		byte[] buf = new byte[4096];
//		DatagramPacket revPaket = new DatagramPacket(buf, buf.length);

		socket.receive(packet);

		// ByteArrayInputStream bytein = new ByteArrayInputStream(packet.getData());

		ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));

		Object res = null;

		res = inputStream.readObject();

		

		inputStream.close();

		return res; // Pacify the compiler
//		return null;
	}

	public int getID() {
		return id;
	}

	public SocketAddress getSocketAddress() {
		return socket.getLocalSocketAddress();
	}

}
