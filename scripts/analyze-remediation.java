import java.nio.file.*;
import java.util.*;
import jdk.jfr.consumer.*;

/** java scripts/analyze-remediation.java <scene.jfr>...; weighted allocation samples, not exact counts. */
class AnalyzeRemediation {
    public static void main(String[] args)throws Exception{
        boolean json=Arrays.asList(args).contains("--json");
        for(String arg:args){
            if(arg.equals("--json")){continue;}
            var cpuSites=new HashMap<String,Long>();long executionSamples=0,renderExecutionSamples=0,gcCollections=0,gcPauseNanos=0;
            var path=Path.of(arg);
            // Duration events may start before this recording; allocation weights can include
            // bytes from a previous recording's disabled interval. Drop each thread's first
            // weighted sample and use the recording boundary for steady-state site analysis.
            java.time.Instant recordingStart=null;
            try(var file=new RecordingFile(path)){
                while(file.hasMoreEvents()){
                    var event=file.readEvent();
                    if(event.getEventType().getName().equals("jdk.ActiveRecording")){
                        var at=event.getInstant("recordingStart");
                        if(recordingStart==null||at.isBefore(recordingStart)){recordingStart=at;}
                    }
                }
            }
            var sampledThreads=new HashSet<Long>();
            var types=new HashMap<String,Long>();var sites=new HashMap<String,Long>();long total=0,project=0,renderTotal=0,renderProject=0;java.time.Instant start=null,end=null;
            try(var file=new RecordingFile(path)){
                while(file.hasMoreEvents()){
                    var event=file.readEvent();if(recordingStart!=null&&event.getStartTime().isBefore(recordingStart)){continue;}if(start==null||event.getStartTime().isBefore(start)){start=event.getStartTime();}if(end==null||event.getEndTime().isAfter(end)){end=event.getEndTime();}
                    String eventType=event.getEventType().getName();
                    if(eventType.equals("jdk.GarbageCollection")){gcCollections++;}
                    if(eventType.equals("jdk.GCPhasePause")){gcPauseNanos+=event.getDuration().toNanos();}
                    if(eventType.equals("jdk.ExecutionSample")){
                        executionSamples++;var sampledThread=event.getThread("sampledThread");
                        if(sampledThread!=null&&sampledThread.getJavaName().equals("Render thread")){
                            renderExecutionSamples++;var trace=event.getStackTrace();if(trace!=null){for(var frame:trace.getFrames()){String name=frame.getMethod().getType().getName();if(name.startsWith("dev.riserecode.")&&!name.contains(".testmixin.")&&!name.contains("Test")){cpuSites.merge(name+"."+frame.getMethod().getName(),1L,Long::sum);break;}}}
                        }
                    }
                    if(!eventType.equals("jdk.ObjectAllocationSample")){continue;}
                    var allocationThread=event.getThread();if(allocationThread==null||sampledThreads.add(allocationThread.getJavaThreadId())){continue;}
                    long weight=event.getLong("weight");total+=weight;
                    boolean render=event.getThread()!=null&&event.getThread().getJavaName().equals("Render thread");if(render){renderTotal+=weight;}types.merge(event.getClass("objectClass").getName(),weight,Long::sum);
                    var trace=event.getStackTrace();if(trace==null){continue;}
                    for(var frame:trace.getFrames()){
                        String name=frame.getMethod().getType().getName();
                        if(name.startsWith("dev.riserecode.")&&!name.contains(".testmixin.")&&!name.contains("IntegrationTest")&&!name.contains("RemediationClientTest")&&!name.endsWith("UpgradeBenchmark")){
                            sites.merge(name+"."+frame.getMethod().getName(),weight,Long::sum);project+=weight;if(render){renderProject+=weight;}break;
                        }
                    }
                }
            }
            if(recordingStart!=null){start=recordingStart;}
            double seconds=java.time.Duration.between(start,end).toNanos()/1e9;
            if(json){
                System.out.printf(Locale.ROOT,"{\"file\":\"%s\",\"seconds\":%.6f,\"sampledBytes\":%d,\"projectBytes\":%d,\"renderBytes\":%d,\"renderProjectBytes\":%d}%n",arg.replace("\\","\\\\").replace("\"","\\\""),seconds,total,project,renderTotal,renderProject);continue;
            }
            System.out.printf(Locale.ROOT,"%s duration=%.3fs sampledAllocation=%.2fMiB projectAttributed=%.2fMiB rate=%.2fMiB/s%n",path,seconds,total/1048576.0,project/1048576.0,total/1048576.0/seconds);
            System.out.printf(Locale.ROOT,"Render thread: sampledAllocation=%.2fMiB projectAttributed=%.2fMiB rate=%.2fMiB/s%n",renderTotal/1048576.0,renderProject/1048576.0,renderTotal/1048576.0/seconds);
            System.out.printf(Locale.ROOT,"Execution samples: total=%d render=%d; GC collections=%d pauseMs=%.3f%n",executionSamples,renderExecutionSamples,gcCollections,gcPauseNanos/1e6);
            System.out.println("Top render-thread project execution sample sites:");
            cpuSites.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(10).forEach(e->System.out.printf(Locale.ROOT,"  %d %s%n",e.getValue(),e.getKey()));
            System.out.println("Top project allocation sites (weighted samples):");
            sites.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(10).forEach(e->System.out.printf(Locale.ROOT,"  %.2fMiB %s%n",e.getValue()/1048576.0,e.getKey()));
            System.out.println("Top allocated types (weighted samples):");
            types.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(5).forEach(e->System.out.printf(Locale.ROOT,"  %.2fMiB %s%n",e.getValue()/1048576.0,e.getKey()));
        }
    }
}
