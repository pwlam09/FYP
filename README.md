# FYP Source Code

Welcome! This is the source code of my final year project.
 
It is a modified version of [Pixelitor](http://pixelitor.sourceforge.net/) with additional features to take a video input (mp4 with soft subtitle in mov_text format) and generate simple manga (Japanese comic).

## System Requirements

- [OpenCV 3.1.0](http://opencv.org/downloads.html) (include /path/to/opencv/build/x64/vc14/bin in system path)
- Eclipse with [m2e](http://www.eclipse.org/m2e/) plugin
- Network connection

## Compilation Instructions

1. Import the project as Maven project in Eclipse.
2. Right click project
3. Select Run As > Maven clean, wait until finish. 
4. Select Run As > Maven Install and again wait until finish (may take a few minutes to install dependencies).

## Running instructions

1. Find the main class **pixelitor.Pixelitor**.
2. Right click the main class > Run As > Java Application. You should see the pop-up window of an application, which is GUI of Pixelitor.
3. In Pixelitor, select File > Open Video
4. Select the video input you would like to process and click OK.

## Important Notes

- This program is tested under Windows. The performance is not guaranteed under other environments.
- Maximum time length of video input is 10 minutes.
- High quality video input takes much longer time (can be 30 minutes or more) to process. Please consider compressing the video if it is 720p or higher.
- For efficiency, the program will skip video analysis if associated .srt and .txt files for key frame extraction are detected. If you wish to rerun the key frame analysis tool, please remove the .srt and .txt file in the same directory of your input video.
- Error message related to "SLF4J" can be ignored.

## Basic Program Structure

List of new packages
- manga.detect: for face and mouth detection
- manga.element: major manga components
- manga.process.subtitle: for subtitle-related processing 
- manga.process.video: for video-related processing

Two additional new classes
- pixelitor.tools.shapes.WordBalloon
- pixelitor.layers.MangaText
  