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
class StackDumpJInternalFrame extends javax.swing.JInternalFrame {

  private static final ThreadLocal<Boolean> IS_UPDATE = ThreadLocal.withInitial(() -> Boolean.FALSE);

  private final StackSampleSupplier samplesSupplier;

  private SampleNode samples;

  private final SpinnerDateModel startModel;

  private final SpinnerDateModel endModel;

  /**
   * Creates new form StackDumpJInternalFrame
   */
  @SuppressForbiden
  StackDumpJInternalFrame(final StackSampleSupplier samplesSupplier,
          final String title, final boolean isgraph) throws IOException {
    super(title);
    this.samplesSupplier = samplesSupplier;
    setName(title);
    Instant min = samplesSupplier.getMin();
    Instant max = samplesSupplier.getMax();
    Date minDate = new Date(min.toEpochMilli());
    Date maxDate = new Date(max.toEpochMilli());
    startModel = new SpinnerDateModel(minDate, minDate, maxDate, Calendar.SECOND);
    endModel = new SpinnerDateModel(maxDate, minDate, maxDate, Calendar.SECOND);
    initComponents();
    ProfileMetaData metaData = samplesSupplier.getMetaData(min, max);
    DefaultComboBoxModel<String> tagsModel = new DefaultComboBoxModel<>(metaData.getTags().toArray(new String[] {}));
    this.tagsSelector.setModel(tagsModel);
    DefaultComboBoxModel<String> contextModel = new DefaultComboBoxModel<>(
            metaData.getContexts().toArray(new String[] {}));
    this.contextSelector.setModel(contextModel);
    this.samples = samplesSupplier.getSamples((String) contextSelector.getSelectedItem(),
              (String) tagsSelector.getSelectedItem(), min, max);
    if (samples == null) {
      this.samples = SampleNode.createSampleNode(
              new StackTraceElement[]{new StackTraceElement("NO SAMPLES", "", "", -1)});
    }
    setViewType(isgraph);
    ssScrollPanel.setVisible(true);
    pack();
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
      Instant start = Instant.ofEpochMilli(((Date) this.startDate.getValue()).getTime());
      Instant end = Instant.ofEpochMilli(((Date) this.endDate.getValue()).getTime());
      ProfileMetaData metaData = samplesSupplier.getMetaData(start, end);
      sync(metaData.getTags(), tagsSelector);
      sync(metaData.getContexts(), contextSelector);
      this.samples = samplesSupplier.getSamples((String) contextSelector.getSelectedItem(),
                (String) tagsSelector.getSelectedItem(), start, end);
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
    samplesVisualizerToolbar = new javax.swing.JToolBar();
    graphToggle = new javax.swing.JToggleButton();
    exportButton = new javax.swing.JButton();
    tagsSelector = new javax.swing.JComboBox<>();
    contextSelector = new javax.swing.JComboBox<>();
    startDate = new javax.swing.JSpinner();
    endDate = new javax.swing.JSpinner();

    setClosable(true);
    setIconifiable(true);
    setMaximizable(true);
    setResizable(true);

    ssScrollPanel.setAutoscrolls(true);

    samplesVisualizerToolbar.setRollover(true);

    graphToggle.setText("graph");
    graphToggle.setFocusable(false);
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

    tagsSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    tagsSelector.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        tagsSelectorItemStateChanged(evt);
      }
    });
    tagsSelector.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        tagsSelectorActionPerformed(evt);
      }
    });
    samplesVisualizerToolbar.add(tagsSelector);

    contextSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    contextSelector.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(java.awt.event.ItemEvent evt) {
        contextSelectorItemStateChanged(evt);
      }
    });
    contextSelector.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        contextSelectorActionPerformed(evt);
      }
    });
    samplesVisualizerToolbar.add(contextSelector);

    startDate.setModel(this.startModel);
    startDate.setEditor(new javax.swing.JSpinner.DateEditor(startDate, "yyyy-MM-dd HH:mm:ss"));
    startDate.setMinimumSize(new java.awt.Dimension(200, 28));
    startDate.setName(""); // NOI18N
    startDate.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        startDateStateChanged(evt);
      }
    });
    samplesVisualizerToolbar.add(startDate);

    endDate.setModel(this.endModel);
    endDate.setEditor(new javax.swing.JSpinner.DateEditor(endDate, "yyyy-MM-dd HH:mm:ss"));
    endDate.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        endDateStateChanged(evt);
      }
    });
    samplesVisualizerToolbar.add(endDate);

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(ssScrollPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 602, Short.MAX_VALUE)
      .add(samplesVisualizerToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
        .add(samplesVisualizerToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(ssScrollPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void graphToggleItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_graphToggleItemStateChanged
    setViewType(evt.getStateChange() == ItemEvent.SELECTED);
  }//GEN-LAST:event_graphToggleItemStateChanged

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

  private void tagsSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagsSelectorActionPerformed

    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_tagsSelectorActionPerformed

  private void contextSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contextSelectorActionPerformed

    try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_contextSelectorActionPerformed

  private void tagsSelectorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_tagsSelectorItemStateChanged

  }//GEN-LAST:event_tagsSelectorItemStateChanged

  private void contextSelectorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_contextSelectorItemStateChanged

  }//GEN-LAST:event_contextSelectorItemStateChanged

  private void startDateStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_startDateStateChanged
   try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_startDateStateChanged

  private void endDateStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_endDateStateChanged
   try {
      update();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }//GEN-LAST:event_endDateStateChanged

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JComboBox<String> contextSelector;
  private javax.swing.JSpinner endDate;
  private javax.swing.JButton exportButton;
  private javax.swing.JToggleButton graphToggle;
  private javax.swing.JToolBar samplesVisualizerToolbar;
  private javax.swing.JScrollPane ssScrollPanel;
  private javax.swing.JSpinner startDate;
  private javax.swing.JComboBox<String> tagsSelector;
  // End of variables declaration//GEN-END:variables
}
