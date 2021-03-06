/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * CochleaAMS1cRollingCochleagramADCDisplayMethodGainGUI.java
 *
 * Created on Sep 21, 2011, 9:48:47 AM
 */
package ch.unizh.ini.jaer.chip.cochlea;

import java.awt.Color;

import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;

import ch.unizh.ini.jaer.chip.cochlea.CochleaAMS1cRollingCochleagramADCDisplayMethod.DisplayControl;

/**
 *  Gain and offset control for trace plotting for CochleaAMS1c ADC sample traces.
 * 
 * @author tobi
 */
public class CochleaAMS1cRollingCochleagramADCDisplayMethodGainGUI extends javax.swing.JPanel {

    private CochleaAMS1cRollingCochleagramADCDisplayMethod gui;
    Color[] colors = {Color.red, Color.green, Color.blue, Color.yellow};
    ch.unizh.ini.jaer.chip.cochlea.CochleaAMS1cRollingCochleagramADCDisplayMethod.DisplayControl displayControl;
    TitledBorder border;

    /** Creates new form CochleaAMS1cRollingCochleagramADCDisplayMethodGainGUI */
    public CochleaAMS1cRollingCochleagramADCDisplayMethodGainGUI(CochleaAMS1cRollingCochleagramADCDisplayMethod gui, CochleaAMS1cRollingCochleagramADCDisplayMethod.DisplayControl displayControl) {
        this.gui = gui;
        this.displayControl = displayControl;
        initComponents();
        gainSp.setValue(getGain());
        offsetSp.setValue(getOffset());
        hideBut.setSelected(isHidden());
        border=new TitledBorder("");
        border.setTitleColor(CochleaAMS1cRollingCochleagramADCDisplayMethod.colors[displayControl.getChan()]);
        setBorderTitle(displayControl);
        setBorder(border);
    }

    private void setBorderTitle(DisplayControl displayControl) {
        border.setTitle("Chan" + displayControl.getChan() + ": " + displayControl.getName());
        repaint();
    }

    final void setOffset(int offset) {
        displayControl.setOffset(offset);
    }

    final void setGain(int gain) {
        displayControl.setGain(gain);
    }

    final int getOffset() {
        return displayControl.getOffset();
    }

    final int getGain() {
        return displayControl.getGain();
    }

    public void setHidden(boolean yes) {
        displayControl.setHidden(yes);
    }

    public boolean isHidden() {
        return displayControl.isHidden();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gainSp = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        offsetSp = new javax.swing.JSpinner();
        hideBut = new javax.swing.JCheckBox();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Chan"));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        gainSp.setToolTipText("ADC values are multipled by this factor after offset is added");
        gainSp.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                gainSpStateChanged(evt);
            }
        });

        jLabel1.setText("gain");

        jLabel2.setText("offset");

        offsetSp.setToolTipText("ADC samples are offset by this value by adding it, and then multipled by gain value");
        offsetSp.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                offsetSpStateChanged(evt);
            }
        });

        hideBut.setText("Hide");
        hideBut.setToolTipText("Hide this trace");
        hideBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(gainSp)
                    .addComponent(offsetSp, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(hideBut)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(gainSp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hideBut))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(offsetSp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void gainSpStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gainSpStateChanged
        try {
            int i = (Integer) gainSp.getValue();
            setGain(i);
            gainSp.setForeground(Color.black);
        } catch (Exception e) {
            gainSp.setValue(getGain());
            gainSp.setForeground(Color.red);
        }
    }//GEN-LAST:event_gainSpStateChanged

    private void offsetSpStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_offsetSpStateChanged
        try {
            int i = (Integer) offsetSp.getValue();
            setOffset(i);
            offsetSp.setForeground(Color.black);
        } catch (Exception e) {
            offsetSp.setValue(getOffset());
            offsetSp.setForeground(Color.red);
        }
    }//GEN-LAST:event_offsetSpStateChanged

    private void hideButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideButActionPerformed
        setHidden(hideBut.isSelected());        // TODO add your handling code here:
    }//GEN-LAST:event_hideButActionPerformed

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        String name = JOptionPane.showInputDialog("Name of trace?",displayControl.getName());
        if(name==null) name="";
        if(name.length()>12) name=name.substring(0,11);
        displayControl.setName(name);
        setBorderTitle(displayControl);
    }//GEN-LAST:event_formMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner gainSp;
    private javax.swing.JCheckBox hideBut;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSpinner offsetSp;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the gui
     */
    public CochleaAMS1cRollingCochleagramADCDisplayMethod getGui() {
        return gui;
    }

    /**
     * @param gui the gui to set
     */
    public void setGui(CochleaAMS1cRollingCochleagramADCDisplayMethod gui) {
        this.gui = gui;
    }
}
