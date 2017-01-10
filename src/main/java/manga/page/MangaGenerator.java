package manga.page;

import pixelitor.colors.FillType;
import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;
import pixelitor.tools.ShapeType;
import pixelitor.tools.ShapesAction;
import pixelitor.tools.Tools;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapes.WordBalloon;
import pixelitor.tools.shapestool.BasicStrokeJoin;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.tools.shapestool.TwoPointBasedPaint;
import video.process.VideoProcessor;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementPermission;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main controller of generating manga
 * 
 * @author PuiWa
 *
 */

public final class MangaGenerator {
	private static int untitledCount = 1;
	private static ArrayList<MangaPage> pageList=new ArrayList<>();
	
	private MangaGenerator() {
		
	}
	
	public static void addNewMangaPage() {
		MangaPage page = new MangaPage();
		pageList.add(page);
	}
	
	public static ImageLayer addNewMangaPanel() {
		ImageLayer newLayer = getActivePage().addNewMangaPanel();
        return newLayer;
	}
	
	public static MangaPage getActivePage() {
		Composition comp = ImageComponents.getActiveCompOrNull();
		for (MangaPage p: pageList) {
			if (p.getComp().equals(comp)) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Set active MangaPanel layer
	 * 
	 * @param i the index of layer of the current active composition
	 */
	public static void setActiveLayer(int i) {
		MangaPanel selectedPanel = getActivePage().getPanels().get(i);
		selectedPanel.getLayer().makeActive(AddToHistory.YES);
	}
	
	/**
	 * Set active internal frame (i.e. MangaPage composition)
	 * 
	 * @param i the index of MangaPage composition of the current active ImageComponent(IC)
	 */
	public static void setActiveComp(int i) {
		List<ImageComponent> icList = ImageComponents.getICList();
		Composition selectedComp = pageList.get(i).getComp();
        for (ImageComponent ic : icList) {
        	if (ic.getComp() == selectedComp) {
        		ImageComponents.setActiveIC(ic, true);
        	}
        }
	}
	
	
	/**
	 * Draw 6 manga panels on different MangaPanel layers
	 */
	public static void drawMangaPanels() {
		MangaPage activePage = getActivePage();
		ArrayList<MangaPanel> panels = activePage.getPanels();
		
		// Declare 6 layers for 6 panels 
    	for (int i=0; i<6; i++) {
    		activePage.addNewMangaPanel();
    	}
    	
    	ImageLayer layer = panels.get(0).getLayer();	//Get 1st canvas for size reference
        Canvas canvas = layer.getComp().getCanvas();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int canvasLeftRightMargin = (int)(canvas.getWidth()*0.05);
        int canvasTopBottomMargin = (int)(canvas.getHeight()*0.05);
        int mangaPanelWidth = (int)((canvas.getWidth()-canvasLeftRightMargin*3)/2);
        int mangaPanelHeight = (int)((canvas.getHeight()-canvasTopBottomMargin*4)/3);
        
        ShapesTool shapesTool = Tools.SHAPES;
        shapesTool.setShapeType(ShapeType.RECTANGLE);
        shapesTool.setAction(ShapesAction.STROKE);
        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
        // set stroke join and width
        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.MITER, 10);
        
        // Add two panels (left and right) at a time
        for (int i=0; i<3; i++) {
            UserDrag leftPanelDrag = new UserDrag(canvasLeftRightMargin, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
            		canvasLeftRightMargin+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
            UserDrag rightPanelDrag = new UserDrag(canvasLeftRightMargin*2+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
            		(canvasLeftRightMargin+mangaPanelWidth)*2, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
            
            // set bounding rectangle of the panels
            panels.get(i*2).setBoudningRect(leftPanelDrag.createPossiblyEmptyRect());
            panels.get(i*2+1).setBoudningRect(rightPanelDrag.createPossiblyEmptyRect());
            
            // Draw panels on different layers
            shapesTool.paintShapeOnIC(panels.get(i*2).getLayer(), leftPanelDrag);
            shapesTool.paintShapeOnIC(panels.get(i*2+1).getLayer(), rightPanelDrag);
        }
	}
	
	public static void drawWordBalloons() {
		MangaPage activePage = getActivePage();
		ImageLayer layer = activePage.getPanels().get(0).addWordBalloonLayer();
		System.out.println(activePage.getPanels().get(0).equals(layer));
		
        ShapesTool shapesTool = Tools.SHAPES;
        shapesTool.setShapeType(ShapeType.WORDBALLOON);
        shapesTool.setAction(ShapesAction.FILL_AND_STROKE);
        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
        shapesTool.setFill(TwoPointBasedPaint.BACKGROUND);
        // reset stroke join and width to default
        shapesTool.setStrokeJoinAndWidth(BasicStrokeJoin.ROUND, 5);
        
        shapesTool.paintShapeOnIC(layer, new UserDrag(0, 0, 200, 200));
	}
	
	public static void drawImgsToPanel() {
//        Composition comp = ImageComponents.getActiveCompOrNull();
//        ImageLayer activeLayer = (ImageLayer) comp.getActiveLayer();
//        ImageLayer newLayer = getActivePage().addNewMangaPanel();
//        BufferedImage img = VideoProcessor.extractFrame();
//        getActivePage().getPanels().get(2).fitImageToPanelBound();
        System.out.println(getActivePage().getPanels().size());
        for (int i = 0; i<getActivePage().getPanels().size(); i++) {
        	MangaPanel panel = getActivePage().getPanels().get(i);
        	panel.fitImageToPanelBound();
        }
//        if (img != null)
//        	newLayer.setImage(img);
	}
}
