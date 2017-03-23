# FYP Source Code

Welcome! This is the source code of my final year project.
 
It is a modified version of [Pixelitor](http://pixelitor.sourceforge.net/) with additional features to take a video input (mp4 with soft subtitle in mov_text format) and generate simple manga (Japanes comic).

## System Requirements

- [OpenCV 3.1.0](http://opencv.org/downloads.html) (include /path/to/opencv/build/x64/vc14/bin in system path)
- [Maven](https://maven.apache.org/) (or m2e for Eclipse)
- Network connection

## Compilation Instructions

To start the program from an IDE, use **pixelitor.Pixelitor** as the main class.

## Running instructions

1. Move to project folder and run "mvn clean install" in command line. (For Eclipse, right click project and select Run As > Maven clean > Maven Install)
2. Compile the program
3. File > Open Video
4. Select the video input
5. Click OK

## Important Notes

- This program is tested under Windows. The performance is not guaranteed under other environments.
- Maximum time length of video input is 10 minutes.
- High quality video input takes much longer time (can be 30 minutes or more) to process. Please consider compressing the video if it is 720p or higher.