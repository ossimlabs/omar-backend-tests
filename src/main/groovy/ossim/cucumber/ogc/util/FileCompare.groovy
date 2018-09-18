package ossim.cucumber.ogc.util

import com.sun.istack.internal.NotNull
import org.apache.commons.io.FileUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import static java.lang.Math.sqrt

class FileCompare
{
    FileCompare() {}

    static boolean checkImages(URL filePath1, URL filePath2, String image_type=null)
    {
        println "\nFileCompare::checkImages(URL,URL)"
        println ">>>    ${filePath1}"
        println ">>>    ${filePath2}"
        String suffix = image_type ? ".${image_type}" : ""
        File file1 = File.createTempFile("tempImage1", suffix)
        File file2 = File.createTempFile("tempImage2", suffix)

        FileUtils.copyURLToFile(filePath1, file1)
        FileUtils.copyURLToFile(filePath2, file2)

        boolean imagesEqual = checkImages(file1, file2)

//        file1.deleteOnExit()
//        file2.deleteOnExit()

        return imagesEqual
    }

    static boolean checkImages(File fileA, File fileB)
    {
      if (!fileA.exists() || !fileB.exists())
          return 0

      println "###    ${fileA}"
      println "###    ${fileB}"
      double correlation = 0
      try
      {
         // Take buffer data from both image files.
         BufferedImage biA = ImageIO.read(fileA)
         DataBuffer dbA = biA.getData().getDataBuffer()
         int sizeA = dbA.getSize()
         BufferedImage biB = ImageIO.read(fileB)
         DataBuffer dbB = biB.getData().getDataBuffer()
         int sizeB = dbB.getSize()
         if (sizeA != sizeB)
         {
            System.out.println("Images are not of same size")
            return 0
         }

         // Compare data-buffer objects by computing normalized cross-correlation:
         double a=0, b=0, sumA2=0, sumB2=0, sumAB=0
         for (int i = 0; i < sizeA; i++)
         {
            a = dbA.getElem(i);
            b = dbB.getElem(i);
            sumA2 += a * a;
            sumB2 += b * b;
            sumAB += a * b;
         }
         correlation = sumAB / sqrt(sumA2 * sumB2);
      }
      catch (Exception e)
      {
         System.out.println("Failed to compare image files ...")
         throw e
      }

      boolean passed = (correlation > 0.99)
      if (passed)
         println "FileCompare::checkImages()  correlation=${correlation}  PASSED"
      else
         println "FileCompare::checkImages()  correlation=${correlation}  FAILED"

      return (passed)
    }
}
