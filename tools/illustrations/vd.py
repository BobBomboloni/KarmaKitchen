"""Tiny helper to author flat vector illustrations once and emit BOTH an SVG (for previewing in a
browser) and an Android VectorDrawable XML. Only features that VectorDrawable supports are used:
paths (fill/stroke/alpha), groups (translate/rotate/scale) and clip paths."""
import math


def f(n):
    s = ("%.2f" % n).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


# ---------------------------------------------------------------- path builders
def circle(cx, cy, r):
    return "M%s,%s a%s,%s 0 1,0 %s,0 a%s,%s 0 1,0 %s,0 Z" % (
        f(cx - r), f(cy), f(r), f(r), f(2 * r), f(r), f(r), f(-2 * r))


def ellipse(cx, cy, rx, ry):
    return "M%s,%s a%s,%s 0 1,0 %s,0 a%s,%s 0 1,0 %s,0 Z" % (
        f(cx - rx), f(cy), f(rx), f(ry), f(2 * rx), f(rx), f(ry), f(-2 * rx))


def rrect(x, y, w, h, r=0):
    if isinstance(r, (int, float)):
        r = (r, r, r, r)
    tl, tr, br, bl = r
    p = ["M%s,%s" % (f(x + tl), f(y)), "H%s" % f(x + w - tr)]
    if tr:
        p.append("A%s,%s 0 0 1 %s,%s" % (f(tr), f(tr), f(x + w), f(y + tr)))
    p.append("V%s" % f(y + h - br))
    if br:
        p.append("A%s,%s 0 0 1 %s,%s" % (f(br), f(br), f(x + w - br), f(y + h)))
    p.append("H%s" % f(x + bl))
    if bl:
        p.append("A%s,%s 0 0 1 %s,%s" % (f(bl), f(bl), f(x), f(y + h - bl)))
    p.append("V%s" % f(y + tl))
    if tl:
        p.append("A%s,%s 0 0 1 %s,%s" % (f(tl), f(tl), f(x + tl), f(y)))
    p.append("Z")
    return " ".join(p)


def poly(pts):
    return "M" + " L".join("%s,%s" % (f(x), f(y)) for x, y in pts) + " Z"


def spark(cx, cy, r, k=0.16):
    """Four-point sparkle with concave sides."""
    c = r * k
    return ("M%s,%s Q%s,%s %s,%s Q%s,%s %s,%s Q%s,%s %s,%s Q%s,%s %s,%s Z" % (
        f(cx), f(cy - r), f(cx + c), f(cy - c), f(cx + r), f(cy),
        f(cx + c), f(cy + c), f(cx), f(cy + r),
        f(cx - c), f(cy + c), f(cx - r), f(cy),
        f(cx - c), f(cy - c), f(cx), f(cy - r)))


# Material "favorite" heart, 24 x 24 box (Apache 2.0). Place it with place().
HEART24 = ("M12,21.35 l-1.45,-1.32 C5.4,15.36 2,12.28 2,8.5 C2,5.42 4.42,3 7.5,3 "
           "c1.74,0 3.41,0.81 4.5,2.09 C13.09,3.81 14.76,3 16.5,3 C19.58,3 22,5.42 22,8.5 "
           "c0,3.78 -3.4,6.86 -8.55,11.54 L12,21.35 Z")


def bez(p0, p1, p2, p3, n):
    pts = []
    for i in range(n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


# ---------------------------------------------------------------- scene graph
class P:
    def __init__(self, d, fill=None, fa=1.0, stroke=None, sw=0, sa=1.0, cap="round", join="round"):
        self.d, self.fill, self.fa = d, fill, fa
        self.stroke, self.sw, self.sa, self.cap, self.join = stroke, sw, sa, cap, join


class G:
    def __init__(self, kids, tx=0, ty=0, rot=0, px=0, py=0, sx=1, sy=1, clip=None):
        self.kids, self.tx, self.ty, self.rot = kids, tx, ty, rot
        self.px, self.py, self.sx, self.sy, self.clip = px, py, sx, sy, clip


def place(kids, cx, cy, s=1.0, rot=0, lc=(0, 0)):
    """Put a group so that its local point `lc` lands on (cx, cy), scaled by s and rotated."""
    return G(kids, tx=cx - lc[0], ty=cy - lc[1], px=lc[0], py=lc[1], sx=s, sy=s, rot=rot)


def heart(cx, cy, size, fill, fa=1.0, rot=0):
    return place([P(HEART24, fill=fill, fa=fa)], cx, cy, s=size / 24.0, rot=rot, lc=(12, 12))


# ---------------------------------------------------------------- emitters
class _Ids:
    n = 0


def _svg_nodes(kids, defs):
    out = []
    for k in kids:
        if isinstance(k, P):
            a = ['d="%s"' % k.d]
            a.append('fill="%s"' % k.fill if k.fill else 'fill="none"')
            if k.fill and k.fa < 1:
                a.append('fill-opacity="%s"' % f(k.fa))
            if k.stroke:
                a += ['stroke="%s"' % k.stroke, 'stroke-width="%s"' % f(k.sw),
                      'stroke-linecap="%s"' % k.cap, 'stroke-linejoin="%s"' % k.join]
                if k.sa < 1:
                    a.append('stroke-opacity="%s"' % f(k.sa))
            out.append("<path %s/>" % " ".join(a))
        else:
            tr = "translate(%s,%s) rotate(%s) scale(%s,%s) translate(%s,%s)" % (
                f(k.tx + k.px), f(k.ty + k.py), f(k.rot), f(k.sx), f(k.sy), f(-k.px), f(-k.py))
            attr = 'transform="%s"' % tr
            if k.clip:
                _Ids.n += 1
                cid = "c%d" % _Ids.n
                defs.append('<clipPath id="%s"><path d="%s"/></clipPath>' % (cid, k.clip))
                attr += ' clip-path="url(#%s)"' % cid
            out.append("<g %s>%s</g>" % (attr, "".join(_svg_nodes(k.kids, defs))))
    return out


def to_svg(kids, w, h):
    defs = []
    body = "".join(_svg_nodes(kids, defs))
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">'
            "<defs>%s</defs>%s</svg>" % (w, h, w, h, "".join(defs), body))


def _vd_nodes(kids, ind):
    out = []
    pad = "    " * ind
    for k in kids:
        if isinstance(k, P):
            a = []
            if k.fill:
                a.append('android:fillColor="%s"' % k.fill)
                if k.fa < 1:
                    a.append('android:fillAlpha="%s"' % f(k.fa))
            if k.stroke:
                a += ['android:strokeColor="%s"' % k.stroke, 'android:strokeWidth="%s"' % f(k.sw),
                      'android:strokeLineCap="%s"' % k.cap, 'android:strokeLineJoin="%s"' % k.join]
                if k.sa < 1:
                    a.append('android:strokeAlpha="%s"' % f(k.sa))
            a.append('android:pathData="%s"' % k.d)
            out.append(pad + "<path\n" + "".join("%s    %s\n" % (pad, x) for x in a).rstrip("\n") + "/>")
        else:
            a = []
            if k.tx: a.append('android:translateX="%s"' % f(k.tx))
            if k.ty: a.append('android:translateY="%s"' % f(k.ty))
            if k.px: a.append('android:pivotX="%s"' % f(k.px))
            if k.py: a.append('android:pivotY="%s"' % f(k.py))
            if k.rot: a.append('android:rotation="%s"' % f(k.rot))
            if k.sx != 1: a.append('android:scaleX="%s"' % f(k.sx))
            if k.sy != 1: a.append('android:scaleY="%s"' % f(k.sy))
            head = pad + "<group" + ("".join("\n%s    %s" % (pad, x) for x in a)) + ">"
            inner = []
            if k.clip:
                inner.append('%s    <clip-path android:pathData="%s"/>' % (pad, k.clip))
            inner += _vd_nodes(k.kids, ind + 1)
            out.append(head + "\n" + "\n".join(inner) + "\n" + pad + "</group>")
    return out


def to_vd(kids, w, h, dp=None):
    dp = dp or w
    dh = round(dp * h / w)
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="%sdp"\n    android:height="%sdp"\n'
            '    android:viewportWidth="%s"\n    android:viewportHeight="%s">\n%s\n</vector>\n' % (
                dp, dh, w, h, "\n".join(_vd_nodes(kids, 1))))
