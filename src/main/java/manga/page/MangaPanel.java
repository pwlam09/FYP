package manga.page;

import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.Selection;
import pixelitor.tools.UserDrag;

/**
 * @author PuiWa
 *
 */
public class MangaPanel {
	private ArrayList<MangaBalloon> balloons;
	private MangaPanelImage panelImg;
	private ImageLayer layer;	// a panel refers to a layer
	private Rectangle2D boundingRect;	//bounding rectangle of panel
	
	public MangaPanel(ImageLayer layer) {
		this.layer = layer;
	}
	
	public MangaPanel(ImageLayer layer, Rectangle2D boundingRect) {
		this.layer = layer;
		this.boundingRect = boundingRect;
	}
	
	public void setBoudningRect(Rectangle2D boundingRect) {
		this.boundingRect = boundingRect;
	}
	
	public Rectangle2D getBoundingRect() {
		return this.boundingRect;
	}
	
	public ImageLayer getLayer() {
		return this.layer;
	}
	
	/**
	 * for testing purpose
	 */
	public void printBoundingRect() {
		System.out.println(this.boundingRect);
	}
	
	public void setImage(MangaPanelImage panelImg) {
		this.panelImg = panelImg;
	}
	
    public void fitImageToPanelBound(BufferedImage img) {
    	Composition activeComp = MangaGenerator.getActivePage().getComp();
        Selection selection = new Selection(boundingRect, activeComp.getIC());
        activeComp.setNewSelection(selection);
        ImageLayer layer = (ImageLayer) activeComp.getActiveLayer();
        layer.setImageWithSelection(img);
    }
}
