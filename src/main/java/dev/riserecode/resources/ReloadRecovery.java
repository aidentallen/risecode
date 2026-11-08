package dev.riserecode.resources;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** Client-thread resource recovery state machine, independent of Minecraft and presentation. */
public final class ReloadRecovery {
    public record Outcome(boolean restored, Throwable original, Throwable recovery) { }
    private enum Phase { PRIMARY, RECOVERING, TERMINAL }
    private final Executor client;
    private final Supplier<CompletionStage<Void>> restoreAndReload;
    private final java.util.function.Consumer<Outcome> terminal;
    private Phase phase=Phase.PRIMARY;
    private Throwable original;
    public ReloadRecovery(Executor client,Supplier<CompletionStage<Void>> restoreAndReload,java.util.function.Consumer<Outcome> terminal) {
        this.client=client;this.restoreAndReload=restoreAndReload;this.terminal=terminal;
    }
    public boolean recovering(){return phase==Phase.RECOVERING;}
    public boolean fail(Throwable failure) {
        if(phase==Phase.TERMINAL){return false;}
        if(phase==Phase.RECOVERING){if(failure==original){return true;}finish(failure);return false;}
        original=failure;phase=Phase.RECOVERING;
        client.execute(()->{
            if(phase!=Phase.RECOVERING){return;}
            try{restoreAndReload.get().whenComplete((ignored,error)->client.execute(()->finish(error)));}
            catch(RuntimeException synchronous){finish(synchronous);}
        });
        return true;
    }
    private void finish(Throwable recovery) {
        if(phase!=Phase.RECOVERING){return;}
        phase=Phase.TERMINAL;terminal.accept(new Outcome(recovery==null,original,recovery));
    }
    public void cancel(){phase=Phase.TERMINAL;}
}
