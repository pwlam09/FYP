package manga.page;

import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.MangaText;
import pixelitor.selection.Selection;
import pixelitor.tools.ShapeType;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapes.WordBalloon;
import video.process.VideoProcessor;

/**
 * @author PuiWa
 *
 */
public class MangaPanel {
	private Composition comp;	// the composition the panel belongs to
	private ImageLayer layer;	// a panel refers to a layer
	private MangaPanelImage panelImg;
	private Rectangle2D bound;	//bounding rectangle of panel
	private ArrayList<MangaBalloon> balloonList;
	
	private static int panelCount = 1;
	
	public MangaPanel(Composition comp) {
		this.comp = comp;
		this.layer = comp.addNewEmptyLayer("Panel "+panelCount, false);
		panelCount++;
		this.panelImg = new MangaPanelImage();
		this.bound = new Rectangle2D.Float();
		this.balloonList = new ArrayList<>();
	}

	public void setBoudningRect(Rectangle2D boundingRect) {
		this.bound = boundingRect;
	}
	
	public Rectangle2D getBoundingRect() {
		return this.bound;
	}
	
	public ImageLayer getLayer() {
		return this.layer;
	}
	
	/**
	 * for testing purpose
	 */
	public void printBoundingRect() {
		System.out.println(this.bound);
	}
	
	public void setImage(MangaPanelImage panelImg) {
		this.panelImg = panelImg;
	}
	
    /**
     * Image fit to panel bounding rectangle, starts from top-left corner.
     * The image will be put into a new layer, bounded by the area within selection.
     */
    public void fitImageToPanelBound() {
    	// setup selection with the bounding rectangle of MangaPanel
        Selection selection = new Selection(bound, comp.getIC());
        comp.setNewSelection(selection);

        panelImg = new MangaPanelImage(comp, VideoProcessor.extractFrame(), bound);
        
        // image to new layer
        ImageLayer layer = panelImg.getLayer();
        
        layer.setImageWithSelection(panelImg.getSubImage());
        
        // Deselect. Don't use selection.die() directly, which leads to error
        comp.deselect(AddToHistory.NO);
    }
    
    public ImageLayer addMangaBalloonLayer() {
    	MangaText mangaTextLayer = new MangaText(comp, "Manga Text");
    	mangaTextLayer.setDefaultSetting();
    	mangaTextLayer.printTextBound();
    	mangaTextLayer.printMangaText();
    	
    	MangaBalloon balloon = new MangaBalloon(comp, mangaTextLayer, new WordBalloon(0, 0, 100, 100));
    	balloonList.add(balloon);
		return balloon.getLayer();
    }

	public ArrayList<MangaBalloon> getBalloons() {
		return balloonList;
	}
}
