package manga.element;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

import org.opencv.core.Rect;

import manga.detect.Face;
import manga.detect.Mouth;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.MangaText;
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
	
	private int balloonNum;
	
	private static int totalBalloonNum = 0;
	private static int textNum = 0;

	public MangaBalloon(MangaPanel panel, MangaText mangaTextLayer, WordBalloon balloonRef) {
		this.panel = panel;
		
		totalBalloonNum++;
		this.balloonLayer = panel.getPage().getComp().addNewEmptyLayer("Balloon "+totalBalloonNum, false);
		
		balloonNum = totalBalloonNum;

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
	
	public WordBalloon getBalloonRef() {
		return balloonRef;
	}

	public int getBalloonNum() {
		return balloonNum;
	}

	public static WordBalloon calculateInitWordBalloonRef(Rectangle2D panelBound, Face speakerFace, Font font, String balloonText) {
		// Initial top-left coordinates for word balloon
		double initialX = panelBound.getX();
		double initialY = panelBound.getY();
		
		ArrayList<Rectangle2D> rectsForBalloon = new ArrayList<>();
		
		if (speakerFace != null) {
			// Speaker face bound relative to panel bound
			Rectangle2D boundToAvoid = new Rectangle2D.Double(
					panelBound.getX()+speakerFace.getBound().x, 
					panelBound.getY()+speakerFace.getBound().y, 
					speakerFace.getBound().width, 
					speakerFace.getBound().height);
			
			// Allow more space for balloon if mouth is detected.
			// The region to avoid is resized to the mouth region.
			// Only mouth region and region above upper side of mouth
			// is avoided.
			if (speakerFace.getMouth() != null) {
				Rect speakerMouthBound = speakerFace.getMouth().getBound();
				boundToAvoid = new Rectangle2D.Double(
						panelBound.getX()+speakerMouthBound.x, 
						panelBound.getY()+speakerFace.getBound().y, 
						speakerMouthBound.width, 
						speakerMouthBound.y-speakerFace.getBound().y+speakerMouthBound.height);
			} else {
				Rect speakerFaceBound = speakerFace.getBound();
				
				// Estimate mouth region
				Rectangle2D speakerMouthBound = new Rectangle2D.Double(
						speakerFaceBound.x+speakerFaceBound.width/4.0, 
						speakerFaceBound.y+speakerFaceBound.height*2.0/3.0, 
						speakerFaceBound.width/2.0, 
						speakerFaceBound.height*1.0/6.0);
				boundToAvoid = new Rectangle2D.Double(
						panelBound.getX()+speakerMouthBound.getX(), 
						panelBound.getY()+speakerFaceBound.y, 
						speakerMouthBound.getWidth(), 
						speakerMouthBound.getY()-speakerFaceBound.y+speakerMouthBound.getHeight()/2);
			}
			
			// 4 biggest rectangles around the face bound within manga panel bound (can be empty rectangle)
			// For calculating space for word balloon
			Rectangle2D rectangleBottom = new Rectangle2D.Double(initialX, boundToAvoid.getMaxY(), 
					panelBound.getWidth(), panelBound.getMaxY()-boundToAvoid.getMaxY());
			Rectangle2D rectangleLeft = new Rectangle2D.Double(initialX, initialY, 
					boundToAvoid.getX()-initialX, panelBound.getHeight());
			Rectangle2D rectangleRight = new Rectangle2D.Double(boundToAvoid.getMaxX(), initialY, 
					panelBound.getMaxX()-boundToAvoid.getMaxX(), boundToAvoid.getHeight());
			
			rectsForBalloon.add(rectangleBottom);
			rectsForBalloon.add(rectangleLeft);
			rectsForBalloon.add(rectangleRight);
		}
		
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(font);
		int fontWidth = fm.stringWidth(balloonText);
		int fontHeight = fm.getHeight();
		
		double newBalloonW = 0.0;
		double newBalloonH = 0.0;
		
		WordBalloon currBalloon = new WordBalloon(initialX, initialY, newBalloonW, newBalloonH);
		
		double longestWordWidth = getLongestWordWidth(fm, balloonText);
		
		double newTextBoundW = longestWordWidth;
		float drawPosY = 0;
		
		// Check while balloon width <= panel width
		while (newBalloonW<=panelBound.getWidth()) {
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
	        float breakWidth = (float) newTextBoundW;
	        drawPosY = 0;
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
	        
	        // Remove extra new line and space
            //drawPosY = drawPosY - layout.getAscent() - layout.getDescent() - layout.getLeading();
            
            // Calculate new balloon width and height based on text bound
			newBalloonW = 2 * Math.sqrt(newTextBoundW / 2 * (newTextBoundW / 2 + drawPosY / 2));
			newBalloonH = 2 * Math.sqrt(drawPosY / 2 * (newTextBoundW / 2 + drawPosY / 2));
			currBalloon = new WordBalloon(initialX, initialY, newBalloonW, newBalloonH);
			
			// If balloon width is already shorter than balloon height and 
			// there is enough space for balloon placement, then return balloon.
			// Else increase balloon text bound and continue
			if (newBalloonH < newBalloonW && hasSpace(rectsForBalloon, currBalloon.getBounds2D())) {
				break;
			} else {
				newTextBoundW = newTextBoundW + (longestWordWidth / 10);
			}
		}
		
		return currBalloon;
	}

	private static boolean hasSpace(ArrayList<Rectangle2D> rectsForBalloon, Rectangle2D balloonBound) {
		if (rectsForBalloon.isEmpty() || rectsForBalloon.size() == 0)
			return true;
		
		for (Rectangle2D rect2D : rectsForBalloon) {
			Rectangle2D translatedBalloonBound = new Rectangle2D.Double(rect2D.getX(), rect2D.getY(), 
					balloonBound.getWidth(), balloonBound.getHeight());
			if (rect2D.contains(translatedBalloonBound))
				return true;
		}
		
		return false;
	}

	private static int getLongestWordWidth(FontMetrics fm, String balloonText) {
		String[] splitedTxt = balloonText.split(" ");
		int longestWordtWidth = fm.stringWidth("a"); 
		for (String str : splitedTxt) {
			int currWordWidth = fm.stringWidth(str);
			if (currWordWidth > longestWordtWidth) {
				longestWordtWidth = currWordWidth;
			}
		}
		return longestWordtWidth;
	}
}
