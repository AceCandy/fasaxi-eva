package cn.acecandy.fasaxi.eva.sql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 卧底 群组实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class WodiGroup {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键")
    private Integer id;

    @Schema(name = "群组id")
    private Long groupId;

    @Schema(name = "首次添加时间")
    private Date firstJoinTime;

    @Schema(name = "最近游戏时间")
    private Date joinTime;

    @Schema(name = "完成游戏次数")
    private Integer finishGame;

    @Schema(name = "最多同时娱乐人数")
    private Integer maxOfPeople;

    @Schema(name = "群组用户名")
    private String userName;

    @Schema(name = "群组名")
    private String title;
}