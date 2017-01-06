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
	
	public ElectionProtocolImpl(String prefix){
		emit_protocol_id = Configuration.getPid(prefix+"."+PAR_EMITTER);
		String tmp[] = prefix.split("\\.");
		election_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.myValue = CommonState.r.nextInt()*(1 - 0) + 0;
		this.neighbors.clear();
	}
	
	@Override
	public void processEvent(Node node, int prot_id, Object msg) {
		long time = System.currentTimeMillis();
			Emitter emitter = (EmitterImpl)node.getProtocol(emit_protocol_id);
			ElectionProtocolImpl prot = (ElectionProtocolImpl) node.getProtocol(election_pid);
			Message rcv_mess = (Message) msg;
			
			if(rcv_mess != null && rcv_mess.getTag() == Utils.ELECTION){
				Object msgContent = rcv_mess.getContent();
				ElectionMessageContent content = null;
				if(msgContent != null){
					content = (ElectionMessageContent)rcv_mess.getContent();
				}
				if(!inElection && content.getValue() > prot.myValue){
					inElection =  true;
					hasSentAck = false;
					
					parentNode = rcv_mess.getIdSrc();
					int value = content.getValue();
					
					//On propage le message d'élection
					broadcast(node, emitter, Utils.ELECTION, content, true);
				}else{
					emitter.emit(getNodeFromId(rcv_mess.getIdSrc()), new Message(node.getID(), rcv_mess.getIdSrc(), Utils.ACK, null, 0));
				}
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.ACK){
				System.out.println("Ack");
				if(prot.awaitingAck.size() != 0){
					//prepAck = evalNode(mess)
					prot.awaitingAck.remove(rcv_mess.getIdSrc());
				}else{
					prot.inElection = false;
					
					if(!prot.isSourceNode){
						prot.hasSentAck = true;
						emitter.emit(node, new Message(node.getID(),prot.parentNode, Utils.ACK, null, 0));
					}else{
						this.broadcast(node, emitter, Utils.LEADER, null, true);
						inElection = false;
						//leaderId = elected;
					}
				}
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.LEADER){
				System.out.println("Leader");
				prot.leaderId = (long)rcv_mess.getContent();
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.PROBE){
				emitter.emit(node, new Message(node.getID(),rcv_mess.getIdSrc(), Utils.REPLY, rcv_mess.getContent(), 0));
			}else if(rcv_mess != null && rcv_mess.getTag() == Utils.REPLY){
				if(!prot.neighbors.contains(rcv_mess.getIdSrc())){
					prot.neighbors.add(rcv_mess.getIdSrc());
				}
			}else{
				prot.timeout = System.currentTimeMillis() + 10;
				broadcast(node, emitter, Utils.PROBE, prot.timeout, false);
			}
			
			
			if(time > prot.timeout){
				prot.neighbors.clear();
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
