package video.process;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.assertj.core.api.BooleanArrayAssert;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_shape.ThinPlateSplineShapeTransformer;
import org.bytedeco.javacpp.RealSense.frame;
import org.bytedeco.javacpp.RealSense.rs_context;
//import org.bytedeco.javacpp.opencv_core.Mat;
//import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.w3c.dom.css.Counter;

import com.sun.xml.internal.ws.api.model.wsdl.editable.EditableWSDLBoundFault;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;

import manga.page.MangaGenerator;
import pixelitor.layers.ImageLayer;
import subtitle.process.SubtitleProcessor;

/**
 * For subtitle and frame extraction using FFmpeg 
 * (absolute path for now and relative path should be used later)
 * 
 * @author PuiWa
 */
public class VideoProcessor {
	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/outfile.mp4";	//for testing
	private static String FFMPEGPath = "C:/PP_program/ffmpeg/ffmpeg-20160820-15dd56c-win64-static/bin";	//for testing
	
	/* internal time base = 1000000, stated under org.bytedeco.javacpp.avutil
	convert timestamp to seconds: divided by internal time base */
//	private static long currTimestamp = 6137000000L; 
	private static long currTimestamp = 6387223000L; 
	private static long endTimestamp = 6510000000L;
	private static boolean startedExtract;
	
	private VideoProcessor() {
		
	}
	
	/**
	 * extract 1st subtitle stream (SRT format) from mp4 video
	 */
	public static void extractSubtitle() {
		// get relative path of FFmpeg, under target/classes/ffmpeg, full path including "/ffpemg.exe"
//		ClassLoader classLoader = VideoProcessor.class.getClassLoader();
//		File ffmpegFile = new File(classLoader.getResource("ffmpeg/ffmpeg.exe").getFile());
//		FFMPEGPath = ffmpegFile.getAbsolutePath();
		
		File ffmpegFile = new File(VideoProcessor.class.getResource("/ffmpeg/ffmpeg.exe").getFile());
		FFMPEGPath = ffmpegFile.getAbsolutePath();
		
		// build process with command for extracting 1st subtitle stream from video (mp4 format), output file name=sub.srt, auto-overwrite
		try {
			ProcessBuilder pb = new ProcessBuilder(FFMPEGPath, "-i", TESTVIDEOPATH, "-an", "-vn", "-c:s:0", "srt", "-y", "sub.srt");
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.PIPE);
			Process p = pb.start();
			p.waitFor();
			// ffmpeg prints result to stderr, redirect to stdout
			BufferedReader err_reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String err_ine;
			while ((err_ine = err_reader.readLine()) != null) {
				System.out.println(err_ine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void extractSubtitleTimestamps() {
		
	}
	
	/**
	 * Extract keyframe through JavaCV wrapper of FFmpeg,
	 * The extracted keyframe is converted into BufferedImage to be drawn in a layer.
	 * <p>
	 * P.S. Setting and getting timestamp may not be accurate. 
	 * Retrieved frame varies, especially starting and ending frame, depending on
	 * starting and ending timestamp.
	 * Other frames remains unchanged.
	 * 
	 * @return BufferedImage of an extracted frame
	 */
	public static BufferedImage extractFrame() {
		BufferedImage img = null;
		//***************** JavaCV runtime error due to frame to image conversion ********************
		// solution found: the frame can only be accessed after grabber.start(), convert frame to image before grabber.stop()
		Java2DFrameConverter frameToImgConverter = new Java2DFrameConverter();
		
		FFmpegFrameGrabber ffmpegFrameGrabber = new FFmpegFrameGrabber(TESTVIDEOPATH);
		Frame ffmpegFrame = null;
		try {
			ffmpegFrameGrabber.start();
//			if (currTimestamp != 0L) {
				ffmpegFrameGrabber.setTimestamp(currTimestamp);
//			}
			if (!startedExtract) {
				startedExtract = true;
			} else {
				ffmpegFrame = ffmpegFrameGrabber.grabKeyFrame();	// skip current keyframe
			}
			ffmpegFrame = ffmpegFrameGrabber.grabKeyFrame();
			if (ffmpegFrame != null && ffmpegFrameGrabber.getTimestamp() <= endTimestamp) {
				img = frameToImgConverter.getBufferedImage(ffmpegFrame);
			}
//			ffmpegFrameGrabber.setTimestamp(3842000000L);
			// set timestamp for next keyframe
//			ffmpegFrame = ffmpegFrameGrabber.grabKeyFrame();	
			currTimestamp = ffmpegFrameGrabber.getTimestamp();
			
			ffmpegFrameGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		******************** Attempt of opencv, unknown runtime error, cannot open open video ***********************
//		OpenCVFrameGrabber opencvFrameGrabber = new OpenCVFrameGrabber(TESTVIDEOPATH);
//		OpenCVFrameConverter.ToMat opencvConverter = new OpenCVFrameConverter.ToMat();
//		Frame opencvFrame = null;
//		int counter = 0;
//		try {
//			opencvFrameGrabber.start();
//			opencvFrameGrabber.setTimestamp(22522083);
//			opencvFrame = opencvFrameGrabber.grabFrame();
//			System.out.println("frame.image: "+opencvFrame.image);
//			img = frameToImgConverter.getBufferedImage(opencvFrame);
//			opencvFrameGrabber.stop();
//			opencvFrameGrabber.release();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return img;
	}
	
	public static void resetCurrTimestamp(long currTimestamp) {
		VideoProcessor.currTimestamp = currTimestamp;
	}

	public static int getKeyFrameCount() {
		FFmpegFrameGrabber ffmpegFrameGrabber = new FFmpegFrameGrabber(TESTVIDEOPATH);
		int keyframeCount = 0;
		long backupTimestamp = currTimestamp;
		try {
			ffmpegFrameGrabber.start();
//			if (currTimestamp != 0L) {
				ffmpegFrameGrabber.setTimestamp(currTimestamp);
//			} 
//			Frame extractedFrame = ffmpegFrameGrabber.grabKeyFrame();
			Frame extractedFrame = null;
			while ((extractedFrame = ffmpegFrameGrabber.grabKeyFrame()) != null && 
					ffmpegFrameGrabber.getTimestamp() <= endTimestamp) {
				keyframeCount++;
//					System.out.printf("frameCount: %d, timestamp: %s\n", frameCount, currTimestamp+"");
			}
//			System.out.printf("last frameCount: %d, last timestamp: %s\n", frameCount, currTimestamp+"");
			ffmpegFrameGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return keyframeCount;
	}
	
	public static long getCurrTimestamp() {
		return currTimestamp;
	}
	
	public static long getNextKeyframeTimestamp(long timestamp) {
		FFmpegFrameGrabber ffmpegFrameGrabber = new FFmpegFrameGrabber(TESTVIDEOPATH);
		long nextTimestamp = 0L;
		try {
			ffmpegFrameGrabber.start();
			ffmpegFrameGrabber.setTimestamp(timestamp);
			ffmpegFrameGrabber.grabKeyFrame();
			ffmpegFrameGrabber.grabKeyFrame();	// jump to next keyframe
			nextTimestamp = ffmpegFrameGrabber.getTimestamp();
			ffmpegFrameGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nextTimestamp;
	}
	
	/**
	 * For testing
	 * Frame to subtitle time mismatch?
	 */
	public static void printFrameSubtitleTimeMatch(long sTimestamp, long eTimestamp) {
		FFmpegFrameGrabber ffmpegFrameGrabber = new FFmpegFrameGrabber(TESTVIDEOPATH);
		long frameTimestamp = 0L;
		try {
			ffmpegFrameGrabber.start();
			ffmpegFrameGrabber.setTimestamp(sTimestamp);
			ffmpegFrameGrabber.grabFrame();
			frameTimestamp = ffmpegFrameGrabber.getTimestamp();
//			System.out.printf("Frame Match Time: %s", SubtitleProcessor.timestampToTimeString(frameTimestamp));
			ffmpegFrameGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static long getEndTimestamp() {
		return endTimestamp;
	}
}
