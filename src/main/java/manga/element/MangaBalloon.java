package manga.element;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

import org.hamcrest.core.Is;

import manga.process.subtitle.SubtitleProcessor;
import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MangaText;
import pixelitor.tools.ShapeType;
import pixelitor.tools.shapes.WordBalloon;

/**
 * @author PuiWa
 * This is for creating a manga balloon.
 * A balloon contains a balloon layer and MangaText layer.
 * The text is bounded by a rectangle within balloon (dead space for lettering).
 */
public class MangaBalloon {
	private MangaPanel panel;	// the panel the balloon belongs to
	
	private MangaText mangaTextLayer;	// MangaText layer
	private ArrayList<String> subtitles;
	
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
		
		this.subtitles = new ArrayList<>();
	}

	public ImageLayer getBallloonLayer() {
		return balloonLayer;
	}
	
	public MangaText getMangaTextLayer() {
		return mangaTextLayer;
	}
	
	public static WordBalloon calculateWordBalloonRef(Rectangle2D availableBound, Rectangle2D textBound, Font font, String balloonText) {
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(font);
		int fontWidth = fm.stringWidth(balloonText);
		int fontHeight = fm.getHeight();
		
		AttributedString attrString = new AttributedString(balloonText);
	    int paragraphStart = 0;
	    int paragraphEnd = 0;
		LineBreakMeasurer lineMeasurer = null;
		
        attrString.addAttribute(TextAttribute.FONT, font);
        
        // Create a new LineBreakMeasurer from the paragraph.
        // It will be cached and re-used.
        if (lineMeasurer == null) {
            AttributedCharacterIterator paragraph = attrString.getIterator();
            paragraphStart = paragraph.getBeginIndex();
            paragraphEnd = paragraph.getEndIndex();
            FontRenderContext frc = fm.getFontRenderContext();
            lineMeasurer = new LineBreakMeasurer(paragraph, frc);
        }
		
        // Set break width to width of Component.
        float breakWidth = (float) textBound.getWidth();
        float drawPosY = 0;
        // Set position to the index of the first character in the paragraph.
        lineMeasurer.setPosition(paragraphStart);
        
        TextLayout layout = null;
        // Get lines until the entire paragraph has been displayed.
        while (lineMeasurer.getPosition() < paragraphEnd) {

            // Retrieve next layout. A cleverer program would also cache
            // these layouts until the component is re-sized.
            layout = lineMeasurer.nextLayout(breakWidth);

            // Compute pen x position. If the paragraph is right-to-left we
            // will align the TextLayouts to the right edge of the panel.
            // Note: this won't occur for the English text in this sample.
            // Note: drawPosX is always where the LEFT of the text is placed.
            float drawPosX = layout.isLeftToRight()
                ? 0 : breakWidth - layout.getAdvance();

            // Move y-coordinate by the ascent of the layout.
            drawPosY += layout.getAscent();

            // Move y-coordinate in preparation for next layout.
            drawPosY += layout.getDescent() + layout.getLeading();
        }
        drawPosY += layout.getAscent();
        drawPosY += layout.getDescent() + layout.getLeading();
		return new WordBalloon(availableBound.getX(), availableBound.getY(), availableBound.getWidth(), drawPosY);
	}
}
