package actor.movehelpers;

import game.Speeds;

public class BulletMoveHelper implements IMoveHelper {

	private static BulletMoveHelper instance;
	
	public static IMoveHelper getInstance() {
		
		if(instance == null) {
			instance = new BulletMoveHelper();
		}
		
		return instance;
		
	}

	@Override
	public float getShift(int delta) {
		return Speeds.bulletSpeed*delta/1000;
	}
	
}
