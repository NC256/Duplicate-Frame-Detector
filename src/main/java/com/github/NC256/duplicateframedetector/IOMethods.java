package com.github.NC256.duplicateframedetector;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;

public class IOMethods {

    /**
     * Extracts all subpixel values from image into an integer array
     * @param p1
     * @return
     */
    public static int[][] readAllSubpixels(PngReader p1){
        IImageLineSet<? extends IImageLine> lines = p1.readRows();
        int[][] subPixels = new int[p1.imgInfo.rows][p1.imgInfo.cols * 3];
        for (int i = 0; i < lines.size(); i++) {
            subPixels[i] = ((ImageLineInt) lines.getImageLine(i)).getScanline();
        }
        return subPixels;
    }

    public static int[][] readAllSubpixels(File f1){
        return readAllSubpixels(new PngReader(f1));
    }

    /**
     * Writes all subpixel values out to a file
     * @param subPixels
     * @param writer
     */
    public static void writeAllRows(int[][] subPixels, PngWriter writer){
        for (int[] subPixel : subPixels) {
            writer.writeRowInt(subPixel);
        }
        writer.end();
    }

    /**
     * Generates numbers onto the end of an existing fileName string but does it
     * live while in a directory
     * @param directory
     * @param fileName
     * @return
     */
    public static File getNextFileName(File directory, String fileName){
        File[] files = directory.listFiles();
        Arrays.sort(files);
        int counter = 1;
        String outputName = String.format("%s%05d.png", fileName, counter);
        File outputFile = null;
        do{
            outputFile = new File(directory.toPath().toAbsolutePath() + "\\" + outputName);
            counter++;
            outputName = String.format("%s%05d.png", fileName, counter);
        }
        while (outputFile.exists());
        return outputFile;
    }

    // Writes a long[][] to a CSV file with specified headers
    public static void dataToCSV (long[][] data, String[] headers, String fileName) throws FileNotFoundException {
        File CsvOut = new File(fileName + ".csv");
        PrintWriter printWriter = new PrintWriter(CsvOut);
        for (String header : headers) {
            printWriter.print(header);
            printWriter.print(",");
        }
        printWriter.println();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                printWriter.print(data[i][j]);
                printWriter.print(",");
            }
            printWriter.println();
        }
        printWriter.close();
    }

    public static void csvThresholdStats(String fileName, String[] headers, int[][]... images) throws FileNotFoundException {
        File output = new File(fileName + ".csv");
        PrintWriter writer = new PrintWriter(output);
        for (String header : headers) {
            writer.print(header);
            writer.print(",");
        }
        writer.println();

        int[][][] imageCopy = images.clone();
        //TODO fix above statement that does not deep copy :/
        long[][] zeroThresholds = new long[100][images.length]; // fill the array that gets written to the CSV
        for (int c = 0; c < zeroThresholds[0].length; c++) {
            for (int r = 0; r < zeroThresholds.length; r++) {
                PixelMethods.clampZeroWithTolerance(imageCopy[c],r);
                zeroThresholds[r][c] = PixelMethods.nonzeroPixelCount(imageCopy[c]);
            }
        }

        for (int i = 0; i < zeroThresholds.length; i++) { // print the array that gets written to the CSV
            for (int j = 0; j < zeroThresholds[0].length; j++) {
                writer.print(zeroThresholds[i][j]);
                writer.print(",");
            }
            writer.println();
        }
        writer.close();
    }

}
