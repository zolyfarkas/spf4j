
package org.spf4j.perf.tsdb;

public interface TSDataHandler {

    void newTable(String tableName, String[] columnNames);

    void newData(String tableName, TimeSeries data);

}
