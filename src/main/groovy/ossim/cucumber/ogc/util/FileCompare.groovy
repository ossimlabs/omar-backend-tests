package ossim.cucumber.ogc.util

import org.apache.commons.io.FileUtils

class FileCompare
{
    FileCompare() {}

    static boolean checkImages(filePath1, filePath2, image_type)
    {
        boolean imagesEqual
        File file1 = File.createTempFile("tempImage1", ".${image_type}")
        File file2 = File.createTempFile("tempImage2", ".${image_type}")

        FileUtils.copyURLToFile(filePath1, file1)
        FileUtils.copyURLToFile(filePath2, file2)

        imagesEqual = FileUtils.contentEquals(file1, file2)

        file1.deleteOnExit()
        file2.deleteOnExit()

        imagesEqual
    }

    static boolean checkImages(File file1, File file2)
    {
        FileUtils.contentEquals(file1, file2)
    }
}
