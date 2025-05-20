package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * wodi 用户日志实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class WodiUserLog {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键")
    private Integer id;

    @Schema(name = "tgId")
    private Long telegramId;

    @Schema(name = "积分")
    private Integer fraction;

    @Schema(name = "Dmail")
    private Integer iv;

    @Schema(name = "飞升奖励")
    private Integer tiv;

    @Schema(name = "是否胜利")
    private Boolean isVictory;

    @Schema(name = "赛季")
    private Integer season;

    @Schema(name = "比赛时间")
    private Date createTime;
}