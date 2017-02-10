package pixelitor.layers;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jdesktop.swingx.painter.AbstractLayoutPainter;

import pixelitor.Composition;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedMangaTextPainter;
import pixelitor.tools.shapes.WordBalloon;

/**
 * @author PuiWa
 *
 */
public class MangaText extends TextLayer {
	private WordBalloon balloonRef;
	private static int textNum = 0;
	
	public MangaText(Composition comp, WordBalloon balloonRef) {
		super(comp, "Text "+(++textNum), new TranslatedMangaTextPainter());
		this.balloonRef = balloonRef;
	}
	
    /**
     * Override to set the bounding paint area within balloon reference.
     * @see pixelitor.layers.TextLayer#paintLayerOnGraphics(java.awt.Graphics2D, boolean)
     */
    @Override
    public void paintLayerOnGraphics(Graphics2D g, boolean firstVisibleLayer) {
    	getTranslatedTextPainter().setFillPaint(getSettings().getColor());
//    	getTranslatedTextPainter().paint(g, null, comp.getCanvasWidth(), comp.getCanvasHeight());
    	Rectangle2D textBound = balloonRef.getTextBound2D();
//    	System.out.println(bound);
    	getTranslatedTextPainter().paint(g, null, (int)textBound.getWidth(), (int)textBound.getHeight());
    }

	public String getText() {
		return getSettings().getText();
	}
	
	public void setText(String text) {
		getSettings().setText(text);
	}
	
	public void setDefaultSetting() {
        this.setSettings(new TextSettings(
            "Default",
            new Font(Font.SANS_SERIF, Font.BOLD, 14),
            new Color(0, 0, 0),
            new AreaEffects(),
            AbstractLayoutPainter.HorizontalAlignment.LEFT,
            AbstractLayoutPainter.VerticalAlignment.TOP,
            false
        ));
	}
	
	/**
	 * for testing
	 */
	public void printMangaText() {
		System.out.println("Name: " + getName());
		System.out.println("Font: "+ getSettings().getFont());
//		calculateFontFitToBalloon();
	}
	
	private void calculateFontFitToBalloon() {
		Font testFont = new Font(Font.SANS_SERIF, Font.BOLD, 100);
		String testString = "testString";
		Canvas c = new Canvas();
		FontMetrics fm = c.getFontMetrics(testFont);
		int fontWidth = fm.stringWidth(testString);
		int fontHeight = fm.getHeight();
		System.out.println("fontWidth: "+fontWidth);
		System.out.println("fontHeight: "+fontHeight);
	}
}
