
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math;

public class ImageDisplay {
    BufferedImage imgOne;
    BufferedImage imgTwo;
    int width = 512;
    int height = 512;
    byte[] bytes = new byte[(int) width * height * 3];

    /**
     * Read Image RGB
     * Reads the image of given width and height at the given imgPath into the provided BufferedImage.
     */
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);
            raf.read(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void modify(int level, BufferedImage img3) {

        int ind = 0;
		double[][] r = new double[height][width];
		double[][] g = new double[height][width];
		double[][] b = new double[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
				r[i][j] = (int) (bytes[ind] & 0xff);
				g[i][j] = (int) (bytes[ind + height * width] & 0xff);
				b[i][j] = (int) (bytes[ind + height * width * 2] & 0xff);
                ind++;
            }
        }

        int size = 512;
        int newSize = (int) Math.pow(2, level);

        double[][] r_ = new double[height][width];
        double[][] g_ = new double[height][width];
        double[][] b_ = new double[height][width];

        for(int temp = size; temp > newSize; temp /= 2) {
            for (int i = 0; i < temp; i++) {
                for (int j = 0; j < temp; j += 2) {
                    r_[i][j / 2] = (r[i][j] + r[i][j + 1]) / 2;
                    g_[i][j / 2] = (g[i][j] + g[i][j + 1]) / 2;
                    b_[i][j / 2] = (b[i][j] + b[i][j + 1]) / 2;
                    r_[i][j / 2 + temp / 2] = (r[i][j] - r[i][j + 1]) / 2;
                    g_[i][j / 2 + temp / 2] = (g[i][j] - g[i][j + 1]) / 2;
                    b_[i][j / 2 + temp / 2] = (b[i][j] - b[i][j + 1]) / 2;
                }
            }
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    r[i][j] = r_[i][j];
                    g[i][j] = g_[i][j];
                    b[i][j] = b_[i][j];
                }
            }

            for (int j = 0; j < temp; j++) {
                for (int i = 0; i < temp; i += 2) {
                    r_[i / 2][j] = (r[i][j] + r[i + 1][j]) / 2;
                    g_[i / 2][j] = (g[i][j] + g[i + 1][j]) / 2;
                    b_[i / 2][j] = (b[i][j] + b[i + 1][j]) / 2;
                    r_[i / 2 + temp / 2][j] = (r[i][j] - r[i + 1][j]) / 2;
                    g_[i / 2 + temp / 2][j] = (g[i][j] - g[i + 1][j]) / 2;
                    b_[i / 2 + temp / 2][j] = (b[i][j] - b[i + 1][j]) / 2;
                }
            }
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    r[i][j] = r_[i][j];
                    g[i][j] = g_[i][j];
                    b[i][j] = b_[i][j];
                }
            }
        }

        double[][] i_r = new double[height][width];
        double[][] i_g = new double[height][width];
        double[][] i_b = new double[height][width];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i >= newSize || j >= newSize) {
                    r[i][j] = 0;
                    g[i][j] = 0;
                    b[i][j] = 0;
                }
            }
        }

        for(int temp = newSize; temp < size; temp *= 2) {
            for (int j = 0; j < temp * 2; j++) {
                for (int i = 0; i < temp; i++) {
                    i_r[2 * i][j] = r[i][j] + r[i + temp][j];
                    i_g[2 * i][j] = g[i][j] + g[i + temp][j];
                    i_b[2 * i][j] = b[i][j] + b[i + temp][j];
                    i_r[2 * i + 1][j] = r[i][j] - r[i + temp][j];
                    i_g[2 * i + 1][j] = g[i][j] - g[i + temp][j];
                    i_b[2 * i + 1][j] = b[i][j] - b[i + temp][j];
                }
            }
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    r[i][j] = i_r[i][j];
                    g[i][j] = i_g[i][j];
                    b[i][j] = i_b[i][j];
                }
            }

            for (int i = 0; i < temp * 2; i++) {
                for (int j = 0; j < temp; j++) {
                    i_r[i][2 * j] = r[i][j] + r[i][j + temp];
                    i_g[i][2 * j] = g[i][j] + g[i][j + temp];
                    i_b[i][2 * j] = b[i][j] + b[i][j + temp];
                    i_r[i][2 * j + 1] = r[i][j] - r[i][j + temp];
                    i_g[i][2 * j + 1] = g[i][j] - g[i][j + temp];
                    i_b[i][2 * j + 1] = b[i][j] - b[i][j + temp];
                }
            }
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    r[i][j] = i_r[i][j];
                    g[i][j] = i_g[i][j];
                    b[i][j] = i_b[i][j];
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pix3 = 0xff000000 | (((int) r[i][j] << 16) | ((int) g[i][j] << 8) | (int) b[i][j]);
                img3.setRGB(j, i, pix3);
            }
        }

    }
    public void showIms(String[] args, int level) {
        // Read in the specified image
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        imgTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imgOne);

        //Modify image
        modify(level, imgTwo);

        // Use label to display the image
        JFrame frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbIm1 = new JLabel(new ImageIcon(imgTwo));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        int level = Integer.parseInt(args[1]);
        if(level == -1) {
            for(int i=0; i<10; i++){
                ren.showIms(args, i);
            }
        } else {
            ren.showIms(args, level);
        }
    }
}
