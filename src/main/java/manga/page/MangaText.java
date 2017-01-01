package manga.page;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pixelitor.Composition;
import pixelitor.filters.painters.TextSettings;
import pixelitor.filters.painters.TranslatedTextPainter;
import pixelitor.layers.TextLayer;

/**
 * @author PuiWa
 *
 */
public class MangaText extends TextLayer {
	public MangaText(Composition comp, String name) {
		super(comp, name);
	}
	
	public String getText() {
		TextSettings settings = this.getSettings();
		return settings.getText();
	}
}
