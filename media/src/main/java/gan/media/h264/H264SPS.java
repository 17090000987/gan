package gan.media.h264;

import java.util.Objects;

/**
 * 没有定义完，用不上，需要用的在下边添加
 */
public class H264SPS implements Cloneable{
    public int level_idc;
    public int seq_parameter_set_id;
    public int seq_scaling_matrix_present_flag;
    public int log2_max_frame_num_minus4;
    public int num_ref_frames;
    public int gaps_in_frame_num_value_allowed_flag;
    public int pic_width_in_mbs_minus1;
    public int pic_height_in_map_units_minus1;

    @Override
    public int hashCode() {
        return Objects.hash(level_idc
                ,seq_scaling_matrix_present_flag
                ,log2_max_frame_num_minus4
                ,num_ref_frames
                ,gaps_in_frame_num_value_allowed_flag
                ,pic_width_in_mbs_minus1
                ,pic_height_in_map_units_minus1);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof H264SPS){
            H264SPS hSps = (H264SPS)obj;
            if(super.equals(obj)){
                return true;
            }else{
                return hSps.level_idc == level_idc
                        &&hSps.seq_parameter_set_id==seq_parameter_set_id
                        &&hSps.seq_scaling_matrix_present_flag == seq_scaling_matrix_present_flag
                        &&hSps.log2_max_frame_num_minus4 == log2_max_frame_num_minus4
                        &&hSps.num_ref_frames == num_ref_frames
                        &&hSps.gaps_in_frame_num_value_allowed_flag == gaps_in_frame_num_value_allowed_flag
                        &&hSps.pic_width_in_mbs_minus1==pic_width_in_mbs_minus1
                        &&hSps.pic_height_in_map_units_minus1 == pic_height_in_map_units_minus1;
            }
        }
        return super.equals(obj);
    }

    @Override
    public H264SPS clone(){
        H264SPS h264SPS = new H264SPS();
        h264SPS.copy(this);
        return h264SPS;
    }

    public void copy(H264SPS h264SPS){
        level_idc = h264SPS.level_idc;
        seq_parameter_set_id = h264SPS.seq_parameter_set_id;
        seq_scaling_matrix_present_flag = h264SPS.seq_scaling_matrix_present_flag;
        log2_max_frame_num_minus4 = h264SPS.log2_max_frame_num_minus4;
        num_ref_frames = h264SPS.num_ref_frames;
        gaps_in_frame_num_value_allowed_flag = h264SPS.gaps_in_frame_num_value_allowed_flag;
        pic_width_in_mbs_minus1 = h264SPS.pic_width_in_mbs_minus1;
        pic_height_in_map_units_minus1 = h264SPS.pic_height_in_map_units_minus1;
    }

}
