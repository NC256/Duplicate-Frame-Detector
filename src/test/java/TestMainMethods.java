import com.github.NC256.duplicateimagetest.PixelMethods;
import org.junit.jupiter.api.Test;
import static com.github.NC256.duplicateimagetest.PixelMethods.IntPixel;
import static com.github.NC256.duplicateimagetest.PixelMethods.FloatingPixel;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMainMethods {

    @Test
    public void testClampToZero(){
        int[] arr = new int[]{1,1,1,1,1,1};
        PixelMethods.clampZeroWithTolerance(arr,50);
        assertArrayEquals(new int[]{0,0,0,0,0,0},arr);
    }

    @Test
    public void testClampToZero2D(){
        int[][] arr = new int[9][9];
        Arrays.stream(arr).forEach(x -> Arrays.fill(x,1));
        PixelMethods.clampZeroWithTolerance(arr,50);
        assertTrue(Arrays.deepEquals(arr,new int[9][9]));
    }

    @Test
    public void testCountNonZeroes(){
        int[] arr = new int[]{1,2,3,4,0,0};
        assertEquals(2, PixelMethods.nonzeroPixelCount(arr));
    }

    @Test
    public void testCountNonZeroes2D(){
        int[][] arr = new int[3][3];
        arr[1][1] = 3;
        arr[2][0] = 2;
        arr[0][2] = 7;
        assertEquals(3, PixelMethods.nonzeroPixelCount(arr));
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
        assertTrue(Arrays.deepEquals(PixelMethods.intsToPixels(arr1), arr2));
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
}
