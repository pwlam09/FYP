package manga.page;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.hamcrest.core.Is;

import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MangaText;
import pixelitor.tools.ShapeType;
import pixelitor.tools.shapes.WordBalloon;
import subtitle.process.SubtitleProcessor;

/**
 * @author PuiWa
 * This is for creating a manga balloon.
 * A balloon contains a balloon layer and MangaText layer.
 * The text is bounded by a rectangle within balloon (deadspace for lettering).
 */
public class MangaBalloon {
	private MangaPanel panel;	// the panel the balloon belongs to
	
	private MangaText mangaTextLayer;	// MangaText layer
	private ArrayList<String> subTextList;
	
	private ImageLayer balloonLayer;	// layer of balloon drawing
	private WordBalloon balloonRef;	// balloon object reference
	
	private static int balloonNum = 0;
	private static int textNum = 0;

	public MangaBalloon(MangaPanel panel, MangaText mangaTextLayer, WordBalloon balloonRef) {
		this.panel = panel;
		
		balloonNum++;
		this.balloonLayer = panel.getPage().getComp().addNewEmptyLayer("Balloon "+balloonNum, false);

		// MangaText extends TextLayer, hence use addLayer instead of addNewEmptyLayer
		textNum++;
		this.mangaTextLayer = mangaTextLayer;
		panel.getPage().getComp().addLayer(mangaTextLayer, AddToHistory.YES, "Text "+textNum, true, false);
		
		this.balloonRef = balloonRef;
		
		this.subTextList = new ArrayList<>();
	}

	public ImageLayer getBallloonLayer() {
		return balloonLayer;
	}
	
	public MangaText getMangaTextLayer() {
		return mangaTextLayer;
	}
	
	public Rectangle2D getBound() {
		return balloonRef.getBounds2D();
	}
	
	public String getLinkedText() {
		String linkedText = "";
		for (String str: subTextList) {
			linkedText = linkedText + str;
		}
		return linkedText;
	}
	
	private WordBalloon calculateWordBalloonRef(String balloonText) {
		Font testFont = new Font(Font.SANS_SERIF, Font.BOLD, 100);
		String testString = "testString";
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(testFont);
		int fontWidth = fm.stringWidth(testString);
		int fontHeight = fm.getHeight();
		System.out.println("fontWidth: "+fontWidth);
		System.out.println("fontHeight: "+fontHeight);
		return null;
	}
}
