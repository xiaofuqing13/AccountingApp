from PIL import Image, ImageDraw

def crop_center_rounded(input_path, output_path, extract_size=(360, 360), radius=80):
    img = Image.open(input_path).convert("RGBA")
    w, h = img.size
    
    # Calculate box to tightly wrap the central glass card
    left = (w - extract_size[0]) // 2
    top = (h - extract_size[1]) // 2
    right = left + extract_size[0]
    bottom = top + extract_size[1]
    
    img_cropped = img.crop((left, top, right, bottom))
    
    # Create smooth rounded mask to remove any surrounding prompt background
    mask = Image.new('L', extract_size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, extract_size[0], extract_size[1]), radius=radius, fill=255)
    
    # Apply mask and save
    result = Image.new('RGBA', extract_size, (0, 0, 0, 0))
    result.paste(img_cropped, (0, 0), mask=mask)
    result.save(output_path, "PNG")

if __name__ == '__main__':
    crop_center_rounded('glass_heart_icon_1.png', 'glass_heart_icon_1_cropped.png', extract_size=(380, 380), radius=90)
    crop_center_rounded('glass_heart_icon_2.png', 'glass_heart_icon_2_cropped.png', extract_size=(380, 380), radius=90)
    print("Cropped successfully!")
