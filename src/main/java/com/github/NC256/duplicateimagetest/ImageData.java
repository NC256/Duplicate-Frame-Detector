package com.github.NC256.duplicateimagetest;

import ar.com.hjg.pngj.PngReader;

import java.io.File;
import java.util.Arrays;

public class ImageData {

    private int[][] subpixelsSRGB;
    private int[][] subpixelsOkLab = null;
    private Colorspace cs;
    private int bitdepth;

    public ImageData (PngReader p){
        subpixelsSRGB = IOMethods.readAllSubpixels(p);
        bitdepth = p.imgInfo.bitDepth;
        cs = Colorspace.sRGB;
    }

    public ImageData (File f1){
        this(new PngReader(f1));
    }

    public ImageData (int[][] subpixels, int bitdepth){
        this.subpixelsSRGB = Arrays.stream(subpixels).map(int[]::clone).toArray(int[][]::new);
        this.bitdepth = bitdepth;
        cs = Colorspace.sRGB;
    }


    public int[][] getSubpixelsSRGB() {
        return Arrays.stream(subpixelsSRGB).map(int[]::clone).toArray(int[][]::new);
    }

    public void setSubpixelsSRGB(int[][] subpixels){
        this.subpixelsSRGB = Arrays.stream(subpixels).map(int[]::clone).toArray(int[][]::new);
    }

    public int[][] getSubpixelsOkLab() {
        if (subpixelsOkLab == null){
            this.convertToOkLab();
        }
        return Arrays.stream(subpixelsOkLab).map(int[]::clone).toArray(int[][]::new);
    }

    public void setSubpixelsOkLab(int[][] subpixels){
        this.subpixelsOkLab = Arrays.stream(subpixels).map(int[]::clone).toArray(int[][]::new);
    }

    public Colorspace getColorspace() {
        return cs;
    }

    public void convertToOkLab (){
        if (cs == Colorspace.OkLab){
            return;
        }
        this.setSubpixelsOkLab(PixelMethods.sRGBToOkLab(this.getSubpixelsSRGB(), bitdepth));
        this.cs = Colorspace.OkLab;
    }
}

enum Colorspace {
    sRGB, OkLab
}
