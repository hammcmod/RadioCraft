# VOX Button Texture Requirements

## File to Edit
`src/main/resources/assets/radiocraft/textures/gui/vhf_handheld_widgets.png`

## Texture Specifications
The texture file is 256×256 pixels.

## VOX Button Sprite Layout

Add a 30×21 pixel button sprite at the following coordinates:

### Button States (30×21 each)
1. **VOX Off (Normal)**: Position `(u=76, v=120)`
2. **VOX Off (Hover)**: Position `(u=106, v=120)` - 30 pixels to the right
3. **VOX On (Normal)**: Position `(u=76, v=141)` - 21 pixels down
4. **VOX On (Hover)**: Position `(u=106, v=141)` - 30 pixels right, 21 down

### Design Recommendations
- Button should display "VOX" text clearly
- Off state: Darker/inactive appearance
- On state: Brighter/active appearance (consider green tint to match LCD indicator)
- Hover states: Slight highlight or border effect
- Style should match existing toggle buttons in the texture (like the power button at u=0, v=0)

## Current Implementation Status
✅ All code changes complete
❌ Texture sprites need to be added

## Testing
Once texture is added, test in-game:
1. Open VHF Handheld radio GUI
2. Verify VOX button appears near LED indicators (to the right of DATA LED)
3. Click to toggle - button should change visual state
4. Green "VOX" text should appear on LCD when enabled
5. With VOX enabled, speaking should transmit without holding PTT
