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

/**
 * Implementation du protocole d'election
 * 
 * 
 * @author olivier && 47hm4n3
 *
 */

public class ElectionProtocolImpl implements ElectionProtocol {
	
	//le noeud partcicipe à une election ou pas
	private boolean inElection;
	//id et valeur du noeud leader du noeud actuel
	private long leaderId;
	protected int myValue;
	//liste des voisins du noeud
	private List<Long> neighbors = new ArrayList<Long>();
	
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_ELECTIONPID = "electionprotocol";
	private int emit_protocol_id;
	
	private boolean hasSentAck;
	private long parentNode;
	private int election_pid;
	
	//liste des acks que le noeud attend 
	private List<Long> awaitingAck = new ArrayList<Long>();
	
	private boolean isSourceNode;
	private Message prepAck;
	// id election à la quelle participe le noeud et id de l'initiateur de celle ci
	private long electionId;
	private long electInitId;
	
	
	private Message standByLeaderMess;
	//valeur du leader du noeud actuel
	private int leaderVal;

	//timeouts des noeuds du voisinage du noeud
	private HashMap<Long, Integer> neighborTimeout= new HashMap<Long, Integer>(); 
	//timeout pire cas
	private int DELTA;
	//le dernier instant ou le neud a executé le processEvent()
	private int timeoutOld = 0;
	//initialisation du timeout entre le noeud et son leader
	private double leaderTimeout = DELTA;
	//initialisation du timeout entre le noeud et ses voisins
	private long timeout = DELTA;
	private Integer neighborsDelta;
	
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
		
			Emitter emitter = (EmitterImpl)node.getProtocol(emit_protocol_id);
			ElectionProtocolImpl prot = (ElectionProtocolImpl) node.getProtocol(election_pid);
			Message rcv_mess = (Message) msg;
			DELTA = ((emitter.getLatency()*2)*(Network.size())+1);
			neighborsDelta = (emitter.getLatency()*2);
			ArrayList<Long> toDelete = new ArrayList<Long>();
			int time = CommonState.getIntTime();
			boolean sendProbe = false;
			
			//gestion des timeouts avec les voisins
			for (Map.Entry<Long, Integer> entry : neighborTimeout.entrySet()){
				//décrémenter du temps passé entre deux executions successives du processEvent
				int val = entry.getValue()-(time-timeoutOld);
				neighborTimeout.put(entry.getKey(), val);
				//si timeout est nul alors le noeud est a supprimer du voisinage
				if (val <= 0){
					toDelete.add(entry.getKey());
				}
			}
			for (long e : toDelete){
				neighborTimeout.remove(e);
				neighbors.remove(e);
			}
			//décrementer le timeout avec le leader
			leaderTimeout -= (time - timeoutOld);
			timeoutOld = time;
			
				// Si Premiere execution
				if(prot.leaderId == -1){
					System.out.println("Premiere execution "+ node.getID());
					prot.triggerElection(prot, node, emitter);
					prot.leaderId = node.getID();
				}
				// Si le noeud n'attend personne alors pas de voisins
				
				// Si pas en election 
				if(!prot.inElection && prot.leaderId != node.getID() && prot.leaderTimeout <= 0){
					System.out.println("Participe à une election "+ node.getID()+" avec "+prot.leaderId);
					prot.triggerElection(prot, node, emitter);
				}
				
			if(rcv_mess != null && rcv_mess.getTag() == Utils.ELECTION){
				System.out.println("ELECTION "+ node.getID()+" de "+rcv_mess.getIdSrc());
				Object msgContent = rcv_mess.getContent();
				ElectionMessageContent content = null;
				if(msgContent != null){
					content = (ElectionMessageContent)rcv_mess.getContent();
				}
				
				// Si  not inELECTION
				if(!inElection || (content.getIdElection() > prot.electionId || ( content.getIdElection() == prot.electionId  && content.getIdElecInit() > prot.electInitId)) ){
					System.out.println("pas en election "+node.getID());
					prot.electionId = content.getIdElection();
					prot.inElection =  true;
					prot.hasSentAck = false;
					prot.electInitId = content.getIdElecInit();
					
					prot.parentNode = rcv_mess.getIdSrc();
					int value = content.getLeaderVal();
					
					prot.awaitingAck = new ArrayList<Long>();
					prot.awaitingAck.addAll(neighbors);
					prot.awaitingAck.remove(prot.parentNode);
					if (prot.awaitingAck.contains(node.getID()))
						prot.awaitingAck.remove(node.getID());
					
					//On propage le message d'élection
					broadcast(node, emitter, Utils.ELECTION, content, true);
				// Sinon on renvoie un ACK
				}else{
					System.out.println("en election "+node.getID());
					// on omet le message d'élection si on est déjà en election
					emitter.emit(node, new Message(node.getID(), rcv_mess.getIdSrc(), Utils.ACK, rcv_mess.getContent(), this.emit_protocol_id));
				}
			// Si ACK
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.ACK){
				System.out.println("ACK "+ node.getID()+" de "+rcv_mess.getIdSrc());
				if(prot.awaitingAck.size() > 0){
					prot.prepAck = evalNode(rcv_mess, prot.prepAck);
					prot.awaitingAck.remove(rcv_mess.getIdSrc());
				}
				
			// Si LEADER 
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.LEADER){
				System.out.println("LEADER "+ node.getID()+" de "+rcv_mess.getIdSrc());
				ElectionMessageContent content = ((ElectionMessageContent)rcv_mess.getContent());
				
				if(prot.inElection && (content.getIdElection() == prot.electionId) && (content.getIdElecInit() == prot.electInitId)){
					System.out.println("mon election "+node.getID());
					System.out.println(">>>"+node.getID()+" leader elu "+content.getLeaderId()+" source "+rcv_mess.getIdSrc());

					// C'est bien mon election et tout braaaah
					prot.leaderTimeout = prot.DELTA;
					prot.leaderId = content.getLeaderId();
					prot .leaderVal = content.getLeaderVal();
					prot.inElection = false;
					
					broadcast(node, emitter, Utils.LEADER, content, true);
				}
				if(!(content.getIdElection() == prot.electionId && content.getIdElecInit() == prot.electInitId)){
					System.out.println("pas mon election "+node.getID());
					if(inElection){
						//standBy
						prot.standByLeaderMess = evalNode(rcv_mess, prot.standByLeaderMess);
						
					}else{
						// C'est une meilleur election et tout braaaah
						if ((content.getLeaderVal() > prot.leaderVal) || ((content.getLeaderVal() == prot.leaderVal) && (content.getLeaderId() > prot.leaderId))  ){
							System.out.println("Leader   "+node.getID()+" leader elu "+content.getLeaderId()+" source "+rcv_mess.getIdSrc());

							prot.leaderTimeout = time + prot.DELTA;
							prot.leaderId = content.getLeaderId();
							prot .leaderVal = content.getLeaderVal();
							prot.inElection = false;
							broadcast(node, emitter, Utils.LEADER, content, true);
						}
					}
				}
				
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.PROBE){
				emitter.emit(node, new Message(node.getID(),rcv_mess.getIdSrc(), Utils.REPLY, rcv_mess.getContent(), this.emit_protocol_id));
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.REPLY){
				if(!prot.neighbors.contains(rcv_mess.getIdSrc())){
					prot.neighbors.add(rcv_mess.getIdSrc());
					ElectionMessageContent content = new ElectionMessageContent(prot.leaderId, prot.myValue, prot.electionId, prot.electInitId, 0);
					emitter.emit(node,  new Message(node.getID(), rcv_mess.getIdSrc(), Utils.LEADER, content, this.emit_protocol_id));
				}
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.BEACON && !prot.inElection){
				prot.leaderTimeout = DELTA;
				broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
				emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
			}else {
				sendProbe = true;
			}
			
			if(prot.inElection && prot.awaitingAck.size() <= 0){
				//prot.inElection = false;
				sendProbe = false;
				
				if(!prot.isSourceNode){
					prot.hasSentAck = true;
					emitter.emit(node, new Message(node.getID(),prot.parentNode, Utils.ACK, prot.prepAck, this.emit_protocol_id));
				}else{
					this.broadcast(node, emitter, Utils.LEADER, prot.prepAck.getContent(), true);
					prot.leaderId =((ElectionMessageContent)prot.prepAck.getContent()).getLeaderId();
					prot.inElection = false;
				}
			}
			
			if(sendProbe){
					System.out.println("PROBE");
					
						prot.timeout = neighborsDelta;
						for(int i = 0; i < neighbors.size(); i++){
							if(!prot.neighborTimeout.containsKey(neighbors.get(i)))
							prot.neighborTimeout.put(neighbors.get(i), neighborsDelta);
						}
						broadcast(node, emitter, Utils.PROBE, prot.timeout, false);
						// TODO numero sequence dans le beacon
					
					
					if(prot.leaderId == node.getID() && !prot.inElection && prot.leaderTimeout <= 0){
						System.out.println("SEND BEACON");
						//broadcast(node, emitter, Utils.BEACON, prot.leaderTimeout, true);
						//emitter.emit(node, new Message(node.getID(), node.getID(),Utils.BEACON, prot.leaderTimeout, this.emit_protocol_id));
					}
			}
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
