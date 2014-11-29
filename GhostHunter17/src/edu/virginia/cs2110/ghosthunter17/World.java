package edu.virginia.cs2110.ghosthunter17;

import java.util.ArrayList;
import java.util.Collection;

import edu.virginia.cs2110.ghosthunter17.GameObject.Direction;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Canvas.VertexMode;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;

public class World {

	private static final float LIGHT_WIDTH = 120; // Degrees
	private static final int SHADOW_COLOR = 0xff888888;

	private ArrayList<GameObject> gameObjects;
	private ArrayList<GameObject> addQueue;
	private ArrayList<GameObject> removeQueue;

	private Player p;
	private boolean paused;
	private Paint pausePaint;

	public World() {
		this(new ArrayList<GameObject>());
	}

	public World(Collection<GameObject> gameObjects) {

		this.gameObjects = new ArrayList<GameObject>();
		gameObjects.addAll(gameObjects);

		this.addQueue = new ArrayList<GameObject>();

		this.removeQueue = new ArrayList<GameObject>();

		paused = false;
		pausePaint = new Paint();
		pausePaint.setTextSize(200);
		pausePaint.setTextAlign(Align.CENTER);

		p = new Player(this, new PointF(500, 500));
		this.gameObjects.add(p);

		this.gameObjects.add(new Circle(this, new PointF(0, 0), new PointF(100,
				100), 50));

		this.gameObjects.add(new Wall(this, new RectF(400, 400, 500, 800)));

		for (int i = 0; i < 5; i++) {
			this.gameObjects.add(new Box(this,
					new PointF((float) Math.random() * 1000, (float) Math
							.random() * 1000), new PointF(
							(float) Math.random() * 1000,
							(float) Math.random() * 1000)));
		}
	}

	public void update(float timePassed) {
		if (!paused) {
			// Add/remove objects added/removed outside update
			gameObjects.addAll(addQueue);
			addQueue.clear();
			gameObjects.removeAll(removeQueue);
			removeQueue.clear();

			// Update GameObjects
			for (GameObject g : gameObjects) {
				g.update(timePassed);
			}

			checkCollision();

			// Add/remove objects added/removed during update loop
			gameObjects.addAll(addQueue);
			gameObjects.removeAll(removeQueue);

		}
	}

	public void checkCollision() {
		for (int i = 0; i < gameObjects.size(); i++) {
			GameObject g1 = gameObjects.get(i);
			for (int j = i + 1; j < gameObjects.size(); j++) {
				GameObject g2 = gameObjects.get(j);
				RectF r1 = g1.getColBounds();
				RectF r2 = g2.getColBounds();
				if (r1.intersect(r2)) {
					float top = Math.abs(r2.bottom - r1.top);
					float bot = Math.abs(r2.top - r1.bottom);
					float left = Math.abs(r2.right - r1.left);
					float right = Math.abs(r2.left - r1.right);

					Direction d1 = Direction.NORTH, d2 = Direction.SOUTH;
					float min = top;

					if (bot < min) {
						min = bot;
						d1 = Direction.SOUTH;
						d2 = Direction.NORTH;
					}
					if (left < min) {
						min = left;
						d1 = Direction.WEST;
						d2 = Direction.EAST;
					}
					if (right < min) {
						d1 = Direction.EAST;
						d2 = Direction.WEST;
					}

					g1.collide(g2, d1);
					g2.collide(g1, d2);
				}
			}
		}
	}

	public void render(Canvas c) {	

		// Render GameObjects
		for (GameObject g : gameObjects) {
			g.render(c);
		}
		
		doLighting(c);

		if (paused) {
			pausePaint.setColor(0x88111111);
			c.drawRect(0, 0, c.getWidth(), c.getHeight(), pausePaint);
			pausePaint.setColor(0xFFFFFFFF);
			c.drawText("Paused", c.getWidth() / 2, c.getHeight() / 2,
					pausePaint);
		}
	}

	private void doLighting(Canvas c) {
		ArrayList<Segment> walls = new ArrayList<Segment>();
		for (GameObject go : gameObjects) {
			if (go instanceof Wall) {
				walls.addAll(((Wall) go).getEdges());
			}
		}
		RectF bounds = new RectF(0, 0, c.getWidth(), c.getHeight());
		walls.addAll(new Wall(null,	bounds).getEdges());
		
		ArrayList<Segment> edges = new ArrayList<Segment>();
		edges.addAll(new Wall(null,	bounds).getEdges());
		
		ArrayList<PointF> lightPts = new ArrayList<PointF>();
		
		lightPts.add(new PointF(p.pos.x + 1000*(float) Math.cos((p.getRot() + LIGHT_WIDTH/2) * Math.PI / 180),
				p.pos.y + 1000*(float) Math.sin((p.getRot() + LIGHT_WIDTH/2) * Math.PI	/ 180)));
		lightPts.add(new PointF(p.pos.x + 1000*(float) Math.cos((p.getRot() - LIGHT_WIDTH/2) * Math.PI / 180),
				p.pos.y	+ 1000*(float) Math.sin((p.getRot() - LIGHT_WIDTH/2) * Math.PI / 180)));
		
		lightPts.set(0,Segment.raycast(new Segment(p.pos, lightPts.get(0)), edges));
		lightPts.set(1,Segment.raycast(new Segment(p.pos, lightPts.get(1)), edges));
		
		PointF v1 = new PointF(lightPts.get(0).x - p.pos.x, lightPts.get(0).y - p.pos.y);
		PointF v2 = new PointF(p.pos.x - lightPts.get(1).x, p.pos.y - lightPts.get(1).y);
		for (int i = 0; i < edges.size(); i++) { 
			float cross1 = Segment.cross(v1, new PointF(edges.get(i).p1.x - p.pos.x, edges.get(i).p1.y - p.pos.y));
			float cross2 = Segment.cross(v2, new PointF(edges.get(i).p1.x - p.pos.x, edges.get(i).p1.y - p.pos.y));
			if ((cross1 < 0 && cross2 < 0)) {
				lightPts.add((i==3&&lightPts.contains(edges.get(0).p1))?lightPts.size()-1:1 ,edges.get(i).p1);
			}
		}
		
		Path path = new Path();
		path.moveTo(p.pos.x, p.pos.y);
		for (int i = 0; i < lightPts.size(); i++) {
			path.lineTo(lightPts.get(i).x, lightPts.get(i).y);
		}
		path.close();
		path.toggleInverseFillType();
		
//		int[] colorArray = new int[7];
//		for (int i = 0; i < colorArray.length; i++){
//			colorArray[i] = SHADOW_COLOR;
//		}
		
		c.save();
		c.clipPath(path);
		c.drawColor(SHADOW_COLOR);
		c.restore();
		path.toggleInverseFillType();
		
		
		for (int i = 0; i < walls.size(); i++) {
			lightPts.clear();
			path.rewind();
			
			lightPts.add(walls.get(i).p1);
			lightPts.add(walls.get(i).p2);
			lightPts.add(Segment.raycast(new Segment(p.pos, lightPts.get(1)), edges));
			lightPts.add(Segment.raycast(new Segment(p.pos, lightPts.get(0)), edges));
			
			v1 = new PointF(p.pos.x - lightPts.get(2).x, p.pos.y - lightPts.get(2).y);
			v2 = new PointF(lightPts.get(3).x - p.pos.x, lightPts.get(3).y - p.pos.y);
			if (Segment.cross(v1, v2) > 0) {
				for (int j = 0; j < edges.size(); j++) { 
					float cross1 = Segment.cross(v1, new PointF(edges.get(j).p1.x - p.pos.x, edges.get(j).p1.y - p.pos.y));
					float cross2 = Segment.cross(v2, new PointF(edges.get(j).p1.x - p.pos.x, edges.get(j).p1.y - p.pos.y));
					if ((cross1 > 0 && cross2 > 0)) {
						lightPts.add((i==3&&lightPts.contains(edges.get(0).p1))?lightPts.size()-1:3 ,edges.get(j).p1);
					}
				}
			}
			else { 
				for (int j = 0; j < edges.size(); j++) { 
					float cross1 = Segment.cross(v1, new PointF(edges.get(j).p1.x - p.pos.x, edges.get(j).p1.y - p.pos.y));
					float cross2 = Segment.cross(v2, new PointF(edges.get(j).p1.x - p.pos.x, edges.get(j).p1.y - p.pos.y));
					if ((cross1 < 0 && cross2 < 0)) {
						lightPts.add(lightPts.size()-1 ,edges.get(j).p1);
					}
				}
			}
			
			path.moveTo(lightPts.get(0).x, lightPts.get(0).y);
			for (int j = 1; j < lightPts.size(); j++) {
				path.lineTo(lightPts.get(j).x, lightPts.get(j).y);
			}
			path.close();
			
//			float[] vertArray = new float[lightPts.size()*2];
//			for (int j = 0; j < lightPts.size(); j++) {
//				vertArray[j*2] = lightPts.get(j).x;
//				vertArray[j*2+1] = lightPts.get(j).y;
//			}
//			
//			pausePaint.setColor(SHADOW_COLOR);
//			c.drawVertices(VertexMode.TRIANGLE_FAN, lightPts.size()*2, vertArray, 0, null, 0, colorArray, 0, null, 0, 0, pausePaint);
			
			c.save();
			c.clipPath(path);
			c.drawColor(SHADOW_COLOR);
			c.restore();
		}

	}

	public void addObject(GameObject g) {
		addQueue.add(g);
	}

	public void removeObject(GameObject g) {
		removeQueue.add(g);
	}

	public void pause() {
		paused = true;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE
				|| event.getAction() == MotionEvent.ACTION_DOWN) {
			if (paused) {
				paused = false;
				return true;
			}
			int index = event.getActionIndex();
			PointerCoords coord = new PointerCoords();
			event.getPointerCoords(index, coord);
			p.setTarget(new PointF(coord.x, coord.y));
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			p.setTarget(null);
			return true;
		} else
			return false;
	}

}
