package com.onemuggle.dag;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DagResult {

    private ListenableFuture<Object> future;

    private List<DagNodeMonitor> monitors;
}
