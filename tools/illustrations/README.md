# Illustration generator

The `illus_*.xml` files in `app/src/main/res/drawable` are generated from `art.py`. You only need this folder if you want to change a colour or a shape; the app does not depend on it.

```
cd tools/illustrations
python3 art.py
```

This writes `out/svg/*.svg` (open them in a browser to preview) and `out/xml/*.xml` (Android vector drawables). Copy the XML files you changed into `app/src/main/res/drawable`.

- `vd.py` is a tiny helper that draws shapes once and writes both formats.
- `art.py` holds the palette at the top and one function per illustration.
