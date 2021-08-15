package com.github.NC256.duplicateimagetest;

import ar.com.hjg.pngj.PngReader;

import java.io.File;
import java.util.Objects;

public class PixelMethods {

    // conversion matrixes code adapted from (public domain) C code from Björn Ottosson
    // https://bottosson.github.io/posts/oklab/
    private static void linearsRGBPixelToOklabPixel(FloatingPixel sRGB) {

        float l = 0.4122214708f * sRGB.R + 0.5363325363f * sRGB.G + 0.0514459929f * sRGB.B;
        float m = 0.2119034982f * sRGB.R + 0.6806995451f * sRGB.G + 0.1073969566f * sRGB.B;
        float s = 0.0883024619f * sRGB.R + 0.2817188376f * sRGB.G + 0.6299787005f * sRGB.B;

        float l_ = (float) Math.cbrt(l);
        float m_ = (float) Math.cbrt(m);
        float s_ = (float) Math.cbrt(s);

        // Original code had LAB instead of RGB...
        sRGB.R = 0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_;
        sRGB.G = 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_;
        sRGB.B = 0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_;

    }

    // conversion matrixes code adapted from (public domain) C code from Björn Ottosson
    // https://bottosson.github.io/posts/oklab/
    private static void oklabPixelToLinearsRGBPixel(FloatingPixel Oklab) {

        // Original code had LAB instead of RGB...
        float l_ = Oklab.R + 0.3963377774f * Oklab.G + 0.2158037573f * Oklab.B;
        float m_ = Oklab.R - 0.1055613458f * Oklab.G - 0.0638541728f * Oklab.B;
        float s_ = Oklab.R - 0.0894841775f * Oklab.G - 1.2914855480f * Oklab.B;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;


        Oklab.R = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        Oklab.G = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        Oklab.B = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

    }

    private static float colorNormalizer(float component, int bitdepth) {
        return (float) (component / (Math.pow(2, bitdepth) - 1));
    }

    private static int colorDenormalizer(float component, int bitdepth) { // Might benefit from better error handling/rounding/etc
        return (int) Math.round(component * (Math.pow(2, bitdepth) - 1));
    }

    private static float linearTosRGB(float component) {
        if (component <= 0.0031308) {
            return (float) (12.92 * component);
        } else {
            return (float) (1.055 * Math.pow(component, (1 / 2.4)) - 0.055);
        }
    }

    private static float sRGBToLinear(float component) {
        if (component <= 0.04045) {
            return (float) (component / 12.92);
        } else {
            return (float) Math.pow(((component + 0.055) / 1.055), 2.4);
        }
    }

    public static IntPixel sRGBToOklabPixelConversion(IntPixel sRGB, int bitdepth) {
        FloatingPixel temp = new FloatingPixel(0, 0, 0);
        // Normalize to 0...1
        temp.R = colorNormalizer(sRGB.R, bitdepth);
        temp.G = colorNormalizer(sRGB.G, bitdepth);
        temp.B = colorNormalizer(sRGB.B, bitdepth);

        // sRGB is nonlinear by default, must be linearized (after being normalized)
        temp.R = sRGBToLinear(temp.R);
        temp.G = sRGBToLinear(temp.G);
        temp.B = sRGBToLinear(temp.B);

        // Time to run it through the conversion code
        linearsRGBPixelToOklabPixel(temp);

        // Time to expand out to a bit depth
        IntPixel output = new IntPixel(colorDenormalizer(temp.R, bitdepth), colorDenormalizer(temp.G, bitdepth), colorDenormalizer(temp.B, bitdepth));
        return output;
    }

    public static IntPixel OklabTosRGBPixelConversion(IntPixel oklab, int bitdepth){
        FloatingPixel temp = new FloatingPixel(0,0,0);

        // Normalize to 0...1
        temp.R = colorNormalizer(oklab.R, bitdepth);
        temp.G = colorNormalizer(oklab.G, bitdepth);
        temp.B = colorNormalizer(oklab.B, bitdepth);

        // Convert to linear sRGB
        oklabPixelToLinearsRGBPixel(temp);

        // Delinearize it
        temp.R = linearTosRGB(temp.R);
        temp.G = linearTosRGB(temp.G);
        temp.B = linearTosRGB(temp.B);

        // Expand back to bit depth
        IntPixel output = new IntPixel(colorDenormalizer(temp.R, bitdepth), colorDenormalizer(temp.G, bitdepth), colorDenormalizer(temp.B, bitdepth));
        return output;
    }

    public static int[][] sRGBToOkLab(int[][] subpixels, int bitdepth) {
        IntPixel[][] intPixels = intsToPixels(subpixels);
        IntPixel[][] output = new IntPixel[intPixels.length][intPixels[0].length];

        for (int i = 0; i < intPixels.length; i++) {
            for (int j = 0; j < intPixels[0].length; j++) {
                output[i][j] = sRGBToOklabPixelConversion(intPixels[i][j], bitdepth);
            }
        }
        return pixelsToInts(output);
    }


    /**
     * This method requires a little bit of backstory so buckle in:
     * The images I was trying to upscale started their existence as YUV420p BT709 in an h.264 video stream.
     * They were then turned into PNG files with FFMPEG into RGB48BE using -sws_flags +accurate_rnd+full_chroma_int
     * According to the PNG standard @see <a href="http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html#C.gAMA">link</a>,
     * a gAMA block may be written in order to denote the power function relationship between the pixels and output intensity.
     * My vague understanding is that this means there is not a linear relationship between all values in the image.
     * Using the math in the spec and looking at what gAMA value was written to my PNG files (50994) leaves an exponent of ~1.96
     *
     * So this method calculates the difference between two subpixel values based on the distance between them on this power function.
     * Sadly this did not end up working much better than the euclidean distance (and what I think I really
     * needed to be looking for was a distance function relative to human perception.)
     * @param p1 First row of subpixel values
     * @param p2 Second row of subpixel values
     * @param output Distance between the pixels according to their gamma power function
     * @param exponent Exponent value from the gAMA chunk translated back into it's equation value
     */
    public static void powerLawPixelDiff (int[] p1, int[] p2, int[] output, double exponent){
        double dp1, dp2;
        for (int i = 0; i < p1.length; i++) {
            dp1 = (double) p1[i] / (double) 65535;
            dp2 = (double) p2[i] / (double) 65535;
            output[i] = (int) Math.rint(Math.abs(Math.pow(dp1,exponent) - Math.pow(dp2, exponent)));
        }
    }

    /**
     * @see #powerLawPixelDiff(int[], int[], int[], double)
     *
     * @param p1 First 2D array of subpixel values
     * @param p2 Second 2D array of subpixel values
     * @param output 2D array of distances
     * @param exponent Exponent value from the gAMA chunk translated back into it's equation value
     */
    public static void powerLawPixelDiff (int[][] p1, int[][] p2, int[][] output, double exponent) {
        // Might be worth it because the floating point math takes much longer than the absPixelDiff integer math
        //IntStream.range(0,output.length).parallel().forEach(x -> powerLawPixelDiff(p1[x], p2[x], output[x]));
        double dp1, dp2;
        for (int r = 0; r < p1.length; r++) {
            for (int c = 0; c < p1[r].length; c++) {
                dp1 = (double) p1[r][c] / (double) 65535;
                dp2 = (double) p2[r][c] / (double) 65535;
                output[r][c] = (int) Math.rint(65535 * Math.abs(Math.pow(dp1, exponent) - Math.pow(dp2, exponent)));
            }
        }
    }

    /**
     * For all values in sp1 and sp2, fills output with the result of Math.abs(sp1[i] - sp2[i])
     * @param sp1 First subpixels
     * @param sp2 Second subpixels
     * @return Absolute difference values
     */
    public static int[][] absSubpixelDiff(int[][] sp1, int[][] sp2){
        // Parallel solution that might have too much overhead
        //IntStream.range(0,output.length).parallel().forEach(x -> absPixelDiff(sp1[x], sp2[x], output[x]));
        int[][] output = new int[sp1.length][sp1[0].length];
        for (int r = 0; r < sp1.length; r++) {
            for (int c = 0; c < sp1[r].length; c++) {
                output[r][c] = Math.abs(sp1[r][c] - sp2[r][c]);
            }
        }
        return output;
    }

    //subpixels sized array in, pixel sized array out
    public static double[][] euclideanPixelDistance (int[][] sp1, int[][] sp2){
        IntPixel[][] ip1 = intsToPixels(sp1);
        IntPixel[][] ip2 = intsToPixels(sp2);
        double[][] result = new double[ip1.length][ip1[0].length];

        for (int x = 0; x < ip1.length; x++) {
            for (int y = 0; y < ip1[0].length; y++) {
                result[x][y] = Math.sqrt(Math.pow(ip1[x][y].R - ip2[x][y].R, 2) + Math.pow(ip1[x][y].G - ip2[x][y].G, 2) + Math.pow(ip1[x][y].B - ip2[x][y].B, 2));
            }
        }
        return result;
    }

    /**
     * Convenience method to generate the difference subpixel values for two images from the file handles
     * @param f1 First PNG file
     * @param f2 Second PNG file
     * @return The subpixel values of the absolute difference image between f1 and f2
     */
    public static int[][] absSubpixelDiff(File f1, File f2){
        int[][] p1p = IOMethods.readAllSubpixels(new PngReader(f1));
        int[][] p2p = IOMethods.readAllSubpixels(new PngReader(f2));
        return PixelMethods.absSubpixelDiff(p1p,p2p);
    }

    /**
     * Counts the number of nonzero pixels given a row of subpixel values
     * @param rowSubPixels Expects a row of subpixels
     * @return The number of pixel values that were nonzero
     */
    public static long nonzeroPixelCount(int[] rowSubPixels){
        long count = 0;
        for (int i = 0; i < rowSubPixels.length; i+=3) {
            if (rowSubPixels[i] > 0 || rowSubPixels[i+1] > 0 || rowSubPixels[i+2] > 0){
                count++;
            }
        }
        return count;
    }

    /**
     * @see #nonzeroPixelCount(int[])
     * @param subPixels 2D array of subpixel values
     * @return The number of pixel values that were nonzero
     */
    public static long nonzeroPixelCount(int[][] subPixels){
        long count = 0;
        for (int[] subPixel : subPixels) {
            count += nonzeroPixelCount(subPixel);
        }
        // Parallel solution that probably gets lost in the overhead
        //AtomicLong atomicLong = new AtomicLong();
        //Arrays.stream(subPixels).parallel().forEach(x -> atomicLong.addAndGet(countNonZeroPixels(x)));
        return count;
    }

    /**
     * All subpixel values less than the tolerance percentage are set to zero
     * @param rowSubPixels A single row of subpixel values
     * @param tolerancePercentage 0-100 valid. 0 will cause no changes
     */
    public static void clampZeroWithTolerance(int[] rowSubPixels, int tolerancePercentage){
        if (tolerancePercentage == 0){
            return;
        }
        //TODO change that hardcoded 65535 to a bitdepth method parameter
        int factor = (int) (65535 * tolerancePercentage * 0.01);
        for (int i = 0; i < rowSubPixels.length; i++) {
            if (rowSubPixels[i] < factor){
                rowSubPixels[i] = 0;
            }
        }
    }

    public static void clampZeroRangeTolerance(double[] rowPixels, int tolerancePercentage, double maximum){
        if (tolerancePercentage == 0){
            return;
        }
        double factor = (maximum * tolerancePercentage * 0.01);
        for (int i = 0; i < rowPixels.length; i++) {
            if (rowPixels[i] < factor){
                rowPixels[i] = 0;
            }
        }
    }

    public static void clampZeroRangeTolerance(double[][] values, int tolerancePercentage, double maximum){
        if (tolerancePercentage == 0){
            return;
        }
        for (double[] value : values) {
            clampZeroRangeTolerance(value,tolerancePercentage,maximum);
        }
    }

    /**
     * @see #clampZeroWithTolerance(int[], int)
     * @param subPixels 2D array of subpixel values
     * @param tolerancePercentage 0-100 valid. 0 will cause no changes
     */
    public static void clampZeroWithTolerance(int[][] subPixels, int tolerancePercentage){
        if (tolerancePercentage == 0){
            return;
        }
        for (int[] subPixel : subPixels) {
            clampZeroWithTolerance(subPixel, tolerancePercentage);
        }
        // Parallel solution that probably gets slower with the overhead
        //Arrays.stream(subPixels).parallel().forEach(x -> Main.clampZeroWithTolerance(x, tolerancePercentage));
    }

    /**
     * In an attempt to distinguish between frame-by-frame noise (which I assumed would be spread out)
     * and genuine but subtle changes (which I assumed would be more grouped up) this method
     * sums up the total number of neighboring pixel relationships where a neighbor is nonzero.
     * Every pixel counts the number of neighbors that are lit up and adds that to the running total.
     * Unfortunately didn't seem to be particularly useful in testing.
     * @param subPixels An image's subpixel values
     * @return The total number of relationships between all pixels.
     */
    public static long totalNonzeroNeighborRelationships(int[][] subPixels){
        IntPixel[][] intPixels = intsToPixels(subPixels);
        long count = 0;
        for (int r = 0; r < intPixels.length; r++) {
            for (int c = 0; c < intPixels[r].length; c++) {
                count += nonZero(r - 1, c, intPixels); // North
                count += nonZero(r - 1,c + 1, intPixels); // North-East
                count += nonZero(r ,c + 1, intPixels); // East
                count += nonZero(r + 1,c + 1, intPixels); // SE
                count += nonZero(r + 1,c, intPixels); // S
                count += nonZero(r + 1,c - 1, intPixels); // SW
                count += nonZero(r ,c - 1, intPixels); //W
                count += nonZero(r - 1,c - 1, intPixels); //NW
            }
        }
        return count;
    }

    /**
     * In another attempt to distinguish between frame-by-frame noise (presumably spread out)
     * and genuine but subtle changes (presumably grouped) this method counts up by 1 for
     * every pixel that has at least 1 nonzero neighbor.
     * Unfortunately also didn't seem to be particularly useful in testing.
     * @param subPixels An image's subpixel values
     * @return The number of pixels that neighbor at least one nonzero pixel
     */
    public static long uniqueNonzeroNeighborCount(int[][] subPixels){
        IntPixel[][] intPixels = intsToPixels(subPixels);

        long count = 0;
        int localSum = 0;
        for (int r = 0; r < intPixels.length; r++) {
            for (int c = 0; c < intPixels[r].length; c++) {
                localSum += nonZero(r - 1, c, intPixels); // North
                localSum += nonZero(r - 1,c + 1, intPixels); // North-East
                localSum += nonZero(r ,c + 1, intPixels); // East
                localSum += nonZero(r + 1,c + 1, intPixels); // SE
                localSum += nonZero(r + 1,c, intPixels); // S
                localSum += nonZero(r + 1,c - 1, intPixels); // SW
                localSum += nonZero(r ,c - 1, intPixels); //W
                localSum += nonZero(r - 1,c - 1, intPixels); //NW
                if (localSum > 0){
                    count++;
                }
                localSum = 0;
            }
        }
        return count;
    }

    /**
     * Returns 1 if the pixel at image[row][col] is nonzero.
     * Also does a bunch of error checking, safe to call on invalid array locations (will return 0).
     */
    public static long nonZero (int row, int col, IntPixel[][] image){
        if (row < 0 || col < 0 || row >= image.length || col >= image[row].length){
            return 0;
        }
        if (image[row][col].R > 0 || image[row][col].G > 0 || image[row][col].B > 0){
            return 1;
        }
        else{
            return 0;
        }
    }

    /**
     *
     * Utility method to convert an array of subpixel values to an array of {@link IntPixel} objects
     * @param subPixels 2D array of subpixel values
     * @return 2D array of {@link IntPixel} objects
     */
    public static IntPixel[][] intsToPixels(int[][] subPixels){
        IntPixel[][] intPixels = new IntPixel[subPixels.length][subPixels[0].length/3];
        for (int r = 0; r < subPixels.length; r++) {
            for (int c = 0; c < subPixels[r].length; c += 3) {
                intPixels[r][c/3] = new IntPixel(subPixels[r][c], subPixels[r][c + 1], subPixels[r][c + 2]);
            }
        }
        return intPixels;
    }

    /**
     * Utility method to convert a 2D array of {@link IntPixel} values to a 2D array of subpixel values
     * @param intPixels 2D array of {@link IntPixel} values
     * @return 2D array of subpixel values
     */
    public static int[][] pixelsToInts(IntPixel[][] intPixels){
        int[][] subPixels = new int[intPixels.length][intPixels[0].length * 3];
        for (int r = 0; r < intPixels.length; r++) {
            for (int c = 0; c < intPixels[0].length; c++) {
                subPixels[r][3 * c] = intPixels[r][c].R;
                subPixels[r][(3 * c) +1] = intPixels[r][c].G;
                subPixels[r][(3 * c) +2] = intPixels[r][c].B;
            }
        }
        return subPixels;
    }

    /**
     * Utility class for convenience of some equations
     */
    public static class IntPixel {
        public int R,G,B;
        public IntPixel(int R, int G, int B){
            this.R = R;
            this.G = G;
            this.B = B;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IntPixel intPixel = (IntPixel) o;

            return (R == intPixel.R) && (G == intPixel.G) && (B == intPixel.B);
        }

        @Override
        public int hashCode() {
            return Objects.hash(R,G,B);
        }
    }

    /**
     * Utility class for convenience of some equations
     */
    public static class FloatingPixel {
        public float R,G,B;
        public FloatingPixel(float R, float G, float B){
            this.R = R;
            this.G = G;
            this.B = B;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FloatingPixel floatingPixel = (FloatingPixel) o;

            return (Float.compare(R, floatingPixel.R) == 0 &&
                    Float.compare(G, floatingPixel.G) == 0 &&
                    Float.compare(B, floatingPixel.B) == 0);
        }

        @Override
        public int hashCode() {
            return Objects.hash(R,G,B);
        }
    }
}
