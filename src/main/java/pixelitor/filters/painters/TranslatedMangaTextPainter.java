package pixelitor.filters.painters;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JComponent;

import org.jdesktop.swingx.painter.effects.AreaEffect;

import manga.element.MangaBalloon;
import manga.element.MangaGenerator;
import manga.element.MangaPanel;
import pixelitor.Composition;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapes.WordBalloon;
import pixelitor.tools.shapestool.ShapesTool;

/**
 * @author PuiWa
 *
 */
public class TranslatedMangaTextPainter extends TranslatedTextPainter {
	/**
	 * auto generated serial version uid
	 */
	private static final long serialVersionUID = -834154941565242740L;
	private Rectangle bound;
	private MangaPanel panel;
	private ImageLayer balloonLayer;
	
	// The LineBreakMeasurer used to line-break the paragraph.
    private LineBreakMeasurer lineMeasurer;    
    
    // index of the first character in the paragraph.
    private int paragraphStart;

    // index of the first character after the end of the paragraph.
    private int paragraphEnd;
    
	private AttributedString attrString;

	/**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Font font = calculateFont(component);
        if (font != null) {
            g.setFont(font);
        }

        Paint paint = getFillPaint();
        if (paint == null) {
            if (component instanceof JComponent) {
                paint = ((JComponent) component).getForeground();
            }
        }

        String text = calculateText(component);
        
        // get the font metrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        //Rectangle2D rect = metrics.getStringBounds(text,g);

        int tw = metrics.stringWidth(text);
        int th = metrics.getHeight();
        Rectangle res = calculateLayout(tw, th, width, height);
        
        // set painter position
        g.translate(res.x, res.y);

        if (isPaintStretched()) {
            paint = calculateSnappedPaint(paint, res.width, res.height);
        }

        if (paint != null) {
            g.setPaint(paint);
        }

//        g.drawString(text, 0, 0 + metrics.getAscent());
        
        // create AttributedString
        attrString = new AttributedString(text);
//        attrString.addAttribute(TextAttribute.FONT, font);
        
        // Create a new LineBreakMeasurer from the paragraph.
        // It will be cached and re-used.
        if (lineMeasurer == null) {
            AttributedCharacterIterator paragraph = attrString.getIterator();
            paragraphStart = paragraph.getBeginIndex();
            paragraphEnd = paragraph.getEndIndex();
            FontRenderContext frc = g.getFontRenderContext();
            lineMeasurer = new LineBreakMeasurer(paragraph, frc);
        }

        // Set break width to width of Component.
        float breakWidth = width;
        float drawPosY = 0;
        // Set position to the index of the first character in the paragraph.
        lineMeasurer.setPosition(paragraphStart);
        
        // Get lines until the entire paragraph has been displayed.
        while (lineMeasurer.getPosition() < paragraphEnd) {

            // Retrieve next layout. A cleverer program would also cache
            // these layouts until the component is re-sized.
            TextLayout layout = lineMeasurer.nextLayout(breakWidth);

            // Compute pen x position. If the paragraph is right-to-left we
            // will align the TextLayouts to the right edge of the panel.
            // Note: this won't occur for the English text in this sample.
            // Note: drawPosX is always where the LEFT of the text is placed.
            float drawPosX = layout.isLeftToRight()
                ? 0 : breakWidth - layout.getAdvance();

            // Move y-coordinate by the ascent of the layout.
            drawPosY += layout.getAscent();

            // Draw the TextLayout at (drawPosX, drawPosY).
            layout.draw(g, drawPosX, drawPosY);

            // Move y-coordinate in preparation for next layout.
            drawPosY += layout.getDescent() + layout.getLeading();
        }
        

		if (getAreaEffects() != null) {
			Shape shape = provideShape(g, component, width, height);
			for (AreaEffect ef : getAreaEffects()) {
				ef.apply(g, shape, width, height);
			}
		}
		
		// reset painter position
		g.translate(-res.x, -res.y);
    }
    
    public void setBound(Rectangle bound) {
    	this.bound = bound;
    }
}
