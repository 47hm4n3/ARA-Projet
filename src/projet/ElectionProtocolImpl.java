package projet;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;
import projet.messages.contents.ElectionMessageContent;

public class ElectionProtocolImpl implements ElectionProtocol {
	
	private boolean inElection;
	private long leaderId;
	protected int myValue;
	private List<Long> neighbors = new ArrayList<Long>();
	
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_ELECTIONPID = "electionprotocol";
	private int emit_protocol_id;
	private boolean hasSentAck;
	private long parentNode;
	private int election_pid;
	private long timeout;
	private List<Long> awaitingAck = new ArrayList<Long>();
	private boolean isSourceNode;
	private ElectionMessageContent prepAck;
	private long electionId;
	private double leaderTimeout;
	
	public ElectionProtocolImpl(String prefix){
		emit_protocol_id = Configuration.getPid(prefix+"."+PAR_EMITTER);
		String tmp[] = prefix.split("\\.");
		election_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.myValue = CommonState.r.nextInt()*(1 - 0) + 0;
		this.neighbors.clear();
	}
	
	@Override
	public void processEvent(Node node, int prot_id, Object msg) {
		long time = CommonState.getIntTime();
		//long time = System.currentTimeMillis();
		
			Emitter emitter = (EmitterImpl)node.getProtocol(emit_protocol_id);
			ElectionProtocolImpl prot = (ElectionProtocolImpl) node.getProtocol(election_pid);
			Message rcv_mess = (Message) msg;
			
			boolean runElection = true;
			if(runElection){
			// Election
			if(rcv_mess != null && rcv_mess.getTag() == Utils.ELECTION){
				
				Object msgContent = rcv_mess.getContent();
				ElectionMessageContent content = null;
				if(msgContent != null){
					content = (ElectionMessageContent)rcv_mess.getContent();
				}
				
				// Si  not inELECTION
				if(!inElection && content.getIdElection() >= prot.electionId){
					prot.inElection =  true;
					prot.hasSentAck = false;
					prot.electionId = content.getIdElection();
					
					prot.parentNode = rcv_mess.getIdSrc();
					int value = content.getLeaderVal();
					
					//On propage le message d'élection
					broadcast(node, emitter, Utils.ELECTION, content, true);
				// Sinon on renvoie un ACK
				}else{
					emitter.emit(node, new Message(node.getID(), rcv_mess.getIdSrc(), Utils.ACK, rcv_mess.getContent(), this.emit_protocol_id));
				}
			// Si ACK
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.ACK){
				
				System.out.println("Ack");
				if(prot.awaitingAck.size() != 0){
					prot.prepAck = evalNode(((ElectionMessageContent)rcv_mess.getContent()), prot);
					prot.awaitingAck.remove(rcv_mess.getIdSrc());
				}else{
					prot.inElection = false;
					
					if(!prot.isSourceNode){
						prot.hasSentAck = true;
						emitter.emit(node, new Message(node.getID(),prot.parentNode, Utils.ACK, prot.prepAck, this.emit_protocol_id));
					}else{
						this.broadcast(node, emitter, Utils.LEADER, ((ElectionMessageContent)rcv_mess.getContent()).getLeaderId(), true);
						prot.leaderId = ((ElectionMessageContent)rcv_mess.getContent()).getLeaderId();
					}
				}
			// Si LEADER 
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.LEADER){
			
				System.out.println("Leader");
				prot.leaderId = (long)rcv_mess.getContent();
			
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.PROBE){
				
				emitter.emit(node, new Message(node.getID(),rcv_mess.getIdSrc(), Utils.REPLY, rcv_mess.getContent(), this.emit_protocol_id));
			
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.REPLY){
				if(!prot.neighbors.contains(rcv_mess.getIdSrc())){
					prot.neighbors.add(rcv_mess.getIdSrc());
				}
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.BEACON && !prot.inElection){
				prot.leaderTimeout = time + ((emitter.getLatency()*2)*Network.size());
				broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
				emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
			}else{
				System.out.println("PROBE");
				if(time > prot.timeout/2){
					prot.timeout = time + (emitter.getLatency()*2)*Network.size();
					broadcast(node, emitter, Utils.PROBE, prot.timeout, false);
				}
				
				if(prot.leaderId == node.getID() && !prot.inElection && time > prot.leaderTimeout/2){
					System.out.println("SEND BEACON");
					prot.leaderTimeout = time + ((emitter.getLatency()*2)*Network.size());
					broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
					emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
				}
			}
			
			if(time > prot.timeout){
				prot.neighbors.clear();
			}
			
			if(time > prot.leaderTimeout && !prot.inElection){
				prot.triggerElection(prot, node, emitter);
			}
			
			}
	}
	
	public void triggerElection(ElectionProtocolImpl prot, Node node, Emitter emitter){
		System.out.println("Trigger Election : "+node.getID());
		prot.isSourceNode = true;
		prot.inElection = true;
		prot.electionId += 1;
		prot.leaderId = node.getID();
		ElectionMessageContent content = new ElectionMessageContent(node.getID(), prot.getMyValue(), prot.electionId, node.getID(), this.emit_protocol_id);
		this.broadcast(node, emitter, Utils.ELECTION, content, true);
	}

	private ElectionMessageContent evalNode(ElectionMessageContent content, ElectionProtocolImpl prot) {
		if(content.getLeaderVal() > prot.prepAck.getLeaderVal())
			return content;
		else 
			return prot.prepAck;
	}

	@Override
	public boolean isInElection() {
		return this.inElection;
	}

	@Override
	public long getIDLeader() {
		return this.leaderId;
	}

	@Override
	public int getMyValue() {
		return this.myValue;
	}

	@Override
	public List<Long> getNeighbors() {
		return this.neighbors;
	}
	
	@Override
	public Protocol clone(){
		try {
			ElectionProtocolImpl prot = (ElectionProtocolImpl)super.clone();
			prot.myValue = CommonState.r.nextInt()*(1 - 0) + 0;
			prot.neighbors = new ArrayList<Long>();
			prot.awaitingAck = new ArrayList<Long>();
			return prot;
		} catch (CloneNotSupportedException e) {
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
	
	private void broadcast(Node node, Emitter emitter, String msgTag, Object content, boolean toNeighborsOnly){
		ElectionProtocolImpl prot = (ElectionProtocolImpl) node.getProtocol(election_pid);
		for(int i =0; i < Network.size(); i++){
			Node nodal = Network.get(i);
			if(!toNeighborsOnly || (toNeighborsOnly && prot.neighbors.contains(nodal.getID()))){
				emitter.emit(node, new Message(node.getID(), nodal.getID(), msgTag, content, this.emit_protocol_id));
			}
		}
	}
}
