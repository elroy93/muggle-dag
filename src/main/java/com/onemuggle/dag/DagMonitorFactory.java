package com.onemuggle.dag;

import java.util.List;

@FunctionalInterface
public interface DagMonitorFactory {

    List<DagNodeMonitor> getMonitors();

}
