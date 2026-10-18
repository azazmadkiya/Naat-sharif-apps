#!/usr/bin/env python3
import subprocess

# Precise parameters matching the user's uploaded icon
# Canvas size: 1024 x 1024
# Background: Deep Islamic emerald green
bg_color = "#0B5D39"
# Marigold / Gold yellow
gold_color = "#FFBA08"

# Geometry:
# Book width: 660px, from x = 182 to x = 842
# Left edge: x = 182
# Spine center: x = 508
# Right edge: x = 842

# Top edge:
# Top left corner: (182, 310)
# Top left arch: rises from 310 to peak at (330, 246), dips to spine notch at (508, 296)
# Top right arch: from spine (508, 296) rises to peak at (694, 246), dips to top right corner at (842, 310)

# Bottom edge:
# Bottom left corner: (182, 715)
# Bottom left curve: curves down to (340, 748), then rises/dips to center bottom spine point (508, 770)
# Bottom right curve: from center bottom (508, 770), curves down to (680, 748), then up to bottom right corner (842, 715)

# Wait, let's look at the bottom curve of the user's icon:
# In the user's icon:
# The bottom edge of the left page:
# Curves down to a trough around x=340, then curves up and into the center bottom V-point!
# Center bottom V-point points DOWNWARDS at (508, 765)!
# Left edge: x=182, from y=310 to y=715 (with slight corner rounding)
# Right edge: x=842, from y=310 to y=715 (with slight corner rounding)

# Inside the right page:
# Turned page: from spine x=508 to vertical right edge of turned page at x=735
# Wait! Look at the green cutout in the user's image:
# Is the turned page narrower or wider?
# Look at the user's image:
# The turned page:
# Its left edge is at the spine (x=508).
# Its right edge is a vertical line at around x=735!
# Then there is a green gap (the background) between x=735 and x=755?
# NO, look at the user's image:
# On the right page:
# There is a large green trapezoid!
# Inside the right half:
# Left side of green shape is a vertical edge at x = 540!
# Right side of green shape is a vertical edge at x = 750!
# Wait! Look closely at the user's image:
# In the right half:
# There is a vertical golden spine / page from x=508 to x=540?
# No! Look at the yellow shape in the right half:
# From x=508 (center spine) to x=735 is a yellow page that is tilted/turned towards us!
# Or wait, look at the GREEN cutout:
# The green cutout has a vertical left border, curved top, vertical right border, curved bottom.
# To the left of the green cutout is yellow: from spine x=508 to x=540? No, that's wide: from x=508 to x=730 is yellow!
# And to the right of the green cutout: there is an outer yellow frame at x=750 to x=842!
# Wait! Let's examine the green cutout again:
# Where is the green cutout located?
# Is the green cutout in the middle of the right page, or on the left of the right page?
# Look at the right half of the book in the user's image:
# Center spine is at the center.
# Right of the center spine:
# There is a yellow vertical surface (facing us).
# Then a vertical green cutout!
# Wait, let's look at the image from user:
# LEFT HALF:
# A large yellow filled page: width from ~18% to 50% of the canvas. Solid yellow.
# RIGHT HALF:
# From 50% to ~68% of the canvas is a yellow vertical block / turned page!
# From ~68% to ~76% is a GREEN cutout!
# From ~76% to ~82% is the outer right yellow frame!

svg_content = f"""<svg width="1024" height="1024" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
  <!-- Green rounded squircle background -->
  <rect width="1024" height="1024" rx="230" fill="{bg_color}"/>

  <!-- Entire Book Outer Silhouette in Golden Yellow -->
  <path d="M 190 310
           C 220 265, 330 240, 508 296
           C 686 240, 796 265, 826 310
           C 838 325, 838 340, 838 360
           L 838 680
           C 838 700, 830 715, 815 722
           C 760 745, 660 755, 508 775
           C 356 755, 256 745, 201 722
           C 186 715, 178 700, 178 680
           L 178 360
           C 178 340, 178 325, 190 310 Z" fill="{gold_color}"/>

  <!-- Center vertical spine slit/divider if any, or green cutout on the right -->
  <!-- Green Cutout on Right Page (creates the 3D turned page effect) -->
  <path d="M 548 335
           C 620 305, 700 305, 750 325
           L 750 675
           C 700 660, 620 665, 548 695
           Z" fill="{bg_color}"/>
</svg>"""

with open("/tmp/book_icon.svg", "w") as f:
    f.write(svg_content)

subprocess.run("ffmpeg -y -i /tmp/book_icon.svg app/src/main/res/drawable/test_user_icon.png", shell=True, check=True)
print("Rendered successfully")
