package projet;

import peersim.core.Node;
import peersim.core.Protocol;

public class PositionProtocolImpl implements PositionProtocol {
	
	private double x;
	private double y;
	private double maxX;
	private double maxY;
	private int maxSpeed;
	private int timePause;
	
	@Override
	public void processEvent(Node arg0, int arg1, Object arg2) {
		// TODO Auto-generated method stub
		
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
			return (Protocol) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
