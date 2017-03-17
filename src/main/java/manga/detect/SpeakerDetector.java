package manga.detect;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

/**
 * @author PuiWa
 *	Detect speaker in image
 */
public class SpeakerDetector {
	private static int subtitleCounter = 0;	//testing
	private static int absoluteFaceSize = 0;
	private static SpeakerDetector instance = new SpeakerDetector();
	private static CascadeClassifier frontalFaceDetector;
	private static CascadeClassifier profileFaceDetector;
	private static CascadeClassifier eyesDetector;
	private static CascadeClassifier leftEyeDetector;
	private static CascadeClassifier rightEyeDetector;
	private static CascadeClassifier mouthDetector;
	
	private SpeakerDetector() {
		System.load(SpeakerDetector.class.getResource("/opencv/opencv_java310.dll").getPath().substring(1));
		frontalFaceDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_frontalface_default.xml").getPath().substring(1));
		profileFaceDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_profileface.xml").getPath().substring(1));
		eyesDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_eye.xml").getPath().substring(1));
		leftEyeDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_mcs_lefteye.xml").getPath().substring(1));
		rightEyeDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_mcs_righteye.xml").getPath().substring(1));
		mouthDetector = new CascadeClassifier(
				SpeakerDetector.class.getResource("/opencv/haarcascade_mcs_mouth.xml").getPath().substring(1));
	}
	
	public static Speaker detectSpeaker(ArrayList<Mat> imgs) {
		System.load(SpeakerDetector.class.getResource("/opencv/opencv_java310.dll").getPath().substring(1));
		
		ArrayList<Face> allFaces = new ArrayList<>();
		ArrayList<CascadeClassifier> faceCascadeClassfiers = new ArrayList<>();
		faceCascadeClassfiers.add(frontalFaceDetector);
		faceCascadeClassfiers.add(profileFaceDetector);
		
		subtitleCounter++;
		
		for (int i=0; i<imgs.size(); i++) {
			// detect both frontal face and profile face from each image
			for (CascadeClassifier faceCascadeClassfier : faceCascadeClassfiers) {
				MatOfRect faceDetections = new MatOfRect();
				Mat grayImg = new Mat();
				Imgproc.cvtColor(imgs.get(i), grayImg, Imgproc.COLOR_BGR2GRAY);
				Imgproc.equalizeHist(grayImg, grayImg);
				if (absoluteFaceSize == 0)
				{
				    int height = grayImg.rows();
				    if (Math.round(height * 0.2f) > 0)
				    {
			            absoluteFaceSize = Math.round(height * 0.2f);
				    }
				}
				faceCascadeClassfier.detectMultiScale(grayImg, faceDetections, 1.1, 1, 0 | Objdetect.CASCADE_SCALE_IMAGE,
		        		new Size(absoluteFaceSize, absoluteFaceSize), new Size());
//		        System.out.println("face array size: "+faceDetections.toArray().length);
		        for (Rect faceRect : faceDetections.toArray()) {
		        	Face face = null;
		        	Rect detectedMouthBound = detectMouth(grayImg, faceRect);
		        	if (detectedMouthBound == null) {
		        		face = new Face(i, imgs.get(i), faceRect, null);
		        	} else {
		        		face = new Face(i, imgs.get(i), faceRect, new Mouth(detectedMouthBound));
		        	}
		        	if (faceCascadeClassfier.equals(frontalFaceDetector)) {
		        		// discard false positive
		        		if (frontalFaceHasEyes(grayImg, faceRect)) {
						    allFaces.add(face);
		        		}
		        	} else {
		        		// not checking for profile face eyes as it is not very accurate
//		        		if (profileFaceHasEye(grayImg, faceRect)) {
						    allFaces.add(face);
//		        		}
					}
		        }
			}
		}
		
		Face speakerFace = getSpeakerFace(groupPossibleFaces(allFaces));
		Mat img = null;
		Rect speakerMouthBound = null;
		Speaker speaker = null;
		if (speakerFace != null) {
			img = speakerFace.getImg();
			if (speakerFace.getMouth() != null) {
				speakerMouthBound = speakerFace.getMouth().getBound();
			}
			speaker = new Speaker(speakerFace);
		}
		return speaker;
	}

	private static HashMap<Face, ArrayList<Face>> groupPossibleFaces(ArrayList<Face> allFaces) {
		HashMap<Face, ArrayList<Face>> faceMap = new HashMap<>();
        
        ArrayList<Face> detectedFaces = allFaces;
		for (int i=0; i<detectedFaces.size(); i++) {
			if (!isGrouped(faceMap, detectedFaces.get(i))) {
				faceMap.put(detectedFaces.get(i), new ArrayList<>());
				Rectangle keyface = new Rectangle(detectedFaces.get(i).getBound().x, detectedFaces.get(i).getBound().y, 
						detectedFaces.get(i).getBound().width, detectedFaces.get(i).getBound().height);
				for (int j=0; j<detectedFaces.size(); j++) {
					if (!isGrouped(faceMap, detectedFaces.get(j))) {
						Rectangle faceToGroup = new Rectangle(detectedFaces.get(j).getBound().x, detectedFaces.get(j).getBound().y, 
								detectedFaces.get(j).getBound().width, detectedFaces.get(j).getBound().height);
						Rectangle faceIntersection = keyface.intersection(faceToGroup);
						if ((faceIntersection.width * faceIntersection.height >= keyface.width * keyface.height * 0.8)) {
							// check if the grouped faces are in consecutive frames
							ArrayList<Face> faceGroup = faceMap.get(detectedFaces.get(i));
							Face faceToCompare = detectedFaces.get(i);
							if (faceGroup.size() > 0) {
								faceToCompare = faceGroup.get(faceGroup.size()-1);
							}
							if (detectedFaces.get(j).follows(faceToCompare)) {
								faceGroup.add(detectedFaces.get(j));
								faceMap.put(detectedFaces.get(i), faceGroup);
							}
						}	
					}
				}
			}
		}
		
		return faceMap;
	}
	
	private static boolean isGrouped(HashMap<Face, ArrayList<Face>> faceMap, Face faceToCheck) {
		for (Map.Entry<Face, ArrayList<Face>> entry : faceMap.entrySet()) {
			Face keyFace = entry.getKey();
			ArrayList<Face> groupedFaces = entry.getValue();
			if (faceToCheck.equals(keyFace) || groupedFaces.contains(faceToCheck)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isMouthInGroupedFaces(ArrayList<Face> groupedFaces) {
		for (Face face:groupedFaces) {
			if (face.getMouth() != null) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get biggest face with mouth region detected as speaker,
	 * If no mouth region, get biggest detected face
	 * 
	 * @param faceMap
	 * @return
	 */
	private static Face getSpeakerFace(HashMap<Face, ArrayList<Face>> faceMap) {
		System.load(SpeakerDetector.class.getResource("/opencv/opencv_java310.dll").getPath().substring(1));
		
		Face selectedFace = null;
		double maxFaceArea = 0.0;
		for (Map.Entry<Face, ArrayList<Face>> entry : faceMap.entrySet()) {
			Face keyFace = entry.getKey();
			ArrayList<Face> groupedFaces = entry.getValue();
			
			// if no mouth detected
			if (keyFace.getMouth() == null && !isMouthInGroupedFaces(groupedFaces)) {
				selectedFace = keyFace;
				for (Face face : groupedFaces) {
					if (face.getBound().area() > maxFaceArea) {
						selectedFace = face;
					}
				}
			}
			
			// if there is any mouth region
			if (keyFace.getMouth() != null) {
				selectedFace = keyFace;
				maxFaceArea = keyFace.getBound().area();
			}
			if (isMouthInGroupedFaces(groupedFaces)) {
				for (Face face : groupedFaces) {
					if (face.getMouth() != null && face.getBound().area() > maxFaceArea) {
						selectedFace = face;
						maxFaceArea = selectedFace.getBound().area();
					}
				}
			}
		}
		return selectedFace;
		
		
		// *****************failed attempt to analyze lip movement*****************
//		HashMap<Face, Double> faceMSEMap = new HashMap<>();
//		
//		for (Map.Entry<Face, ArrayList<Face>> faceEntry : faceMap.entrySet()) {
//			Face keyFace = faceEntry.getKey();
//			Mat keyImg = keyFace.getImg();
//			Mouth keyFaceMouth = keyFace.getMouth();
//			
//			ArrayList<Face> groupedFaces = faceEntry.getValue();
//
//			// resize grouped faces of each key face to calculate Mean Squared Difference (MSD)
//			Mouth mouthRef = keyFaceMouth;
//			if (keyFaceMouth != null || (mouthRef = isMouthInGroupedFaces(groupedFaces)) != null) {
//				if (keyFaceMouth == null) {
//					keyFaceMouth = mouthRef;
//				}
//				Mat keyMouthImg = keyImg.submat(keyFaceMouth.getBound());
//				HashMap<Face, Double> msdMap = new HashMap<>();
//				
//				for (Face groupedFace : groupedFaces) {
//					if (groupedFace.getMouth() != null) {
//						Mat absDiffResult = new Mat();
//						Mat mouthImgResized = new Mat();
//						Size keyFaceMouthSize = keyFaceMouth.getBound().size();
//						Imgproc.resize(keyImg.submat(groupedFace.getMouth().getBound()), mouthImgResized, keyFaceMouthSize);
//						Core.absdiff(keyMouthImg, mouthImgResized, absDiffResult);
//						Scalar scalar = Core.sumElems(absDiffResult.mul(absDiffResult));
//						double sse = scalar.val[0] + scalar.val[1] + scalar.val[2];
//						double mse  = sse / (double)(keyMouthImg.channels() * keyMouthImg.total());
//						msdMap.put(groupedFace, mse);
//					}
//				}
//				Double maxMse = 0.0;
//				for (Map.Entry<Face, Double> msdEntry : msdMap.entrySet()) {
//					maxMse = Math.max(maxMse, msdEntry.getValue());
//				}
//				for (Map.Entry<Face, Double> msdEntry : msdMap.entrySet()) {
//					if (maxMse == msdEntry.getValue()) {
//						faceMSEMap.put(msdEntry.getKey(), maxMse);
//						break;
//					}
//				}
//			}
//		}
//
//		Double maxMse = 0.0;
//		for (Map.Entry<Face, Double> msdEntry : faceMSEMap.entrySet()) {
//			maxMse = Math.max(maxMse, msdEntry.getValue());
//		}
//		for (Map.Entry<Face, Double> msdEntry : faceMSEMap.entrySet()) {
//			if (maxMse == msdEntry.getValue()) {
//				return msdEntry.getKey();
//			}
//		}
		
//		return null;
	}
	
	private static boolean frontalFaceHasEyes(Mat img, Rect faceRect) {
		Rectangle upperface = new Rectangle(faceRect.x, faceRect.y, faceRect.width, faceRect.height/2);
		
		MatOfRect eyesDetections = new MatOfRect();
		eyesDetector.detectMultiScale(img, eyesDetections);
		
        for (Rect eyesRect : eyesDetections.toArray()) {
        	Rectangle2D eyesConvertedRect = new Rectangle2D.Double(eyesRect.x, eyesRect.y, eyesRect.width, eyesRect.height);
        	Rectangle2D faceEyesIntersection = eyesConvertedRect.createIntersection(upperface);
        	if (faceEyesIntersection.getWidth() * faceEyesIntersection.getHeight() >= eyesConvertedRect.getWidth() * eyesConvertedRect.getHeight() * 0.8) {
        		return true;
        	}
        }
        
		return false;
	}

	private static boolean profileFaceHasEye(Mat img, Rect faceRect) {
		Rectangle upperFaceLeft = new Rectangle(faceRect.x, faceRect.y, faceRect.width/2, faceRect.height/2);
		Rectangle upperFaceRight = new Rectangle(faceRect.x+faceRect.width/2, faceRect.y, faceRect.width/2, faceRect.height/2);
		
		ArrayList<CascadeClassifier> eyeCascadeClassfiers = new ArrayList<>();
		eyeCascadeClassfiers.add(leftEyeDetector);
		eyeCascadeClassfiers.add(rightEyeDetector);
		
		// detect both frontal face and profile face from each image
		for (CascadeClassifier eyeCascadeClassfier : eyeCascadeClassfiers) {
			MatOfRect eyeDetections = new MatOfRect();
			eyeCascadeClassfier.detectMultiScale(img, eyeDetections);
			Rectangle2D faceEyeIntersection = null;
	        for (Rect eyeRect : eyeDetections.toArray()) {
	        	Rectangle2D eyeConvertedRect = new Rectangle2D.Double(eyeRect.x, eyeRect.y, eyeRect.width, eyeRect.height);
	        	if (eyeCascadeClassfier.equals(leftEyeDetector)) {
		        	faceEyeIntersection = eyeConvertedRect.createIntersection(upperFaceLeft);
	        	} else {
	        		faceEyeIntersection = eyeConvertedRect.createIntersection(upperFaceRight);
	        	}
	        	if (faceEyeIntersection.getWidth() * faceEyeIntersection.getHeight() >= eyeConvertedRect.getWidth() * eyeConvertedRect.getHeight() * 0.8) {
	        		return true;
	        	}
	        }
		}
    
		return false;
	}
	
	public static ArrayList<Face> detectFaces(Mat img) {
		System.load(SpeakerDetector.class.getResource("/opencv/opencv_java310.dll").getPath().substring(1));
		
		ArrayList<Face> allFaces = new ArrayList<>();
		ArrayList<CascadeClassifier> faceCascadeClassfiers = new ArrayList<>();
		faceCascadeClassfiers.add(frontalFaceDetector);
		faceCascadeClassfiers.add(profileFaceDetector);

		// detect both frontal face and profile face from each image
		for (CascadeClassifier faceCascadeClassfier : faceCascadeClassfiers) {
			MatOfRect faceDetections = new MatOfRect();
			Mat grayImg = new Mat();
			Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_BGR2GRAY);
			Imgproc.equalizeHist(grayImg, grayImg);
			if (absoluteFaceSize == 0)
			{
			    int height = grayImg.rows();
			    if (Math.round(height * 0.2f) > 0)
			    {
		            absoluteFaceSize = Math.round(height * 0.2f);
			    }
			}
			faceCascadeClassfier.detectMultiScale(grayImg, faceDetections, 1.1, 1, 0 | Objdetect.CASCADE_SCALE_IMAGE,
	        		new Size(absoluteFaceSize, absoluteFaceSize), new Size());
	        for (Rect faceRect : faceDetections.toArray()) {
	        	Face face = null;
	        	Rect detectedMouthBound = detectMouth(grayImg, faceRect);
	        	// set framIndex to -1, not functional here, change?
	        	if (detectedMouthBound == null) {
	        		face = new Face(-1, img, faceRect, null);
	        	} else {
	        		face = new Face(-1, img, faceRect, new Mouth(detectedMouthBound));
	        	}
	        	if (faceCascadeClassfier.equals(frontalFaceDetector)) {
	        		// discard false positive
	        		if (frontalFaceHasEyes(grayImg, faceRect)) {
					    allFaces.add(face);
	        		}
	        	} else {
	        		// not checking for profile face eyes as it is not very accurate
//		        		if (profileFaceHasEye(grayImg, faceRect)) {
					    allFaces.add(face);
//		        		}
				}
	        }
		}
		
		return allFaces;
	}
	
	/**
	 * For frontal face only
	 * @param img
	 * @param faceRect
	 * @return
	 */
	private static Rect detectMouth(Mat img, Rect faceRect) {
		System.load(SpeakerDetector.class.getResource("/opencv/opencv_java310.dll").getPath().substring(1));
		
        MatOfRect mouthDetections = new MatOfRect();
        // mouth region detected may exceed faceRect, cannot use submat here
//        Mat roi = img.submat(faceRect);
        mouthDetector.detectMultiScale(img, mouthDetections);

		Rectangle upperface = new Rectangle(faceRect.x, faceRect.y, faceRect.width, faceRect.height/2);
		Rectangle lowerface = new Rectangle(faceRect.x, faceRect.y+faceRect.height/2, faceRect.width, faceRect.height/2);
		
		ArrayList<Rect> possibleMouths = new ArrayList<>();
        
		// find all possible mouths for a face
        for (Rect mouthRect : mouthDetections.toArray()) {
        	boolean isPossibleMouth = false;
        	// the top corners of mouth region are within face
    		if (faceRect.contains(new Point(mouthRect.x, mouthRect.y)) && faceRect.contains(new Point(mouthRect.x+mouthRect.width, mouthRect.y))) {
        		Rectangle mouthRectJava = new Rectangle(mouthRect.x, mouthRect.y, mouthRect.width, mouthRect.height);
        		Rectangle upperfaceIntersection = upperface.intersection(mouthRectJava);
        		Rectangle lowerfaceIntersection = lowerface.intersection(mouthRectJava);
//        		if ((upperfaceIntersection.width * upperfaceIntersection.height < mouthRectJava.width * mouthRectJava.height * 0.3) &&
//    				(lowerfaceIntersection.width * lowerfaceIntersection.height > mouthRectJava.width * mouthRectJava.height * 0.7)) {
//        			isPossibleMouth = true;
//        		}
        		// mouth largely intersects with lower part of face
        		if (lowerfaceIntersection.width * lowerfaceIntersection.height >= mouthRectJava.width * mouthRectJava.height * 0.7)
        			isPossibleMouth = true;
    		}
        	if (isPossibleMouth) {
        		possibleMouths.add(mouthRect);
        	}
        }
        
        Rect selectedMouth = null;
    	if (!possibleMouths.isEmpty()) {
	        selectedMouth = possibleMouths.get(0);
	        // if more than 1 mouths, select the one closest to center of lower part of face
	        if (possibleMouths.size() > 1) {
	        	double maxDiff = 10000.00;
	        	Point2D lowerfaceCentre = new Point2D.Double(lowerface.x+lowerface.width/2, lowerface.y+lowerface.height/2);
    	        for (Rect possibleMouthRect : possibleMouths) {
    	        	Point2D mouthCentre = new Point2D.Double(possibleMouthRect.x+possibleMouthRect.width/2, possibleMouthRect.y+possibleMouthRect.height/2);
    	        	if (Math.abs(mouthCentre.distance(lowerfaceCentre)) < maxDiff) {
    	        		maxDiff = Math.abs(mouthCentre.distance(lowerfaceCentre));
    	        		selectedMouth = possibleMouthRect;
    	        	}
    	        }
	        }
    	}
    	
    	return selectedMouth;
	}
	
	private static void printGroupedFaces(HashMap<Face, ArrayList<Face>> faceMap) {
		for (Map.Entry<Face, ArrayList<Face>> entry : faceMap.entrySet()) {
			Face keyFace = entry.getKey();
			ArrayList<Face> groupedFaces = entry.getValue();
			System.out.print("Key face: "+keyFace);
			System.out.println("No. of grouped faces: "+groupedFaces.size());
		}
	}
}
