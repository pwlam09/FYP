package manga.process.video;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import manga.element.MangaGenerator;
import pixelitor.gui.GlobalKeyboardWatch;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.io.FileChoosers;
import pixelitor.io.FileExtensionUtils;
import pixelitor.io.OpenSaveManager;
import pixelitor.utils.AppPreferences;
import pixelitor.utils.Messages;

public class VideoFileChooser {
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/outfile.mp4";	//for testing
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/clip.mp4";	//for testing
	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/Spotlight/clip.mp4";	//for testing
	
	private static JFileChooser openFileChooser;
	
	private static File lastOpenDir = AppPreferences.loadLastOpenDir();
	
	public static final FileFilter mpegFilter = new FileNameExtensionFilter("MPEG files", "mp4");
	
	private static final FileFilter[] DEFAULT_OPEN_SAVE_FILTERS = {mpegFilter};
	private static final FileFilter[] NON_DEFAULT_OPEN_SAVE_FILTERS = {};
	
	private VideoFileChooser() {}
	
    private static void initOpenFileChooser() {
        assert SwingUtilities.isEventDispatchThread();
        if (openFileChooser == null) {
            //noinspection NonThreadSafeLazyInitialization
            openFileChooser = new JFileChooser(lastOpenDir);
            openFileChooser.setName("open");

            setDefaultOpenExtensions();
        }
    }
    
    public static void open() {
        initOpenFileChooser();

        GlobalKeyboardWatch.setDialogActive(true);
        int status = openFileChooser.showOpenDialog(PixelitorWindow.getInstance());
        GlobalKeyboardWatch.setDialogActive(false);

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openFileChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            lastOpenDir = selectedFile.getParentFile();

            /**
             * @author PuiWa
             * initialize the settings for manga
             */
            MangaGenerator.preprocessing(selectedFile.getAbsolutePath());
//            AudioProcessor.extractAudio();
        	MangaGenerator.addNewMangaPage();
        	MangaGenerator.drawMangaPanels();
//        	SubtitleProcessor.printSubText();
//        	SubtitleProcessor.printSubText(VideoProcessor.getCurrTimestamp(), VideoProcessor.getEndTimestamp());
        	// balloon and text layer can be drawn at last to ensure they are on top of all layers, or they needed to be pushed to top layers
        	MangaGenerator.drawImgsToPanel();
        	MangaGenerator.drawWordBalloons();
        	MangaGenerator.pushBalloonsAndTextToTop();
        } else if (status == JFileChooser.CANCEL_OPTION) {
            // cancelled
        }
    }
    
    private static void handleUnsupportedExtensionLoading(String fileName) {
        String extension = FileExtensionUtils.getFileExtension(fileName);
        String msg = "Could not load " + fileName + ", because ";
        if (extension == null) {
            msg += "it has no extension.";
        } else {
            msg += "files of type " + extension + " are not supported.";
        }
        Messages.showError("Error", msg);
    }
    
    private static void setDefaultOpenExtensions() {
        addDefaultFilters(openFileChooser);
    }
    
    private static void addDefaultFilters(JFileChooser chooser) {
        // remove first the non-default filters in case they are there
        for (FileFilter filter : NON_DEFAULT_OPEN_SAVE_FILTERS) {
            chooser.removeChoosableFileFilter(filter);
        }

        for (FileFilter filter : DEFAULT_OPEN_SAVE_FILTERS) {
            chooser.addChoosableFileFilter(filter);
        }
    }
}
