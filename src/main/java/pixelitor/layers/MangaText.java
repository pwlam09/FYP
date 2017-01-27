package pixelitor.layers;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.swing.Painter;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.jdesktop.swingx.painter.AbstractLayoutPainter;
import org.jdesktop.swingx.painter.TextPainter;

import manga.page.MangaPanel;
import pixelitor.Composition;
import pixelitor.filters.comp.Flip;
import pixelitor.filters.comp.Rotate;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.filters.painters.TextAdjustmentsPanel;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedMangaTextPainter;
import pixelitor.filters.painters.TranslatedTextPainter;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.history.AddToHistory;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.History;
import pixelitor.history.NewLayerEdit;
import pixelitor.history.TextLayerChangeEdit;
import pixelitor.history.TextLayerRasterizeEdit;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.UpdateGUI;
import pixelitor.utils.Utils;
import pixelitor.utils.test.RandomGUITest;

/**
 * @author PuiWa
 *
 */
public class MangaText extends TextLayer {
	private MangaPanel panel;
	private static int textNum = 0;
	
	public MangaText(Composition comp, String name) {
		super(comp, "Text "+(++textNum), new TranslatedMangaTextPainter());
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
            new Font(Font.SANS_SERIF, Font.BOLD, 100),
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
