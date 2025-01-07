package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * wodi top棒
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class WodiTop {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键")
    private Integer id;

    @Schema(name = "tgId")
    private Long telegramId;

    @Schema(name = "姓")
    private String firstName;

    @Schema(name = "名")
    private String lastName;

    @Schema(name = "用户名")
    private String userName;

    @Schema(name = "等级")
    private Integer level;

    @Schema(name = "赛季")
    private Integer season;

    @Schema(name = "飞升时间")
    private Date upTime;
}