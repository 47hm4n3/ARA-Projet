package projet.messages.contents;

public class ElectionMessageContent {
	private Long leaderId = null;
	private Integer leaderVal = null;
	private Long idElection = null;
	private Long idElectionInit = null;
	private int niveauInArbre = 0;
	
	public ElectionMessageContent(long leaderId, int leaderVal, long idElection, long idElecInit, int niveauInArbre){
		this.leaderId = leaderId;
		this.leaderVal = leaderVal;
		this.idElection = idElection;
		this.idElectionInit = idElecInit;
		this.niveauInArbre = niveauInArbre;
	}

	public int getNiveauArbre() {
		return niveauInArbre;
	}

	public void setNiveauArbre(int niveauInArbre) {
		this.niveauInArbre = niveauInArbre;
	}

	public Long getIdElection() {
		return idElection;
	}

	public void setIdElection(Long idElection) {
		this.idElection = idElection;
	}

	public int getLeaderVal() {
		return leaderVal;
	}

	public void setLeaderVal(int leaderVal) {
		this.leaderVal = leaderVal;
	}

	public Long getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(Long leaderId) {
		this.leaderId = leaderId;
	}

	public Long getIdElecInit() {
		return idElectionInit;
	}

	public void setIdElecInit(Long idElectionInit) {
		this.idElectionInit = idElectionInit;
	}
	
}
