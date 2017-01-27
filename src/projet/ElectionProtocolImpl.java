package projet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
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

	private List<Long> awaitingAck = new ArrayList<Long>();
	private boolean isSourceNode;
	private Message prepAck;
	private long electionId;
	private long electInitId;

	private Message standByLeaderMess;
	private int leaderVal;

	
	//HashMap avec <id du noeud, un timer perso (initialisé a timeout et décrémenté a chhaque appel du process event)>
	private HashMap<Long, Integer> neighborTimeout= new HashMap<Long, Integer>(); 
	private int DELTA;
	
	private int timeoutOld=0;
	private int timeoutNow;
	private double leaderTimeout = DELTA;
	private long timeout = DELTA;
	
	public ElectionProtocolImpl(String prefix){
		emit_protocol_id = Configuration.getPid(prefix+"."+PAR_EMITTER);
		String tmp[] = prefix.split("\\.");
		
		election_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.myValue = (int)(CommonState.r.nextDouble()*Network.size()*3);
		this.neighbors.clear();
		this.timeout = CommonState.getIntTime();
		this.leaderTimeout = DELTA;
		this.leaderId = -1;
	}
	
	@Override
	public void processEvent(Node node, int prot_id, Object msg) {
		//long time = CommonState.getIntTime();
		//long time = System.currentTimeMillis();
		
			Emitter emitter = (EmitterImpl)node.getProtocol(emit_protocol_id);
			ElectionProtocolImpl prot = (ElectionProtocolImpl) node.getProtocol(election_pid);
			Message rcv_mess = (Message) msg;
			DELTA = ((emitter.getLatency()*2)*(Network.size()-1));
			ArrayList<Long> toDelete = new ArrayList<Long>();
			timeoutNow = CommonState.getIntTime();
			for (Map.Entry<Long, Integer> entry : neighborTimeout.entrySet()){
				int val = entry.getValue()-(timeoutNow-timeoutOld);
				
				neighborTimeout.put(entry.getKey(), val);
				if (entry.getValue() <= 0){
					toDelete.add(entry.getKey());
				}
			}
			for (long e : toDelete){
				neighborTimeout.remove(e);
				neighbors.remove(e);
			}

			leaderTimeout -= (timeoutNow-timeoutOld);
			timeoutOld = timeoutNow;
			
			
			//boolean runElection = true;
			//if(runElection){
				// Si Premiere execution
				if(prot.leaderId == -1){
					//prot.triggerElection(prot);
					prot.leaderId = node.getID();
				}
				// Election
				if(neighborTimeout.size() == 0){
					prot.neighbors.clear();
				}
				
				if(!prot.inElection && prot.leaderId != node.getID() && timeoutNow > prot.leaderTimeout){ 
					prot.triggerElection(prot, node, emitter);
				}
				
			if(rcv_mess != null && rcv_mess.getTag() == Utils.ELECTION){
				
				Object msgContent = rcv_mess.getContent();
				ElectionMessageContent content = null;
				if(msgContent != null){
					content = (ElectionMessageContent)rcv_mess.getContent();
				}
				
				// Si  not inELECTION
				if(!inElection || (content.getIdElection() > prot.electionId || ( content.getIdElection() == prot.electionId  && content.getIdElecInit() > prot.electInitId)) ){
					prot.electionId = content.getIdElection();
					prot.inElection =  true;
					prot.hasSentAck = false;
					prot.electInitId = content.getIdElecInit();
					
					prot.parentNode = rcv_mess.getIdSrc();
					int value = content.getLeaderVal();
					
					prot.awaitingAck = new ArrayList<Long>();
					prot.awaitingAck.addAll(neighbors);
					prot.awaitingAck.remove(prot.parentNode);
					
					//On propage le message d'élection
					broadcast(node, emitter, Utils.ELECTION, content, true);
				// Sinon on renvoie un ACK
				}else{
					// on omet le message d'élection si on est déjà en election
					emitter.emit(node, new Message(node.getID(), rcv_mess.getIdSrc(), Utils.ACK, rcv_mess.getContent(), this.emit_protocol_id));
				}
			// Si ACK
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.ACK){
				
				if(prot.awaitingAck.size() > 0){
					prot.prepAck = evalNode(rcv_mess, prot.prepAck);
					prot.awaitingAck.remove(rcv_mess.getIdSrc());
				}
				
			// Si LEADER 
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.LEADER){
				ElectionMessageContent content = ((ElectionMessageContent)rcv_mess.getContent());
				
				if(prot.inElection && (content.getIdElection() == prot.electionId) && (content.getIdElecInit() == prot.electInitId)){
					System.out.println("Leader   "+node.getID()+" leader elu "+content.getLeaderId()+" source "+rcv_mess.getIdSrc());

					// C'est bien mon election et tout braaaah
					prot.leaderTimeout = timeoutNow + prot.DELTA;
					prot.leaderId = content.getLeaderId();
					prot .leaderVal = content.getLeaderVal();
					prot.inElection = false;
					
					broadcast(node, emitter, Utils.LEADER, content, true);
				}
				if(!(content.getIdElection() == prot.electionId && content.getIdElecInit() == prot.electInitId)){
					if(inElection){
						//standBy
						prot.standByLeaderMess = evalNode(rcv_mess, prot.standByLeaderMess);
						
					}else{
						// C'est une meilleur election et tout braaaah
						if ((content.getLeaderVal() > prot.leaderVal) || ((content.getLeaderVal() == prot.leaderVal) && (content.getLeaderId() > prot.leaderId))  ){
							System.out.println("Leader   "+node.getID()+" leader elu "+content.getLeaderId()+" source "+rcv_mess.getIdSrc());

							prot.leaderTimeout = timeoutNow + prot.DELTA;
							prot.leaderId = content.getLeaderId();
							prot .leaderVal = content.getLeaderVal();
							prot.inElection = false;
							broadcast(node, emitter, Utils.LEADER, content, true);
						}
					}
				}
				
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.PROBE){
				//je reinitialise le timeout du noeud src dans la HashMap 
				prot.neighborTimeout.put(rcv_mess.getIdSrc(), DELTA);
				emitter.emit(node, new Message(node.getID(),rcv_mess.getIdSrc(), Utils.REPLY, rcv_mess.getContent(), this.emit_protocol_id));
			
				
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.REPLY){
				if(!prot.neighbors.contains(rcv_mess.getIdSrc())){
					prot.neighbors.add(rcv_mess.getIdSrc());
					ElectionMessageContent content = new ElectionMessageContent(prot.leaderId, prot.myValue, prot.electionId, prot.electInitId, 0);
					emitter.emit(node,  new Message(node.getID(), rcv_mess.getIdSrc(), Utils.LEADER, content, this.emit_protocol_id));
				}
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.BEACON && !prot.inElection){
				prot.leaderTimeout = timeoutNow + prot.DELTA;
				broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
				//emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
			}else{
				//System.out.println("PROBE");
				
					prot.timeout = timeoutNow + (int)(DELTA / Network.size());
					broadcast(node, emitter, Utils.PROBE, prot.timeout, false);
					// TODO numero sequence dans le beacon
				
				
				if(prot.leaderId == node.getID() && !prot.inElection && timeoutNow > prot.leaderTimeout){
					//System.out.println("SEND BEACON");
					prot.leaderTimeout = timeoutNow + prot.DELTA;
					//broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
					//emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
				}
			}
			
			if(prot.inElection && prot.awaitingAck.size() <= 0){
				//prot.inElection = false;
				
				if(!prot.isSourceNode){
					prot.hasSentAck = true;
					emitter.emit(node, new Message(node.getID(),prot.parentNode, Utils.ACK, prot.prepAck, this.emit_protocol_id));
				}else{
					this.broadcast(node, emitter, Utils.LEADER, prot.prepAck.getContent(), true);
					prot.leaderId =((ElectionMessageContent)prot.prepAck.getContent()).getLeaderId();
					prot.inElection = false;
				}
				
			}
			
		//}
	}
	
	public void triggerElection(ElectionProtocolImpl prot, Node node, Emitter emitter){
		System.out.println("Trigger Election : "+node.getID());
		prot.isSourceNode = true;
		prot.inElection = true;
		prot.electionId += 1;
		prot.leaderId = node.getID();
		prot.electInitId = node.getID();
		ElectionMessageContent content = new ElectionMessageContent(prot.leaderId, prot.myValue, prot.electionId, prot.electInitId, 0);
		prot.prepAck = new Message(node.getID(), node.getID(), Utils.ACK, content, prot.election_pid);
		
		content = new ElectionMessageContent(node.getID(), prot.getMyValue(), prot.electionId, node.getID(), this.emit_protocol_id);
		this.broadcast(node, emitter, Utils.ELECTION, content, true);
	}

	private Message evalNode(Message content, Message toComp) {
		if(content == null && toComp != null)
			return toComp;
		if(content != null && toComp == null)
			return content;
		
		if(((ElectionMessageContent)content.getContent()).getLeaderVal() > ((ElectionMessageContent)toComp.getContent()).getLeaderVal())
			return content;
		else 
			return toComp;
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
			prot.myValue = (int)(CommonState.r.nextDouble()*Network.size()*3);
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
