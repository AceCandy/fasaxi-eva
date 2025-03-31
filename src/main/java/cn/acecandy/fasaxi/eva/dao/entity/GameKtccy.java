package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 看图猜成语 实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class GameKtccy {

    @TableId
    @Schema(name = "主键 id")
    private Long id;

    @Schema(name = "图片链接")
    private String picUrl;

    @Schema(name = "文件绝对路径")
    private String fileUrl;

    @Schema(name = "答案")
    private String answer;

    @Schema(name = "提示")
    private String tips;

    @Schema(name = "来源 1-遇见 2-慕名 3-领酷 4-繁华落寞 5-问情/桑帛云")
    private Integer source;

    @Schema(name = "使用次数")
    private Integer playTime;
}