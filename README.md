#  how to generate 2-colors, PNG8, 600px png

```sh
$ identify -verbose pngs/g*.png | egrep "Colors:|Depth:|Version|Filename:"
  Filename: pngs/g.png
  Depth: 8-bit
  Colors: 58
  Filename: pngs/g2.png
  Depth: 8-bit
  Colors: 2
  Filename: pngs/g3.png
  Depth: 8/1-bit
  Colors: 2
  Version: ImageMagick 7.1.0-2 Q16 x86_64 2021-06-25 https://imagemagick.org
```

![img1](pngs/img.png)

![img2](pngs/img_1.png)

![img3](pngs/img_2.png)
