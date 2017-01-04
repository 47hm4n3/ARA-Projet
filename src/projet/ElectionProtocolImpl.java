package projet;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;
import projet.messages.contents.ElectionMessageContent;

public class ElectionProtocolImpl implements ElectionProtocol {
	
	private boolean inElection;
	private long leaderId;
	private int myValue;
	private List<Long> neighbors = new ArrayList<Long>();
	
	private static final String PAR_EMITTER = "emitter";
	private int emit_protocol_id;
	private boolean hasSentAck;
	private long parentNode;
	
	public ElectionProtocolImpl(String prefix){
		emit_protocol_id = Configuration.getPid(prefix+"."+PAR_EMITTER);
	}
	
	@Override
	public void processEvent(Node node, int arg1, Object msg) {
		Emitter emitter = (EmitterImpl)node.getProtocol(emit_protocol_id);
		Message rcv_mess = (Message)msg;
		
		if(rcv_mess.getTag() == Utils.ELECTION){
			ElectionMessageContent content = (ElectionMessageContent)rcv_mess.getContent();
			
			/*
			if(!inElection || isSuperiorSRC(mess.src)){
					inElection = true
					hasSentAck = false
			
					parentNode = mess.sender
					awaitingAck = neighbors - parentNode
					src = mess.src
					broadcast(ELECTION, awaitingAck)
				}else{
					send(ACK, mess.sender)
				}	
			 */
			if(!inElection || content.getSnd() != null){
				inElection =  true;
				hasSentAck = false;
				
				parentNode = rcv_mess.getIdSrc();
				int value = content.getValue();
				
				//On propage le message d'élection
				for(int i =0; i < Network.size(); i++){
					Node nodal = Network.get(i);
					emitter.emit(node, new Message(node.getID(), nodal.getID(), Utils.ELECTION, null, 0));
				}
			}else{
				emitter.emit(getNodeFromId(rcv_mess.getIdSrc()), new Message(node.getID(), rcv_mess.getIdSrc(), Utils.ACK, null, 0));
			}
		}else if(rcv_mess.getTag() == Utils.ACK){
			/*if(mess != null && awaitingAck.size != 0){
				prepAck = evalNode(mess)
				awaitingAck.remove(mess.sender)
			}else{
				inElection = false
				
				if(!isSourceNode){
					hasSentAck = true
					send(ACK, parentNode)
				}else{
					broadcast(LEADER, neighbors)
					inElection = false
					leader = elected
				}
			}*/
		}else if(rcv_mess.getTag() == Utils.LEADER){
			leaderId = (long)rcv_mess.getContent();
		}else if(rcv_mess.getTag() == Utils.PROBE){
			emitter.emit(getNodeFromId(rcv_mess.getIdSrc()), new Message(node.getID(),rcv_mess.getIdSrc(), Utils.REPLY, null, 0));
		}else if(rcv_mess.getTag() == Utils.REPLY){
			
		}else{
			//Vu que je n'ai rien à traiter, vlan ! Je heartbeat mes voisins
			/*code de probe / broadcast */
			for(int i =0; i < Network.size(); i++){
				Node nodal = Network.get(i);
				emitter.emit(node, new Message(node.getID(), nodal.getID(), Utils.PROBE, null, 0));
			}
		}
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

	private Node getNodeFromId(long id){
		for(int i =0; i < Network.size(); i++){
			Node nodal = Network.get(i);
			if(nodal.getID() == id)
				return nodal;
		}
		return null;
	}
}
