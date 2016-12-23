package pixelitor.tools.shapes;

import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;

/**
 * @author PuiWa
 * Balloon shape based on https://commons.wikimedia.org/wiki/File:Speech_balloon.svg
 */
public class WordBalloon extends GeneralShape {
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
	}
}
