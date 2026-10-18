#!/usr/bin/env python3
import subprocess

# Colors from the user's icon
# Deep Islamic emerald green
BG_COLOR = "#0A5C36"
# Vibrant golden yellow
GOLD_COLOR = "#FFBA08"

# We will generate a 1024x1024 master icon
cmd = f"""
convert -size 1024x1024 xc:none \\
  -fill "{BG_COLOR}" \\
  -draw "roundrectangle 0,0 1024,1024 220,220" \\
  /tmp/icon_bg.png
"""
subprocess.run(cmd, shell=True, check=True)
print("Background generated")
