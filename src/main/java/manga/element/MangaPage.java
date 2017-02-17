package manga.element;

import java.util.ArrayList;

import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.colors.FillType;
import pixelitor.history.AddToHistory;
import pixelitor.layers.ImageLayer;

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

	private static int pageNum = 0;
	
	public MangaPage() {
		this.panels = new ArrayList<>();
		this.comp = NewImage.addNewImage(FillType.WHITE, 629, 877, "Page " + (++pageNum));
	}
	
	public Composition getComp() {
		return this.comp;
	}
	
	public ImageLayer addNewMangaPanel() {
		MangaPanel newPanel = new MangaPanel(this);
		panels.add(newPanel);
        return newPanel.getLayer();
	}
	
	public ArrayList<MangaPanel> getPanels() {
		return this.panels;
	}
	
	
	/**
	 * Set active MangaPanel layer
	 * 
	 * @param i the index of layer of the composition
	 */
	public ImageLayer setActivePanelLayer(int i) {
		MangaPanel selectedPanel = panels.get(i);
		selectedPanel.getLayer().makeActive(AddToHistory.YES);
		return selectedPanel.getLayer();
	}
}
