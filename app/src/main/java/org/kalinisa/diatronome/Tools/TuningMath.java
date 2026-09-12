package org.kalinisa.diatronome.Tools;
public final class TuningMath {
    // User-facing order: first/thinnest string at the top, sixth/thickest at bottom.
    public static final String[] NOTES={"E₄","B₃","G₃","D₃","A₂","E₂"};
    public static final String[] LETTERS={"E","B","G","D","A","E"};
    private static final int[] MIDI={64,59,55,50,45,40};
    public static double frequency(int string){return 440*Math.pow(2,(MIDI[string]-69)/12.0);}
    public static double cents(double hz,int string){return hz>0&&!Double.isNaN(hz)&&!Double.isInfinite(hz)?1200*Math.log(hz/frequency(string))/Math.log(2):Double.NaN;}
    public static String direction(double cents){return Double.isNaN(cents)?"拨动琴弦":Math.abs(cents)<=5?"已调准":cents>0?"调低":"调高";}
    public static int nearestString(double hz){
        int best=0;for(int i=1;i<6;i++)if(Math.abs(cents(hz,i))<Math.abs(cents(hz,best)))best=i;return best;
    }
    public static String measuredNote(double hz){
        if(!(hz>0)||!(!Double.isNaN(hz)&&!Double.isInfinite(hz)))return "—";
        int midi=(int)Math.round(69+12*Math.log(hz/440)/Math.log(2));
        String[] names={"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};
        return names[((midi%12+12)%12)]+(((int)Math.floor(midi/12.0))-1);
    }
    private TuningMath(){}
}
