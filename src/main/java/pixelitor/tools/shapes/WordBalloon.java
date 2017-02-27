package pixelitor.tools.shapes;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * @author PuiWa
 * Balloon shape based on https://commons.wikimedia.org/wiki/File:Speech_balloon.svg
 */
public class WordBalloon implements Shape {
	private Shape wordBalloon;
	private Rectangle2D textBound;
	
	public WordBalloon(double x, double y, double width, double height) {
		GeneralPath path = new GeneralPath();
		double tailStartX;
		double tailStartY;
		double arcStartX;
		double arcStartY;
		double startDeg;

		 // initial setting for positive width and height
		tailStartX = x + 0.89200864f * width;
		tailStartY = y + 0.94623656f * height;
		arcStartX = x;
		arcStartY = y;
		startDeg = -36.25;
		
		if (width < 0) {
			if (height < 0) {
				startDeg = 180 - Math.abs(startDeg);
			}
			else {
				startDeg = -121.61;
			}
		}
		else {
			if (height < 0) {
				startDeg = 180 - 121.61;
			}
		}
		
		if (width < 0) {
			arcStartX = x - Math.abs(width);
		}
		
		if (height < 0) {
			arcStartY = y - 0.83333333f*Math.abs(height);
		}
		
		Arc2D arc = new Arc2D.Double(arcStartX, arcStartY, Math.abs(width), 0.83333333f*Math.abs(height), startDeg, 337.86, Arc2D.OPEN);
		
		Point2D endPt1 = arc.getStartPoint();

		path = new GeneralPath(arc);
		
		path.lineTo((float)tailStartX, (float)tailStartY);
		path.lineTo((float)endPt1.getX(), (float)endPt1.getY());
		
		wordBalloon = path;
		
		// set text bound
		Rectangle2D arcBound = arc.getBounds2D();
		
		Ellipse2D ellipse = new Ellipse2D.Double(arc.getX() , arc.getY(), arcBound.getWidth(), arcBound.getHeight());
		
		double ellipseCentreX = ellipse.getCenterX();
		double ellipseCentreY = ellipse.getCenterY();
		
		double a = ellipse.getWidth() / 2;
		double b = ellipse.getHeight() / 2;
		
//		double textBoundX = ((a * b) / 
//				Math.sqrt(Math.pow(a, 2) * Math.pow(ellipse.getY()-ellipseCentreY, 2) + Math.pow(b, 2) * Math.pow(ellipse.getX()-ellipseCentreX, 2))) * 
//				(ellipse.getX()-ellipseCentreX) + ellipseCentreX;
//		double textBoundY = ((a * b) / 
//				Math.sqrt(Math.pow(a, 2) * Math.pow(ellipse.getY()-ellipseCentreY, 2) + Math.pow(b, 2) * Math.pow(ellipse.getX()-ellipseCentreX, 2))) * 
//				(ellipse.getY()-ellipseCentreY) + ellipseCentreY;
		
		double textBoundX = -a * Math.sqrt(2) / 2 + ellipseCentreX;
		double textBoundY = -b * Math.sqrt(2)/2 + ellipseCentreY;
		
//		double textBoundWidth = (textBoundX-ellipseCentreX) * 2;
//		double textBoundHeight = (textBoundY-ellipseCentreY) * 2;
		
		double textBoundWidth = a * Math.sqrt(2) / 2 * 2;
		double textBoundHeight = b * Math.sqrt(2) / 2 * 2;
		
		textBound = new Rectangle2D.Double(textBoundX, textBoundY, Math.abs(textBoundWidth), Math.abs(textBoundHeight));
	}
	
	public Rectangle2D getTextBound2D() {
		return textBound;
	}

	@Override
	public Rectangle getBounds() {
		// TODO Auto-generated method stub
		return wordBalloon.getBounds();
	}

	@Override
	public Rectangle2D getBounds2D() {
		// TODO Auto-generated method stub
		return wordBalloon.getBounds2D();
	}

	@Override
	public boolean contains(double x, double y) {
		// TODO Auto-generated method stub
		return wordBalloon.contains(x, y);
	}

	@Override
	public boolean contains(Point2D p) {
		// TODO Auto-generated method stub
		return wordBalloon.contains(p);
	}

	@Override
	public boolean intersects(double x, double y, double w, double h) {
		// TODO Auto-generated method stub
		return wordBalloon.intersects(x, y, w, h);
	}

	@Override
	public boolean intersects(Rectangle2D r) {
		// TODO Auto-generated method stub
		return wordBalloon.intersects(r);
	}

	@Override
	public boolean contains(double x, double y, double w, double h) {
		// TODO Auto-generated method stub
		return wordBalloon.contains(x, y, w, h);
	}

	@Override
	public boolean contains(Rectangle2D r) {
		// TODO Auto-generated method stub
		return wordBalloon.contains(r);
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		// TODO Auto-generated method stub
		return wordBalloon.getPathIterator(at);
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		// TODO Auto-generated method stub
		return wordBalloon.getPathIterator(at, flatness);
	}
}
