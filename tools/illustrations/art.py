"""KarmaKitchen illustration set. Run: python3 art.py  -> writes out/svg/*.svg and out/xml/*.xml"""
import os
from vd import *

# ------------------------------------------------------------------ palette
WHITE = "#F4F7F2"
CREAM = "#F6EAD0"; CREAM_D = "#E3CFA4"
GREEN_L = "#B5E3AE"; GREEN = "#7DC87A"; GREEN_M = "#4FA05B"; GREEN_D = "#2F6F3E"; GREEN_DD = "#1F4A2A"
AMBER_L = "#FFD58A"; AMBER = "#FFB347"; AMBER_D = "#F28C28"; AMBER_DD = "#C76A12"
RED = "#E2543B"; RED_D = "#C23F2A"; CORAL = "#FF7468"
YELLOW = "#F4CC3A"; YELLOW_D = "#DCAB2B"
BREAD_L = "#EBB874"; BREAD = "#D99A52"; BREAD_D = "#B8782F"
CLAY = "#D9774B"; CLAY_L = "#F0A27A"; CLAY_D = "#B45B33"
INK = "#1C2A22"; INK2 = "#2B3A31"
GREY_L = "#E8EFE6"; GREY = "#A7B6A8"; GREY_D = "#CBD6CB"
BLUE_L = "#BFE4F7"; BLUE = "#7EC8F0"; BLUE_D = "#4FA3D1"
SKIN = "#F2C093"; SKIN_D = "#DDA574"; HAIR = "#2B1D14"
MAROON = "#9E3B3B"


def shadow(cx, cy, rx, ry=None, a=0.28):
    return P(ellipse(cx, cy, rx, ry or rx * 0.14), fill="#000000", fa=a)


# ================================================================== STEP ART (120 x 120)
def camera():
    k = [shadow(60, 108, 40, 5)]
    k.append(P(rrect(40, 20, 40, 22, (9, 9, 0, 0)), fill=GREY_L))
    k.append(P(rrect(88, 26, 14, 10, 4), fill=AMBER))
    k.append(P(rrect(10, 36, 100, 68, 15), fill=GREY_L))
    k.append(P(rrect(10, 78, 100, 26, (0, 0, 15, 15)), fill=GREY_D))
    k.append(P(rrect(17, 41, 86, 3, 1.5), fill=WHITE, fa=0.8))
    k.append(P(rrect(19, 46, 17, 11, 3.5), fill=AMBER_L))
    k.append(P(rrect(22, 48.5, 6, 3, 1.5), fill=WHITE, fa=0.9))
    k.append(P(circle(62, 71, 29), fill=GREY))
    k.append(P(circle(62, 71, 25), fill=INK))
    k.append(P(circle(62, 71, 18), fill=GREEN_D))
    k.append(P(circle(62, 71, 11), fill=GREEN_DD))
    k.append(P(circle(55, 64, 4.5), fill=WHITE, fa=0.85))
    k.append(P(circle(70, 78, 2.2), fill=WHITE, fa=0.5))
    k.append(P(spark(104, 13, 9), fill=AMBER_L))
    k.append(P(spark(14, 20, 6), fill=GREEN))
    k.append(P(spark(111, 36, 4), fill=GREEN_L))
    return k, 120, 120


def mappin():
    k = []
    tile = rrect(8, 26, 104, 82, 16)
    k.append(P(tile, fill=GREEN_M))
    m = [P(circle(30, 46, 18), fill=GREEN, fa=0.9),
         P("M8,92 C30,78 52,104 76,94 S104,88 112,92 V108 H8 Z", fill=BLUE),
         P("M8,66 H112", stroke=CREAM, sw=8, cap="butt"),
         P("M46,26 V108", stroke=CREAM, sw=8, cap="butt"),
         P("M86,26 L112,56", stroke=CREAM, sw=6, cap="butt"),
         P(rrect(56, 74, 18, 10, 3), fill=GREEN_D, fa=0.5),
         P(rrect(14, 74, 22, 10, 3), fill=GREEN_D, fa=0.5),
         P(rrect(56, 34, 22, 12, 3), fill=GREEN_D, fa=0.5)]
    k.append(G(m, clip=tile))
    # route along the road, ending at the pin
    for i in range(6):
        k.append(P(circle(32 + i * 7.5, 66, 2.1), fill=GREEN_D))
    k.append(P(circle(19, 66, 6.5), fill=GREEN_D))
    k.append(P(circle(19, 66, 2.6), fill=CREAM))
    k.append(shadow(78, 67, 10, 3.2, 0.3))
    k.append(P("M78,66 C78,66 59,48 59,32 A19,19 0 1 1 97,32 C97,48 78,66 78,66 Z", fill=CORAL))
    k.append(P("M78,66 C78,66 97,48 97,32 A19,19 0 0 0 78,13 Z", fill=RED_D, fa=0.3))
    k.append(P(circle(78, 32, 8), fill=WHITE))
    k.append(P(spark(106, 14, 6), fill=AMBER_L))
    k.append(P(spark(12, 22, 5), fill=GREEN_L))
    return k, 120, 120


def scooter():
    k = [shadow(60, 106, 46, 4.5)]
    # delivery box on the rack
    k.append(P(rrect(4, 22, 42, 34, 7), fill=AMBER))
    k.append(P(rrect(4, 22, 42, 11, (7, 7, 0, 0)), fill=AMBER_D))
    k.append(P(rrect(21, 29, 8, 6, 2), fill=AMBER_DD))
    k.append(heart(25, 46, 11, CREAM))
    # rider
    k.append(P("M60,52 L66,72 L78,78", stroke="#4A5D73", sw=8, cap="round", join="round"))
    k.append(P(rrect(46, 26, 22, 32, 11), fill=GREEN_D))
    k.append(P("M62,36 L90,40", stroke=GREEN_D, sw=7, cap="round"))
    k.append(P(circle(58, 20, 9.5), fill=SKIN))
    k.append(P("M48,20 A10,10 0 0 1 68,20 Z", fill=AMBER))
    k.append(P(rrect(60, 18, 9, 4, 2), fill=AMBER_DD, fa=0.5))
    # scooter body
    k.append(P(rrect(14, 58, 46, 26, 13), fill=GREEN))
    k.append(P(rrect(44, 74, 42, 10, 5), fill=GREEN_M))
    k.append(P("M80,80 L85,46 C86,41 90,40 93,41 L97,42 C100,43 100,46 99,50 L93,84 Z", fill=GREEN))
    k.append(P(rrect(18, 52, 34, 9, 4.5), fill=INK2))
    k.append(P("M91,43 L87,34 L99,32", stroke=GREY, sw=4, cap="round", join="round"))
    k.append(P(circle(100, 50, 5), fill=AMBER_L))
    k.append(P(circle(100, 50, 2.4), fill=AMBER))
    # wheels
    for cx in (30, 94):
        k.append(P(circle(cx, 92, 14), fill=INK2))
        k.append(P(circle(cx, 92, 8), fill=GREY))
        k.append(P(circle(cx, 92, 3), fill=INK))
    k.append(P(spark(110, 14, 7), fill=AMBER_L))
    return k, 120, 120


def polaroid():
    k = []
    inner = [shadow(60, 112, 34, 4, 0.22)]
    inner.append(P(rrect(24, 16, 76, 92, 8), fill="#000000", fa=0.22))
    inner.append(P(rrect(20, 12, 76, 92, 8), fill=WHITE))
    photo = rrect(27, 19, 62, 62, 4)
    inner.append(P(photo, fill=AMBER_L))
    ph = [P(rrect(27, 66, 62, 20, 0), fill=AMBER, fa=0.55),
          P(ellipse(58, 88, 26, 17), fill=MAROON),
          P(rrect(53, 64, 10, 12, 3), fill=SKIN_D),
          P(circle(58, 52, 17), fill=SKIN),
          P("M40.5,52 C39,32 77,32 75.5,52 C71,44 46,44 40.5,52 Z", fill=HAIR),
          P(circle(51, 54, 2.1), fill=INK),
          P(circle(65, 54, 2.1), fill=INK),
          P(circle(46.5, 60, 3.4), fill=CORAL, fa=0.45),
          P(circle(69.5, 60, 3.4), fill=CORAL, fa=0.45),
          P("M49.5,60 Q58,71 66.5,60 Z", fill=INK),
          P("M52,60.4 Q58,63.6 64,60.4 L63.2,62.6 Q58,65.2 52.8,62.6 Z", fill=WHITE)]
    inner.append(G(ph, clip=photo))
    inner.append(P(rrect(28, 90, 40, 4.5, 2.2), fill=GREY, fa=0.7))
    inner.append(P(rrect(28, 97, 26, 4.5, 2.2), fill=GREY, fa=0.45))
    k.append(G(inner, rot=-6, px=60, py=60))
    k.append(heart(98, 20, 20, CORAL, rot=14))
    k.append(P(spark(14, 26, 6), fill=AMBER_L))
    k.append(P(spark(108, 78, 5), fill=GREEN_L))
    return k, 120, 120


def karma():
    k = [shadow(60, 108, 42, 5)]
    # gift card behind
    card = [P(rrect(0, 0, 66, 44, 8), fill=AMBER),
            P(rrect(0, 11, 66, 10, 0), fill=AMBER_D),
            P(circle(14, 33, 5.5), fill=CREAM, fa=0.95),
            P(rrect(26, 29, 30, 4.5, 2.2), fill=CREAM, fa=0.8),
            P(rrect(26, 36, 20, 3.5, 1.7), fill=CREAM, fa=0.5)]
    k.append(place(card, 76, 40, rot=10, lc=(33, 22)))
    # coin stack
    for i, y in enumerate((92, 83, 74)):
        k.append(P(rrect(16, y, 46, 10, 0), fill=YELLOW_D))
        k.append(P(ellipse(39, y + 10, 23, 7), fill=YELLOW_D))
        k.append(P(ellipse(39, y, 23, 7), fill=YELLOW))
        k.append(P(ellipse(39, y, 16, 4.2), fill=YELLOW_D, fa=0.5))
    # front coin
    k.append(P(circle(80, 78, 25), fill=YELLOW_D))
    k.append(P(circle(80, 76, 25), fill=YELLOW))
    k.append(P(circle(80, 76, 18.5), fill=YELLOW_D, fa=0.35))
    k.append(P(circle(80, 76, 16), fill=YELLOW))
    k.append(heart(80, 76, 17, AMBER_DD))
    k.append(P("M65,62 A19,19 0 0 1 84,57", stroke=WHITE, sw=3, sa=0.6, cap="round"))
    k.append(P(spark(14, 38, 7), fill=AMBER_L))
    k.append(P(spark(106, 100, 5), fill=GREEN_L))
    return k, 120, 120


# ================================================================== FOOD ITEMS (96 x 96)
def bowl(sh=True):
    k = [shadow(48, 86, 32, 4.5)] if sh else []
    for dx in (-12, 0, 12):
        k.append(P("M%s,34 c-5,-6 5,-10 0,-17" % f(48 + dx), stroke=WHITE, sw=3, sa=0.5, cap="round"))
    k.append(P("M20,52 C20,38 34,30 48,30 C62,30 76,38 76,52 Z", fill=WHITE))
    k.append(P("M24,52 C28,43 38,48 46,43 C54,38 66,44 72,52 Z", fill=AMBER))
    k.append(P(circle(40, 36, 2.4), fill=GREEN))
    k.append(P(circle(58, 40, 2), fill=GREEN))
    k.append(P(circle(50, 34, 1.8), fill=GREEN_M))
    k.append(P("M12,50 H84 C84,72 70,84 48,84 C26,84 12,72 12,50 Z", fill=CLAY))
    k.append(P("M12,50 H84 C84,56 82,62 79,67 C70,60 26,60 17,67 C14,62 12,56 12,50 Z", fill=CLAY_L, fa=0.55))
    k.append(P(rrect(9, 46, 78, 8, 4), fill=CLAY_L))
    k.append(P("M20,66 C24,74 34,80 46,80", stroke=CLAY_D, sw=3, sa=0.35, cap="round"))
    return k, 96, 96


def bread(sh=True):
    k = [shadow(48, 82, 34, 4.5)] if sh else []
    k.append(P("M12,50 C12,32 28,22 48,22 C68,22 84,32 84,50 V64 A9,9 0 0 1 75,73 H21 A9,9 0 0 1 12,64 Z", fill=BREAD))
    k.append(P("M12,50 C12,32 28,22 48,22 C68,22 84,32 84,50 C74,40 60,36 48,36 C36,36 22,40 12,50 Z", fill=BREAD_L, fa=0.55))
    for x in (30, 46, 62):
        k.append(P("M%s,34 L%s,50" % (f(x + 3), f(x - 3)), stroke=BREAD_D, sw=4, cap="round", sa=0.8))
    k.append(P("M12,64 A9,9 0 0 0 21,73 H75 A9,9 0 0 0 84,64 V60 C70,66 26,66 12,60 Z", fill=BREAD_D, fa=0.4))
    return k, 96, 96


def apple(sh=True):
    k = [shadow(48, 86, 28, 4)] if sh else []
    k.append(P("M48,30 C40,23 18,27 18,52 C18,70 32,86 42,86 C45,86 46,84 48,84 C50,84 51,86 54,86 "
               "C64,86 78,70 78,52 C78,27 56,23 48,30 Z", fill=RED))
    k.append(P("M48,30 C56,23 78,27 78,52 C78,70 64,86 54,86 C60,76 66,64 64,50 C63,40 56,34 48,30 Z",
               fill=RED_D, fa=0.45))
    k.append(P("M28,44 C30,36 36,33 42,33", stroke=WHITE, sw=4, sa=0.45, cap="round"))
    k.append(P("M48,31 Q47,22 52,15", stroke=BREAD_D, sw=3.5, cap="round"))
    k.append(P("M51,26 C52,17 60,12 68,13 C68,22 61,28 51,26 Z", fill=GREEN))
    return k, 96, 96


def carrot(sh=True):
    body = [P("M36,34 C36,26 60,26 60,34 C60,54 54,72 48,88 C42,72 36,54 36,34 Z", fill=AMBER_D),
            P("M54,30 C60,34 60,34 60,34 C60,54 54,72 48,88 C54,66 56,48 54,30 Z", fill=AMBER_DD, fa=0.4),
            P("M40,42 H46", stroke=AMBER_DD, sw=3, sa=0.55),
            P("M50,54 H56", stroke=AMBER_DD, sw=3, sa=0.55),
            P("M41,64 H46", stroke=AMBER_DD, sw=3, sa=0.55),
            P("M48,30 C44,20 38,16 33,10 C41,10 47,15 49,24 Z", fill=GREEN_M),
            P("M48,30 C52,18 57,12 64,8 C64,17 57,25 50,30 Z", fill=GREEN),
            P("M48,30 C46,20 47,12 50,5 C55,12 53,23 48,30 Z", fill=GREEN_D)]
    k = [shadow(48, 90, 24, 3.5)] if sh else []
    k.append(place(body, 48, 52, rot=28, lc=(48, 52)))
    return k, 96, 96


def pack(sh=True):
    k = [shadow(48, 86, 30, 4)] if sh else []
    k.append(P("M70,22 L82,28 V74 L70,80 Z", fill=AMBER_DD))
    k.append(P(rrect(22, 16, 48, 66, 6), fill=AMBER))
    k.append(P(rrect(22, 16, 48, 12, (6, 6, 0, 0)), fill=AMBER_D))
    k.append(P(rrect(22, 38, 48, 24, 0), fill=CREAM))
    k.append(P(circle(46, 50, 9), fill=GREEN))
    k.append(P("M42,54 C42,46 50,44 52,44 C52,52 48,56 42,54 Z", fill=WHITE, fa=0.9))
    k.append(P(rrect(28, 68, 22, 3.5, 1.7), fill=AMBER_DD, fa=0.5))
    return k, 96, 96


def milk(sh=True):
    k = [shadow(48, 86, 26, 4)] if sh else []
    k.append(P(rrect(26, 34, 44, 50, 4), fill=WHITE))
    k.append(P("M26,36 L36,16 H60 L70,36 Z", fill=BLUE_L))
    k.append(P(rrect(36, 10, 24, 9, 2.5), fill=BLUE_D))
    k.append(P(rrect(26, 50, 44, 18, 0), fill=BLUE))
    k.append(P("M48,53 C52,58 54,60 54,63 A6,6 0 0 1 42,63 C42,60 44,58 48,53 Z", fill=WHITE))
    k.append(P("M60,36 H70 V84 A4,4 0 0 1 66,84 H60 Z", fill=BLUE_D, fa=0.18))
    return k, 96, 96



# ================================================================== TIER MEDALS (96 x 96)
def _mix(hex_a, hex_b, t):
    a = [int(hex_a[i:i + 2], 16) for i in (1, 3, 5)]
    b = [int(hex_b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#%02X%02X%02X" % tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def _star(cx, cy, ro, ri):
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        r = ro if i % 2 == 0 else ri
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    return pts


def medal(base):
    light = _mix(base, "#FFFFFF", 0.5)
    dark = _mix(base, "#000000", 0.28)
    k = [shadow(48, 91, 24, 3.2)]
    k.append(P(poly([(27, 3), (45, 3), (59, 42), (41, 42)]), fill=GREEN_M))
    k.append(P(poly([(51, 3), (69, 3), (55, 42), (37, 42)]), fill=GREEN))
    k.append(P(circle(48, 62, 28), fill=dark))
    k.append(P(circle(48, 60, 28), fill=base))
    k.append(P(circle(48, 60, 21.5), fill=light, fa=0.55))
    k.append(P(circle(48, 60, 18.5), fill=base))
    k.append(P(poly(_star(48, 60, 12, 5)), fill=light))
    k.append(P("M28,48 A24,24 0 0 1 44,37", stroke="#FFFFFF", sw=3, sa=0.55, cap="round"))
    return k, 96, 96


# ================================================================== KARMA COIN (96 x 96)
def karma_coin():
    """The app's currency: a gold coin with a bowl of food on its face and a crown above it."""
    k = [shadow(48, 91, 26, 3.4)]
    # coin: edge, face, raised rim, embossed face
    k.append(P(circle(48, 61, 31), fill=YELLOW_D))
    k.append(P(circle(48, 58, 31), fill=YELLOW))
    k.append(P(circle(48, 58, 25.5), fill=YELLOW_D, fa=0.55))
    k.append(P(circle(48, 58, 23), fill="#F8D95C"))
    # bowl of food
    k.append(P("M32,57 H64 C64,68 57.5,74 48,74 C38.5,74 32,68 32,57 Z", fill=AMBER_DD))
    k.append(P(rrect(30, 54, 36, 6, 3), fill=AMBER_D))
    k.append(P("M36,54 C36,46 42,42 48,42 C54,42 60,46 60,54 Z", fill=WHITE))
    k.append(P("M40,50 C43,46 47,48 49,45 C52,43 56,46 57,50 Z", fill=AMBER, fa=0.9))
    # crown sitting on the top edge
    k.append(P("M29,26 L25,11 L37,19 L48,7 L59,19 L71,11 L67,26 Z", fill=AMBER_D))
    k.append(P("M29,26 L25,11 L37,19 L48,7 L48,26 Z", fill=AMBER, fa=0.9))
    k.append(P(rrect(28, 24, 40, 8, 3), fill=AMBER_DD))
    k.append(P(rrect(28, 24, 40, 3.5, 1.7), fill=AMBER_L, fa=0.6))
    for cx, cy in ((25, 11), (48, 7), (71, 11)):
        k.append(P(circle(cx, cy, 3.4), fill=CORAL))
        k.append(P(circle(cx - 0.8, cy - 0.8, 1.1), fill=WHITE, fa=0.8))
    k.append(P("M22,48 A28,28 0 0 1 36,32", stroke=WHITE, sw=3, sa=0.5, cap="round"))
    return k, 96, 96


# ================================================================== GIVE-BACK ART (96 x 96)
def tree(sh=True):
    k = [shadow(48, 88, 26, 3.6)] if sh else []
    k.append(P("M43,58 H53 L55,86 H41 Z", fill=BROWN if 'BROWN' in globals() else BREAD_D))
    k.append(P(circle(48, 38, 24), fill=GREEN_M))
    k.append(P(circle(32, 52, 17), fill=GREEN))
    k.append(P(circle(64, 52, 17), fill=GREEN))
    k.append(P(circle(48, 56, 16), fill=GREEN_M))
    k.append(P(circle(40, 30, 9), fill=GREEN_L, fa=0.45))
    k.append(heart(48, 48, 14, "#FFFFFF", fa=0.9))
    k.append(P(spark(80, 20, 6), fill=AMBER_L))
    k.append(P(spark(14, 34, 5), fill=GREEN_L))
    return k, 96, 96


# ================================================================== ROLE ART (120 x 120)
def ngo_kitchen():
    """A community kitchen: the receiver's picture."""
    k = [shadow(60, 108, 46, 5)]
    # chimney with steam
    k.append(P(rrect(86, 22, 12, 24, 2), fill=CLAY_D))
    k.append(P("M92,20 c-3,-4 3,-6 0,-11", stroke=WHITE, sw=3, sa=0.5, cap="round"))
    # walls
    k.append(P(rrect(16, 52, 88, 54, 6), fill=CREAM))
    k.append(P(rrect(16, 92, 88, 14, (0, 0, 6, 6)), fill=CREAM_D))
    # roof
    k.append(P("M8,56 L60,16 L112,56 Z", fill=CLAY))
    k.append(P("M8,56 L60,16 L60,56 Z", fill=CLAY_L, fa=0.45))
    k.append(P(rrect(8, 52, 104, 7, 3), fill=CLAY_D))
    # heart sign in the gable
    k.append(P(circle(60, 40, 13), fill=WHITE))
    k.append(heart(60, 41, 17, CORAL))
    # door and windows
    k.append(P(rrect(49, 72, 22, 34, (11, 11, 0, 0)), fill=GREEN_D))
    k.append(P(circle(66, 91, 1.8), fill=AMBER_L))
    for x in (24, 84):
        k.append(P(rrect(x, 68, 16, 16, 3), fill=BLUE_L))
        k.append(P("M%s,68 V84 M%s,76 H%s" % (f(x + 8), f(x), f(x + 16)), stroke=CREAM, sw=2))
    # a bush and a bowl on the step
    k.append(P(circle(108, 100, 9), fill=GREEN_M))
    k.append(P(circle(100, 104, 7), fill=GREEN))
    k.append(P(spark(14, 28, 6), fill=AMBER_L))
    return k, 120, 120

# ================================================================== HERO (176 x 144)
def hero_food():
    k = [shadow(94, 128, 72, 7, 0.3)]
    for kids, cx, cy, s, rot in ((carrot(False)[0], 134, 46, 0.9, 20), (bread(False)[0], 36, 90, 1.0, -8),
                                 (bowl(False)[0], 94, 80, 1.32, 0), (apple(False)[0], 140, 106, 0.84, 8)):
        k.append(place(kids, cx, cy, s=s, rot=rot, lc=(48, 48)))
    k.append(P(spark(20, 28, 8), fill=AMBER_L))
    k.append(P(spark(166, 20, 6), fill=GREEN_L))
    k.append(P(spark(74, 12, 5), fill=AMBER_L))
    return k, 176, 144


ART = {
    "illus_step_camera": camera, "illus_step_map": mappin, "illus_step_pickup": scooter,
    "illus_step_smile": polaroid, "illus_step_karma": karma,
    "illus_food_meal": bowl, "illus_food_bread": bread, "illus_food_fruit": apple,
    "illus_food_veg": carrot, "illus_food_pack": pack, "illus_food_dairy": milk,
    "illus_hero_food": hero_food, "illus_karma_coin": karma_coin, "illus_cause_tree": tree, "illus_role_ngo": ngo_kitchen,
    "illus_medal_bronze": lambda: medal("#D4915A"), "illus_medal_silver": lambda: medal("#B7C0C9"),
    "illus_medal_gold": lambda: medal("#F2C14E"), "illus_medal_platinum": lambda: medal("#8FD3E8"),
}

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    for sub in ("svg", "xml"):
        os.makedirs(os.path.join(here, "out", sub), exist_ok=True)
    for name, fn in ART.items():
        kids, w, h = fn()
        open(os.path.join(here, "out", "svg", name + ".svg"), "w").write(to_svg(kids, w, h))
        dp = {"illus_hero_food": 176}.get(name, 120 if name.startswith("illus_step") else 96)
        open(os.path.join(here, "out", "xml", name + ".xml"), "w").write(to_vd(kids, w, h, dp))
    print("wrote", len(ART), "illustrations")
