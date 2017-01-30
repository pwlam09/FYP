package manga.element;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.Rect;

import pixelitor.Composition;
import pixelitor.layers.ImageLayer;

/**
 * @author PuiWa
 *
 */
public abstract class MangaElement extends ImageLayer {

	private Rectangle2D boundingRect;	//with top-left corner coordinates, positive x/y/w/h
	
	public MangaElement(Composition comp, BufferedImage pastedImage, String name, int width, int height, Rectangle2D boundingArea) {
		super(comp, pastedImage, name, width, height);
		boundingRect = boundingArea;
	}

	public void cut() {
		
	}

	public void paste() {
		
	}

	public void add() {
		
	}

	public void remove() {
		
	}

	public Rectangle2D getPosition() {
		return boundingRect;
	}

	public void setSize(Rectangle2D boundingArea) {
		boundingRect = boundingArea;
	}

	/**
	 * @param startX top-left x of ending position
	 * @param startY top-left y of ending position
	 */
	public void moveTo(int startX, int startY) {
		
	}

}
