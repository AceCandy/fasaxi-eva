package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * x 邀请实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class XInvite {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键")
    private Long id;

    @Schema(name = "邀请链接")
    private String url;

    @Schema(name = "邀请人")
    private Long inviterId;

    @Schema(name = "被邀请人")
    private Long inviteeId;

    @Schema(name = "创建时间")
    private Date createTime;

    @Schema(name = "加入时间")
    private Date joinTime;

    @Schema(name = "领取时间")
    private Date collectTime;
}