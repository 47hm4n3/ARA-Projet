package projet;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;

/**
 * Implementation du protocole d'emission de messages
 * 
 * 
 * @author olivier && 47hm4n3
 *
 */

public class EmitterImpl implements Emitter {
	
	//la latence du reseau (temps d'arrivée du message)
	private int latency = 1;
	//portée d'émission/reception du noeud
	private int scope = 100;
	//protocoles dont a besoin l'execution
	private int pos_protocol_id;
	private int emit_protocol_id;
	private int election_pid;

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_ELECTIONPID = "election";

	public EmitterImpl(String prefix){
		String tmp[] = prefix.split("\\.");
		pos_protocol_id=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emit_protocol_id=Configuration.lookupPid(tmp[tmp.length - 1]);
		election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
	}

	@Override
	public void emit(Node host, Message msg) {
		//récuperation des protocoles
		EmitterImpl emitter = (EmitterImpl) host.getProtocol(emit_protocol_id);
		PositionProtocolImpl hostPos = (PositionProtocolImpl) host.getProtocol(pos_protocol_id);
		//pour tous les noeuds du réseau
		for(int i =0; i < Network.size(); i++){
			Node node = Network.get(i);
			//l'envoi de messages ne concerne que les autres noeuds (pas le noeud lui même)
			if (node.getID() != host.getID()){
				PositionProtocolImpl nodePos = (PositionProtocolImpl) node.getProtocol(pos_protocol_id);
				//l'envoi de messages ne peut atteindre que les noeuds dans la portée (scope)
				if(msg.getIdDest() == node.getID()){
					if(!(Math.pow(nodePos.getX() - hostPos.getX(),2) + Math.pow(nodePos.getY() - hostPos.getY(),2) > Math.pow(emitter.scope, 2))){
						// the node is the dest and is in our scope, so we send the message
						EDSimulator.add(this.getLatency(), msg, node, election_pid);
					}
				}
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
