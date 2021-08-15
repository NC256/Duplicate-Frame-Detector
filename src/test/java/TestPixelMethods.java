import com.github.NC256.duplicateimagetest.PixelMethods;
import org.junit.jupiter.api.Test;
import static com.github.NC256.duplicateimagetest.PixelMethods.IntPixel;
import static com.github.NC256.duplicateimagetest.PixelMethods.FloatingPixel;

import java.util.Arrays;

import static com.github.NC256.duplicateimagetest.PixelMethods.absSubpixelDiff;
import static com.github.NC256.duplicateimagetest.PixelMethods.euclideanPixelDistance;
import static com.github.NC256.duplicateimagetest.PixelMethods.sRGBToOklabPixelConversion;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPixelMethods {

    @Test
    public void testClampZeroWithTolerance(){
        int[] arr = new int[]{1,1,1,1,1,1};
        PixelMethods.clampZeroWithTolerance(arr,50);
        assertArrayEquals(new int[]{0,0,0,0,0,0},arr);

        int[][] arr2 = new int[9][9];
        Arrays.stream(arr2).forEach(x -> Arrays.fill(x,1));
        PixelMethods.clampZeroWithTolerance(arr2,50);
        assertTrue(Arrays.deepEquals(arr2,new int[9][9]));
    }

    @Test
    public void testClampZeroRangeTolerance(){
        double[] arr = new double[]{1.0,1.0,1.0,1.0,1.0,4.0};
        PixelMethods.clampZeroRangeTolerance(arr,50, 4);
        assertArrayEquals(arr, new double[]{0.0,0.0,0.0,0.0,0.0,4.0});

        double[][] arr2 = new double[9][9];
        Arrays.stream(arr2).forEach(x -> Arrays.fill(x,1.0));
        PixelMethods.clampZeroRangeTolerance(arr2,50,4);
        assertTrue(Arrays.deepEquals(arr2, new double[9][9]));
    }

    @Test
    public void testNonzeroPixelCount(){
        int[] arr = new int[]{1,2,3,4,5,6};
        assertEquals(2, PixelMethods.nonzeroPixelCount(arr));
        int[] arr2 = new int[]{0,0,0,0,1,0};
        assertEquals(1, PixelMethods.nonzeroPixelCount(arr2));

        int[][] arr3 = new int[3][3];
        arr3[1][1] = 3;
        arr3[2][0] = 2;
        arr3[0][2] = 7;
        assertEquals(3, PixelMethods.nonzeroPixelCount(arr3));
    }

    @Test
    public void testIntsToPixels(){
        int[][] arr1 = new int[][]{{1, 2, 3, 4, 5, 6, 7, 8, 9},
                                   {9, 8, 7, 6, 5, 4, 3, 2, 1},
                                   {0, 0, 0, 0, 0, 0, 0, 0, 0}};
        IntPixel[][] arr2 = new IntPixel[][]
                         {{new IntPixel(1, 2, 3), new IntPixel(4, 5, 6), new IntPixel(7, 8, 9)}
                        , {new IntPixel(9, 8, 7), new IntPixel(6, 5, 4), new IntPixel(3, 2, 1)}
                        , {new IntPixel(0, 0, 0), new IntPixel(0, 0, 0), new IntPixel(0, 0, 0)}};
        IntPixel[][] answer = PixelMethods.intsToPixels(arr1);
        assertTrue(Arrays.deepEquals(answer, arr2));
    }

    @Test
    public void testPixelsToInts() {
        int[][] arr1 = new int[][]{{1, 2, 3, 4, 5, 6, 7, 8, 9},
                                   {9, 8, 7, 6, 5, 4, 3, 2, 1},
                                   {0, 0, 0, 0, 0, 0, 0, 0, 0}};
        IntPixel[][] arr2 = new IntPixel[][]
                         {{new IntPixel(1, 2, 3), new IntPixel(4, 5, 6), new IntPixel(7, 8, 9)}
                        , {new IntPixel(9, 8, 7), new IntPixel(6, 5, 4), new IntPixel(3, 2, 1)}
                        , {new IntPixel(0, 0, 0), new IntPixel(0, 0, 0), new IntPixel(0, 0, 0)}};
        assertTrue(Arrays.deepEquals(arr1, PixelMethods.pixelsToInts(arr2)));
    }

    @Test
    public void testEuclideanPixelDistance(){
        int[][] p1 = new int[3][9];
        int[][] p2 = new int[3][9];

        p1[0][0] = 255;
        p2[0][0] = 128;

        p1[2][6] = 37;
        p1[2][7] = 105;
        p1[2][8] = 208;

        p2[2][6] = 233;
        p2[2][7] = 3;
        p2[2][8] = 0;


        double[][] answer = euclideanPixelDistance(p1,p2);
        assertEquals(answer[0][0], 127);

        double distance = Math.sqrt(Math.pow(37-233,2) + Math.pow(105-3,2) + Math.pow(208-0,2));
        assertEquals(distance, answer[2][2]);
    }

    @Test
    public void testAbsPixelDiff(){
        int[][] sp1 = new int[3][9];
        int[][] sp2 = new int[3][9];

        sp1[0][0] = 100;

        sp1[2][6] = 50;
        sp2[2][6] = 100;

        int[][] result = absSubpixelDiff(sp1,sp2);
        assertEquals(100,result[0][0]);
        assertEquals(50,result[2][6]);
    }

    @Test
    public void testsRGBToOklabPixelConversion(){

        // Verified with help from Culori
        // https://culorijs.org/api/

        IntPixel pix1 = new IntPixel(255,255,255);
        IntPixel result1 = sRGBToOklabPixelConversion(pix1,8);
        assertEquals(255, result1.R);
        assertEquals(0, result1.G);
        assertEquals(0, result1.B);

        IntPixel pix2 = new IntPixel(128,128,128);
        IntPixel result2 = sRGBToOklabPixelConversion(pix2,8);
        assertEquals(153,result2.R);
        assertEquals(0, result2.G);
        assertEquals(0, result2.B);

        IntPixel pix3 = new IntPixel(25,133,234);
        IntPixel result3 = sRGBToOklabPixelConversion(pix3,8);
        assertEquals(156,result3.R);
        assertEquals(-13, result3.G);
        assertEquals(-43, result3.B);

        IntPixel pix4 = new IntPixel(255,5,255);
        IntPixel result4 = sRGBToOklabPixelConversion(pix4,8);
        assertEquals(179, result4.R);
        assertEquals(70, result4.G);
        assertEquals(-43, result4.B);

        IntPixel pix5 = new IntPixel(35082, 26532, 65093);
        IntPixel result5 = sRGBToOklabPixelConversion(pix5, 16);
        assertEquals(40980, result5.R);
        assertEquals(4530, result5.G);
        assertEquals(-13220, result5.B);

    }
}
