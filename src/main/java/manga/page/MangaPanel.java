package manga.page;

import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.Selection;
import pixelitor.tools.UserDrag;
import video.process.VideoProcessor;

/**
 * @author PuiWa
 *
 */
public class MangaPanel {
	private ArrayList<MangaBalloon> balloons;
	private MangaPanelImage panelImg;
	private ImageLayer layer;	// a panel refers to a layer
	private Rectangle2D boundingRect;	//bounding rectangle of panel
	private Composition comp;	// the composition the panel belongs to
	
	public MangaPanel(ImageLayer layer) {
		this.layer = layer;
		this.panelImg = new MangaPanelImage();
		this.boundingRect = new Rectangle2D.Float();
	}
	
	public MangaPanel(ImageLayer layer, Rectangle2D boundingRect) {
		this.layer = layer;
		this.boundingRect = boundingRect;
	}
	
	public MangaPanel(Composition comp, ImageLayer layer) {
		this.comp = comp;
		this.layer = layer;
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
	
    /**
     * Image fit to panel bounding rectangle, starts from top-left corner.
     * The image will be put into a new layer, bounded by the area within selection.
     */
    public void fitImageToPanelBound() {
    	Composition activeComp = MangaGenerator.getActivePage().getComp();
    	
    	// setup selection with the bounding rectangle of MangaPanel
        Selection selection = new Selection(boundingRect, activeComp.getIC());
        activeComp.setNewSelection(selection);

        panelImg = new MangaPanelImage(VideoProcessor.extractFrame(), boundingRect);
        
        // image to new layer
        ImageLayer layer = panelImg.getLayer();
        
        layer.setImageWithSelection(panelImg.getSubImage());
        
        // Deselect. Don't use selection.die() directly, which leads to error
        activeComp.deselect(AddToHistory.NO);
    }
    
    public ImageLayer addWordBalloonLayer() {
		ImageLayer newLayer = comp.addNewEmptyLayer("Balloon", false);
        return newLayer;
    }
}
