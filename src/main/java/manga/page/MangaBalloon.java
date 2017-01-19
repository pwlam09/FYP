package manga.page;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MangaText;
import pixelitor.tools.ShapeType;
import pixelitor.tools.shapes.WordBalloon;

/**
 * @author PuiWa
 *
 */
public class MangaBalloon {
	private MangaText text;
	private ImageLayer layer;	// layer of balloon
	private WordBalloon balloon;
	private static int balloonNum = 0;
	private static int textNum = 0;

	public MangaBalloon(Composition comp, MangaText text, WordBalloon balloon) {
		// MangaText extends TextLayer, hence use addLayer instead of addNewEmptyLayer
		textNum++;
		this.text = text;
		comp.addLayer(text, AddToHistory.YES, "Text "+textNum, true, false);
		
		this.balloon = balloon;
		balloonNum++;
		this.layer = comp.addNewEmptyLayer("Balloon "+balloonNum, false);
	}

	public ImageLayer getLayer() {
		return layer;
	}
	
	public MangaText getMangaText() {
		return text;
	}
	
}
