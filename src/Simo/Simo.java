/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Simo;

/**
 *
 * @author Linghui Luo
 */
public class Simo extends javax.swing.JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = -8189133802326536564L;

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting
		// code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.
		 * html
		 */
		try {
			for (final javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (final ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(Simo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (final InstantiationException ex) {
			java.util.logging.Logger.getLogger(Simo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (final IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(Simo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (final javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(Simo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Simo().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jButton_multiMode;

	private javax.swing.JButton jButton_singleMode;
	// End of variables declaration//GEN-END:variables

	/**
	 * Creates new form Simo
	 */
	public Simo() {
		this.initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		this.jButton_singleMode = new javax.swing.JButton();
		this.jButton_multiMode = new javax.swing.JButton();

		this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		this.setTitle("Simulator");
		this.setBackground(java.awt.SystemColor.textHighlight);
		this.setResizable(false);

		this.jButton_singleMode.setBackground(javax.swing.UIManager.getDefaults().getColor("textHighlight"));
		this.jButton_singleMode.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
		this.jButton_singleMode.setText("Single-Test-Mode");
		this.jButton_singleMode.setActionCommand("Single-Test-Mode");
		this.jButton_singleMode.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				Simo.this.jButton_singleModeActionPerformed(evt);
			}
		});

		this.jButton_multiMode.setBackground(javax.swing.UIManager.getDefaults().getColor("textHighlight"));
		this.jButton_multiMode.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
		this.jButton_multiMode.setText("Multi-Tests-Mode");
		this.jButton_multiMode.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				Simo.this.jButton_multiModeActionPerformed(evt);
			}
		});

		final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup()
				.addGap(74, 74, 74)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(this.jButton_multiMode, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(this.jButton_singleMode, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addContainerGap(72, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addGap(65, 65, 65)
						.addComponent(this.jButton_singleMode, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18)
						.addComponent(this.jButton_multiMode, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(71, Short.MAX_VALUE)));

		this.pack();
		this.setLocationRelativeTo(null);
	}// </editor-fold>//GEN-END:initComponents

	private void jButton_multiModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton_multiModeActionPerformed
		this.setVisible(false);
		new MultiModeFrame(this, true).setVisible(true);
	}// GEN-LAST:event_jButton_multiModeActionPerformed

	private void jButton_singleModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton_singleModeActionPerformed
		this.setVisible(false);
		new SingleModeFrame(this, true).setVisible(true);
	}// GEN-LAST:event_jButton_singleModeActionPerformed
}
