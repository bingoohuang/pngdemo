# 

```sh
$ identify -verbose g.png | egrep "Colors:|Depth:|Version"
  Depth: 8-bit
  Colors: 58
  Version: ImageMagick 7.1.0-4 Q16 x86_64 2021-07-18 https://imagemagick.org
$ identify -verbose g2.png | egrep "Colors:|Depth:|Version"
  Depth: 8-bit
  Colors: 2
  Version: ImageMagick 7.1.0-4 Q16 x86_64 2021-07-18 https://imagemagick.org
$ identify -verbose g3.png | egrep "Colors:|Depth:|Version"
  Depth: 8-bit
  Colors: 2
  Version: ImageMagick 7.1.0-4 Q16 x86_64 2021-07-18 https://imagemagick.org
```

![img_1.png](img_1.png)