package video.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;

import org.opencv.core.Mat;

/**
 * @author PuiWa
 * For subtitle and frame extraction using FFmpeg 
 * (absolute path for now and relative path should be used later)
 */
public class VideoProcessor {
	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/outfile.mp4";	//for testing
	private static String FFMPEGPath = "C:/PP_program/ffmpeg/ffmpeg-20160820-15dd56c-win64-static/bin";	//for testing
	
	private VideoProcessor() {
		
	}
	
	/**
	 * extract 1st subtitle stream (SRT format) from mp4 video
	 */
	public static void extractSubtitle() {
		// get relative path of FFmpeg 
		File testf = null;
		try {
			testf = new File( VideoProcessor.class.getResource( "/ffmpeg" ).toURI() );
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		FFMPEGPath = testf.getAbsolutePath();
		
		// build process with command for extracting 1st subtitle stream from video (mp4 format), output file name=sub1.srt, auto-overwrite
		try {
			ProcessBuilder pb = new ProcessBuilder(FFMPEGPath+"/ffmpeg.exe", "-i", TESTVIDEOPATH, "-an", "-vn", "-c:s:0", "srt", "-y", "sub.srt");
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			Process p = pb.start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * extract all I-frame?
	 * originally thinking of OpenCV, right now FFmpeg seems more feasible? or JavaCV?
	 */
	public static void extractFrames() {
		
	}
}
