package projet;

import java.util.ArrayList;
import java.util.List;

import peersim.core.Node;
import peersim.core.Protocol;

public class ElectionProtocolImpl implements ElectionProtocol {
	
	private boolean inElection;
	private long leaderId;
	private int myValue;
	private List<Long> neighbors = new ArrayList<Long>();
	
	@Override
	public void processEvent(Node arg0, int arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInElection() {
		return this.inElection;
	}

	@Override
	public long getIDLeader() {
		// TODO Auto-generated method stub
		return this.leaderId;
	}

	@Override
	public int getMyValue() {
		// TODO Auto-generated method stub
		return this.myValue;
	}

	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub
		return this.neighbors;
	}
	
	@Override
	public Protocol clone(){
		try {
			return (Protocol) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
