package manga.process.video;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

/**
 * For subtitle and frame extraction using FFmpeg 
 * (absolute path for now and relative path should be used later)
 * 
 * @author PuiWa
 */
public class VideoProcessor {
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/outfile.mp4";	//for testing
//	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/The_Imitation_Game/clip.mp4";	//for testing
	private static final String TESTVIDEOPATH = "C:/PP_file/cityuFYP/dl_dvd/Spotlight/clip.mp4";	//for testing
	private static String FFMPEGPath = "C:/PP_program/ffmpeg/ffmpeg-20160820-15dd56c-win64-static/bin";	//for testing
	private static String VIDEOSEGMENTATION;	//for testing
	
	// time base = 1^(-3)s
	
	private static ArrayList<FrameImage> frameImgs = new ArrayList<>(); 
	
	private VideoProcessor() {
		
	}
	
	public static void preprocessing() {
//		extractKeyFrameInfo();
		extractSRT();
		// store key frames for later use
		frameImgs = extractKeyFrames();
	}
	
	private static boolean extractKeyFrameInfo() {
		File VIDEOSEGMENTATIONFILE = new File(VideoProcessor.class.getResource("/video_segmentation/video_segmentation.exe").getFile());
		VIDEOSEGMENTATION = VIDEOSEGMENTATIONFILE.getAbsolutePath();
		
		try {
			ProcessBuilder pb = new ProcessBuilder(VIDEOSEGMENTATION, TESTVIDEOPATH);
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
		return true;
	}
	
	/**
	 * extract 1st subtitle stream (SRT format) from mp4 video
	 */
	private static boolean extractSRT() {
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
		return true;
	}
	
	public static int getKeyFrameCount() {
		return frameImgs.size();
	}
	
	public static ArrayList<FrameImage> extractKeyFrames() {
		File opencvDll = new File(VideoProcessor.class.getResource("/opencv/opencv_java310.dll").getFile());
		String openCVPath = opencvDll.getAbsolutePath();
//		System.load(System.getProperty("user.dir")+"/opencv_java310.dll");
		System.load(openCVPath);
		
		ArrayList<FrameImage> allFrameImgs = new ArrayList<>();
		VideoCapture vid = new VideoCapture(TESTVIDEOPATH);
		File clipShots = new File(getVideoDirPath()+"/clip_shots.txt");
//		System.out.println(getVideoDirPath()+"/clip_shots.txt");
		FileReader fr = null;
		try {
			fr = new FileReader(clipShots);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader br = new BufferedReader(fr);
		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				String[] frameNumbers = line.split(" ");
				vid.set(1, Double.parseDouble(frameNumbers[2]));
				Mat opencvImg = new Mat();
				if (!vid.isOpened()) {
					System.out.println("error open video");
				} else {
					vid.read(opencvImg);
					if (opencvImg != null) {
						long cShotTimestamp = (long) vid.get(0);
						vid.set(1, Double.parseDouble(frameNumbers[0]));
						long sShotTimestamp = (long) vid.get(0);
						vid.set(1, Double.parseDouble(frameNumbers[1]));
						long eShotTimestamp = (long) vid.get(0);
//						System.out.println("stimestamp: "+sShotTimestamp+"");
//						System.out.println("ctimestamp: "+cShotTimestamp+"");
//						System.out.println("etimestamp: "+eShotTimestamp+"");
						allFrameImgs.add(new FrameImage(opencvImg, cShotTimestamp, sShotTimestamp, eShotTimestamp));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		vid.release();
		return allFrameImgs;
	}
	
	/**
	 * Return timestamp of next key frame based on the timestamp provided.
	 * Return timestamp of last frame if it is the end of video. 
	 * @param timestamp timestamp of current key frame
	 * @return timestamp of next key frame
	 */
	public static long getNextKeyframeTimestamp(long timestamp) {
		long nextTimestamp = 0L;
		for (FrameImage aFrameImg: VideoProcessor.frameImgs) {
			if (aFrameImg.getcShotTimestamp() > timestamp) {
				nextTimestamp = aFrameImg.getcShotTimestamp();
				break;
			}
		}
		return nextTimestamp;
	}
	
	public static long getsShotTimestamp(long cShotTimestamp) {
		for (FrameImage frameImg : VideoProcessor.frameImgs) {
			if (frameImg.getcShotTimestamp() == cShotTimestamp) {
				return frameImg.getsShotTimestamp();
			}
		}
		return 0L;
	}

	public static long geteShotTimestamp(long cShotTimestamp) {
		for (FrameImage frameImg : VideoProcessor.frameImgs) {
			if (frameImg.getcShotTimestamp() == cShotTimestamp) {
				return frameImg.geteShotTimestamp();
			}
		}
		return 0L;
	}
	
	private static String getVideoDirPath() {
		int eIndex = VideoProcessor.TESTVIDEOPATH.lastIndexOf('/');
		String filepath = VideoProcessor.TESTVIDEOPATH.substring(0, eIndex);
		return filepath;
	}

	public static ArrayList<FrameImage> getKeyFrames() {
		return frameImgs;
	}
}
