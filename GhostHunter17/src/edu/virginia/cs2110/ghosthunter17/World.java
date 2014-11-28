package edu.virginia.cs2110.ghosthunter17;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.virginia.cs2110.ghosthunter17.GameObject.Direction;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path.FillType;
import android.graphics.Path;
import android.graphics.Point;
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
			
			c.save();
			c.clipPath(path);
			c.drawColor(SHADOW_COLOR);
			c.restore();
		}
		
		
		
//		// Points to do a raycast to
//		ArrayList<PointF> castPoints = new ArrayList<PointF>();
//
//		// Points on the edges of the light
//		castPoints.add(new PointF(p.pos.x + 10000 * (float) Math.cos((p.getRot() + LIGHT_WIDTH/2) * Math.PI / 180),
//				p.pos.y + 10000	* (float) Math.sin((p.getRot() + LIGHT_WIDTH/2) * Math.PI	/ 180)));
//		castPoints.add(new PointF(p.pos.x + 10000 * (float) Math.cos((p.getRot() - LIGHT_WIDTH/2) * Math.PI / 180),
//				p.pos.y	+ 10000 * (float) Math.sin((p.getRot() - LIGHT_WIDTH/2) * Math.PI / 180)));
//		
//		// Add all the end points inside the width of the light
//		for (Segment s : walls) {
//			float ang1 = (float) (Math
//					.atan2(s.p1.y - p.pos.y, s.p1.x - p.pos.x) * 180 / Math.PI);
//			float ang2 = (float) (Math
//					.atan2(s.p2.y - p.pos.y, s.p2.x - p.pos.x) * 180 / Math.PI);
//			float angleDiff = (p.getRot() - ang1 + 180) % 360 - 180;
//			if (angleDiff <= LIGHT_WIDTH / 2 && angleDiff >= -LIGHT_WIDTH / 2) {
//				castPoints.add(s.p1);
//			}
//
//			angleDiff = (p.getRot() - ang2 + 180) % 360 - 180;
//			if (angleDiff <= LIGHT_WIDTH / 2 && angleDiff >= -LIGHT_WIDTH / 2) {
//				castPoints.add(s.p2);
//			}
//		}
//
//		// Sort by angle
//		Collections.sort(castPoints, new Comparator<PointF>() {
//
//			@Override
//			public int compare(PointF lhs, PointF rhs) {
//				double det = (lhs.x - p.pos.x) * (rhs.y - p.pos.y) - (lhs.y - p.pos.y) * (rhs.x - p.pos.x);
//				if (det > 0) {
//					return 1;
//				}
//				else if (det < 0) { 
//					return -1;
//				}
//				else { 
//					return 0;
//				}
//			}
//		});
//		
//		
//		
//		List<PointF> dPath = new ArrayList<PointF>();
//		Path path = new Path();
//		path.moveTo(p.pos.x, p.pos.y);
//		pausePaint.setColor(0xffff0000);
//		pausePaint.setStrokeWidth(10);
//		for (PointF cast : castPoints) {
//			c.drawLine(p.pos.x, p.pos.y, cast.x, cast.y, pausePaint);
//			PointF inter = Segment.raycast(new Segment(p.pos, cast), walls);
//			path.lineTo(inter.x, inter.y);
//			dPath.add(inter);
////			if (PointF.length(cast.x - p.pos.x, cast.y - p.pos.y) < PointF
////					.length(inter.x - p.pos.x, inter.y - p.pos.y)) {
////				path.lineTo(cast.x, cast.y);
////			}
//		}
//		path.close();
//		c.clipPath(path);

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
