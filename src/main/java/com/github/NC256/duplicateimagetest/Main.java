package com.github.NC256.duplicateimagetest;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {
    public static void main(String[] args) throws IOException {

        File f1 = new File("E:\\Test Frames\\14.png");
        File f2 = new File("E:\\Test Frames\\15.png");

        PixelMethods.IntPixel ip = new PixelMethods.IntPixel(0,0,0);
        PixelMethods.IntPixel result = new PixelMethods.IntPixel(0,0,0);


        ImageData pic1 = new ImageData(f1);
        ImageData pic2 = new ImageData(f2);


        int[][] pic1subpixels = IOMethods.readAllSubpixels(f1);
        int[][] pic2subpixels = IOMethods.readAllSubpixels(f2);

        int[][] oklab1 = PixelMethods.sRGBToOkLab(pic1subpixels,16);
        int[][] oklab2 = PixelMethods.sRGBToOkLab(pic2subpixels,16);

        int[][] srgbdif = PixelMethods.absSubpixelDiff(pic1subpixels,pic2subpixels);
        int[][] oklabdiff = PixelMethods.absSubpixelDiff(oklab1,oklab2);

        boolean anyNegatives = negativesToZeroes(oklabdiff);
        System.out.println("anyNegatives = " + anyNegatives);

        PngWriter outputlab = new PngWriter(new File("oklabTest.png"), new PngReader(f1).imgInfo);
        IOMethods.writeAllRows(oklabdiff,outputlab);
        PngWriter outputsrgb = new PngWriter(new File("srgbTest.png"), new PngReader(f2).imgInfo);
        IOMethods.writeAllRows(srgbdif,outputsrgb);

        IOMethods.csvThresholdStats("testOutput", new String[]{"sRGB, OkLab"}, srgbdif, oklabdiff);

        double[][] srgbEuclidean = PixelMethods.euclideanPixelDistance(pic1subpixels,pic2subpixels);
        double[][] oklabEuclidean = PixelMethods.euclideanPixelDistance(oklab1,oklab2);

        csvEuclideanThreshold("EuclideanDistance", new String[]{"sRGB", "OkLab"},srgbEuclidean, oklabEuclidean);

        int[][] srgbEuclideanSubpixels = pixelsToGreyscaleWritable(normalizeEuclideanDoubles(srgbEuclidean));
        int[][] oklabEuclideanSubpixels = pixelsToGreyscaleWritable(normalizeEuclideanDoubles(oklabEuclidean));

        PngWriter outputEuclideanLab = new PngWriter(new File ("euclideanOklab.png"), new PngReader(f1).imgInfo);
        IOMethods.writeAllRows(oklabEuclideanSubpixels,outputEuclideanLab);

        PngWriter outputEuclideanSrgb = new PngWriter(new File ("euclideanSrgb.png"), new PngReader(f2).imgInfo);
        IOMethods.writeAllRows(srgbEuclideanSubpixels, outputEuclideanSrgb);

    }



    public static int[][] pixelsToGreyscaleWritable (int[][] pixels){
        int[][] subpixels = new int[pixels.length][pixels[0].length * 3];
        for (int r = 0; r < pixels.length; r++) {
            for (int c = 0; c < subpixels[0].length; c++) {
                subpixels[r][c] = pixels[r][c/3];
            }
        }
        return subpixels;
    }

    public static void csvEuclideanThreshold (String fileName, String[] headers, double[][]... images) throws FileNotFoundException {
        File output = new File(fileName + ".csv");
        PrintWriter writer = new PrintWriter(output);
        for (String header : headers) {
            writer.print(header);
            writer.print(",");
        }
        writer.println();

        double[][][] imageCopy = new double[images.length][images[0].length][images[0][0].length];
        for (int i = 0; i < images.length; i++) {
            for (int r = 0; r < images[0].length; r++) {
                for (int c = 0; c < images[0][0].length; c++) {
                    imageCopy[i][r][c] = images[i][r][c];
                }
            }
        }
        long[][] zeroThresholds = new long[100][images.length];
        for (int c = 0; c < zeroThresholds[0].length; c++) {
            for (int r = 0; r < zeroThresholds.length; r++) {
                PixelMethods.clampZeroRangeTolerance(imageCopy[c],r, 113509.949674); //corner to corner in 255x255x255 cube
                zeroThresholds[r][c] = countNonzeroValues(imageCopy[c]);
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

    public static long countNonzeroValues (double[][] arr){
        long count = 0;
        for (double[] doubles : arr) {
            for (int c = 0; c < arr[0].length; c++) {
                if (doubles[c] != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean negativesToZeroes (int[][] arr){
        boolean wereThereAnyZeroes = false;
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[0].length; j++) {
                if (arr[i][j] < 0){
                    arr[i][j] = 0;
                    wereThereAnyZeroes = true;
                }
            }
        }
        return wereThereAnyZeroes;
    }

    // Normalize 16 bit single value doubles so they can be written to an image
    public static int[][] normalizeEuclideanDoubles(double[][] input) {
        int[][] normalized = new int[input.length][input[0].length];
        double slope = 65535.0 / 113509.949674; //133509 = largest distance (65535,65535,65535) -> (0,0,0)
        for (int r = 0; r < input.length; r++) {
            for (int c = 0; c < input[0].length; c++) {
                normalized[r][c] = (int) Math.round(input[r][c] * slope);
            }
        }
        return normalized;
    }

    //TODO delete, but maybe document why it didn't work first
    public static void oldDuplicationCheckingCode () throws InterruptedException, FileNotFoundException {
        File originalFrames = new File("");
        File[] frames = originalFrames.listFiles();
        Arrays.sort(frames);
        Boolean[] isDuplicate = new Boolean[frames.length - 1];
        String[] duplicateResultStrings = new String[isDuplicate.length];

        int start = 0;
        int end = 0;
        int increment = 2000;
        ArrayList<CompareFramesThread> threads = new ArrayList<>();
        while (true){
            start = end;
            end += increment;
            if (start >= isDuplicate.length){
                break;
            }
            if (end > isDuplicate.length){
                end = isDuplicate.length;
            }
            CompareFramesThread cft = new CompareFramesThread(frames, isDuplicate, start, end);
            System.out.println("Starting new thread to process frames: " + start + " through " + end);
            threads.add(cft);
            cft.start();
        }
        System.out.println("Wow spun up " + threads.size() + " threads!");
        int count = 1;
        for (CompareFramesThread thread : threads) {
            thread.join();
            System.out.println("Joined #" + count);
            count++;
        }

//        for (int i = 0; i < isDuplicate.length; i++) {
//            isDuplicate[i] = areTheseFramesEqual(frames[i],frames[i+1]);
//        }
        for (int i = 0; i < isDuplicate.length; i++) {
            if (isDuplicate[i] == null){
                System.out.println((i+1) + "-->" + (i+2) + ": Unsure" );
                duplicateResultStrings[i] = ((i+1) + "-->" + (i+2) + ": Unsure");
            }
            else if (isDuplicate[i]){
                System.out.println((i+1) + "-->" + (i+2) + ": Repeat" );
                duplicateResultStrings[i] = ((i+1) + "-->" + (i+2) + ": Repeat");
            }
            else{
                System.out.println((i+1) + "-->" + (i+2) + ": Different" );
                duplicateResultStrings[i] = ((i+1) + "-->" + (i+2) + ": Different");
            }
        }

        //GRADIENT ATTACK DEFENSE CODE
        System.out.println("BEGIN GRADIENT DEFENSE SWEEP");
        Boolean didRepeat = null;
        int span = 0;
        int from = -1, to = -1;
        for (int i = 0; i < isDuplicate.length; i++) {
            didRepeat = isDuplicate[i];
            if (didRepeat == null || !didRepeat){ //null or different frame
                if (span == 0 || span == 1){ //nothing long enough to check
                    span = 0;
                    continue;
                }
                else if (span > 0){

                    //Boolean areEqual = areTheseFramesEqual(frames[from], frames[to]);
                    Boolean areEqual = true;
                    if (areEqual == null || areEqual){
                        System.out.println("Gradient check on frames " + (from + 1) + "-->" + (to + 1) + " found equivalence. " +
                                "Assuming the whole range is good.");
                    }
                    else{
                        System.out.println("Gradient check on frames " + (from + 1) + "-->" + (to+1) + " was not equal! " +
                                "Marking all frames as different.");
                        for (int f = from; f < to; f++) {
                            duplicateResultStrings[f] = duplicateResultStrings[f] + " (GRADIENT ATTACK?)";
                        }
                    }
                    span = 0;
                }
            }
            else if (didRepeat){ //repeated frame
                if (span == 0) { //start a new span
                    span = 1;
                    from = i;
                    to = i + 1;
                }
                else if (span > 0){
                    span++;
                    to = i + 1;
                }
            }
        }


        File output = new File("Results.txt");
        PrintWriter pw = new PrintWriter(output);
        for (String duplicateResultString : duplicateResultStrings) {
            pw.println(duplicateResultString);
        }
        pw.close();
    }

}

