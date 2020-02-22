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

import com.google.common.primitives.Ints;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.spf4j.base.Pair;
import org.spf4j.base.SuppressForbiden;
import gnu.trove.set.hash.THashSet;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.spf4j.avro.csv.CsvEncoder;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvWriter;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.ms.tsdb.AvroMeasurementStoreReader;
import org.spf4j.tsdb2.Charts2;
import org.spf4j.tsdb2.avro.Observation;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL", "SE_BAD_FIELD"})
public class AvroTSViewJInternalFrame extends javax.swing.JInternalFrame {

  private static final long serialVersionUID = 1L;

  private final File tsDb;

  private final AvroMeasurementStoreReader reader;

  /**
   * Creates new form TSDBViewJInternalFrame
   */
  @SuppressForbiden
  public AvroTSViewJInternalFrame(final File tsDb) throws IOException {
    super(tsDb.getPath());
    String fileName = tsDb.getName();
    setName(fileName);
    this.tsDb = tsDb;
    initComponents();
    if (fileName.endsWith(".tabledef.avro")) {
      reader = new AvroMeasurementStoreReader(tsDb.toPath());
    } else {
      throw new IllegalArgumentException("Not a tabledef.avrro file " + tsDb);
    }
    Collection<Schema> measurements = reader.getMeasurements((x) -> true);
    long startDateMillis = System.currentTimeMillis();
    try (AvroCloseableIterable<Observation> scan = reader.getObservations()) {
      for (Observation obs : scan) {
        long ts = obs.getRelTimeStamp();
        if (ts < startDateMillis) {
          startDateMillis = ts;
        }
      }
    }

    Map<String, DefaultMutableTreeNode> gNodes = new HashMap<>();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(fileName);
    for (Schema info : measurements) {
      String groupName = info.getName();
      Pair<String, String> pair = Pair.from(groupName);
      if (pair == null) {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(info);
        root.add(child);
      } else {
        groupName = pair.getFirst();
        DefaultMutableTreeNode gNode = gNodes.get(groupName);
        if (gNode == null) {
          gNode = new DefaultMutableTreeNode(groupName);
          gNodes.put(groupName, gNode);
          root.add(gNode);
        }
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(info);
        gNode.add(child);
      }
    }
    measurementTree.setModel(new DefaultTreeModel(root));
    measurementTree.setCellRenderer(new TreeCellRenderer());
    measurementTree.setVisible(true);
    this.startDate.setValue(new Date(startDateMillis));
  }

  private static class TreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public Component getTreeCellRendererComponent(final JTree tree, final Object node,
            final boolean selected, final boolean expanded, final boolean leaf,
            final int row, final boolean hasFocus) {
      Object value = ((DefaultMutableTreeNode) node).getUserObject();
      if (value instanceof String) {
        this.setText((String) value);
      } else if (value instanceof Schema) {
        this.setText(((Schema) value).getName());
      } else {
        throw new IllegalStateException("Unsupported object type " + value.getClass());
      }
      return this;
    }
  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
   * content of this method is always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    rightPanel = new javax.swing.JPanel();
    mainSplitPannel = new javax.swing.JSplitPane();
    jPanel2 = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    measurementTree = new javax.swing.JTree();
    chartPannel = new javax.swing.JScrollPane();
    jToolBar1 = new javax.swing.JToolBar();
    plotButton = new javax.swing.JButton();
    exportButton = new javax.swing.JButton();
    aggregationMillis = new javax.swing.JTextField();
    startDate = new javax.swing.JSpinner();
    endDate = new javax.swing.JSpinner();

    org.jdesktop.layout.GroupLayout rightPanelLayout = new org.jdesktop.layout.GroupLayout(rightPanel);
    rightPanel.setLayout(rightPanelLayout);
    rightPanelLayout.setHorizontalGroup(
      rightPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 448, Short.MAX_VALUE)
    );
    rightPanelLayout.setVerticalGroup(
      rightPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(0, 306, Short.MAX_VALUE)
    );

    setClosable(true);
    setMaximizable(true);
    setResizable(true);

    mainSplitPannel.setDividerSize(5);
    mainSplitPannel.setPreferredSize(new java.awt.Dimension(600, 500));

    measurementTree.setAutoscrolls(true);
    jScrollPane1.setViewportView(measurementTree);

    org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
      jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
    );
    jPanel2Layout.setVerticalGroup(
      jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
    );

    mainSplitPannel.setLeftComponent(jPanel2);
    mainSplitPannel.setRightComponent(chartPannel);

    jToolBar1.setFloatable(false);
    jToolBar1.setRollover(true);

    plotButton.setText("Plot");
    plotButton.setFocusable(false);
    plotButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    plotButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    plotButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        plotButtonActionPerformed(evt);
      }
    });
    jToolBar1.add(plotButton);

    exportButton.setText("Export");
    exportButton.setFocusable(false);
    exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    exportButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exportButtonActionPerformed(evt);
      }
    });
    jToolBar1.add(exportButton);

    aggregationMillis.setText("0");
    aggregationMillis.setMinimumSize(new java.awt.Dimension(80, 24));
    aggregationMillis.setPreferredSize(new java.awt.Dimension(80, 24));
    aggregationMillis.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        aggregationMillisActionPerformed(evt);
      }
    });
    jToolBar1.add(aggregationMillis);

    startDate.setModel(new javax.swing.SpinnerDateModel());
    startDate.setEditor(new javax.swing.JSpinner.DateEditor(startDate, "yyyy-MM-dd HH:mm:ss"));
    startDate.setMinimumSize(new java.awt.Dimension(200, 28));
    startDate.setName(""); // NOI18N
    jToolBar1.add(startDate);

    endDate.setModel(new javax.swing.SpinnerDateModel());
    endDate.setEditor(new javax.swing.JSpinner.DateEditor(endDate, "yyyy-MM-dd HH:mm:ss"));
    jToolBar1.add(endDate);

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
        .addContainerGap()
        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
          .add(layout.createSequentialGroup()
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 627, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(0, 0, Short.MAX_VALUE))
          .add(mainSplitPannel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 695, Short.MAX_VALUE))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
      .add(layout.createSequentialGroup()
        .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
        .add(mainSplitPannel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  @SuppressFBWarnings("UP_UNUSED_PARAMETER")
    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed
      TreePath[] selectionPaths = measurementTree.getSelectionPaths();
      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
      chartPannel.setViewportView(content);
      try {
        Set<Schema> selectedTables = getSelectedTables(selectionPaths);
        for (Schema tableName : selectedTables) {
          addChartToPanel(tableName, content);
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      chartPannel.repaint();
    }//GEN-LAST:event_plotButtonActionPerformed

  @SuppressFBWarnings("UP_UNUSED_PARAMETER")
    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
      TreePath[] selectionPaths = measurementTree.getSelectionPaths();
      Set<Schema> selectedTables = getSelectedTables(selectionPaths);
      if (!selectedTables.isEmpty()) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          for (Schema metric : selectedTables) {
            try (AvroCloseableIterable<TimeSeriesRecord> obs = reader.getMeasurementData(metric,
                    Instant.EPOCH, Instant.now())) {
              CsvWriter writer = Csv.CSV.writer(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8));
              CsvEncoder encoder = new CsvEncoder(writer, Schema.createArray(metric));
              encoder.writeHeader();
              GenericDatumWriter dw = new GenericDatumWriter(metric);
              for (TimeSeriesRecord o : obs) {
                dw.write(o, encoder);
              }
              encoder.flush();
            } catch (IOException ex) {
              throw new UncheckedIOException(ex);
            }
          }
        }
      }
    }//GEN-LAST:event_exportButtonActionPerformed

  @SuppressFBWarnings("UP_UNUSED_PARAMETER")
  private void aggregationMillisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggregationMillisActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_aggregationMillisActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField aggregationMillis;
  private javax.swing.JScrollPane chartPannel;
  private javax.swing.JSpinner endDate;
  private javax.swing.JButton exportButton;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JToolBar jToolBar1;
  private javax.swing.JSplitPane mainSplitPannel;
  private javax.swing.JTree measurementTree;
  private javax.swing.JButton plotButton;
  private javax.swing.JPanel rightPanel;
  private javax.swing.JSpinner startDate;
  // End of variables declaration//GEN-END:variables

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  private static Set<Schema> getSelectedTables(@Nullable final TreePath[] selectionPaths) {
    if (selectionPaths == null) {
      return Collections.EMPTY_SET;
    }
    Set<Schema> result = new THashSet<>();
    for (TreePath path : selectionPaths) {
      Object[] pathArr = path.getPath();
      if (pathArr.length < 2) {
        continue;
      }
      DefaultMutableTreeNode colNode = (DefaultMutableTreeNode) pathArr[1];
      int depth = colNode.getDepth();
      if (depth == 0) {
        result.add((Schema) colNode.getUserObject());
      } else {
        Enumeration childEnum = colNode.children();
        while (childEnum.hasMoreElements()) {
          DefaultMutableTreeNode child = (DefaultMutableTreeNode) childEnum.nextElement();
          result.add((Schema) child.getUserObject());
        }
      }
    }
    return result;
  }

  private void addChartToPanel(final Schema table, final JPanel content) throws IOException {
    long startTime = ((Date) startDate.getValue()).getTime();
    long endTime = ((Date) endDate.getValue()).getTime();
    Integer aggMillis = Ints.tryParse(aggregationMillis.getText());
    if (aggMillis == null) {
      aggMillis = 0;
    }
    if (Charts2.canGenerateHeatChart(table)) {
      JFreeChart chart = Charts2.createHeatJFreeChart(reader, table,
              startTime, endTime, aggMillis);
      ChartPanel pannel = new ChartPanel(chart);
      pannel.setPreferredSize(new Dimension(600, 800));
      pannel.setDomainZoomable(false);
      pannel.setMouseZoomable(false);
      pannel.setRangeZoomable(false);
      pannel.setZoomAroundAnchor(false);
      pannel.setZoomInFactor(1);
      pannel.setZoomOutFactor(1);
      content.add(pannel);
    }
    if (Charts2.canGenerateMinMaxAvgCount(table)) {
      JFreeChart chart = Charts2.createMinMaxAvgJFreeChart(reader, table,
              startTime, endTime, aggMillis);
      ChartPanel pannel = new ChartPanel(chart);
      pannel.setPreferredSize(new Dimension(600, 600));
      content.add(pannel);

    }
    if (Charts2.canGenerateCount(table)) {
      JFreeChart chart = Charts2.createCountJFreeChart(reader, table,
              startTime, endTime, aggMillis);
      ChartPanel pannel = new ChartPanel(chart);
      pannel.setPreferredSize(new Dimension(600, 600));
      content.add(pannel);
    } else {
      List<JFreeChart> createJFreeCharts = Charts2.createJFreeCharts(reader, table, startTime, endTime, aggMillis);
      for (JFreeChart chart : createJFreeCharts) {
        ChartPanel pannel = new ChartPanel(chart);
        pannel.setPreferredSize(new Dimension(600, 600));
        content.add(pannel);
      }
    }
  }
}
