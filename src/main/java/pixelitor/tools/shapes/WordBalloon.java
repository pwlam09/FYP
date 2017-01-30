package pixelitor.tools.shapes;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.mockito.Matchers;

/**
 * @author PuiWa
 * Balloon shape based on https://commons.wikimedia.org/wiki/File:Speech_balloon.svg
 */
public class WordBalloon extends GeneralShape {
	private Rectangle2D textBound;
	
	public WordBalloon(double x, double y, double width, double height) {
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
		Point2D endPt2 = arc.getEndPoint();
		
		path.moveTo(endPt1.getX(), endPt1.getY());
		path.lineTo(tailStartX, tailStartY);
		path.lineTo(endPt2.getX(), endPt2.getY());
		
		path.append(arc, false);
		
		// set text bound
		Rectangle2D arcBound = arc.getBounds2D();
		
		Ellipse2D ellipse = new Ellipse2D.Double(arc.getX() , arc.getY(), arcBound.getWidth(), arcBound.getHeight());
		
		double ellipseCentreX = ellipse.getCenterX();
		double ellipseCentreY = ellipse.getCenterY();
		
		double a = ellipse.getWidth() / 2;
		double b = ellipse.getHeight() / 2;
		
		double textBoundX = ((a * b) / 
				Math.sqrt(Math.pow(a, 2) * Math.pow(ellipse.getY()-ellipseCentreY, 2) + Math.pow(b, 2) * Math.pow(ellipse.getX()-ellipseCentreX, 2))) * 
				(ellipse.getX()-ellipseCentreX) + ellipseCentreX;
		double textBoundY = ((a * b) / 
				Math.sqrt(Math.pow(a, 2) * Math.pow(ellipse.getY()-ellipseCentreY, 2) + Math.pow(b, 2) * Math.pow(ellipse.getX()-ellipseCentreX, 2))) * 
				(ellipse.getY()-ellipseCentreY) + ellipseCentreY;
		
		double textBoundWidth = (textBoundX-ellipseCentreX) * 2;
		double textBoundHeight = (textBoundY-ellipseCentreY) * 2;
		
		textBound = new Rectangle2D.Double(textBoundX, textBoundY, Math.abs(textBoundWidth), Math.abs(textBoundHeight));
	}
	
	public Rectangle2D getTextBound2D() {
		return textBound;
	}
}
