/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.ui;
//CHECKSTYLE:OFF

import org.spf4j.stackmonitor.StackSampleSupplier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileFilter;
import org.spf4j.base.Methods;
import org.spf4j.base.SuppressForbiden;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.ProfileMetaData;
import org.spf4j.stackmonitor.SampleNode;

/**
 * will need to add some standard filtering:
 *
 * Pair.of(sun.misc.Unsafe.class.getName(), "park")); Pair.of(java.lang.Object.class.getName(), "wait"));
 * Pair.of(java.lang.Thread.class.getName(), "sleep")); Pair.of("java.net.PlainSocketImpl", "socketAccept"));
 * Pair.of("java.net.PlainSocketImpl", "socketConnect"));
 *
 * @author zoly
 */
@SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL", "SE_BAD_FIELD", "UP_UNUSED_PARAMETER"})
class ComparisonStackDumpJInternalFrame extends javax.swing.JInternalFrame {

  private static final ThreadLocal<Boolean> IS_UPDATE = ThreadLocal.withInitial(() -> Boolean.FALSE);

  private final StackSampleSupplier samplesSupplierA;

  private final StackSampleSupplier samplesSupplierB;

  private SampleNode samples;

  private final IntervalModels aSpinners;

  private final IntervalModels bSpinners;

  private static final class IntervalModels {
    private final SpinnerDateModel start;

    private final SpinnerDateModel end;

    public IntervalModels(final SpinnerDateModel start, final SpinnerDateModel end) {
      this.start = start;
      this.end = end;
    }

  }

  @SuppressForbiden
  private static IntervalModels createIntervaModels(final StackSampleSupplier samplesSupplier) throws IOException {
    Instant min = samplesSupplier.getMin();
    Instant max = samplesSupplier.getMax();
    long startMillis = min.toEpochMilli();
    Date minDate = new Date(startMillis - startMillis % 1000);
    long endMillis = max.toEpochMilli();
    Date maxDate = new Date(endMillis - endMillis % 1000 + 1000);
    return new IntervalModels(new SpinnerDateModel(minDate, minDate, maxDate, Calendar.SECOND),
             new SpinnerDateModel(maxDate, minDate, maxDate, Calendar.SECOND));
  }

  /**
   * Creates new form StackDumpJInternalFrame
   */
  ComparisonStackDumpJInternalFrame(final StackSampleSupplier samplesSupplierA,
          final StackSampleSupplier samplesSupplierB,
          final String title, final boolean isgraph) throws IOException {
    super(title);
    this.samplesSupplierA = samplesSupplierA;
    this.samplesSupplierB = samplesSupplierB;
    setName(title);
    // A components:
    this.aSpinners = createIntervaModels(samplesSupplierA);
    this.bSpinners = createIntervaModels(samplesSupplierB);

    initComponents();
    ProfileMetaData metaDataA = samplesSupplierA.getMetaData(aSpinners.start.getDate().toInstant(),
            aSpinners.end.getDate().toInstant());
    DefaultComboBoxModel<String> tagsModelA = new DefaultComboBoxModel<>(metaDataA.getTags().toArray(new String[] {}));
    this.tagsSelectorA.setModel(tagsModelA);
    DefaultComboBoxModel<String> contextModelA = new DefaultComboBoxModel<>(
            metaDataA.getContexts().toArray(new String[] {}));
    this.contextSelectorA.setModel(contextModelA);

    ProfileMetaData metaDataB = samplesSupplierB.getMetaData(bSpinners.start.getDate().toInstant(),
            bSpinners.end.getDate().toInstant());
    DefaultComboBoxModel<String> tagsModelB = new DefaultComboBoxModel<>(metaDataB.getTags().toArray(new String[] {}));
    this.tagsSelectorB.setModel(tagsModelB);
    DefaultComboBoxModel<String> contextModelB = new DefaultComboBoxModel<>(
            metaDataB.getContexts().toArray(new String[] {}));
    this.contextSelectorB.setModel(contextModelB);

    updateSampleNode();
    setViewType(isgraph);
    ssScrollPanel.setVisible(true);
    pack();
  }

  private void updateSampleNode() throws IOException {
    SampleNode sampleNodeA = samplesSupplierA.getSamples(
            (String) contextSelectorA.getSelectedItem(), (String) tagsSelectorA.getSelectedItem(),
            aSpinners.start.getDate().toInstant(), aSpinners.end.getDate().toInstant());
    SampleNode sampleNodeB = samplesSupplierB.getSamples(
            (String) contextSelectorB.getSelectedItem(), (String) tagsSelectorB.getSelectedItem(),
            bSpinners.start.getDate().toInstant(), bSpinners.end.getDate().toInstant());
    this.samples = SampleNode.diffAnnotate(Methods.ROOT, sampleNodeA, sampleNodeB);
    if (samples == null) {
      this.samples = SampleNode.createSampleNode(
              new StackTraceElement[]{new StackTraceElement("NO SAMPLES", "", "", -1)});
    }
  }

  @Override
  public final void pack() {
    super.pack();
  }


  private static void sync(final Collection<String> newValues, final JComboBox<String> combo) {
    Set<String> newvals = new HashSet<>(newValues);
    int l = combo.getItemCount();
    List<Integer> toRemove = new ArrayList<>(4);
    for (int i = 0; i < l; i++) {
      String item = combo.getItemAt(i);
      if (!newvals.remove(item)) {
        toRemove.add(i);
      }
    }
    for (Integer remove : toRemove) {
      combo.removeItemAt(remove);
    }
    for (String newval : newvals) {
      combo.addItem(newval);
    }
  }

  private void update() throws IOException {
    if (IS_UPDATE.get()) {
      return;
    }
    IS_UPDATE.set(Boolean.TRUE);
    try {
      Instant startA = Instant.ofEpochMilli(((Date) this.startDateA.getValue()).getTime());
      Instant endA = Instant.ofEpochMilli(((Date) this.endDateA.getValue()).getTime());
      Instant startB = Instant.ofEpochMilli(((Date) this.startDateB.getValue()).getTime());
      Instant endB = Instant.ofEpochMilli(((Date) this.endDateB.getValue()).getTime());
      ProfileMetaData metaDataA = samplesSupplierA.getMetaData(startA, endA);
      sync(metaDataA.getTags(), tagsSelectorA);
      sync(metaDataA.getContexts(), contextSelectorA);
      ProfileMetaData metaDataB = samplesSupplierB.getMetaData(startB, endB);
      sync(metaDataB.getTags(), tagsSelectorB);
      sync(metaDataB.getContexts(), contextSelectorB);

      this.samples =SampleNode.diffAnnotate(Methods.ROOT,
              samplesSupplierA.getSamples((String) contextSelectorA.getSelectedItem(),
                (String) tagsSelectorA.getSelectedItem(), startA, endA),
              samplesSupplierB.getSamples((String) contextSelectorB.getSelectedItem(),
                (String) tagsSelectorB.getSelectedItem(), startB, endB)
              );
      if (this.samples == null) {
        this.samples = SampleNode.createSampleNode(
                new StackTraceElement[]{new StackTraceElement("NO SAMPLES", "", "", -1)});
      }
      resetViewType(graphToggle.isSelected());
    } finally {
      IS_UPDATE.set(Boolean.FALSE);
    }
  }

  private void resetViewType(final boolean isgraph) {
    if (isgraph) {
      ssScrollPanel.setViewportView(new HotFlameStackPanel(Methods.ROOT, this.samples, new LinkedList<>()));
    } else {
      ssScrollPanel.setViewportView(new FlameStackPanel(Methods.ROOT, this.samples, new LinkedList<>()));
    }
  }


  private void setViewType(final boolean isgraph) {
    StackPanelBase view = (StackPanelBase) ssScrollPanel.getViewport().getView();
    if (isgraph) {
      graphToggle.setSelected(true);
      //ssScrollPanel.setViewportView(new ZStackPanel(this.samples));
      if (view != null) {
        ssScrollPanel.setViewportView(new HotFlameStackPanel(view.getMethod(), view.getSamples(), view.getHistory()));
      } else {
        ssScrollPanel.setViewportView(new HotFlameStackPanel(Methods.ROOT, this.samples, new LinkedList<>()));
      }
    } else {
      graphToggle.setSelected(false);
      if (view != null) {
        ssScrollPanel.setViewportView(new FlameStackPanel(view.getMethod(), view.getSamples(), view.getHistory()));
      } else {
        ssScrollPanel.setViewportView(new FlameStackPanel(Methods.ROOT, this.samples, new LinkedList<>()));
      }
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
   * content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  @SuppressFBWarnings
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    ssScrollPanel = new javax.swing.JScrollPane();
    jPanel1 = new javax.swing.JPanel();
    jToolBar2 = new javax.swing.JToolBar();
    labelA = new javax.swing.JLabel();
    tagsSelectorA = new javax.swing.JComboBox<>();
    contextSelectorA = new javax.swing.JComboBox<>();
    startDateA = new javax.swing.JSpinner();
    endDateA = new javax.swing.JSpinner();
    jToolBar3 = new javax.swing.JToolBar();
    labelB = new javax.swing.JLabel();
    tagsSelectorB = new javax.swing.JComboBox<>();
    contextSelectorB = new javax.swing.JComboBox<>();
    startDateB = new javax.swing.JSpinner();
    endDateB = new javax.swing.JSpinner();
    samplesVisualizerToolbar = new javax.swing.JToolBar();
    graphToggle = new javax.swing.JToggleButton();
    exportButton = new javax.swing.JButton();

    setClosable(true);
    setIconifiable(true);
    setMaximizable(true);
    setResizable(true);

    ssScrollPanel.setAutoscrolls(true);

    jToolBar2.setRollover(true);

    labelA.setText("A:");
    jToolBar2.add(labelA);

    tagsSelectorA.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    tagsSelectorA.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        tagsSelectorAItemStateChanged(evt);
      }
    });
    tagsSelectorA.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        tagsSelectorAActionPerformed(evt);
      }
    });
    jToolBar2.add(tagsSelectorA);

    contextSelectorA.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    contextSelectorA.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        contextSelectorAItemStateChanged(evt);
      }
    });
    contextSelectorA.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        contextSelectorAActionPerformed(evt);
      }
    });
    jToolBar2.add(contextSelectorA);

    startDateA.setModel(this.aSpinners.start);
    startDateA.setMinimumSize(new java.awt.Dimension(200, 28));
    startDateA.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        startDateAStateChanged(evt);
      }
    });
    jToolBar2.add(startDateA);

    endDateA.setModel(this.aSpinners.end);
    endDateA.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        endDateAStateChanged(evt);
      }
    });
    jToolBar2.add(endDateA);

    jToolBar3.setRollover(true);

    labelB.setText("B:");
    jToolBar3.add(labelB);

    tagsSelectorB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    tagsSelectorB.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        tagsSelectorBItemStateChanged(evt);
      }
    });
    tagsSelectorB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        tagsSelectorBActionPerformed(evt);
      }
    });
    jToolBar3.add(tagsSelectorB);

    contextSelectorB.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    contextSelectorB.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        contextSelectorBItemStateChanged(evt);
      }
    });
    contextSelectorB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        contextSelectorBActionPerformed(evt);
      }
    });
    jToolBar3.add(contextSelectorB);

    startDateB.setModel(this.bSpinners.start);
    startDateB.setMinimumSize(new java.awt.Dimension(200, 28));
    startDateB.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        startDateBStateChanged(evt);
      }
    });
    jToolBar3.add(startDateB);

    endDateB.setModel(this.bSpinners.end);
    endDateB.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        endDateBStateChanged(evt);
      }
    });
    jToolBar3.add(endDateB);

    samplesVisualizerToolbar.setRollover(true);

    graphToggle.setText("graph");
    graphToggle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    graphToggle.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    graphToggle.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        graphToggleItemStateChanged(evt);
      }
    });
    samplesVisualizerToolbar.add(graphToggle);

    exportButton.setText("export");
    exportButton.setFocusable(false);
    exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    exportButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exportButtonActionPerformed(evt);
      }
    });
    samplesVisualizerToolbar.add(exportButton);

    org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(jToolBar2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .add(jToolBar3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(samplesVisualizerToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 108, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(jPanel1Layout.createSequentialGroup()
        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(jToolBar2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
          .add(samplesVisualizerToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(jToolBar3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
    );

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(ssScrollPanel)
      .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(ssScrollPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  @SuppressFBWarnings({ "PATH_TRAVERSAL_IN", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "UP_UNUSED_PARAMETER" })
  // this is a local app, FB sees problems with Try -> {} genereted code......
  private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
    JFileChooser fc = new JFileChooser();
    FileFilter[] xFF = fc.getChoosableFileFilters();
    for (FileFilter ff : xFF) {
      fc.removeChoosableFileFilter(ff);
    }
    fc.addChoosableFileFilter(Spf4jFileFilter.D3_JSON);
    fc.addChoosableFileFilter(Spf4jFileFilter.SPF4J_JSON);
    fc.addChoosableFileFilter(Spf4jFileFilter.SSDUMP2);
    fc.addChoosableFileFilter(Spf4jFileFilter.SSDUMP2_GZ);
    fc.setFileFilter(Spf4jFileFilter.D3_JSON);
    int retrival = fc.showSaveDialog(null);
    if (retrival == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fc.getSelectedFile();
      FileFilter fileFilter = fc.getFileFilter();
      if (!(fileFilter instanceof Spf4jFileFilter)) {
        throw new IllegalArgumentException("Invalid file type selected " + fileFilter);
      }
      String suffix = ((Spf4jFileFilter) fileFilter).getSuffix();
      if (!selectedFile.getName().endsWith(suffix)) {
        selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + suffix);
      }
      if (Spf4jFileFilter.D3_JSON.accept(selectedFile)) { // D3 format.
        try (BufferedWriter wr = Files.newBufferedWriter(selectedFile.toPath(), StandardCharsets.UTF_8)) {
          samples.writeD3JsonTo(wr);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      } else if (Spf4jFileFilter.SPF4J_JSON.accept(selectedFile)) {
        try (BufferedWriter wr = Files.newBufferedWriter(selectedFile.toPath(), StandardCharsets.UTF_8)) {
          samples.writeJsonTo(wr);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      } else if (Spf4jFileFilter.SSDUMP2.accept(selectedFile) || Spf4jFileFilter.SSDUMP2_GZ.accept(selectedFile)) {
        try {
          Converter.save(selectedFile, samples);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      } else {
        throw new IllegalArgumentException("No ecognized extension for file " + selectedFile);
      }
    }
  }//GEN-LAST:event_exportButtonActionPerformed

  private void graphToggleItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_graphToggleItemStateChanged
    setViewType(evt.getStateChange() == ItemEvent.SELECTED);
  }//GEN-LAST:event_graphToggleItemStateChanged

  private void endDateBStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_endDateBStateChanged
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_endDateBStateChanged

  private void startDateBStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_startDateBStateChanged
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_startDateBStateChanged

  private void contextSelectorBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contextSelectorBActionPerformed
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_contextSelectorBActionPerformed

  private void contextSelectorBItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_contextSelectorBItemStateChanged
    // TODO add your handling code here:
  }//GEN-LAST:event_contextSelectorBItemStateChanged

  private void tagsSelectorBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagsSelectorBActionPerformed
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_tagsSelectorBActionPerformed

  private void tagsSelectorBItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_tagsSelectorBItemStateChanged
    // TODO add your handling code here:
  }//GEN-LAST:event_tagsSelectorBItemStateChanged

  private void endDateAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_endDateAStateChanged
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_endDateAStateChanged

  private void startDateAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_startDateAStateChanged
    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_startDateAStateChanged

  private void contextSelectorAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contextSelectorAActionPerformed

    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_contextSelectorAActionPerformed

  private void contextSelectorAItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_contextSelectorAItemStateChanged

  }//GEN-LAST:event_contextSelectorAItemStateChanged

  private void tagsSelectorAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagsSelectorAActionPerformed

    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_tagsSelectorAActionPerformed

  private void tagsSelectorAItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_tagsSelectorAItemStateChanged

  }//GEN-LAST:event_tagsSelectorAItemStateChanged

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JComboBox<String> contextSelectorA;
  private javax.swing.JComboBox<String> contextSelectorB;
  private javax.swing.JSpinner endDateA;
  private javax.swing.JSpinner endDateB;
  private javax.swing.JButton exportButton;
  private javax.swing.JToggleButton graphToggle;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JToolBar jToolBar2;
  private javax.swing.JToolBar jToolBar3;
  private javax.swing.JLabel labelA;
  private javax.swing.JLabel labelB;
  private javax.swing.JToolBar samplesVisualizerToolbar;
  private javax.swing.JScrollPane ssScrollPanel;
  private javax.swing.JSpinner startDateA;
  private javax.swing.JSpinner startDateB;
  private javax.swing.JComboBox<String> tagsSelectorA;
  private javax.swing.JComboBox<String> tagsSelectorB;
  // End of variables declaration//GEN-END:variables
}
