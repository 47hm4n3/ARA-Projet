package projet;

import peersim.core.Node;
import peersim.core.Protocol;

public class EmitterImpl implements Emitter {
	
	private int latency;
	private int scope;
	
	@Override
	public void emit(Node host, Message msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getLatency() {
		// TODO Auto-generated method stub
		return this.latency;
	}

	@Override
	public int getScope() {
		// TODO Auto-generated method stub
		return this.scope;
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
