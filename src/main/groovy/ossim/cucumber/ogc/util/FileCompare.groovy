package ossim.cucumber.ogc.util

import com.sun.istack.internal.NotNull
import org.apache.commons.io.FileUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer

class FileCompare
{
    FileCompare() {}

    static boolean checkImages(String filePath1, String filePath2, String image_type = null)
    {
        println "\n### Entering (1) FileCompare::checkImages(${filePath1}, ${filePath2})"
        String suffix = image_type ? ".${image_type}" : ""
        File file1 = File.createTempFile("tempImage1", suffix)
        File file2 = File.createTempFile("tempImage2", suffix)

        FileUtils.copyURLToFile(filePath1, file1)
        FileUtils.copyURLToFile(filePath2, file2)

        imagesEqual = FileUtils.contentEquals(file1, file2)
        println "Exact match: $imagesEqual\n"

        file1.deleteOnExit()
        file2.deleteOnExit()

        imagesEqual
    }

    static boolean checkImages(File file1, File file2)
    {
        println "\n### Entering (2) FileCompare::checkImages(${file1}, ${file2})"

        double matchPercent = compareImage(file1, file2)
        println "Image match percent: $matchPercent\n"
        return (matchPercent > 90.0)
    }

    private static double compareImage(@NotNull File fileA, @NotNull File fileB) {
        if (!fileA.exists() || !fileB.exists()) return 100
        double percentage = 0
        try {
            // Take buffer data from both image files.
            BufferedImage biA = ImageIO.read(fileA)
            DataBuffer dbA = biA.getData().getDataBuffer()
            int sizeA = dbA.getSize()
            BufferedImage biB = ImageIO.read(fileB)
            DataBuffer dbB = biB.getData().getDataBuffer()
            int sizeB = dbB.getSize()

            // Compare data-buffer objects.
            int count = 0
            if (sizeA == sizeB) {
                for (int i = 0; i < sizeA; i++) {
                    if (dbA.getElem(i) == dbB.getElem(i)) {
                        count = count + 1
                    }
                }
                percentage = (count * 100) / sizeA
            } else {
                System.out.println("Images are not of same size")
            }

        } catch (Exception e) {
            System.out.println("Failed to compare image files ...")
            throw e
        }
        return percentage
    }
}
