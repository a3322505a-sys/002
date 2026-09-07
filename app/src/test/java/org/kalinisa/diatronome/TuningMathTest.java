package org.kalinisa.diatronome;
import org.junit.Test;
import org.kalinisa.diatronome.Tools.TuningMath;
import static org.junit.Assert.*;
public class TuningMathTest {
    @Test public void sixStringsAndSignedOffsets(){
        double[] hz={329.627557,246.941651,195.997718,146.832384,110,82.406889};
        for(int i=0;i<6;i++){
            assertEquals(hz[i],TuningMath.frequency(i),0.00001);
            assertEquals(0,TuningMath.cents(hz[i],i),0.001);
            assertEquals(25,TuningMath.cents(hz[i]*Math.pow(2,25/1200.0),i),0.001);
            assertEquals("调低",TuningMath.direction(25));assertEquals("调高",TuningMath.direction(-25));
        }
    }
    @Test public void silenceIsNeverInTune(){
        assertTrue(Double.isNaN(TuningMath.cents(0,5)));assertTrue(Double.isNaN(TuningMath.cents(Double.POSITIVE_INFINITY,5)));
        assertEquals("拨动琴弦",TuningMath.direction(Double.NaN));assertEquals("已调准",TuningMath.direction(5));assertEquals("调低",TuningMath.direction(5.01));
    }
}
