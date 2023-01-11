
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage imgOne;
	BufferedImage imgTemp;
	BufferedImage imgTwo;
	int width = 1920; // default image width and height
	int height = 1080;
	String imagePath;
	double[][][] YUV = new double[width][height][3];
	int Y_sampling;
	int U_sampling;
	int V_sampling;
	double scaledWidth;
	double scaledHeight;
	int ifAntialiasing;

	public ImageDisplay(String[] args) {
		this.imagePath = args[0];
		this.Y_sampling = Integer.valueOf(args[1]);
		this.U_sampling = Integer.valueOf(args[2]);
		this.V_sampling = Integer.valueOf(args[3]);
		this.scaledWidth = Double.parseDouble(args[4]);
		this.scaledHeight = Double.parseDouble(args[5]);
		this.ifAntialiasing = Integer.valueOf(args[6]);
		this.imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.imgTemp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, imagePath, imgOne);
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

	public void showIms(){

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));
		lbIm2 = new JLabel(new ImageIcon(imgTwo));

		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();

		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 0.5;
		c1.gridx = 0;
		c1.gridy = 0;

		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.anchor = GridBagConstraints.CENTER;
		c2.weightx = 0.5;
		c2.gridx = 1;
		c2.gridy = 0;
		frame.getContentPane().add(lbIm1, c1);
		frame.getContentPane().add(lbIm2, c2);

		frame.pack();
		frame.setVisible(true);
	}

	public void RGBtoYUV() {
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				int rgb = imgOne.getRGB(x, y);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				YUV[x][y][0] = 0.299 * r + 0.587 * g + 0.144 * b;
				YUV[x][y][1] = 0.596 * r - 0.274 * g - 0.322 * b;
				YUV[x][y][2] = 0.211 * r - 0.523 * g + 0.312 * b;
			}
		}
	}

	public void YUVtoRGB() {
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				double Y = YUV[x][y][0];
				double U = YUV[x][y][1];
				double V = YUV[x][y][2];

				int r = (int) Math.round(1 * Y + 0.956 * U + 0.621 * V);
				int g = (int) Math.round(1 * Y - 0.272 * U - 0.647 * V);
				int b = (int) Math.round(1 * Y - 1.106 * U + 1.703 * V);
				r = r < 0 ? 0 : r;
				r = r > 255 ? 255 : r;
				g = g < 0 ? 0 : g;
				g = g > 255 ? 255 : g;
				b = b < 0 ? 0 : b;
				b = b > 255 ? 255 : b;
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				imgTemp.setRGB(x, y, pix);
			}
		}
	}

	public void subSamplingProcessing() {
		for(int x=0; x<width; x++) {
			for (int y=0; y < height; y++) {
				if(x % Y_sampling != 0) {
					if((x / Y_sampling + 1) * Y_sampling < width){
						YUV[x][y][0] = (YUV[(x / Y_sampling) * Y_sampling][y][0] + YUV[(x / Y_sampling + 1) * Y_sampling][y][0]) / 2;
					}else {
						YUV[x][y][0] = YUV[(x / Y_sampling) * Y_sampling][y][0];
					}
				}
				if(x % U_sampling != 0) {
					if((x / U_sampling + 1) * U_sampling < width){
						YUV[x][y][1] = (YUV[(x / U_sampling) * U_sampling][y][1] + YUV[(x / U_sampling + 1) * U_sampling][y][1]) / 2;
					}else {
						YUV[x][y][1] = YUV[(x / U_sampling) * U_sampling][y][1];
					}
				}
				if(x % V_sampling != 0) {
					if((x / V_sampling + 1) * V_sampling < width){
						YUV[x][y][2] = (YUV[(x / V_sampling) * V_sampling][y][2] + YUV[(x / V_sampling + 1) * V_sampling][y][2]) / 2;
					}else {
						YUV[x][y][2] = YUV[(x / V_sampling) * V_sampling][y][2];
					}
				}
			}
		}
	}

	public void scaling() {
		int newWidth = (int) (scaledWidth * width);
		int newHeight = (int) (scaledHeight * height);
		imgTwo = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		int[][] neighbors = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}};
		for(int x=0; x<newWidth; x++) {
			for (int y=0; y < newHeight; y++) {
				int mapping_x = (int) (x * ((double) width / newWidth));
				int mapping_y = (int) (y * ((double) height / newHeight));
				if(ifAntialiasing == 1) {
					int count = 0;
					int R_sum = 0;
					int G_sum = 0;
					int B_sum = 0;
					for(int[] neighbor: neighbors){
						int neighbor_x = mapping_x + neighbor[0];
						int neighbor_y = mapping_y + neighbor[1];
						if(neighbor_x > 0 && neighbor_x < width && neighbor_y > 0 && neighbor_y < height) {
							int rgb = imgTemp.getRGB(neighbor_x, neighbor_y);
							R_sum += (rgb >> 16) & 0xff;
							G_sum += (rgb >> 8) & 0xff;
							B_sum += rgb & 0xff;
							count++;
						}
					}
					int pix = 0xff000000 | (((R_sum/count) & 0xff) << 16) | (((G_sum/count) & 0xff) << 8) | ((B_sum/count) & 0xff);
					imgTwo.setRGB(x, y, pix);
				} else {
					imgTwo.setRGB(x, y, imgTemp.getRGB(mapping_x, mapping_y));
				}
			}
		}
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay(args);
		ren.RGBtoYUV();
		ren.subSamplingProcessing();
		ren.YUVtoRGB();
		ren.scaling();
		ren.showIms();
	}

}
