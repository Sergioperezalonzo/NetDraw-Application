package netgame.draw;

import netgame.common.Hub;

/**
 * When this program is run, it starts a netgame Hub that will
 * listen on port 32001 until this program is shut down.  The
 * Hub is a basic hub that simply forwards any messages that it
 * gets from clients to all connected clients (including the
 * client who sent the message).
 */
public class NetDrawServer {

	public static void main(String[] args) {
		try {
			new Hub(32001);
		}
		catch (Exception e) {
			System.out.println("Unable to start server on port 32001.");
		}
	}
	
}
