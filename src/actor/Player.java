package actor;


import game.Damages;
import game.GameEngine;
import game.Level;
import game.Resources;
import game.Speeds;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Vector2f;

import actor.movehelpers.BulletMoveHelper;
import actor.movehelpers.PlayerBulletMoveHelper;

public class Player implements IDrawable,IMoveable, IShooter {
	
	private Level level;
	GameEngine engine;
	GameContainer container;
	private int shotLast;
	private ImageHelper imagehelper;

	
	public Player(GameEngine engine, GameContainer container, String graphicsLocation,Level level) throws SlickException {
		imagehelper = new ImageHelper(graphicsLocation);
		imagehelper.setTopY((float) (level.getMaxYBounds()-imagehelper.getHeight()));
		imagehelper.setTopX((float) ((level.getMaxXBounds()-imagehelper.getWidth())/2));
		
		this.level = level;
		this.engine = engine;
		this.container = container;
		shotLast = 0;
	}

	@Override
	public void render(Graphics g) {
		imagehelper.render(g);
	}

	@Override
	public void moveLeft(int delta) {
		float shift = Speeds.playerspeed*delta/1000;
		imagehelper.setTopX(Math.max(level.getMinXBounds(), imagehelper.getTopX()-shift));
		
	}

	@Override
	public void moveRight(int delta) {
		float shift = Speeds.playerspeed*delta/1000;
		imagehelper.setTopX(Math.min(level.getMaxXBounds()-imagehelper.getWidth(), imagehelper.getTopX()+shift));

		
	}

	@Override
	public void moveDown(int delta) {
		float shift = Speeds.playerspeed*delta/1000;
		imagehelper.setTopY(Math.min(level.getMaxYBounds()-imagehelper.getHeight(), imagehelper.getTopY()+shift));
	}

	@Override
	public void moveUp(int delta) {
		float shift = Speeds.playerspeed*delta/1000;
		imagehelper.setTopY(Math.max(level.getMinYBounds(),imagehelper.getTopY()-shift));
	}

	public Vector2f getPosition() {
		return new Vector2f(getBoundingBox().getCenter());
	}


	@Override
	public void shoot(int delta) {
		shotLast += delta;
		if (shotLast >= 1000 / Speeds.playerFireSpeed){
			Vector2f dir = new Vector2f(0, -1);
			Vector2f startPos = new Vector2f(imagehelper.getTopX()+imagehelper.getWidth()/2, imagehelper.getTopY() - 1);
			try {
				engine.registerBullet(new Bullet(container, Resources.bullet1, level, startPos,dir,PlayerBulletMoveHelper.getInstance(), true, Damages.playerDamage));
			} catch (SlickException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			shotLast = 0;
		}
	}


	public Rectangle getBoundingBox() {
		return imagehelper.getBoundingBox();
	}
	
	public void addDelta(int delta){
		shotLast += delta;
	}

}