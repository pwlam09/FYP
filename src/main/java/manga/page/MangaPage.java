package manga.page;

import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_shape.ThinPlateSplineShapeTransformer;

import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.colors.FillType;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
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
	private Composition comp;	// a page refers to a composition
	
	private double topMargin;
	private double bottomMargin;
	private double leftMargin;
	private double rightMargin;

	private static int pageNum = 1;
	
	public MangaPage() {
		this.panels = new ArrayList<>();
		String title = "Page " + pageNum;
		pageNum++;
		this.comp = NewImage.addNewImage(FillType.WHITE, 600, 1000, title);
	}
	
	public Composition getComp() {
		return this.comp;
	}
	
	public ImageLayer addNewMangaPanel() {
		ImageLayer newLayer = this.comp.addNewEmptyLayer(null, false);
		panels.add(new MangaPanel(newLayer));
        return newLayer;
	}
	
	public ArrayList<MangaPanel> getPanels() {
		return this.panels;
	}
}
