package projet;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

public class EmitterImpl implements Emitter {
	
	private int latency;
	private int scope = 100;
	private int pos_protocol_id;
	private int emit_protocol_id;
	
	private static final String PAR_POSITIONPID = "positionprotocol";

	public EmitterImpl(String prefix){
		String tmp[] = prefix.split("\\.");
		pos_protocol_id=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emit_protocol_id=Configuration.lookupPid(tmp[tmp.length - 1]);
	}
	
	@Override
	public void emit(Node host, Message msg) {
		EmitterImpl emitter = (EmitterImpl) host.getProtocol(emit_protocol_id);
		PositionProtocolImpl hostPos = (PositionProtocolImpl) host.getProtocol(pos_protocol_id);
		
		for(int i =0; i < Network.size(); i++){
			Node node = Network.get(i);
			PositionProtocolImpl nodePos = (PositionProtocolImpl) node.getProtocol(pos_protocol_id);
			if(!(Math.pow(nodePos.getX() - hostPos.getX(),2) + Math.pow(nodePos.getY() - hostPos.getY(),2) > Math.pow(emitter.scope, 2))){
				// Is in the circle !
				// so we have to deliver the message !
			}
		}
	}

	@Override
	public int getLatency() {
		return this.latency;
	}

	@Override
	public int getScope() {
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
