/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * RacetrackFrame.java
 *
 * Created on 16.06.2010, 14:46:19
 */

package ch.unizh.ini.jaer.projects.virtualslotcar;

import java.awt.geom.Point2D;

import com.jogamp.opengl.GLCapabilities;

import com.jogamp.opengl.util.FPSAnimator;

/**
 * Frame for displaying the OpenGL display of the race
 * @author Michael Pfeiffer
 */
public class RacetrackFrame extends javax.swing.JFrame {

	// OpenGL canvas to draw on
	RaceDisplay trackDisplay;

	// The race track for the race
	SlotcarTrack raceTrack;

	// The car and driver object
	Slotcar myCar;

	// Race mode: running or stopped
	boolean raceMode;

	// Controller thread
	Thread controller;

	// Animator thread
	FPSAnimator fps;


	/** Creates new form RacetrackFrame */
	public RacetrackFrame() {
		raceTrack = null;
		myCar = null;
		initComponents();
		createOpenGLCanvas();
		raceMode = false;
		controller = null;
		fps = null;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		GoButton = new javax.swing.JButton();
		trackPanel = new javax.swing.JPanel();
		drawCurve = new javax.swing.JCheckBox();
		ThrottleSlider = new javax.swing.JSlider();
		jLabel1 = new javax.swing.JLabel();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Slotcar Racetrack");
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt) {
				formComponentResized(evt);
			}
		});

		GoButton.setText("Go!");
		GoButton.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				GoButtonMouseClicked(evt);
			}
		});

		trackPanel.setPreferredSize(new java.awt.Dimension(450, 450));

		javax.swing.GroupLayout trackPanelLayout = new javax.swing.GroupLayout(trackPanel);
		trackPanel.setLayout(trackPanelLayout);
		trackPanelLayout.setHorizontalGroup(
			trackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGap(0, 488, Short.MAX_VALUE)
			);
		trackPanelLayout.setVerticalGroup(
			trackPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGap(0, 453, Short.MAX_VALUE)
			);

		drawCurve.setText("Draw Curvature");
		drawCurve.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				drawCurveActionPerformed(evt);
			}
		});

		ThrottleSlider.setToolTipText("Speed of the car");
		ThrottleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
			@Override
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				ThrottleSliderStateChanged(evt);
			}
		});

		jLabel1.setText("Throttle");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(trackPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
						.addComponent(GoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(drawCurve, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addComponent(ThrottleSlider, 0, 0, Short.MAX_VALUE))
						.addContainerGap())
			);
		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(trackPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
					.addGroup(layout.createSequentialGroup()
						.addComponent(drawCurve)
						.addGap(21, 21, 21)
						.addComponent(jLabel1)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(ThrottleSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 318, Short.MAX_VALUE)
						.addComponent(GoButton)))
						.addContainerGap())
			);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
		// Resize OpenGL window

		System.out.println("Resizing: " + this.getSize());
		System.out.println("Resizing: " + trackPanel.getSize());
		trackDisplay.setSize(trackPanel.getSize());
	}//GEN-LAST:event_formComponentResized

	private void GoButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_GoButtonMouseClicked
		if (raceMode == false) {
			// start the race
			raceTrack.initCarState();
			myCar = new Slotcar(raceTrack);
			myCar.setDriveCar(true);
			myCar.setThrottleValue((float) ThrottleSlider.getValue() / (float) ThrottleSlider.getMaximum());
			myCar.setDrawCircle(drawCurve.isSelected());

			trackDisplay.setCar(myCar);

			// Start car controller
			// controller = new Thread(myCar);
			// controller.start();

			fps = new FPSAnimator(trackDisplay,60);
			fps.start();
			raceMode = true;

			// Change button text
			GoButton.setText("Stop");
		}
		else {
			// Stop controller and animation
			myCar.setDriveCar(false);
			fps.stop();

			GoButton.setText("Go!");

			raceMode = false;
		}
	}//GEN-LAST:event_GoButtonMouseClicked

	private void ThrottleSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ThrottleSliderStateChanged
		// TODO add your handling code here:
		double newThrottle = (double) ThrottleSlider.getValue() / (double) ThrottleSlider.getMaximum();
		if (myCar != null) {
			myCar.setThrottleValue((float) newThrottle);
		}

	}//GEN-LAST:event_ThrottleSliderStateChanged

	private void drawCurveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawCurveActionPerformed
		if (myCar != null) {
			myCar.setDrawCircle(drawCurve.isSelected());
		}
	}//GEN-LAST:event_drawCurveActionPerformed

	private void createOpenGLCanvas() {
		if (trackDisplay != null)
		{
			trackPanel.remove(trackDisplay);
		}
		else {
			// design capabilities of opengl canvas
			GLCapabilities caps = new GLCapabilities(null);
			caps.setDoubleBuffered(true);
			caps.setHardwareAccelerated(true);
			caps.setAlphaBits(8);
			caps.setRedBits(8);
			caps.setGreenBits(8);
			caps.setBlueBits(8);

			trackDisplay = new RaceDisplay(caps);
			trackPanel.add(trackDisplay);
		}
	}

	/**
	 * Sets a new track for the race.
	 * @param newTrack The new race track
	 */
	public void setTrack(SlotcarTrack newTrack, float stepsize) {
		raceTrack = newTrack;
		trackDisplay.setTrack(raceTrack, stepsize);
	}

	public void setCar(Slotcar newCar) {
		myCar = newCar;
		myCar.setTrack(raceTrack);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				SlotcarTrack rT = new SlotcarTrack();
				rT.addPoint(new Point2D.Float(0, 0));
				rT.addPoint(new Point2D.Float(1, 0));
				rT.addPoint(new Point2D.Float(0.5f, -0.5f));
				rT.addPoint(new Point2D.Float(-0.5f, 0.25f));
				rT.updateTrack();

				RacetrackFrame rf = new RacetrackFrame();
				rf.setVisible(true);

				rf.setTrack(rT, 0.01f);


			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton GoButton;
	private javax.swing.JSlider ThrottleSlider;
	private javax.swing.JCheckBox drawCurve;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JPanel trackPanel;
	// End of variables declaration//GEN-END:variables

}