package de.uni_stuttgart.ipvs.ids.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Vector;

/**
 * Part b) Extend the method receiveMessages to return all DatagramPackets that
 * were received during the given timeout.
 * 
 * Also implement unpack() to conveniently convert a Collection of
 * DatagramPackets containing ValueResponseMessages to a collection of
 * VersionedValueWithSource objects.
 * 
 */
public class NonBlockingReceiver {

	protected DatagramSocket socket;

	public NonBlockingReceiver(DatagramSocket socket) {
		this.socket = socket;
	}

	public Vector<DatagramPacket> receiveMessages(int timeoutMillis, int expectedMessages) throws IOException {
		// TODO: Impelement me!
		// TODO: 没理解时间到底怎么个模式，循环不对
		Vector<DatagramPacket> res = new Vector<>();

		long startTime = System.currentTimeMillis();
		while (res.size() < expectedMessages || (System.currentTimeMillis() - startTime) > timeoutMillis) {
			try {
				byte[] buf = new byte[4096];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.setSoTimeout(timeoutMillis);
				socket.receive(packet);

				res.add(packet);
			} catch (SocketTimeoutException e) {
				break;
			}

		}
		System.err.println(res.isEmpty());
		socket.setSoTimeout(0);
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <T> Collection<MessageWithSource<T>> unpack(Collection<DatagramPacket> packetCollection)
			throws IOException, ClassNotFoundException {
		// TODO: Impelement me!
		Vector<MessageWithSource<T>> ret = new Vector<>();

		for (DatagramPacket packet : packetCollection) {
			ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
			MessageWithSource<T> mws = new MessageWithSource<T>(packet.getSocketAddress(), (T) iStream.readObject());
			iStream.close();
			ret.add(mws);
		}

		return ret;

	}

}
