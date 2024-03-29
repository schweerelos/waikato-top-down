package game;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.Music;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import actor.Bullet;
import actor.InputFeeder;
import actor.KeyboardInput;
import actor.PlaybackInput;
import actor.Player;
import actor.PlayerGhost;
import actor.Turret;
import actor.movehelpers.ScrollingMovehelper;

public class GameEngine extends BasicGame{

	Level level;
	Player player;
	KeyboardInput playerInput;
	
	private int score = 0;
	private int round = 0;	// How many runs through is the player (0 == 0 ghosts)
	
	EntityManager entityManager;
	private boolean godMode;
	private boolean gameOver;
	private boolean levelFinished;
	private boolean towersGone;
	private Music music;
	private GameState state;
	

	private List<List<boolean[]>> playbackInputs = new ArrayList<List<boolean[]>>();
	private ArrayList<PlayerGhost> ghosts;
	private ArrayList<PlaybackInput> ghostInputs;
	private StatusBar statusBar;
	private SplashScreen splashScreen;
	private GameOverScreen gameOverScreen;
	
	private int gameoverwaited;
	
	public GameEngine(String title) {
		super(title);
		state = GameState.TITLE;
		score = 0;
		try {
			music = new Music("res/music/music.ogg");
		} catch (SlickException e) {
			e.printStackTrace();
		}
		music.loop();
	}
	
	
	public void stateTransition(GameContainer container) throws SlickException {
		switch(state) {
		case GAME_OVER:
		case TITLE:
			restartGame(container);
			state = GameState.PLAYING;
			break;
		case PLAYING:
			state = GameState.GAME_OVER;
			gameOverScreen.setFinalScore(score);
			gameOverScreen.setFinalLevel(round + 1);
			break;
		}
	}

	private void restartGame(GameContainer container) throws SlickException {
		playbackInputs = new ArrayList<List<boolean[]>>();
		score = 0;
		round = 0;
		music = new Music("res/music/music.ogg");
		music.loop();
		init(container);
	}


	@Override
	public boolean closeRequested() {
		if (music != null && music.playing()) {
			music.stop();
		}
		return true;
	}

	@Override
	public void render(GameContainer container, Graphics g)
			throws SlickException {
		if(state == GameState.TITLE) {
			splashScreen.render(g);
			
		}
		else if (state == GameState.PLAYING) {
			level.render(g);
			for (Turret turret : entityManager.getTurrets()) {
				turret.render(g);
			}
			for (TextBubble tb : entityManager.getTextBubbles()) {
				tb.render(g);
			}
			for(Bullet bullet : entityManager.getBullets()) {
				bullet.render(g);
			}
			
			for(Player ghost : ghosts) {
				ghost.render(g);
			}
			
			
			
			
			player.render(g);
			
			statusBar.render(g);
		}
		else if(state == GameState.GAME_OVER) {
			gameOverScreen.render(g);
		}
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		container.setShowFPS(false);
		splashScreen = new SplashScreen(this, container, Resources.splashImage);
		gameOverScreen = new GameOverScreen(this, container, Resources.gameOverImage);
		
		statusBar = new StatusBar(this, container, Resources.statusBackground);
		int statusHeight = statusBar.getHeight();
		
		int reservedBottomSpace = 0;
		if (playbackInputs != null && !playbackInputs.isEmpty()) {
			reservedBottomSpace = 91;
		}
		level = new Level(this,container, statusHeight, Resources.background,ScrollingMovehelper.getInstance());
		player = new Player(this, container, statusHeight, Resources.player, level, reservedBottomSpace);
		ghosts = new ArrayList<PlayerGhost>();
		ghostInputs = new ArrayList<PlaybackInput>();
		float strength = 0.9f;
		for(int i = playbackInputs.size() - 1; i >= 0; i--) {
			PlayerGhost ghost = new PlayerGhost(this,container, statusHeight, Resources.ghost,level, strength);
			ghosts.add(ghost);
			ghostInputs.add(new PlaybackInput(ghost,playbackInputs.get(i)));
			strength -= 0.1f;	// TODO maybe should set this in damages etc
		}
		playerInput = new KeyboardInput(player, container);
		
		entityManager = new EntityManager(this, container,level);
		
		//entityManager.generateRandomTurrets(Damages.initialTurretCount + round * Damages.turretIncreaseRate);
		entityManager.loadSavedTurrets();
		
		
	}

	@Override
	public void update(GameContainer container, int delta)
			throws SlickException {
		if (container.getInput().isKeyDown(Keyboard.KEY_ESCAPE)) {
			container.exit();
		}
		
		
		switch(state) {

		case GAME_OVER:
			gameoverwaited += delta;
			
			if(container.getInput().isKeyPressed(Input.KEY_SPACE) && gameoverwaited >= 2000) {
				gameoverwaited = 0;
				stateTransition(container);
			}
			break;
		case TITLE:
			if(container.getInput().isKeyPressed(Input.KEY_SPACE)) {
				stateTransition(container);
			}
			break;
		case PLAYING:
			updateGamePlan(container, delta);
			break;
			
		}
		
		
	}


	private void updateGamePlan(GameContainer container, int delta)
			throws SlickException {
		level.updatePos(delta);
		playerInput.poll(delta);
		
		for(PlaybackInput pbInput: ghostInputs) {
			pbInput.poll(delta);
		}
		
		entityManager.destroyOffscreen();

		List<Turret> turrets = new ArrayList<Turret>();
		turrets.addAll(entityManager.getTurrets());
		for (Turret turret : turrets) {
			if (Damages.turretCollision) detectCollisions(turret);
			turret.shoot(delta);
		}
		
		List<Bullet> bullets = new ArrayList<Bullet>();
		bullets.addAll(entityManager.getBullets());
		for (Bullet bullet : bullets) {
			bullet.advance(delta);
			detectCollisions(bullet);
		}
		
		for (InputFeeder feeder : entityManager.getInputFeeders()) {
			feeder.poll(delta);
		}
		List<TextBubble> bubbles = new ArrayList<TextBubble>();
		bubbles.addAll(entityManager.getTextBubbles());
		for (TextBubble bubble : bubbles) {
			if (bubble.canBeDiscarded()) entityManager.destroyBubble(bubble);
			else bubble.reduceAlpha();
		}
		
		if(level.isTransitionStarted()) {
			player.setAlpha(player.getAlpha()-Speeds.playerFadeRate);
			for(PlayerGhost ghost : ghosts) {
				ghost.setAlpha(ghost.getAlpha() - Speeds.gostFadeRate);
			}
		} else {
			if(player.getAlpha() <= player.getStrength()) {
				player.setAlpha(player.getAlpha()+Speeds.playerFadeRate);
			}
			for(PlayerGhost ghost : ghosts) {
				if(ghost.getAlpha() <= ghost.getStrength()) {
					ghost.setAlpha(ghost.getAlpha()+Speeds.gostFadeRate);
				}
			}
			
		}
		
		if(towersGone && levelFinished) {
			
			restartLevel(container);
		}
		
		if(gameOver) {
			gameOver = false;
			gameoverwaited = 0;
			stateTransition(container);
		}
	}


	private void restartLevel(GameContainer container) throws SlickException {
		playbackInputs.add(playerInput.getInputsTriggered());
		while (playbackInputs.size() > 10) {
			playbackInputs.remove(0);
		}
		levelFinished = false;
		towersGone = false;
		round++;
		init(container);
		
	}
	
	// See if the player has run into anything
	private void detectCollisions(Turret turret) throws SlickException {
		if (turret.getBoundingBox().intersects(player.getBoundingBox())) {
			turret.takeDamage(Damages.collisionModifier * Damages.playerDamage);
			player.takeDamage(Damages.collisionModifier * Damages.turretDamage);
			if (turret.getHealth() < 0) {
				entityManager.destroyTurret(turret);
				entityManager.addTextBubble(new TextBubble(Resources.effectKaboomYellowSmall, turret.getX(), turret.getY()));
				SoundEffectManager.getInstance().playerCollision();
			}
			if (player.getHealth() < 0) {
				gameOver = true;
				SoundEffectManager.getInstance().playerExplosion();
			}
		}
	}

	private void detectCollisions(Bullet bullet) throws SlickException {
		Rectangle bulletBB = bullet.getBoundingBox();
		if (bullet.isPlayerFired()) {
			List<Turret> turrets = new ArrayList<Turret>();
			turrets.addAll(entityManager.getTurrets());
			for (Turret turret : turrets) {
				// TODO do this for ghosts and enemy ships etc too
				Rectangle turretBB = turret.getBoundingBox();
				if (turretBB.intersects(bulletBB)) {
					turret.takeDamage(bullet.getDamageRating());
					SoundEffectManager.getInstance().turretHit();
					if (turret.getHealth() <= 0) {
						score +=turret.getScoreValue();
						entityManager.destroyTurret(turret);
						SoundEffectManager.getInstance().turretExplosion();
						entityManager.addTextBubble(new TextBubble(Resources.effectBoomGreenSmall, turret.getX(), turret.getY()));
					}
					entityManager.destroyBullet(bullet);
				}
			}
			// Now check if this is hitting the player (e.g. ghost bullets)
			if (bullet.getBoundingBox().intersects(player.getBoundingBox())) {
				player.takeDamage(bullet.getDamageRating());
				entityManager.destroyBullet(bullet);
				SoundEffectManager.getInstance().playerHit();
				if (player.getHealth() <= 0) {
					SoundEffectManager.getInstance().playerExplosion();
					gameOver = true;
				}
			}
		} else {
			// we know it's a bullet that can potentially hurt the player
			Rectangle playerBB = player.getBoundingBox();
			if (playerBB.intersects(bulletBB)) {
				SoundEffectManager.getInstance().playerHit();
				if (!isGodMode()) {
					player.takeDamage(bullet.getDamageRating());
					if (player.getHealth() <= 0) {
						System.out.println("player dies");
						gameOver = true;
					}
				}
				entityManager.destroyBullet(bullet);
			}
		}
	}

	private boolean isGodMode() {
		return godMode;
	}

	public void setGodMode(boolean godMode) {
		this.godMode = godMode;
	}

	public static void main(String[] args) throws SlickException {
		         AppGameContainer app = 
					new AppGameContainer(new GameEngine("Memories that haunt you"),800,600,false);
		         app.setVSync(true);
		         
		         app.start();
	}

	public Player getPlayer() {
		return player;
	}

	public void registerBullet(Bullet bullet) {
		entityManager.addBullet(bullet);
	}

	public void levelStopped() {
		levelFinished = true;
	}
	
	public void towersGone(boolean status) {
		towersGone = status;
	}

	public int getScore() {
		return score;
	}


	public int getRound() {
		return round;
	}
}
