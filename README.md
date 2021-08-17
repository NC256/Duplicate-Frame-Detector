# What is this?

This is the result of an experimental attempt to detect duplicate frames in some 2D animation video files I had. Unfortunately, it does not work (yet?).

Every existing solution I found was based on a "similarity" measurement, which is useful for live action footage which is inherently filled with sensor noise, but isn't very precise. I thought it would be easier to create a more exact solution for 2D animation, which has much simpler visuals.

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

I looked for existing software to do this task. And it exists, but nothing promised to truly detect duplicates. It only promised to detect frames that [do not differ greatly](https://ffmpeg.org/ffmpeg-filters.html#mpdecimate) from the previous frame. There were also some image-based solutions that worked based on [perceptual hasing](https://en.wikipedia.org/wiki/Perceptual_hashing) which is itself based on similarity. At this point I didn't understand why every algorithm was designed to do fuzzy comparisons.

The first problem was getting access to the video data. I couldn't think of a good reason to spend the extra effort parsing the raw H.264 data myself so I settled for extracting the frames with FFmpeg, which is basically the swiss army knife for video file manipulation.

### But what image format to extract to?

FFprobe (FFmpeg's sibling that does video file analysis) tells me that my video is "yuv420p".

I'm used to thinking of RGB `(red intensity, green intensity, blue intensity)`, but YUV is `(luminance intensity, blue difference, red difference)`. The "420" references to 4:2:0 [chroma subsampling](https://en.wikipedia.org/wiki/Chroma_subsampling). Color scientists figured out a long time ago that human eyes are more sensitive to changes in brightness/intensity/luminance than they are sensitive to changes in color. So somebody smartly decided that while storing that beautiful 1080p video footage, they should store the Y' luma channel in full resolution, but store the U and V color channels in a lower resolution, thereby saving a lot of bits. And turns out, you can barely tell a difference.

Now one of the great tragedies of digital video is that you lose some data almost every time you have to convert or encode something. There's too many reasons why this happens to explain them all, but the general rule of thumb is that you want to do a few conversions as possible in order to preserve quality.

