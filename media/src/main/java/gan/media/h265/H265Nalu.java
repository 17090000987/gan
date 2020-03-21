package gan.media.h265;

public class H265Nalu {
    public static final int SLICE_TRAIL_N = 0;
    public static final int SLICE_TRAIL_R = 1;
    public static final int SLICE_TSA_N = 2;
    public static final int SLICE_TLA = 3;
    public static final int SLICE_STSA_N = 4;
    public static final int SLICE_STSA_R = 5;
    public static final int SLICE_RADL_N = 6;
    public static final int SLICE_DLP = 7;
    public static final int SLICE_RASL_N = 8;
    public static final int SLICE_TFD = 9;

    public static final int SLICE_BLA = 16;
    public static final int SLICE_BLANT = 17;
    public static final int SLICE_BLA_N_LP = 18;
    public static final int SLICE_IDR = 19;
    public static final int SLICE_IDR_N_LP = 20;
    public static final int SLICE_CRA = 21;

    public static final int VPS = 32;
    public static final int SPS = 33;
    public static final int PPS = 34;
    public static final int ACCESS_UNIT_DELIMITER = 35;
    public static final int EOS = 36;
    public static final int EOB = 37;
    public static final int FILLER_DATA =38;
    public static final int SEI = 39;
    public static final int SEI_SUFFIX = 40;
}
