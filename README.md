# What is this?

My goal was to detect exact-duplicate frames in some 2D animation footage. When I started this project I had no idea if this goal was reasonable or not (turns out it is a lot harder than I expected, this codebase hasn't achieved that goal yet).

## The Setting

My initial assumptions:
   1. Subsequent frames with no visually noticeable difference would have identical or nearly-identical digital pixel values.
   2. Because I was working with simple 2D animation, subsequent frames wouldn't have any sensor noise, and would be easily detectable.
   3. Calculating the "difference" between two frames was a straightforward concept.

Some limitations:
   1. I would work with the frames extracted into individuals images instead of inside the codec (H.264) for simplicity reasons.
   2. I didn't want to use any AI or neural networks as they would never offer a 100% confidence rating (and there seemed to be value in having a definite algorithm).
   3. I wasn't using existing image comparison techniques ([perceptual hashes](https://en.wikipedia.org/wiki/Perceptual_hashing)) as they were also based on fuzzy comparisons.

## The Setup

The H.264 video I had on hand was encoded as YUV with 4:2:0 subsampling. 

I initially looked for an image format that supported YUV so I didn't have to lose anything to the color conversion process, but sadly none of the popular formats were suitable. WebP is the only one with YUV support, but only in [lossy encoding mode](https://developers.google.com/speed/webp/faq#what_color_spaces_does_the_webp_format_support). 

I then spent some time trying to find a lossless conversion method between YUV and RGB. The results were mixed, confusing, and I found indications that there are multiple *different* equations used in the process. 

I ran an experiment with FFmpeg where I extracted a frame as raw YUV, converted it to PNG, back to YUV, back to PNG, rinse repeat.
Here's a crop of the source image:

![A small crop of a larger image, taken from the source material](./readmeImages/croppedConversionOriginal.png)

Here's what it looked like after being converted between PNG and raw YUV a thousand times (steady state was reached around 650):

![The same crop as the previous image, but after 1000 conversions](./readmeImages/croppedConversion1000.png)

Through manual testing I found that using `-sws_flags +accurate_rnd+full_chroma_int` on FFmpeg during the conversion process resolved this issue (the [docs](https://ffmpeg.org/ffmpeg-scaler.html#toc-Scaler-Options) are not clear on what *exactly* these flags do). Though it still introduced minor noise, it reached steady state after a small number of conversions and more importantly, still looked visually equivaelent to the source. In the end I settled on PNG with 16 bits per channel with the hope that expanding to a bigger bit depth would reduce round-off errors in the conversion process.


# Attempt #1 - What is color anyway?

I decided to compare frames pixel-wise. So I take some pixel in frame 1, and compare it to the same pixel in frame 2. And I calculate the difference between each color channel. If you do this for every pixel you can actually generate a new picture, a "difference frame" if you will. If you load two images into a program like Gimp or Photoshop and set the layer blending mode to "difference" you can perfectly replicate what I'm describing. The image will be solid black anywhere the colors have not changed, otherwise it'll light up (depending on how "far apart" the color values of the pixels were from each other).

It was at this point I discovered that assumption #1's and 2 were incorrect. Even on frames that looked visually identical, frames I'm almost certain that the original artists simply duplicated for timing reasons, the color values are not identical.

Here's a cropped excerpt image of two identical looking frames overlaid in Gimp 2.10.20r1, blended with difference mode and merged down into one, then the exposure cranked way up:
![H.264 quantization noise, I think](./readmeImages/pic1.PNG)

I'm pretty sure I'm looking at H.264 encoder/quantization noise (if you know what I'm looking at here, please let me know!). My source was a blu-ray disc, so I can't imagine it was too many steps away from a high bitrate source, but it dashed my hopes of perfect frame duplication.


# Attempt #2 - Maybe this is salvagable.

Okay so duplicate looking frames weren't actually duplicate, but they were pretty close. Maybe I could still detect duplicates by introducing a tolerance rating. When I would examine individual pixels in subsequent frames they would often differ by only 1-3%. So I began to examine lots of pairs of frames.

Here's the percent differences in pixel colors for two identical looking frames. There are ~400,000 pixels that don't match values perfectly between frames, but only 4 pixels manage to deviate more than 5%
```
>0% diff, >1% , >2% , 3% , 4%, etc 
[401955, 21552, 1897, 191, 21, 4, 1, 0, 0, 0,...]
```
Here's the difference values for two frames where some tiny objects disappear in the background, resulting in a small number of pixels having a sizable change. A handful of pixels are more than 90% different from their values on the prior frame.
```
[925395, 113369, 13884, 3415, 2142, 1883, 1792, 1744, 1678, 1623, 1571, 1535, 1489, 1453, 1415, 1365, 1320, 1273, 1224, 1194, 1162, 1128, 1092, 1063, 1039, 1013, 995, 967, 952, 926, 907, 884, 857, 838, 828, 819, 809, 790, 772, 759, 753, 739, 732, 722, 717, 711, 707, 706, 697, 689, 683, 678, 673, 667, 660, 651, 642, 635, 629, 619, 610, 596, 582, 572, 567, 558, 549, 540, 532, 527, 516, 510, 497, 486, 476, 467, 460, 453, 444, 434, 422, 406, 389, 363, 327, 278, 225, 180, 135, 96, 64, 43, 19, 8, 6, 2, 0, 0, 0, 0]
```
So far so good, all I need to do is choose the right tolerances. What % of pixels at % difference will qualify two frames as "different"? What % will qualify them as "the same"? I began to run some tests on the entire set of frames...and quickly discovered a problem.

Here's the values for two frames that have subtle differences but are definitely visually distinct.
```
[1270464, 595110, 351411, 206595, 131779, 28208, 4108, 571, 64, 12, 0, 0, 0,...]
```
I discovered that the presense of any gradient-like changes in the animation (such as something beginning to glow or a cloud of gas dissipating in the air) would result in a long string of frames where uniform subtle differences would occur. I was unable to find tolerances that were sensitive enough to detect these gradiential sections without resulting in a huge number of identical frames being marked as different.

# Attempt #3 - Now what?








It ended up being a long journey into pixel formats, colorspaces, and the fundamental details of digital color representation. It's a lot more complex than I ever realized and a frequently misunderstood topic, which made finding accurate, specific, and useful documentation difficult. The journey was also filled with H.264 quantization noise, but more on that later.


# What approaches have I tried?

First I extract all the frames into 48-bit RGB PNG files using FFmpeg using its `-sws_flags +accurate_rnd+full_chroma_int` arguments to get better color conversion (the [docs](https://ffmpeg.org/ffmpeg-scaler.html#toc-Scaler-Options) are not very specific about what these flags do but my own testing showed they led to less [generation loss](https://en.wikipedia.org/wiki/Generation_loss) over repeated conversions.

1. For every RGB subpixel value in two subsequent frames, compute `abs(subpixel1 - subpixel2)` and generate a "difference frame" from all these computations (this is generally equivalent to putting two images in an editing program and setting the blending mode to "difference"). I would then take the difference frame and find the intensity percentage of every subpixel value and tally them up in a spreadsheet.
   1. Unfortunately two frames that appeared visually identical, when compared pixel by pixel, would usually have small changes in exact color value.
   2. Here's an excerpt image of two identical looking frames overlaid in Gimp 2.10.20r1, blended with difference mode and merged down into one, then the exposure cranked way up: ![H.264 quantization noise, I think](./readmeImages/pic1.PNG) I'm fairly certain this is H.264 quantization patterns and noise (especially the blue and orange patterns), only so clearly seen because the encoder input was so similar, but if anyone has better explanation, please let me know!
2. Part 2


# Story of this project

## Duplicate frames in animated content? Sounds easy.

You sound like me when I started this project!

My original premise was that `there would be no difference between two subsequent frames where no noticeable visual changes take place`. Definitely untrue for live action footage due to [sensor noise](https://en.wikipedia.org/wiki/Image_noise#In_digital_cameras). But animated content doesn't use a real camera. It's also ideal because there are many frames where nothing moves. Static shots, animation drawn to [not update every frame](https://en.wikipedia.org/wiki/Inbetweening#Frame_frequency), and simple visuals gave me hope that I could precisely detect duplicate frames.

I looked for existing software to do this task. And it exists, but nothing promised to truly detect duplicates. It only promised to detect frames that [do not differ greatly](https://ffmpeg.org/ffmpeg-filters.html#mpdecimate) from the previous frame. There were also some image-based solutions that worked based on [perceptual hashing](https://en.wikipedia.org/wiki/Perceptual_hashing) which is itself based on similarity. At this point I didn't understand why every algorithm was designed to do fuzzy comparisons.

The first problem was getting access to the video data. I couldn't think of a good reason to spend the extra effort parsing the raw H.264 data myself so I settled for extracting the frames with FFmpeg, which is basically the swiss army knife for video file manipulation.

### But what image format to extract to?

FFprobe (FFmpeg's sibling that does video file analysis) tells me that my video is "yuv420p".

I'm used to thinking of RGB `(red intensity, green intensity, blue intensity)`, but YUV is `(luminance intensity, blue difference, red difference)`. The "420" references to 4:2:0 [chroma subsampling](https://en.wikipedia.org/wiki/Chroma_subsampling). Color scientists figured out a long time ago that human eyes are more sensitive to changes in brightness/intensity/luminance than they are sensitive to changes in color. So somebody smartly decided that while storing that beautiful 1080p video footage, they should store the Y' luma channel in full resolution, but store the U and V color channels in a lower resolution, thereby saving a lot of bits. And turns out, you can barely tell a difference.

Now one of the great tragedies of digital video is that you lose some data almost every time you have to convert or encode something. There's too many reasons why this happens to explain them all, but the general rule of thumb is that you want to do a few conversions as possible in order to preserve quality.

