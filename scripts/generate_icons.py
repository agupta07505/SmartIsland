import os
from PIL import Image, ImageDraw

logo_path = r"A:\SmartIsland\assets\logo.png"
res_dir = r"A:\SmartIsland\app\src\main\res"

# Densities configuration
# Format: (suffix, adaptive_size, adaptive_logo_size, legacy_size, legacy_logo_size, round_logo_size)
configs = [
    ("mdpi", 108, 70, 48, 40, 36),
    ("hdpi", 162, 105, 72, 60, 54),
    ("xhdpi", 216, 140, 96, 80, 72),
    ("xxhdpi", 324, 210, 144, 120, 108),
    ("xxxhdpi", 432, 280, 192, 160, 144),
]

def generate():
    if not os.path.exists(logo_path):
        print(f"Error: Logo file not found at {logo_path}")
        return
        
    img = Image.open(logo_path)
    print(f"Loaded logo from {logo_path} (size: {img.size}, format: {img.format})")

    for suffix, a_sz, a_logo_sz, l_sz, l_logo_sz, r_logo_sz in configs:
        density_dir = os.path.join(res_dir, f"mipmap-{suffix}")
        os.makedirs(density_dir, exist_ok=True)
        print(f"Generating icons for mipmap-{suffix}...")
        
        # 1. Adaptive foreground (transparent background)
        a_foreground = Image.new("RGBA", (a_sz, a_sz), (0, 0, 0, 0))
        logo_resized = img.resize((a_logo_sz, a_logo_sz), Image.Resampling.LANCZOS)
        offset = (a_sz - a_logo_sz) // 2
        a_foreground.paste(logo_resized, (offset, offset), logo_resized)
        a_foreground.save(os.path.join(density_dir, "ic_launcher_foreground.png"), "PNG")
        
        # 2. Legacy launcher icon (solid black square background)
        l_icon = Image.new("RGBA", (l_sz, l_sz), (0, 0, 0, 255)) # black background
        logo_resized_legacy = img.resize((l_logo_sz, l_logo_sz), Image.Resampling.LANCZOS)
        offset_legacy = (l_sz - l_logo_sz) // 2
        l_icon.paste(logo_resized_legacy, (offset_legacy, offset_legacy), logo_resized_legacy)
        # Convert to RGB since it has a solid black background
        l_icon_rgb = l_icon.convert("RGB")
        l_icon_rgb.save(os.path.join(density_dir, "ic_launcher.png"), "PNG")
        
        # 3. Legacy round launcher icon (solid black circle on transparent background)
        r_icon = Image.new("RGBA", (l_sz, l_sz), (0, 0, 0, 0))
        draw = ImageDraw.Draw(r_icon)
        draw.ellipse([(0, 0), (l_sz - 1, l_sz - 1)], fill=(0, 0, 0, 255))
        logo_resized_round = img.resize((r_logo_sz, r_logo_sz), Image.Resampling.LANCZOS)
        offset_round = (l_sz - r_logo_sz) // 2
        r_icon.paste(logo_resized_round, (offset_round, offset_round), logo_resized_round)
        r_icon.save(os.path.join(density_dir, "ic_launcher_round.png"), "PNG")

    print("All icons generated successfully!")

if __name__ == "__main__":
    generate()
