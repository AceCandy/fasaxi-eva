package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * wodi 用户实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class WodiUser {

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

    @Schema(name = "完成游戏次数")
    private Integer completeGame;

    @Schema(name = "平民场次")
    private Integer wordPeople;

    @Schema(name = "卧底场次")
    private Integer wordSpy;

    @Schema(name = "平民胜场")
    private Integer wordPeopleVictory;

    @Schema(name = "卧底胜场")
    private Integer wordSpyVictory;

    @Schema(name = "积分")
    private Integer fraction;

    @Schema(name = "首次添加时间")
    private Date createTime;
}