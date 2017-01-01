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
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.tools.shapestool.TwoPointBasedPaint;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;

import java.awt.geom.Rectangle2D;
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
	private static ArrayList<ImageLayer> layerList=new ArrayList<>();
	private static ArrayList<Composition> compositionList=new ArrayList<>();
	
	
	private MangaGenerator() {
		
	}
	
	public static void addNewMangaPage() {
		String title = "Untitled" + untitledCount;
		compositionList.add(NewImage.addNewImage(FillType.WHITE, 600, 1000, title));
		untitledCount++;
	}
	
	public static ImageLayer addNewPanelLayer() {
        Composition comp = ImageComponents.getActiveCompOrNull();
        ImageLayer newLayer=comp.addNewEmptyLayer(null, false);
        layerList.add(newLayer);
        return newLayer;
	}
	
	/**
	 * Set active layer
	 * 
	 * @param i the index of layer of the current active composition
	 */
	public static void setActiveLayer(int i) {
		layerList.get(i).makeActive(AddToHistory.YES);
	}
	
	/**
	 * Set active internal frame (i.e. composition)
	 * 
	 * @param i the index of composition of the current active ImageComponent(IC)
	 */
	public static void setActiveComp(int i) {
		List<ImageComponent> icList = ImageComponents.getICList();
		Composition selectedComp = compositionList.get(i);
        for (ImageComponent ic : icList) {
        	if (ic.getComp() == selectedComp) {
        		ImageComponents.setActiveIC(ic, true);
        	}
        }
	}
	
	
	/**
	 * Draw 6 manga panels on different layers
	 */
	public static void drawMangaPanels() {
		// Declare 6 layers for 6 panels 
    	for (int i=0; i<6; i++) {
    		addNewPanelLayer();
    	}
    	
    	ImageLayer layer = layerList.get(0);	//Get 1st canvas for size reference
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
        
        // Add two panels (left and right) at a time
        for (int i=0; i<3; i++) {
            UserDrag leftPanelDrag = new UserDrag(canvasLeftRightMargin, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
            		canvasLeftRightMargin+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
            UserDrag rightPanelDrag = new UserDrag(canvasLeftRightMargin*2+mangaPanelWidth, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin, 
            		(canvasLeftRightMargin+mangaPanelWidth)*2, (canvasTopBottomMargin+mangaPanelHeight)*i+canvasTopBottomMargin+mangaPanelHeight);
            
            // Draw panels on different layers
            shapesTool.paintShapeOnIC(layerList.get(i*2), leftPanelDrag);
            shapesTool.paintShapeOnIC(layerList.get(i*2+1), rightPanelDrag);
        }
	}
	
	public static void drawWordBalloons() {
        ShapesTool shapesTool = Tools.SHAPES;
        shapesTool.setShapeType(ShapeType.WORDBALLOON);
        shapesTool.setAction(ShapesAction.FILL_AND_STROKE);
        shapesTool.setStrokFill(TwoPointBasedPaint.FOREGROUND);
        shapesTool.setFill(TwoPointBasedPaint.BACKGROUND);
        
        shapesTool.paintShapeOnIC(layerList.get(0), new UserDrag(0, 0, 200, 200));
	}
}
