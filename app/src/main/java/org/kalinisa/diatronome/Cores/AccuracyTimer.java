package org.kalinisa.diatronome.Cores;

/** Monotonic deadlines. Slow callbacks skip expired deadlines instead of bursting. */
public class AccuracyTimer extends Thread {
    public interface AccuracyTimerTask extends Runnable { void interrupt(); }
    private final Object guard=new Object();
    private volatile boolean cancelled;
    private volatile long periodNanos;
    private AccuracyTimerTask task;
    private long deadline;
    public AccuracyTimer(){super("TuneBeat-clock");}
    public void updatePeriodMs(long periodMs){if(periodMs<=0)throw new IllegalArgumentException();periodNanos=periodMs*1000000L;}
    public void scheduleAtFixedRate(AccuracyTimerTask next,long delayMs,long periodMs){
        synchronized(guard){
            if(cancelled)throw new IllegalStateException("Timer stopped");
            task=next;updatePeriodMs(periodMs);deadline=System.nanoTime()+Math.max(0,delayMs)*1000000L;
            if(getState()==State.NEW)start();guard.notifyAll();
        }
    }
    @Override public void run(){
        while(!cancelled){
            AccuracyTimerTask current;
            synchronized(guard){
                while(!cancelled){long wait=deadline-System.nanoTime();if(wait<=0)break;
                    try{guard.wait(wait/1000000L,(int)(wait%1000000L));}catch(InterruptedException e){if(cancelled)return;}
                }
                if(cancelled)return;current=task;
            }
            current.run();
            synchronized(guard){
                deadline+=periodNanos;long now=System.nanoTime();
                if(deadline<now)deadline+=((now-deadline)/periodNanos+1)*periodNanos;
            }
        }
    }
    public void cancel(){
        cancelled=true;AccuracyTimerTask current;
        synchronized(guard){current=task;guard.notifyAll();}
        if(current!=null)current.interrupt();interrupt();
        if(Thread.currentThread()!=this)try{join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
    }
    public void purge(){}
}
