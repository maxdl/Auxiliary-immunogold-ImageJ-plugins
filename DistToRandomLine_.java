/*
    plugin : DistToRandomLine_.java
    date   : May 13, 2014
    author : Max Larsson
    e-mail : max.larsson@liu.se

    This ImageJ plugin is used to quantify particle labelling in 
	relation to randomly placed line regions of interest. It is used 
	in conjunction with DistToPath.py.
        
*/

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.Font.*;
import java.lang.Object.*;
import java.lang.Integer.*;
import java.lang.Math.*;
import java.io.*;
import java.util.*;
import java.util.Random;
import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.text.*;



interface OptionsDTRL {
    Color lineCol = Color.cyan;
    Color particleCol = Color.yellow;
    Color shellCol = Color.blue;
}

public class DistToRandomLine_ extends PlugInFrame implements OptionsDTRL, ActionListener {
    
    Panel panel;
    static Frame instance;
    static Frame infoFrame;
    GridBagLayout infoPanel;
    Label profile_nLabel;
    Label pnLabel;    
    Label linePlacedLabel;        
    Label commentLabel;
    Label scaleLabel;
    ProfileDataDTRL profile;
    ImagePlus imp;
    boolean UseRGB = true;        

    
    public DistToRandomLine_() {
        super("DistToRandomLine");
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        profile = new ProfileDataDTRL();
        IJ.register(DistToRandomLine_.class);
        setLayout(new FlowLayout());
        setBackground(SystemColor.control);
        panel = new Panel();
        panel.setLayout(new GridLayout(0, 1, 4, 1));
        panel.setBackground(SystemColor.control);    
        panel.setFont(new Font("Helvetica", 0, 12));
        addButton("Save profile");
        addButton("Clear profile");
		panel.add(new Label(""));		
        addButton("Suggest random line");		
		addButton("Use this line");		
        panel.add(new Label(""));
        addButton("Define particle list");		
		panel.add(new Label(""));        
        addButton("Add comment");
        addButton("Set profile n");
        addButton("Options...");        
        add(panel);
        pack();
        setVisible(true);
        infoFrame = new Frame("Profile info");
        infoPanel = new GridBagLayout();
        infoFrame.setFont(new Font("Helvetica", 0, 10));
        infoFrame.setBackground(SystemColor.control);
        infoFrame.setLocation(0, instance.getLocation().x + instance.getSize().height + 3);
        infoFrame.setIconImage(instance.getIconImage());
        infoFrame.setResizable(false);
        GridBagConstraints c = new GridBagConstraints();    
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.gridwidth = 1;
        addStaticInfoLabel("Profile n:", Label.LEFT, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        profile_nLabel = new Label(IJ.d2s(profile.ntot, 0), Label.RIGHT);
        addVarInfoLabel(profile_nLabel, c);                
        c.gridwidth = 1;                
        addStaticInfoLabel("Particles:", Label.LEFT, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        pnLabel = new Label(IJ.d2s(profile.pn, 0), Label.RIGHT);
        addVarInfoLabel(pnLabel, c);
		c.gridwidth = 1;       
        addStaticInfoLabel("Line defined:", Label.LEFT, c);
        c.gridwidth = GridBagConstraints.REMAINDER;    
		linePlacedLabel = new Label("no", Label.RIGHT);
		addVarInfoLabel(linePlacedLabel, c);                                                
        c.gridwidth = 1;                
        addStaticInfoLabel("Pixel width:", Label.LEFT, c);               
        c.gridwidth = GridBagConstraints.REMAINDER;
        scaleLabel = new Label("N/D", Label.RIGHT);
        addVarInfoLabel(scaleLabel, c);                                             
        c.gridwidth = 1;
        addStaticInfoLabel("Comment:", Label.LEFT, c);                               
        c.gridwidth = GridBagConstraints.REMAINDER;
        commentLabel = new Label("", Label.RIGHT);
        addVarInfoLabel(commentLabel, c);
        infoFrame.setLayout(infoPanel);
        infoFrame.pack();
        infoFrame.setSize(instance.getSize().width, infoFrame.getSize().height);
        infoFrame.setVisible(true);
        instance.requestFocus();
    }
    
    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        panel.add(b);
    }

    void addStaticInfoLabel(String name, int alignment, GridBagConstraints c) {
         Label l = new Label(name, alignment);
         infoPanel.setConstraints(l, c);
         infoFrame.add(l);
    }   
    
    void addVarInfoLabel(Label l, GridBagConstraints c) {
         infoPanel.setConstraints(l, c);
         infoFrame.add(l);
    }   
                
    PolygonRoi getPointRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != roi.POINT) {
            IJ.error("DistToRandomLine", "Point selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    double getPixelWidth() {
		double pixelwidth;
		
        Calibration c = imp.getCalibration();
        if (c.getUnit() == "micron") {
            pixelwidth = c.pixelWidth * 1000;
        } else {
            pixelwidth = c.pixelWidth;
        }
		return pixelwidth;
	}

    String getUnit() {
		String unit;
		
        Calibration c = imp.getCalibration();
        if (c.getUnit() == "micron") {
            unit = "nm";
        } else {
            unit = c.getUnit();
        }
		return unit;
	}
	
	
    void updateInfoPanel() {
        int i, psdntot = 0;
        double pixelwidth;
		String unit;
            
        profile_nLabel.setText(IJ.d2s(profile.ntot, 0));
        pnLabel.setText(IJ.d2s(profile.pn, 0));
        if (profile.linePlaced) {
            linePlacedLabel.setText("yes");
        }             
		unit = getUnit();
		pixelwidth = getPixelWidth();
        if (unit.equals("micron")) {
            pixelwidth = pixelwidth * 1000;
			unit = "nm";
        } 
        scaleLabel.setText(IJ.d2s(pixelwidth, 2) + " " + unit);
        commentLabel.setText(profile.comment);                
        infoFrame.setVisible(true);
    }
    
	void addPath(Vector<Roi> pathli, Shape shape, Color col, BasicStroke stroke) {
		Roi r = new ShapeRoi(shape);
		r.setInstanceColor(col);
		r.setStroke(stroke);
		pathli.addElement(r);
	}
            
    public void actionPerformed(ActionEvent e) {
		PolygonRoi p;
        int i;
                
        imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.beep();
            IJ.showStatus("No image");
            return;
        }
        String command = e.getActionCommand();
        if (command == null) {
            return; 
        }
        if (UseRGB && imp.getType() != ImagePlus.COLOR_RGB) {
            imp.setProcessor(imp.getTitle(), imp.getProcessor().convertToRGB());
        }                
        if (command.equals("Save profile")) {
            if (profile.dirty == false) {
                IJ.showMessage("Nothing to save.");
            } else {
                boolean saved = profile.save(imp);
                if (saved) {
                    profile.clear(imp);
                }
            }
        }
        if (command.equals("Clear profile")) {
            if (profile.dirty) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                    "DistToRandomLine", "Save current\nprofile?");
                if (d.yesPressed()) {
                    profile.dirty = !profile.save(imp);
                } else if (!d.cancelPressed()) {
                    profile.dirty = false;
                }                                
            }
            if (!profile.dirty) {
                profile.clear(imp);
                IJ.showStatus("Profile cleared.");
            }   
        }   
        if (command.equals("Use this line")) {
            if (!profile.isSameImage(imp) ||
                 profile.isPlaced(imp, profile.linePlaced, "Line")) { 
                return;
            }    		
			profile.line = profile.suggested_line;
			ShapeRoi lroi = new ShapeRoi(profile.line);
			if (UseRGB) { 
				imp.setColor(lineCol);
			} else { 
				imp.setColor(Color.black);
			}                                                
			lroi.drawPixels(imp.getProcessor());
			ShapeRoi sroi = new ShapeRoi(profile.suggested_shell);
			if (UseRGB) { 
				imp.setColor(shellCol);
			} else { 
				imp.setColor(Color.black);
			}                                                			
			sroi.drawPixels(imp.getProcessor());
			imp.getCanvas().setDisplayList(null);
			profile.lineBoundingRect = sroi.getBounds();
			profile.linePlaced = true;
			profile.dirty = true;
        }   
        if (command.equals("Define particle list")) {
            if (!profile.isSameImage(imp) ||
                profile.isDefined(imp, profile.pn, 0, "Particles")) { 
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                profile.pn = p.getNCoordinates();
                int[] x = p.getXCoordinates();
                int[] y = p.getYCoordinates();
                profile.px = new double[profile.pn]; 
                profile.py = new double[profile.pn];     
                Rectangle r = p.getBoundingRect();
                for (i = 0; i < profile.pn; i++) {
                    profile.px[i] = (double) x[i] + r.x;
                    profile.py[i] = (double) y[i] + r.y;
                }
                if (UseRGB) { 
                    imp.setColor(particleCol);
                } else { 
                    imp.setColor(Color.white);
                }                                                
                p.drawPixels();
                imp.setColor(Color.black);                                
                profile.dirty = true;
            }   
        }
       if (command.equals("Suggest random line")) {             
			if (getUnit().equals(" ")) {
				IJ.error("Error: scale has not been set");
				return;
			}
		    double pixelwidth = getPixelWidth();
			double length = profile.randomLineLength / pixelwidth;
			double d = profile.shellWidth / pixelwidth;			
			if (length > imp.getHeight() / 2) {
				IJ.error("Error: line length can not be greater than 1/2 of the image height.\n"+
						 "Please adjust in Options.");
				return;
			}
			if (d > length / 2) {
				IJ.error("Error: shell width can not be greater than 1/2 of the line length.\n"+
						 "Please adjust in Options.");
				return;
			}			
            Random rnd = new Random();			
			double x1=0, y1=0, x2=0, y2=0;				
			Polygon r = new Polygon();
			Rectangle imgRect = new Rectangle(0, 0, imp.getWidth(), imp.getHeight());
			while (true) {
				x1 = rnd.nextInt(imp.getWidth()-1)+1;
				y1 = rnd.nextInt(imp.getHeight()-1)+1;			
				double alpha = Math.asin(rnd.nextFloat());
				if (rnd.nextBoolean()) {
					alpha = -alpha;  // to get the full range of possible angles
				}
				x2 = x1 + Math.cos(alpha) * length;
				y2 = y1 + Math.sin(alpha) * length;
				double dx = Math.cos(Math.PI / 2 - alpha) * d;
				double dy = Math.sin(Math.PI / 2 - alpha) * d;
				r.addPoint((int) Math.round(x1 - dx), (int) Math.round(y1 + dy));
				r.addPoint((int) Math.round(x1 + dx), (int) Math.round(y1 - dy));			
				r.addPoint((int) Math.round(x2 + dx), (int) Math.round(y2 - dy));			
				r.addPoint((int) Math.round(x2 - dx), (int) Math.round(y2 + dy));
				if (imgRect.contains(r.getBounds())) {
					break;
				}
				r.reset();
			}
			Vector<Roi> pathli = new Vector<Roi>();
			profile.suggested_shell = r;
			profile.suggested_line = new Line2D.Double(x1, y1, x2, y2);
			addPath(pathli, profile.suggested_shell, Color.magenta, 
					new BasicStroke(2, BasicStroke.CAP_BUTT, 
					   				BasicStroke.JOIN_MITER, 
									1, new float[] {5, 5}, 1));
			addPath(pathli, profile.suggested_line, Color.green, 
					new BasicStroke(2, BasicStroke.CAP_BUTT, 
					   				BasicStroke.JOIN_MITER, 
									1, new float[] {5, 5}, 1));
			imp.getCanvas().setDisplayList(pathli);
			imp.getCanvas().repaint();
		}
        if (command.equals("Set profile n")) {
            String s = IJ.getString("Set profile n", IJ.d2s(profile.ntot, 0));
            profile.ntot = java.lang.Integer.parseInt(s);
        }     
        if (command.equals("Add comment")) {
            String s = IJ.getString("Comment: ", profile.comment);
            if (s != "") {
                profile.comment = s;
                profile.dirty = true;
            }
        }
		if (command.equals("Options...")) {
            GenericDialog gd = new GenericDialog("Options");
            gd.setInsets(0, 0, 0);            
            gd.addCheckbox("Use RGB colour", UseRGB);            
            gd.addMessage("Random line dimensions:");
            gd.addNumericField("Random line length (metric units):", profile.randomLineLength, 0);
            gd.addNumericField("Shell width (metric units):", profile.shellWidth, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            UseRGB = gd.getNextBoolean();
            profile.randomLineLength = (int) gd.getNextNumber();
            if (profile.randomLineLength <=0) {
                IJ.error("Horizontal n must be larger than 0. Reverting to default value (500).");
                profile.randomLineLength = 500;
            }
            profile.shellWidth = (int) gd.getNextNumber();            
        }                        
	    updateInfoPanel();
        imp.updateAndDraw();
        IJ.showStatus("");
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID()==WindowEvent.WINDOW_CLOSING) {
            infoFrame.dispose();
            infoFrame = null;
            instance = null;
        }
    }

} // end of DistToRandomLine_


class ProfileDataDTRL implements OptionsDTRL {
    boolean dirty;
	Line2D.Double line, suggested_line;
	Polygon suggested_shell;
	int randomLineLength=500, shellWidth=30;
    double[] px, py;
    int i, n, ntot, pn;
    boolean linePlaced;
    Rectangle lineBoundingRect;    	
    int imgID, prevImgID;
    String ID, comment, prevImg;
    
    ProfileDataDTRL () {
        this.n = 0;
        this.ntot = 1;
        this.prevImg = "";
        this.imgID = 0;
        this.dirty = false;
        this.pn = 0;
        this.linePlaced = false;
        this.comment = "";
        this.ID = "";
    }
    

    public String showPCoords(int n) {
        return(IJ.d2s(this.px[n], 2) + ", " + IJ.d2s(this.py[n], 2));
    }
	
    public boolean isSameImage(ImagePlus imp) {
        if (!this.dirty || this.imgID == 0) {
            this.imgID = imp.getID();
            return true;
        } else if (this.imgID == imp.getID()) {
            return true;
        } else {
            IJ.error("DistToRandomLine", "All measurements must be performed on the same " + 
                                   "image.");
            return false;
        }
    }       
        
    public boolean isDefined(ImagePlus imp, int var_to_check, int not_defined_val, String warnstr) {
        if (var_to_check != not_defined_val) {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                             "DistToRandomLine", "Warning:\n" + warnstr + " already defined.\nOverwrite?");
            if (!d.yesPressed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPlaced(ImagePlus imp, boolean var_to_check, String warnstr) {
        if (var_to_check) {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                             "DistToRandomLine", "Warning:\n" + warnstr + " already placed.\nOverwrite?");
            if (!d.yesPressed()) {
                return true;
            }
        }
        return false;
    }
    
    private boolean CheckProfileDataDTRL(ImagePlus imp) {
        String[] warnstr;
        int i, nwarn = 0;
        
        warnstr = new String[9];
        Calibration c = imp.getCalibration();
        if (c.getUnit().equals(" ")) {
            IJ.error("DistToRandomLine", "Error: The scale has not been set.");
            return false;
        }
        if (!this.linePlaced) {
            IJ.error("DistToRandomLine", "Error: Line not defined.");
            return false;
        }
        if (this.pn == 0) {
            warnstr[nwarn++] = "No particle coordinates defined.";
        }          
        if (nwarn > 0) {
            for (i = 0; i < nwarn; i++) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                    "DistToRandomLine", "Warning:\n" + warnstr[i] + "\nContinue anyway?");
                if (!d.yesPressed()) {
                    return false;
                }
            }
        }       
        return true;
    }
        
    
    public boolean save(ImagePlus imp) {
        int i, p, k;
        double pixelwidth;
        String s, unit;
        
        IJ.showStatus("Saving profile...");
        if (!CheckProfileDataDTRL(imp)) {
            return false;
        }
        Calibration c = imp.getCalibration();
        if (c.pixelWidth != c.pixelHeight) {
            IJ.showMessage("Warning: pixel aspect ratio is not 1.\n" +
                           "Only pixel WIDTH is used.");
        }
        try {
            if (imp.getTitle() != this.prevImg) {
                    this.n = 0;
                    this.prevImg = imp.getTitle();
            } 
            this.n++;
            s = IJ.getString("Profile ID: ", IJ.d2s(this.ntot, 0));
            if (s != "") {
                this.ID = s;
            }
            SaveDialog sd = new SaveDialog("Save profile", 
                                           imp.getTitle() + "." + 
                                           IJ.d2s(this.n, 0), ".d2p");
            if (sd.getFileName() == null) {
                this.n--;                                            
                return false;
            }
            PrintWriter outf = 
                new PrintWriter(
                    new BufferedWriter(
                        new FileWriter(sd.getDirectory() + 
                                       sd.getFileName())));
            outf.println("IMAGE " + imp.getTitle());                    
            outf.println("PROFILE_ID " + this.ID);
            outf.println("COMMENT " + this.comment);
            if (c.getUnit() == "micron") {
                pixelwidth = c.pixelWidth * 1000;
				unit = "nm";
            } else {
                pixelwidth = c.pixelWidth;
				unit = c.getUnit();
            }
            outf.println("PIXELWIDTH " + IJ.d2s(pixelwidth) + " " + unit);
            outf.println("PATH");
			outf.println("  " + IJ.d2s(this.line.getX1()) + ", " + IJ.d2s(this.line.getY1()));
			outf.println("  " + IJ.d2s(this.line.getX2()) + ", " + IJ.d2s(this.line.getY2()));
            outf.println("END");            
            outf.println("PARTICLES");
            if (this.pn > 0) { 
                for (i = 0; i < this.pn; i++) {
                    outf.println("  " + this.showPCoords(i));
                }
            }
            outf.println("END");
            outf.close();                    
        } catch (Exception e) {
            return false;
        }
        writeIDtext(imp);                
        this.ntot++;        
        SaveDialog sd = new SaveDialog("Save analyzed image", 
                                       imp.getShortTitle(), 
                                       ".a.tif");
        if (sd.getFileName() != null) {
            FileSaver saveTiff = new FileSaver(imp);
            saveTiff.saveAsTiff(sd.getDirectory() + sd.getFileName());
        }                                             
        return true;
    }       

    private void writeIDtext(ImagePlus imp) {
        TextRoi idLabel;
        int k, i, locx, locy;
        
        idLabel = new TextRoi(0, 0, imp);
        for (i=0; i < this.ID.length(); i++) {
            idLabel.addChar(this.ID.charAt(i));
        }
        locy = this.lineBoundingRect.y - idLabel.getBounds().height - 5;
        locx = this.lineBoundingRect.x;
        if (locy < 0) {
            locy = this.lineBoundingRect.y + this.lineBoundingRect.height + 5;
            if (locy + idLabel.getBounds().height > imp.getHeight()) {
                     locy = this.lineBoundingRect.y;
                locx = this.lineBoundingRect.x - idLabel.getBounds().width - 5;
                if (locx < 0) {
                   locx = this.lineBoundingRect.x + this.lineBoundingRect.width;
                }                     
            }
        }
        if (locx + idLabel.getBounds().width > imp.getWidth()) {
            locx = imp.getWidth() - idLabel.getBounds().width - 5;
        }
        idLabel.setLocation(locx, locy);
        idLabel.setFont(idLabel.getFont(), 24, idLabel.getStyle());
        if (imp.getType() == ImagePlus.COLOR_RGB) { 
            imp.setColor(lineCol);
        } else { 
            imp.setColor(Color.white);
        }
        idLabel.drawPixels(); 
        imp.setColor(Color.black);        
    }        
    
    
    public void clear(ImagePlus imp) {       
        this.dirty = false;
        this.pn = 0;
		this.linePlaced = false;
        this.comment = "";
        this.ID = "";
        Analyzer a = new Analyzer(imp);
        a.getResultsTable().reset();
        a.updateHeadings();
    }
} // end of DistToRandomLine
