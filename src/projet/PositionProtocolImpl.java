package projet;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Implementation du protocole de simulation de mouvement Random Way Point
 * 
 * 
 * @author olivier && 47hm4n3
 *
 */
public class PositionProtocolImpl implements PositionProtocol {
	//initialisation des cooredonnées du noeud
	private double x = 0;
	private double y = 0;
	//initialisation des coordonnées de destination du noeud
	private double targetX = 0;
	private double targetY = 0;
	//définition de la taille de l'espace de simulation
	private double maxX = 500;
	private double maxY = 500;
	//définition de la vitesse maximale de mouvement du noeud
	private int maxSpeed = 1;
	//définition d'un temps d'arrêt 
	private int timePause = 5000;
	//variable representant la vitesse actuelle du noeud
	private double currentSpeed;
	//variable representant les distances restantes pour atteindre le point destination
	private double mvtX;
	private double mvtY;
	//variable representant l'angle entre le point source et le point destination
	private double angle;

	private static final String PAR_MAXX_VAL = "maxX_val";
	private static int protocol_id;

	public PositionProtocolImpl(String prefix){
		//this.maxX = Double.parseDouble(PAR_MAXX_VAL);
		String tmp[] = prefix.split("\\.");
		protocol_id=Configuration.lookupPid(tmp[tmp.length - 1]);
		//génération des coordonnées (aléatoires) de départ 
		this.x = CommonState.r.nextDouble()*(this.maxX - 0) + 0;
		this.y = CommonState.r.nextDouble()*(this.maxY - 0) + 0;
		this.targetX = this.x;
		this.targetY = this.y;
	}

	@Override
	public void processEvent(Node node, int arg1, Object arg2) {
		PositionProtocolImpl pos = (PositionProtocolImpl) node.getProtocol(protocol_id);

		int delay = 0;
		//calcul de l'angle entre les deux positions de départ et destination du noeud
		pos.angle = Math.atan2(pos.targetY - pos.y, pos.targetX - pos.x);
		//si le noeud a atteint sa destination
		if(Math.abs(pos.x - pos.targetX) == 0 && Math.abs(pos.y - pos.targetY) == 0){
			//génération d'une nouvelle position destination (aléatoire)
			pos.targetX = CommonState.r.nextDouble()*(pos.maxX - 0) + 0;
			pos.targetY = CommonState.r.nextDouble()*(pos.maxY - 0) + 0;
			//le noeud fait une pause avant de repartir
			delay = timePause;
			//calcul d'une nouvelle vitesse (aléatoire) pour ce déplacement ci
			pos.currentSpeed = CommonState.r.nextDouble()*(pos.maxSpeed - 0.5) + 0.5;
		}else{//le noeud n'a pas encore atteint la destination
			//calcul des distances (sur les deux axes) restantes pour atteindre la destination
			pos.mvtX = Math.cos(pos.angle) * pos.currentSpeed;
			pos.mvtY = Math.sin(pos.angle) * pos.currentSpeed;
			//correction de la vitesse si distance restante est plus petite que ce que permet notre vitesse
			if ( pos.mvtX > Math.abs(pos.x - pos.targetX))
				pos.mvtX = Math.abs(pos.x - pos.targetX);
			if( pos.mvtY > Math.abs(pos.y - pos.targetY))
				pos.mvtY = Math.abs(pos.y - pos.targetY);
			//nouvelles coordonnées du noeud
			pos.x += pos.mvtX;
			pos.y += pos.mvtY;
		}
	}

	@Override
	public double getY() {
		// TODO Auto-generated method stub
		return this.y;
	}

	@Override
	public double getX() {
		// TODO Auto-generated method stub
		return this.x;
	}

	@Override
	public int getMaxSpeed() {
		// TODO Auto-generated method stub
		return this.maxSpeed;
	}

	@Override
	public double getMaxX() {
		// TODO Auto-generated method stub
		return this.maxX;
	}

	@Override
	public double getMaxY() {
		// TODO Auto-generated method stub
		return this.maxY;
	}

	@Override
	public int getTimePause() {
		// TODO Auto-generated method stub
		return this.timePause;
	}

	@Override
	public Protocol clone(){
		try {
			PositionProtocolImpl cloned = (PositionProtocolImpl) super.clone();
			cloned.x = CommonState.r.nextDouble()*(cloned.maxX - 0) + 0;
			cloned.y = CommonState.r.nextDouble()*(cloned.maxY - 0) + 0;
			cloned.targetX = cloned.x;
			cloned.targetY = cloned.y;
			return cloned;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
