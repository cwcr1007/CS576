A sample program to read and display an image in JavaFX panels. By default, this program will read in the first frame of a given .rgb video file.


Unzip the folder to where you want.
To run the code from command line, first compile with:

>> javac ImageDisplay.java

and then, you can run it to take in two parameters "pathToRGBVideo", "Y", "U", "V", "Sw", "Sh", "A":

>> java ImageDisplay pathToRGBVideo Y U V Sw Sh A

where "Y", "U", "V" are integers control the subsampling of your Y U and V spaces respectively,
"Sw", "Sh" are single precision floats Sw and Sh which take positive values < 1.0,
"A" to suggest that antialiasing. 0 indicates no antialiasing and vice versa