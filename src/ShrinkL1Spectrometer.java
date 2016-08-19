import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import javax.imageio.ImageIO;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;

public class ShrinkL1Spectrometer {
  public static void main(String[] argv) throws java.io.IOException {
    // Don't pop up Java dock icon just because we're using AWT classes
    System.setProperty("java.awt.headless", "true");

    if (argv.length != 2) {
      System.err.println("1st argument is path to .h5 file to read");
      System.err.println("2nd argument is dataset name (e.g. Sky_View_Factor)");
      System.exit(1);
    }
    File h5File = new File(argv[0]);
    String datasetName = argv[1];

    NetcdfFile ncfile = NetcdfFile.open(h5File.getAbsolutePath());

    Variable var = ncfile.findVariable(datasetName);
    //Array data = var.read();

    for (int layerNum : new int[] { 100, 200, 300, 400 }) {
      Array data;
      try {
        data = var.read(new int[]{layerNum, 0, 0}, new int[] {1, 13303, 987});
      } catch (ucar.ma2.InvalidRangeException e) {
        throw new RuntimeException(e);
      }
      System.out.println(Arrays.toString(var.getShape())); //[1, 13303, 987]
      int inputWidth = var.getShape()[2];
      int inputHeight = var.getShape()[1];
      //NCdumpW.printArray(data, "ATCOR_Input_File", System.out, null);
      //byte[] bytes = (byte[])data.get1DJavaArray(byte.class);
      short[] shorts = (short[])data.get1DJavaArray(short.class);

      short minValue = Short.MAX_VALUE;
      short maxValue = Short.MIN_VALUE;
      for (int i = 0; i < shorts.length; i++) {
        short value = shorts[i];

        if (value == 15000) {
          value = 0;
        }

        if (value < minValue) {
          minValue = value;
        }
        if (value > maxValue) {
          maxValue = value;
        }
      }
      System.out.println("minValue: " + minValue + ", maxValue: " + maxValue);

      byte[] bytes = new byte[shorts.length * 2];
      for (int i = 0; i < shorts.length; i++) {
        short value = shorts[i];
        if (value == 15000) {
          bytes[i * 2] = 0;
          bytes[i * 2 + 1] = 0;
        } else {
          bytes[i * 2] = (byte)(value * 255 / maxValue);
          bytes[i * 2 + 1] = (byte)255;
        }
      }

      ColorModel colorModel = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_GRAY),
        new int[] { 8, 8 }, true, true, Transparency.BITMASK, DataBuffer.TYPE_BYTE);
      SampleModel sm = colorModel.createCompatibleSampleModel(inputWidth, inputHeight);
      DataBuffer buffer = new DataBufferByte(bytes, bytes.length);
      WritableRaster raster = Raster.createWritableRaster(sm, buffer, null);
      BufferedImage image = new BufferedImage(colorModel, raster, false, null);

      File pngFile = new File("shrink_l1_spectrometer." + layerNum + ".png");
      ImageIO.write(image, "png", pngFile);
      System.err.println("Wrote " + pngFile);
    } // next layerNum

    ncfile.close();
  } // end main
}
