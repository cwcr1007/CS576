
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.*;


public class VideoDisplay {

	JFrame frame;
	JLabel lbIm1;
	ArrayList<String> foregroundFileNameList;
	ArrayList<String> backgroundFileNameList;
	int width = 640; // default image width and height
	int height = 480;
	BufferedImage fgImage;
	BufferedImage bgImage;
	BufferedImage nextImage;

	public VideoDisplay(String[] args) {
		foregroundFileNameList = RetrieveFileNames(args[0]);
		backgroundFileNameList = RetrieveFileNames(args[1]);
		fgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		bgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		nextImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}

	private ArrayList<String> RetrieveFileNames(String videoPath) {
		ArrayList<String> fileNames = new ArrayList<String>();
		File[] files = new File(videoPath).listFiles();
		for (File file: files) {
			if (file.isFile()) {
				fileNames.add(videoPath + "/" + file.getName());
			}
		}
		Collections.sort(fileNames);
		return fileNames;
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public BufferedImage changeGreenScreen(String fgFileName, String bgFileName, BufferedImage fgImage, BufferedImage bgImage) {
		readImageRGB(width, height, fgFileName, fgImage);
		readImageRGB(width, height, bgFileName, bgImage);
		float[] hsv = new float[3];
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				Color c = new Color(fgImage.getRGB(j, i));
				int r = c.getRed();
				int g = c.getGreen();
				int b = c.getBlue();
				Color.RGBtoHSB(r, g, b, hsv);
				hsv[0] = hsv[0] * 180;
				if (hsv[0] >= 40 && hsv[0] <= 90 && hsv[1] >= 0.4 && hsv[2] >= 0.3 ) {
					fgImage.setRGB(j, i, bgImage.getRGB(j, i));
				}

			}
		}

		int[][] neighbors = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}};

		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				Color c = new Color(fgImage.getRGB(j, i));
				int r = c.getRed();
				int g = c.getGreen();
				int b = c.getBlue();
				Color.RGBtoHSB(r, g, b, hsv);
				hsv[0] = hsv[0] * 180;

				if(hsv[0] >= 40 && hsv[0] <= 90 && hsv[1] >= 0.15 && hsv[2] > 0.15){
					int count = 0;
					int R_sum = 0;
					int G_sum = 0;
					int B_sum = 0;
					for(int[] neighbor: neighbors){
						int neighbor_x = j + neighbor[0];
						int neighbor_y = i + neighbor[1];
						if(neighbor_x >= 0 && neighbor_x < width && neighbor_y >= 0 && neighbor_y < height) {
							int rgb = fgImage.getRGB(neighbor_x, neighbor_y);
							R_sum += (rgb >> 16) & 0xff;
							G_sum += (rgb >> 8) & 0xff;
							B_sum += rgb & 0xff;
							count++;
						}
					}

					int pix = 0xff000000 | (((R_sum/count) & 0xff) << 16) | (((G_sum/count) & 0xff) << 8) | ((B_sum/count) & 0xff);
					fgImage.setRGB(j, i, pix);

				}
			}
		}
		return fgImage;
	}

	public BufferedImage subtractStaticBackground(String fgFileName, String bgFileName, String nextFileName, BufferedImage fgImage, BufferedImage bgImage, BufferedImage nextImage) {

		readImageRGB(width, height, fgFileName, fgImage);
		readImageRGB(width, height, bgFileName, bgImage);
		readImageRGB(width, height, nextFileName, nextImage);

		int diff;
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				Color currColor = new Color(fgImage.getRGB(j, i));
				Color nextColor = new Color(nextImage.getRGB(j, i));

				diff = Math.abs(currColor.getRed() - nextColor.getRed()) + Math.abs(currColor.getGreen() - nextColor.getGreen()) + Math.abs(currColor.getBlue() - nextColor.getBlue());

				if(diff <= 5) {
					fgImage.setRGB(j, i, bgImage.getRGB(j, i));
				}
			}
		}
		return fgImage;
	}

	public void mode_1() throws InterruptedException {
		for (int i = 0; i < foregroundFileNameList.size(); i++) {
			changeGreenScreen(foregroundFileNameList.get(i), backgroundFileNameList.get(i), fgImage, bgImage);
			DisplayVideo(fgImage);
			Thread.sleep(41);
		}
	}

	public void mode_0() throws InterruptedException {
		for (int i = 0; i < foregroundFileNameList.size(); i++) {
			if(i == foregroundFileNameList.size() - 1) {
				subtractStaticBackground(foregroundFileNameList.get(i), backgroundFileNameList.get(i), foregroundFileNameList.get(i-1), fgImage, bgImage, nextImage);
			} else {
				//System.out.println(foregroundFileNameList.get(i) + " " + foregroundFileNameList.get(i+1));
				subtractStaticBackground(foregroundFileNameList.get(i), backgroundFileNameList.get(i), foregroundFileNameList.get(i+1), fgImage, bgImage, nextImage);
			}
			DisplayVideo(fgImage);
			Thread.sleep(41);
		}
	}

	public void showIms(){
		// Use label to display the image
		frame = new JFrame();
		lbIm1 = new JLabel();
		frame.setSize(width, height);

		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		frame.getContentPane().add(lbIm1);
		frame.setVisible(true);
	}

	public void DisplayVideo(BufferedImage img) {
		lbIm1.setIcon(new ImageIcon(img));
	}

	public static void main(String[] args) throws InterruptedException{
		VideoDisplay ren = new VideoDisplay(args);
		ren.showIms();
		int mode = Integer.parseInt(args[2]);

		if(mode == 1) {
			ren.mode_1();
		} else {
			ren.mode_0();
		}
	}
}
