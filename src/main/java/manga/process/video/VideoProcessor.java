package manga.process.video;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

import org.mockito.internal.verification.api.VerificationDataInOrderImpl;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import manga.detect.Face;
import manga.detect.Speaker;
import manga.detect.SpeakerDetector;
import manga.process.subtitle.Subtitle;
import manga.process.subtitle.SubtitleProcessor;

/**
 * For subtitle and frame extraction using FFmpeg 
 * (absolute path for now and relative path should be used later)
 * 
 * @author PuiWa
 */
public class VideoProcessor {
	private static String FFMPEGPath = "C:/PP_program/ffmpeg/ffmpeg-20160820-15dd56c-win64-static/bin";	//for testing
	private static String videoSegmentationFilePath;	//for testing
	
	// time base = 1^(-3)s
	
	private static ArrayList<KeyFrame> allKeyFrames = new ArrayList<>(); 
	
	private VideoProcessor() {
		
	}
	
	public static void preprocessing(String videoPath) {
		extractKeyFrameInfo(videoPath);
		extractSRT(videoPath);
		// store key frames for later use
		allKeyFrames = extractKeyFrames(videoPath);
		for (KeyFrame keyFrame : allKeyFrames) {
			System.out.println(keyFrame);
		}
	}
	
	private static boolean extractKeyFrameInfo(String videoPath) {
		File videoSegmentationFile = new File(VideoProcessor.class.getResource("/video_segmentation/video_segmentation.exe").getFile());
		videoSegmentationFilePath = videoSegmentationFile.getAbsolutePath();
		
		if (!requiredDocsExist(videoPath)) {
			System.out.println("Extracting key frame information...");
			try {
				ProcessBuilder pb = new ProcessBuilder(videoSegmentationFilePath, videoPath);
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
		return true;
	}
	
	/**
	 * Check if clip_shots.txt is in the same directory of video
	 */
	private static boolean requiredDocsExist(String videoPath) {
		String clipShotsFilePath = null;
		if (videoPath.lastIndexOf("/") != -1) {
			clipShotsFilePath = videoPath.substring(0, videoPath.lastIndexOf("/"))+"/clip_shots.txt";
		} else {
			clipShotsFilePath = videoPath.substring(0, videoPath.lastIndexOf("\\"))+"/clip_shots.txt";
		}
		File videoShotFile = new File(clipShotsFilePath);
		if (videoShotFile.exists()) {
			return true;
		}
		return false;
	}

	/**
	 * extract 1st subtitle stream (SRT format) from mp4 video
	 */
	private static boolean extractSRT(String videoPath) {
		File ffmpegFile = new File(VideoProcessor.class.getResource("/ffmpeg/ffmpeg.exe").getFile());
		FFMPEGPath = ffmpegFile.getAbsolutePath();
		
		// build process with command for extracting 1st subtitle stream from video (mp4 format), output file name=sub.srt, auto-overwrite
		try {
			ProcessBuilder pb = new ProcessBuilder(FFMPEGPath, "-i", videoPath, "-an", "-vn", "-c:s:0", "srt", "-y", "sub.srt");
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
		return allKeyFrames.size();
	}
	
	public static ArrayList<KeyFrame> extractKeyFrames(String videoPath) {
		File opencvDll = new File(VideoProcessor.class.getResource("/opencv/opencv_java310.dll").getFile());
		String openCVPath = opencvDll.getAbsolutePath();
		System.load(openCVPath);
		
		ArrayList<KeyFrame> allFrameImgs = new ArrayList<>();
		VideoCapture vid = new VideoCapture(videoPath);
		File clipShots = new File(getVideoDirPath(videoPath)+"/clip_shots.txt");
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
				vid.set(Videoio.CAP_PROP_POS_FRAMES, Double.parseDouble(frameNumbers[2]));
				Mat opencvImg = new Mat();
				if (!vid.isOpened()) {
					System.out.println("error open video");
				} else {
					vid.read(opencvImg);
					if (opencvImg != null) {
						double cShotTimestamp = vid.get(Videoio.CAP_PROP_POS_MSEC);
						vid.set(Videoio.CAP_PROP_POS_FRAMES, Double.parseDouble(frameNumbers[0]));
						double sShotTimestamp = vid.get(Videoio.CAP_PROP_POS_MSEC);
						vid.set(Videoio.CAP_PROP_POS_FRAMES, Double.parseDouble(frameNumbers[1]));
						double eShotTimestamp = vid.get(Videoio.CAP_PROP_POS_MSEC);
//						System.out.println("stimestamp: "+sShotTimestamp+"");
//						System.out.println("ctimestamp: "+cShotTimestamp+"");
//						System.out.println("etimestamp: "+eShotTimestamp+"");
						allFrameImgs.add(new KeyFrame(opencvImg, cShotTimestamp, sShotTimestamp, eShotTimestamp));
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
	public static double getNextKeyframeTimestamp(double timestamp) {
		double nextTimestamp = 0L;
		for (KeyFrame aFrameImg: VideoProcessor.allKeyFrames) {
			if (aFrameImg.getcShotTimestamp() > timestamp) {
				nextTimestamp = aFrameImg.getcShotTimestamp();
				break;
			}
		}
		return nextTimestamp;
	}
	
	public static double getsShotTimestamp(double cShotTimestamp) {
		for (KeyFrame frameImg : VideoProcessor.allKeyFrames) {
			if (frameImg.getcShotTimestamp() == cShotTimestamp) {
				return frameImg.getsShotTimestamp();
			}
		}
		return 0;
	}

	public static double geteShotTimestamp(double cShotTimestamp) {
		for (KeyFrame frameImg : VideoProcessor.allKeyFrames) {
			if (frameImg.getcShotTimestamp() == cShotTimestamp) {
				return frameImg.geteShotTimestamp();
			}
		}
		return 0;
	}
	
	private static String getVideoDirPath(String videoPath) {
		int eIndex = -1;
		if (videoPath.lastIndexOf('/') != -1) {
			eIndex = videoPath.lastIndexOf('/');
		} else {
			eIndex = videoPath.lastIndexOf('\\');
		}
		String filepath = videoPath.substring(0, eIndex);
		return filepath;
	}

	public static ArrayList<KeyFrame> getKeyFrames() {
		return allKeyFrames;
	}
	
	public static void detectSpeakersPosition(String videoPath) {
		File opencvDll = new File(VideoProcessor.class.getResource("/opencv/opencv_java310.dll").getFile());
		String openCVPath = opencvDll.getAbsolutePath();
		System.load(openCVPath);
		
		ArrayList<Subtitle> allSubtitles = SubtitleProcessor.getAllSubtitles();
		VideoCapture vid = new VideoCapture(videoPath);
		int counter = 0;
		Mat opencvImg = new Mat();
		if (!vid.isOpened()) {
			System.out.println("error open video");
		} else {
			for (Subtitle subtitle:allSubtitles) {
				ArrayList<Mat> interFrames = new ArrayList<>();
				vid.set(Videoio.CAP_PROP_POS_MSEC, subtitle.geteTime());
				double eFrame = vid.get(Videoio.CAP_PROP_POS_FRAMES);
				vid.set(Videoio.CAP_PROP_POS_MSEC, subtitle.getsTime());
				double sFrame = vid.get(Videoio.CAP_PROP_POS_FRAMES);
				double frameInterval = (eFrame-sFrame) / 10;
				System.out.println("CAP_PROP_POS_MSEC: "+vid.get(Videoio.CAP_PROP_POS_MSEC));
				while (vid.get(Videoio.CAP_PROP_POS_FRAMES)<=eFrame) {
					vid.read(opencvImg);
					interFrames.add(opencvImg.clone());
					vid.set(Videoio.CAP_PROP_POS_FRAMES, vid.get(Videoio.CAP_PROP_POS_FRAMES)+frameInterval);
				}
				System.out.printf("Detecting speaker face ... %d%%\n", 
						(int) Math.floor(vid.get(Videoio.CAP_PROP_POS_FRAMES ) / vid.get(Videoio.CAP_PROP_FRAME_COUNT) * 100));
				Face speakerFace = SpeakerDetector.detectSpeakerFace(interFrames);
				if (speakerFace != null) {
					Speaker speaker = new Speaker(speakerFace);
//					System.out.println("speaker detected");
					subtitle.setSpeaker(speaker);
				} else {
					subtitle.setSpeaker(null);
				}
			}
		}
		vid.release();
	}
}
