package manga.process.video;

import java.io.File;
import java.sql.Date;
import java.util.Calendar;

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

/**
 * @author PuiWa
 *
 */
public class VideoFileChooser {
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/outfile.mp4";	//for testing
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/clip.mp4";	//for testing
	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/Spotlight/clip.mp4";	//for testing
	
	private static JFileChooser openFileChooser;
	
	private static File lastOpenDir = AppPreferences.loadLastOpenDir();
	
	public static final FileFilter mpegFilter = new FileNameExtensionFilter("MPEG files", "mp4");
	
	private static final FileFilter[] DEFAULT_OPEN_SAVE_FILTERS = {mpegFilter};
	private static final FileFilter[] NON_DEFAULT_OPEN_SAVE_FILTERS = {};
	
	public static final String[] SUPPORTED_INPUT_EXTENSIONS = {"mp4"};
	
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

            if (FileExtensionUtils.isSupportedExtension(fileName, VideoFileChooser.SUPPORTED_INPUT_EXTENSIONS)) {
            	// Get current time
            	long start = System.currentTimeMillis();
                
                /**
                 * @author PuiWa
                 * initialize the settings for manga
                 */
                MangaGenerator.preprocessing(selectedFile.getAbsolutePath());
//                AudioProcessor.extractAudio();
            	MangaGenerator.addMangaPages();
            	MangaGenerator.setAndDrawMangaPanels();
//            	SubtitleProcessor.printSubText();
//            	SubtitleProcessor.printSubText(VideoProcessor.getCurrTimestamp(), VideoProcessor.getEndTimestamp());
            	// balloon and text layer can be drawn at last to ensure they are on top of all layers, or they needed to be pushed to top layers
            	MangaGenerator.drawImgsToPanel();
            	MangaGenerator.drawWordBalloons();
            	MangaGenerator.pushBalloonsAndTextToTop();

            	// Get elapsed time in milliseconds
            	long elapsedTimeMillis = System.currentTimeMillis()-start;

            	// Get elapsed time in seconds
            	float elapsedTimeSec = elapsedTimeMillis/1000F%60;

            	// Get elapsed time in minutes
            	float elapsedTimeMin = elapsedTimeMillis/(60*1000F);
            	
                System.out.printf("Elapsed time: %d:%d.%d\n", (int)elapsedTimeMin, (int)elapsedTimeSec, (int)(elapsedTimeMillis % 1000F));
            } else { // unsupported extension
                handleUnsupportedExtensionLoading(fileName);
            }
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
