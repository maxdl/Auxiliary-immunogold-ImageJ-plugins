/*
    plugin : ParticleDensityRandomBox_.java
    date   : May 13, 2014
    author : Max Larsson
    e-mail : max.larsson@liu.se

    This ImageJ plugin is used to quantify particle labelling in 
	randomly placed rectangular regions of interest. It is used 
	in conjunction with PointDensity.py.
        
*/

import java.awt.*;
import java.awt.event.*;
import java.awt.Font.*;
import java.lang.Object.*;
import java.lang.Integer.*;
import java.io.*;
import java.util.*;
import java.util.Random;
import ij.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.gui.TextRoi.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.measure.*;
import ij.text.*;


interface OptionsPDRB {
    Color profileCol = Color.blue;
    Color particleCol = Color.green;
    Color boxCol = Color.magenta;           
}


public class ParticleDensityRandomBox_ extends PlugInFrame implements OptionsPDRB, ActionListener {
    
    Panel panel;
    static Frame instance;
    static Frame infoFrame;
    GridBagLayout infoPanel;
    Label profile_nLabel;
    Label pnLabel;
    Label boxPlacedLabel;
    Label commentLabel;
    Label scaleLabel;
    ProfileDataPDRB profile;
    ImagePlus imp;
    boolean UseRGB = true;    
    
    public ParticleDensityRandomBox_() {
        super("ParticleDensityRandomBox");
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        profile = new ProfileDataPDRB();
        IJ.register(ParticleDensityRandomBox_.class);
        setLayout(new FlowLayout());
        setBackground(SystemColor.control);
        panel = new Panel();
        panel.setLayout(new GridLayout(0, 1, 4, 1));
        panel.setBackground(SystemColor.control);    
        panel.setFont(new Font("Helvetica", 0, 12));
        addButton("Save profile");
        addButton("Clear profile");
        panel.add(new Label(""));
        addButton("Suggest random box");
        addButton("Use this box");
        panel.add(new Label(""));        
        addButton("Define particle list");		
		panel.add(new Label(""));
        panel.add(new Label("Other:"));                
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
        addStaticInfoLabel("Box placed:", Label.LEFT, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        boxPlacedLabel = new Label("no", Label.RIGHT);
        addVarInfoLabel(boxPlacedLabel, c);
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
        
    PolygonRoi getPolygonRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != roi.POLYGON) {
            IJ.error("ParticleDensity", "Polygon selection required.");
            return null;
        } else {
            return (PolygonRoi) roi; 
        }
    }
        
    PolygonRoi getPointRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != roi.POINT) {
            IJ.error("ParticleDensity", "Point selection required.");
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
        profile_nLabel.setText(IJ.d2s(profile.ntot, 0));
        pnLabel.setText(IJ.d2s(profile.pn, 0));
        if (profile.boxPlaced) {
            boxPlacedLabel.setText("yes");    
        }
		String unit = getUnit();
		double pixelwidth = getPixelWidth();
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
            }  else { 
				boolean saved = profile.save(imp);
                if (saved) {
                    profile.clear(imp);
                }
            }
        }
        if (command.equals("Clear profile")) {
            if (profile.dirty) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                    "Vesicle", "Save current\nprofile?");
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
        if (command.equals("Define particle list")) {
            if (!profile.isSameImage(imp) ||
                profile.isDefined(imp, profile.pn, 0, "Particles")) { 
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                profile.pn = p.getNCoordinates();
                int[] x = p.getXCoordinates();
                int[] y = p.getYCoordinates();
                profile.px = new float[profile.pn]; 
                profile.py = new float[profile.pn];     
                Rectangle r = p.getBoundingRect();
                for (int i = 0; i < profile.pn; i++) {
                    profile.px[i] = (float) x[i] + r.x;
                    profile.py[i] = (float) y[i] + r.y;
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
        if (command.equals("Use this box")) {
            if (!profile.isSameImage(imp) ||
                 profile.isPlaced(imp, profile.boxPlaced, "Box")) { 
                return;
            }    		
			profile.box = profile.suggestedBox;
			ShapeRoi broi = new ShapeRoi(profile.box);
			if (UseRGB) { 
				imp.setColor(boxCol);
			} else { 
				imp.setColor(Color.black);
			}                                                
			broi.drawPixels(imp.getProcessor());
			imp.getCanvas().setDisplayList(null);
			profile.boundingRect = broi.getBounds();
			profile.boxPlaced = true;
			profile.dirty = true;
        } 		
        if (command.equals("Suggest random box")) {             
			if (getUnit().equals(" ")) {
				IJ.error("Error: scale has not been set");
				return;
			}
		    double pixelwidth = getPixelWidth();
			double length = profile.boxSize / pixelwidth;
			if (length > imp.getHeight() / 2) {
				IJ.error("Error: line length can not be greater than 1/2 of the image height.\n"+
						 "Please adjust in Options.");
				return;
			}
            Random rnd = new Random();			
			double x=0, y=0;				
			Polygon r = new Polygon();
			Rectangle imgRect = new Rectangle(0, 0, imp.getWidth(), imp.getHeight());
			while (true) {
				x = rnd.nextInt(imp.getWidth() - 1) + 1;
				y = rnd.nextInt(imp.getHeight() - 1) + 1;			
				double alpha = Math.asin(rnd.nextFloat());
				if (rnd.nextBoolean()) {
					alpha = -alpha;  // to get the full range of possible angles
				}
				double a = Math.cos(alpha) * length;
				double b = Math.sin(alpha) * length;
				r.addPoint((int) Math.round(x), (int) Math.round(y));
				r.addPoint((int) Math.round(x + a), (int) Math.round(y - b));			
				r.addPoint((int) Math.round(x + a - b), (int) Math.round(y - a - b));			
				r.addPoint((int) Math.round(x - b), (int) Math.round(y - a));
				if (imgRect.contains(r.getBounds())) {
					break;
				}
				r.reset();
			}
			Vector<Roi> pathli = new Vector<Roi>();
			profile.suggestedBox = r;
			addPath(pathli, profile.suggestedBox, Color.magenta, 
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
            gd.addNumericField("Box side length (metric units):", profile.boxSize, 0);			
            gd.addCheckbox("Use RGB colour", UseRGB);
            gd.setInsets(15, 0, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            profile.boxSize = (int) gd.getNextNumber();				
            UseRGB = gd.getNextBoolean();            
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

} // end of ParticleDensityRandomBox_


class ProfileDataPDRB implements OptionsPDRB {  
    boolean dirty;
    float[] px, py;
    int n, ntot, pn, i;
	Polygon box, suggestedBox;
    boolean boxPlaced;
    int imgID, prevImgID;
    String ID, comment, prevImg;
    Rectangle boundingRect;
	int boxSize = 500;
    
    ProfileDataPDRB () {
        this.n = 0;
        this.ntot = 1;
        this.prevImg = "";
        this.imgID = 0;
        this.dirty = false;
        this.pn = 0;
        this.boxPlaced = false;
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
            IJ.error("ParticleDensity", "All measurements must be performed on the same " + 
                                   "image.");
            return false;
        }
    }       
        
    public boolean isDefined(ImagePlus imp, int var_to_check, int not_defined_val, String warnstr) {
        if (var_to_check != not_defined_val) {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                             "ParticleDensity", "Warning:\n" + warnstr + " already defined.\nOverwrite?");
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
          
    private boolean CheckProfileData(ImagePlus imp) {
        String[] warnstr;
        int i, nwarn = 0;
        
        warnstr = new String[9];
        Calibration c = imp.getCalibration();
        if (c.getUnit() == "" || c.getUnit() == "pixel" || 
            c.getUnit() == "inch" ) {
            warnstr[nwarn++] = "It appears the scale has not been set.";
        }
        if (this.pn == 0) {
            warnstr[nwarn++] = "No particle coordinates defined.";
        }   
        if (!this.boxPlaced) {
            warnstr[nwarn++] = "Box has not been placed.";
        }        
        if (nwarn > 0) {
            for (i = 0; i < nwarn; i++) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(), 
                    "ParticleDensity", "Warning:\n" + warnstr[i] + "\nContinue anyway?");
                if (!d.yesPressed()) {
                    return false;
                }
            }
        }       
        return true;
    }
        
    
    public boolean save(ImagePlus imp) {
        int i, j, p;
		double pixelwidth;
		String unit;
        
        IJ.showStatus("Saving profile...");
        if (!CheckProfileData(imp)) {
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
			String s = IJ.d2s(this.ntot, 0);
            s = IJ.getString("Profile ID: ", s);
            if (s != "") {
                this.ID = s;
            }
            SaveDialog sd = new SaveDialog("Save profile", 
                                           imp.getTitle() + "." + 
                                           IJ.d2s(this.n, 0), ".pd");
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
            outf.println("PROFILE_BORDER");
            for (i = 0; i < 4; i++) {
				outf.println("  " + IJ.d2s(this.box.xpoints[i], 2) +
							 ", " + IJ.d2s(this.box.ypoints[i], 2));
            }
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
        TextRoi profileLabel;
        int locx, locy;
        
        profileLabel = new TextRoi(0, 0, imp);
        for (i=0; i < this.ID.length(); i++) {
            profileLabel.addChar(this.ID.charAt(i));
        }
        locy = this.boundingRect.y - profileLabel.getBounds().height - 5;
        locx = this.boundingRect.x;
        if (locy < 0) {
            locy = this.boundingRect.y + this.boundingRect.height + 5;
            if (locy + profileLabel.getBounds().height 
                 > imp.getHeight()) {
                     locy = this.boundingRect.y;
                locx = this.boundingRect.x - profileLabel.getBounds().width - 5;
                if (locx < 0) {
                   locx = this.boundingRect.x + this.boundingRect.width;
                }                     
            }
        }
        if (locx + profileLabel.getBounds().width > imp.getWidth()) {
            locx = imp.getWidth() - profileLabel.getBounds().width - 5;
        }
        profileLabel.setLocation(locx, locy);
        profileLabel.setFont(profileLabel.getFont(), 24, profileLabel.getStyle());
        if (imp.getType() == ImagePlus.COLOR_RGB) { 
            imp.setColor(profileCol);
        } else { 
            imp.setColor(Color.white);
        }
        profileLabel.drawPixels(); 
        imp.setColor(Color.black);        
    }
    
    
    public void clear(ImagePlus imp) {
        this.dirty = false;        
        this.pn = 0;
        this.boxPlaced = false;        
        this.comment = "";
        this.ID = "";
        Analyzer a = new Analyzer(imp);
        a.getResultsTable().reset();
        a.updateHeadings();
    }
} // end of ParticleDensity
