package manga.page;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.colors.FillType;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

/**
 * This is for creating a page of manga.
 * A page contains a list of manga panels.
 * 
 * @author PuiWa
 */
public class MangaPage {
	private ArrayList<MangaPanel> panels;
	private double topMargin;
	private double bottomMargin;
	private double leftMargin;
	private double rightMargin;

	private static int pageNum = 1;
	
	public static Composition addNewMangaPage() {
		String title = "Page " + pageNum;
		pageNum++;
		return NewImage.addNewImage(FillType.WHITE, 600, 1000, title);
	}
}
