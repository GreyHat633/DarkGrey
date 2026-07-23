import cv2
import numpy as np
import os
from scipy.interpolate import griddata

# Load image
img_path = 'src/main/resources/assets/dark_grey/textures/items/itanis.png'
img = cv2.imread(img_path, cv2.IMREAD_UNCHANGED)
if img is None:
    print('Image not found!')
    exit(1)

h, w = img.shape[:2]

# Create coordinate grid
X, Y = np.meshgrid(np.arange(w), np.arange(h))

for frame in range(3):
    pull_factor = [0.3, 0.6, 1.0][frame]
    max_shift = w * 0.20 * pull_factor
    
    # We want to pull the string towards bottom-right (w, h)
    # The string is around the line x + y = w
    # Center of the string is (w/2, h/2)
    # The bow body is around x + y = w/2 (closer to top-left)
    # We need a deformation field.
    
    shift_X = np.zeros((h, w), dtype=np.float32)
    shift_Y = np.zeros((h, w), dtype=np.float32)
    
    proj_diag = (X + Y) / np.sqrt(2)
    proj_str = (X - Y) / np.sqrt(2)
    
    # String pulling: maximum at the center of the string
    string_center_diag = w / np.sqrt(2)
    dist_diag = np.abs(proj_diag - string_center_diag)
    dist_str = np.abs(proj_str)
    
    # Weight for the string center
    string_weight = np.exp(-(dist_diag**2) / (w * w * 0.05)) * np.exp(-(dist_str**2) / (w * w * 0.1))
    
    # Shift the string
    shift_X += string_weight * max_shift
    shift_Y += string_weight * max_shift
    
    # Bow limbs bending: tips are at (w, 0) and (0, h)
    # They should bend towards the bottom-right as well
    # Tip weight
    tip_weight = np.exp(-(proj_diag**2) / (w * w * 0.15))
    tip_bending = w * 0.05 * pull_factor * tip_weight
    shift_X += tip_bending
    shift_Y += tip_bending
    
    # Ensure the handle (top-left) doesn't move
    handle_weight = np.exp(-((X - w/4)**2 + (Y - h/4)**2) / (w * w * 0.05))
    shift_X *= (1 - handle_weight)
    shift_Y *= (1 - handle_weight)
    
    map_x = (X - shift_X).astype(np.float32)
    map_y = (Y - shift_Y).astype(np.float32)
    
    out = cv2.remap(img, map_x, map_y, cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(0,0,0,0))
    
    # Clean up alpha channel artifacts from interpolation
    alpha = out[:, :, 3]
    out[alpha < 128] = 0
    out[alpha >= 128, 3] = 255
    
    out_path = f'src/main/resources/assets/dark_grey/textures/items/itanis_pulling_{frame}.png'
    cv2.imwrite(out_path, out)
    print(f'Saved {out_path}')
