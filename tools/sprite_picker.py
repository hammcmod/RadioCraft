"""
GUI Sprite Picker Tool for RadioCraft
Click on sprites in a texture to get their UV coordinates
"""
import tkinter as tk
from tkinter import filedialog, messagebox
from PIL import Image, ImageTk
import pyperclip

class SpritePickerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("RadioCraft Sprite Picker")
        
        # State
        self.image = None
        self.photo = None
        self.scale = 2  # Zoom level
        self.start_x = None
        self.start_y = None
        self.rect_id = None
        self.texture_width = 256
        self.texture_height = 256
        
        # UI Setup
        self.setup_ui()
        
    def setup_ui(self):
        # Control panel
        control_frame = tk.Frame(self.root)
        control_frame.pack(side=tk.TOP, fill=tk.X, padx=5, pady=5)
        
        tk.Button(control_frame, text="Load PNG", command=self.load_image).pack(side=tk.LEFT, padx=5)
        
        tk.Label(control_frame, text="Texture Size:").pack(side=tk.LEFT, padx=5)
        self.width_entry = tk.Entry(control_frame, width=5)
        self.width_entry.insert(0, "256")
        self.width_entry.pack(side=tk.LEFT)
        tk.Label(control_frame, text="x").pack(side=tk.LEFT)
        self.height_entry = tk.Entry(control_frame, width=5)
        self.height_entry.insert(0, "256")
        self.height_entry.pack(side=tk.LEFT)
        
        tk.Label(control_frame, text="Zoom:").pack(side=tk.LEFT, padx=(20,5))
        tk.Button(control_frame, text="-", command=self.zoom_out).pack(side=tk.LEFT)
        self.zoom_label = tk.Label(control_frame, text="2x", width=4)
        self.zoom_label.pack(side=tk.LEFT)
        tk.Button(control_frame, text="+", command=self.zoom_in).pack(side=tk.LEFT)
        
        # Canvas with scrollbars
        canvas_frame = tk.Frame(self.root)
        canvas_frame.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        self.canvas = tk.Canvas(canvas_frame, bg='gray', cursor='crosshair')
        
        h_scrollbar = tk.Scrollbar(canvas_frame, orient=tk.HORIZONTAL, command=self.canvas.xview)
        h_scrollbar.pack(side=tk.BOTTOM, fill=tk.X)
        
        v_scrollbar = tk.Scrollbar(canvas_frame, orient=tk.VERTICAL, command=self.canvas.yview)
        v_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        self.canvas.config(xscrollcommand=h_scrollbar.set, yscrollcommand=v_scrollbar.set)
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        # Bind mouse events
        self.canvas.bind("<ButtonPress-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_release)
        self.canvas.bind("<Motion>", self.on_motion)
        
        # Info panel
        info_frame = tk.Frame(self.root)
        info_frame.pack(side=tk.BOTTOM, fill=tk.X, padx=5, pady=5)
        
        self.coord_label = tk.Label(info_frame, text="Mouse: (0, 0)", anchor=tk.W)
        self.coord_label.pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        self.result_label = tk.Label(info_frame, text="Click and drag to select sprite", anchor=tk.W, fg='blue')
        self.result_label.pack(side=tk.LEFT, fill=tk.X, expand=True)
        
    def load_image(self):
        filepath = filedialog.askopenfilename(
            title="Select Texture PNG",
            initialdir="c:/Dev/RadioCraft/src/main/resources/assets/radiocraft/textures/gui",
            filetypes=[("PNG files", "*.png"), ("All files", "*.*")]
        )
        
        if filepath:
            self.image = Image.open(filepath)
            self.texture_width = self.image.width
            self.texture_height = self.image.height
            
            self.width_entry.delete(0, tk.END)
            self.width_entry.insert(0, str(self.texture_width))
            self.height_entry.delete(0, tk.END)
            self.height_entry.insert(0, str(self.texture_height))
            
            self.display_image()
            
    def display_image(self):
        if self.image:
            # Update texture size from entries
            try:
                self.texture_width = int(self.width_entry.get())
                self.texture_height = int(self.height_entry.get())
            except:
                pass
            
            # Scale image
            scaled_width = int(self.image.width * self.scale)
            scaled_height = int(self.image.height * self.scale)
            scaled_image = self.image.resize((scaled_width, scaled_height), Image.NEAREST)
            
            self.photo = ImageTk.PhotoImage(scaled_image)
            self.canvas.delete("all")
            self.canvas.create_image(0, 0, anchor=tk.NW, image=self.photo)
            self.canvas.config(scrollregion=(0, 0, scaled_width, scaled_height))
            
    def zoom_in(self):
        if self.scale < 8:
            self.scale += 1
            self.zoom_label.config(text=f"{self.scale}x")
            self.display_image()
            
    def zoom_out(self):
        if self.scale > 1:
            self.scale -= 1
            self.zoom_label.config(text=f"{self.scale}x")
            self.display_image()
            
    def on_motion(self, event):
        if self.image:
            x = int(self.canvas.canvasx(event.x) / self.scale)
            y = int(self.canvas.canvasy(event.y) / self.scale)
            self.coord_label.config(text=f"Mouse: ({x}, {y})")
            
    def on_press(self, event):
        if self.image:
            self.start_x = int(self.canvas.canvasx(event.x) / self.scale)
            self.start_y = int(self.canvas.canvasy(event.y) / self.scale)
            
    def on_drag(self, event):
        if self.image and self.start_x is not None:
            end_x = int(self.canvas.canvasx(event.x) / self.scale)
            end_y = int(self.canvas.canvasy(event.y) / self.scale)
            
            # Draw selection rectangle
            if self.rect_id:
                self.canvas.delete(self.rect_id)
                
            x1, y1 = min(self.start_x, end_x) * self.scale, min(self.start_y, end_y) * self.scale
            x2, y2 = max(self.start_x, end_x) * self.scale, max(self.start_y, end_y) * self.scale
            
            self.rect_id = self.canvas.create_rectangle(x1, y1, x2, y2, outline='red', width=2)
            
    def on_release(self, event):
        if self.image and self.start_x is not None:
            end_x = int(self.canvas.canvasx(event.x) / self.scale)
            end_y = int(self.canvas.canvasy(event.y) / self.scale)
            
            # Calculate sprite dimensions
            u = min(self.start_x, end_x)
            v = min(self.start_y, end_y)
            width = abs(end_x - self.start_x)
            height = abs(end_y - self.start_y)
            
            if width > 0 and height > 0:
                result = f"u={u}, v={v}, width={width}, height={height}"
                self.result_label.config(text=result)
                
                # Copy to clipboard
                code = f"{u}, {v}, {width}, {height}, {self.texture_width}, {self.texture_height}"
                try:
                    pyperclip.copy(code)
                    messagebox.showinfo("Copied!", 
                        f"UV Coordinates:\n{result}\n\n"
                        f"Texture Size: {self.texture_width}x{self.texture_height}\n\n"
                        f"Full parameters copied to clipboard:\n{code}")
                except:
                    messagebox.showinfo("Sprite Selected", 
                        f"UV Coordinates:\n{result}\n\n"
                        f"Texture Size: {self.texture_width}x{self.texture_height}\n\n"
                        f"Use these values: {code}")
                
            self.start_x = None
            self.start_y = None

if __name__ == "__main__":
    root = tk.Tk()
    root.geometry("800x600")
    app = SpritePickerApp(root)
    root.mainloop()
