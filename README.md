# What is this?

My goal was to detect exact-duplicate frames in some 2D animation footage. When I started this project I had no idea if this goal was reasonable or not (turns out it is a lot harder than I expected, this codebase hasn't achieved its goal yet).

## The Setting

My initial assumptions:
   1. Subsequent frames with no visually noticeable difference would have identical or nearly-identical digital pixel values.
   2. Because I was working with simple 2D animation, subsequent frames wouldn't have any sensor noise, and would be easily detectable even if not exact digital duplicates.
   3. Calculating the "difference" between two frames was a straightforward concept.

Some limitations:
   1. I would work with the frames extracted into individuals images instead of inside the codec (H.264) for simplicity reasons.
   2. I didn't want to use any AI or neural networks as they would never offer a 100% confidence rating (and there seemed to be value in having a definite algorithm).
   3. I wasn't using existing image comparison techniques ([perceptual hashes](https://en.wikipedia.org/wiki/Perceptual_hashing)) as they were also based on fuzzy comparisons.

## The Setup

The H.264 video I had on hand was encoded as YUV with 4:2:0 subsampling. 

I initially looked for an image format that supported YUV so I didn't have to lose anything to the color conversion process, but sadly none of the popular file formats were suitable. WebP had YUV support, but only in [lossy encoding mode](https://developers.google.com/speed/webp/faq#what_color_spaces_does_the_webp_format_support). Tiff also had support, but in an extension to the spec (and some image editing programs had trouble opening it, I suspect support for more than [baseline](https://en.wikipedia.org/wiki/TIFF#Part_1:_Baseline_TIFF) is spotty).

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
>0% diff, >1% , >2% , etc 
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
I discovered that the presense of any gradient-like changes in the animation (such as something beginning to glow or a cloud of gas dissipating in the air) would result in a long string of frames where uniform subtle differences would occur. I was unable to find tolerances that were sensitive enough to detect these gradiential sections without resulting in a huge number of identical frames being marked as distinct.

# Attempt #3 - Back to fundamentals

In hitting the walls described above I began to fall down the rabbit hole of, "What is color, how has it been digitalized, and how are different representations distinct?". I thought color was simple but turns out you could write an entire book in answer to those questions.

### Color is really really really complicated (and confusing)

I want to emphasize this as it's probably the most important thing I've learned. Our digital color representations trace directly back to some really messy and hard to quantify things, like the physics of light, the biology of our eyes and brains, and psychological interferences, like how we perceive patterns and are easily tricked by visual illusions. I think I always thought of digital color as really simple. There was red, green, and blue, and they would slide around from 0 to 255. But when I began to dig into the topic it was almost overwhelming. As far as I can tell, we've been trying really hard for over a hundred years to try and capture "human color perception" inside of a bunch of equations (and we've  experienced mixed results).

And also the field is really confusing. Even when I finally found experts discussing the topic, they have to devote considerable time to fighting misconceptions surrounding terminology.

### Charles Poynton has a goldmine of articles on the topic

I was drowning in confusing terminology on Wikipedia and Stackoverflow until I finally stumbled across Charles Poynton's website: http://www.poynton.ca/

He has several links on his site about color, gamma, and video engineering. I found them to be highly relevant and very precise.

### Things are often not linear

Here's an example of something complicated *and* confusing (and highly relevant to the problem I was trying to solve). Human "brightness" perception is nonlinear (think about the difference between pure darkness, lighting a single candle, and then lighting a second candle, is the change from 1 candle to 2 as big as the change from 0 to 1? How about the difference between 99 candles and 100?). So when we got around to packing light into 256 integer values some people realized it would be more efficient if we didn't store things linearly (enter stage left, "gamma transfer functions"). What this means that the bits in an image file might not be linearly related to how bright your computer monitor should light itself up when displaying that image. Take a peek at this article where Dr. Poynton answers a bunch of questions surrounding the concept of gamma (especially question 5): http://www.poynton.ca/PDFs/GammaFAQ.pdf

Think about what this nonlinearity might mean when you're trying to calculate a "difference" between two given pixels buy just directly cracking open a PNG and comparing values.


# Attempt #4 - A new approach

I want to talk for a second about my third assumption. The one where I said that "comparing" two colors was a straightforward concept. Turns out, it's really not. And the way I was going about it (the absolute difference of individual RGB values in a PNG file) is really really not the way to go.

I have now realized that what I really wanted was to calculate the *perceptual* difference between two frames, pixels, or colors. What I really wanted was a LAB colorspace, which, according to Wikipedia, are designed to be *perceptually uniform*. RGB representations are, as far as I can tell, not suitable for this goal. 


# Where to go next?








